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
 * 最後月利率計算策略
 *
 * V3 對應：cv210p_f30_select_loan_2 (pk_cv_cv210p.pck lines 1117-1220)
 *
 * **計算方式**：
 * 1. 計算年天數（考慮閏年）
 * 2. 計算總天數（begin_date 至 end_date）
 * 3. 查詢最後月利率（使用 end_date 的月份）
 * 4. 計算利息：intAmt = ROUND(principal × (actualRate/10000) × (days/yearDays), precision)
 *
 * **與 LoanRateStrategy 差異**：
 * - LoanRateStrategy：逐月查詢利率，日加權平均
 * - LastMonthRateStrategy：僅查詢最後月利率，直接計算利息
 *
 * **適用場景**：
 * - rate_type '4'：貸款利率（最後月）
 * - 投資型保單（insurance_type_3 IN ['F','G','H']）的 rate_type '4' 使用 rate_calc_A（fa0/fb0）
 * - 傳統型保單使用本 Strategy（f30）
 *
 * @see LoanRateStrategy 貸款利率（逐月日加權）
 */
@Component
class LastMonthRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(RateType.LOAN_RATE_LAST_MONTH)

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
        if (input.beginDate > input.endDate) {
            // V3 lines 1134-1139: beginDate > endDate 時回傳零值
            logger.debug { "Invalid input: beginDate > endDate" }
            return InterestRateCalculationResult.zero()
        }

        return calculateLastMonthRate(input, precision)
    }

    /**
     * 最後月利率計算
     *
     * V3 對應：cv210p_f30_select_loan_2 (lines 1117-1220)
     *
     * **計算邏輯**：
     * 1. 計算年天數（考慮閏年）
     * 2. 計算總天數（begin_date 至 end_date）
     * 3. 調整 end_date 至月初（用於 QIRAT 查詢）
     * 4. 查詢該月利率（QIRAT type='2'）
     * 5. 計算利息：intAmt = ROUND(principal × (actualRate/10000) × (days/yearDays), precision)
     *
     * **已知利率處理**（V3 lines 1148-1155）：
     * - 如果 input.actualRate != 0，直接使用已知利率計算利息
     * - 否則查詢 QIRAT
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（含 actualRate 和 intAmt）
     */
    private fun calculateLastMonthRate(
        input: InterestRateInput,
        precision: Int
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 1: 計算年天數（閏年考慮）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算總天數
        val totalDays = interestCalcHelper.calculateDays(beginDate, endDate)

        // Step 3: 調整 end_date 至月初（用於 QIRAT 查詢）
        // V3 lines 1145-1146: p_date2 := to_date(to_char(end_date, 'yyyy/mm') || '/01', 'yyyy/mm/dd')
        val lastMonthDate = interestCalcHelper.toMonthStart(endDate)

        // Step 4: 查詢該月利率或使用已知利率
        val actualRate = if (input.actualRate.compareTo(BigDecimal.ZERO) != 0) {
            // 如果已知利率，直接使用（V3 lines 1148-1155）
            input.actualRate
        } else {
            // 否則查詢 QIRAT（int_rate_type='2'）
            // V3 lines 1179-1205: 查詢 QIRAT，邊界處理（MIN/MAX 日期）
            val rateLookup = qiratRateLookup.lookupRate(input, "2", lastMonthDate)
            rateLookup.adjustedRate  // 已套用減碼/折扣
        }

        // Step 5: 計算利息
        // V3 lines 1152-1154 (已知利率) / lines 1216-1218 (查詢利率)
        // intAmt = ROUND(principal × (actualRate/10000 / yearDays × days), p_num)
        val intAmt = if (input.principalAmt.compareTo(BigDecimal.ZERO) != 0 && totalDays > 0) {
            input.principalAmt
                .multiply(actualRate.divide(BigDecimal("10000"), 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal(totalDays).divide(BigDecimal(yearDays), 10, RoundingMode.HALF_UP))
                .setScale(precision, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        logger.debug {
            "Last month rate calculated: totalDays=$totalDays, yearDays=$yearDays, " +
                "actualRate=$actualRate, intAmt=$intAmt"
        }

        return InterestRateCalculationResult(
            actualRate = actualRate,
            intAmt = intAmt,
            monthlyDetails = emptyList()  // 最後月利率不回傳月份明細
        )
    }
}
