package com.vlife.cv.interest.strategy

import com.vlife.common.util.DateUtils
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.helper.InterestCalcHelper
import com.vlife.cv.interest.helper.QiratRateLookup
import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PlanNote
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

private val logger = KotlinLogging.logger {}

/**
 * 宣告利率 12 月平均計算策略
 *
 * V3 對應：cv210p_f50_select_avg_rate (pk_cv_cv210p.pck lines 1386-1434)
 *
 * **計算方式**：
 * 1. 從 end_date 往前推 12 個月
 * 2. 逐月查詢 QIRAT 宣告利率（int_rate_type='5'）
 * 3. 計算 12 個月利率的簡單平均
 *
 * **前置條件**（CV210P-R-006）：
 * - **僅適用於利變壽險**：insurance_type_3='I'
 * - V3 lines 2069-2074：非利變壽險時 fallthrough 至 f20（LoanRateStrategy）
 * - V4 由 InterestRateService 判斷前置條件，本 Strategy 僅實作計算邏輯
 *
 * **與 DividendRateStrategy 差異**：
 * - DividendRateStrategy：從 begin_date 開始逐月查詢（QIRAT type='0'）
 * - AvgDeclaredRateStrategy：從 end_date 往前推 12 個月（QIRAT type='5'）
 *
 * @see DividendRateStrategy 分紅利率（月加權平均）
 */
@Component
class AvgDeclaredRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(RateType.AVG_DECLARED_RATE)

    override fun calculate(
        input: InterestRateInput,
        precision: Int,
        plan: Pldf?,
        planNote: PlanNote?
    ): InterestRateCalculationResult {
        // 邊界檢查
        if (input.beginDate == null || input.endDate == null) {
            logger.debug { "Invalid input: beginDate or endDate is null" }
            return InterestRateCalculationResult.zero()
        }

        return calculateAvgDeclaredRate(input)
    }

    /**
     * 宣告利率 12 月平均計算
     *
     * V3 對應：cv210p_f50_select_avg_rate (lines 1386-1434)
     *
     * **計算邏輯**：
     * 1. 調整 end_date 至月初（用於 QIRAT 查詢起點）
     * 2. 逐月往前推 12 個月（迴圈 1..12）
     * 3. 每月查詢 QIRAT type='5'（宣告利率）
     * 4. 累加 12 個月利率
     * 5. actualRate = totalRate / 12（簡單平均）
     *
     * **QIRAT 查詢邊界處理**（V3 lines 1401-1427）：
     * - 優先使用 pk_lib_qiratproc.f99_get_qirat
     * - 查無結果時使用 f99_get_qirat_max（取 <= p_date2 的最大日期）
     * - 仍查無時使用 MIN 日期
     * - 全部失敗時該月利率為 0
     *
     * **⚠️ 特殊處理**（V3 lines 1400-1404）：
     * - pk_sub_submonth.SubtractMonth(p_date2, 1)：往前推一個月
     * - V4 使用 DateUtils.subtractMonths() 或 addMonths(-1)
     *
     * @param input 利率計算輸入
     * @return 計算結果（actualRate 為 12 月平均）
     */
    private fun calculateAvgDeclaredRate(
        input: InterestRateInput
    ): InterestRateCalculationResult {
        val endDate = input.endDate!!

        // Step 1: 調整 end_date 至月初
        // V3 line 1397: p_date2 := pk_ut_date.setdd(end_date, '01')
        var currentDate = interestCalcHelper.toMonthStart(endDate)

        // Step 2: 逐月往前推 12 個月，累加利率
        var totalRate = BigDecimal.ZERO

        for (i in 1..12) {
            // 往前推一個月
            // V3 line 1400: p_date2 := pk_sub_submonth.SubtractMonth(p_date2, 1)
            currentDate = DateUtils.addMonths(currentDate, -1) ?: currentDate

            // 查詢該月宣告利率（QIRAT type='5'）
            // V3 lines 1401-1427: f99_get_qirat → f99_get_qirat_max → MIN 日期
            val monthlyRate = queryDeclaredRate(input, currentDate)

            totalRate = totalRate.add(monthlyRate)

            logger.debug { "Month $i: date=$currentDate, rate=$monthlyRate" }
        }

        // Step 3: 計算 12 個月平均
        // V3 line 1431: p_actual_rate := p_actual_rate / 12
        val averageRate = totalRate.divide(
            BigDecimal("12"),
            10,  // 保留 10 位小數（利率精度）
            RoundingMode.HALF_UP
        )

        logger.debug {
            "Avg declared rate calculated: totalRate=$totalRate, avgRate=$averageRate"
        }

        return InterestRateCalculationResult(
            actualRate = averageRate,
            intAmt = BigDecimal.ZERO,  // f50 僅計算利率，不計算利息
            monthlyDetails = emptyList()  // 不回傳月份明細
        )
    }

    /**
     * 查詢單月宣告利率
     *
     * V3 對應：cv210p_f50_select_avg_rate lines 1401-1427
     *
     * **查詢順序**：
     * 1. pk_lib_qiratproc.f99_get_qirat(sub_acnt_plan_code, '5', p_date2)
     * 2. 查無 → pk_lib_qiratproc.f99_get_qirat_max(sub_acnt_plan_code, '5', p_date2)
     * 3. 仍查無 → 查詢 MIN(int_rate_date_str)，若 p_date2 <= MIN，使用 MIN 日期的利率
     * 4. 全部失敗 → 利率 = 0
     *
     * **⚠️ V4 簡化**：
     * - QiratService.getEffectiveRate() 已內建邊界處理（MAX/MIN 日期）
     * - 無需在 Strategy 層重複實作 f99_get_qirat_max 邏輯
     *
     * @param input 利率計算輸入（含 subAcntPlanCode）
     * @param queryDate 查詢日期
     * @return 該月利率（查無時為 0）
     */
    private fun queryDeclaredRate(
        input: InterestRateInput,
        queryDate: java.time.LocalDate
    ): BigDecimal {
        // 查詢 QIRAT type='5'（宣告利率）
        // V3 lines 1401-1427：嘗試 f99_get_qirat → f99_get_qirat_max → MIN 日期
        // V4：QiratService.getEffectiveRate() 已內建邊界處理
        val rateLookup = qiratRateLookup.lookupRate(input, "5", queryDate)

        // V3 f50 **不套用** rate_sub/rate_disc（直接累加 QIRAT 原始利率）
        // V3 證據：pk_cv_cv210p.pck line 1428
        //   p_actual_rate := p_actual_rate + g_qirat.int_rate
        // 使用的是 QIRAT 原始利率（int_rate），未套用 rate_sub/rate_disc
        //
        // QiratRateLookup.lookupRate() 回傳 originalRate 和 adjustedRate 兩者
        // f50 使用 originalRate（與 f10/f12/f20 使用 adjustedRate 不同）
        return rateLookup.originalRate
    }
}
