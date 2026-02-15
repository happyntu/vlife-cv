package com.vlife.cv.interest.strategy

import com.vlife.common.util.DateUtils
import com.vlife.common.util.MathUtils
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.InterestRateConstants
import com.vlife.cv.interest.MonthlyRateDetail
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
 * 寬限期利息計算策略（Free-Look 利息）
 *
 * V3 對應：
 * - cv210p_rate_calc_B (pk_cv_cv210p.pck lines 2041-2074)
 * - cv210p_fc0_select_rate (pk_cv_cv210p.pck lines 350-487)
 *
 * **計算方式**：
 * 1. 適用於投資型保單之寬限期利息計算
 * 2. rate_type 'A' / 'B' / 'E' 對應不同的 int_rate_type
 *    - 'A', 'B' → int_rate_type '1'（Free-Look 利率）
 *    - 'E' → int_rate_type '9'（投資收益利率）
 * 3. 各月利息計算：逐月查詢利率，逐月計算並累加利息
 * 4. actualRate = 日加權平均利率
 *
 * **rate_type 說明**：
 * - 'A'：Free-Look 利息（基本）
 * - 'B'：Free-Look 利息（變體，通常用於特定險種）
 * - 'E'：投資收益利率
 *
 * **V3 副作用（CV210P-R-007）**：
 * - fc0_select_rate 會設定 `g_iri.sub_acnt_plan_code := g_ps_plnt_1.free_look_rate_cd`（line 392）
 * - V4 不實作此副作用，因為 InterestRateInput 為 immutable data class
 *
 * **注意事項**：
 * - 僅適用於投資型保單（insurance_type_3 IN ['F', 'G', 'H']）
 * - 非投資型保單遇到此 rate_type 時，V3 會 fallback 至 f20（LoanRateStrategy）
 * - 捨入時機：逐月 ROUND（與 LoanRateStrategy 不同，LoanRateStrategy 是累計後一次 ROUND）
 *
 * @see LoanRateStrategy 貸款利率（累計後一次 ROUND）
 * @see DepositRateStrategy 存款利率（存款類日加權）
 */
@Component
class FreeLookRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(
        RateType.FREE_LOOK_A,
        RateType.FREE_LOOK_B,
        RateType.FREE_LOOK_E
    )

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
        if (input.beginDate >= input.endDate) {  // V3 line 380
            logger.debug { "Invalid input: beginDate >= endDate" }
            return InterestRateCalculationResult.zero()
        }

        // 決定 int_rate_type（V3 fc0_select_rate lines 410-426）
        val intRateType = when (input.rateType) {
            RateType.FREE_LOOK_E -> "9"  // 投資收益利率
            else -> "1"  // Free-Look 利率（rate_type 'A', 'B'）
        }

        // V3 副作用：設定 sub_acnt_plan_code 為 free_look_rate_cd（line 392）
        // V4 處理：使用 planNote 的 freeLookRateCode，若 input 無 sub_acnt_plan_code 則使用 planNote 提供值
        val effectiveSubAcntPlanCode = input.subAcntPlanCode
            ?: planNote?.freeLookRateCode
            ?: run {
                logger.debug { "No sub_acnt_plan_code or freeLookRateCode available" }
                return InterestRateCalculationResult.zero()
            }

        // 建立有效 input（包含 free_look_rate_cd）
        val effectiveInput = input.copy(subAcntPlanCode = effectiveSubAcntPlanCode)

        return calculateFreeLookInterest(effectiveInput, intRateType, precision)
    }

    /**
     * Free-Look 利息計算（逐月計算，日加權）
     *
     * V3 對應：cv210p_fc0_select_rate (lines 350-487)
     *
     * @param input 利率計算輸入
     * @param intRateType QIRAT 利率類型（'1' 或 '9'）
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（actualRate 為日加權平均，intAmt 為累計利息）
     */
    private fun calculateFreeLookInterest(
        input: InterestRateInput,
        intRateType: String,
        precision: Int
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 1: 計算年天數（考慮閏年）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算月數（V3 lines 398-399：無條件 +1）
        // var 使用理由：V3 fc0 無條件 +1
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        months += 1  // P0-005: V3 fc0 line 399 無條件 +1（非有條件）

        // P0-006: V3 fc0 使用固定 begin_date 查詢 QIRAT（lines 426-429）
        val fixedQueryDate = interestCalcHelper.toMonthStart(beginDate)

        // P1-005: 先查詢一次 QIRAT，若查無則 RETURN（V3 line 473）
        val firstRateLookup = qiratRateLookup.lookupRate(input, intRateType, fixedQueryDate)
        if (firstRateLookup.adjustedRate.compareTo(BigDecimal.ZERO) == 0) {
            logger.debug { "QIRAT not found for int_rate_type=$intRateType, date=$fixedQueryDate" }
            return InterestRateCalculationResult.zero()
        }

        // Step 3: 逐月計算天數（使用固定利率，V3 fc0）
        // var 使用理由：迴圈中累加日加權利率與日期遞增
        var totalRateWeighted = BigDecimal.ZERO
        var totalDays = 0
        var currentDate = beginDate

        val monthlyDetails = mutableListOf<MonthlyRateDetail>()

        for (i in 1..months) {
            // 下個月的起始日
            val nextMonthDate = DateUtils.addMonths(currentDate, 1) ?: currentDate

            // 計算該月份的結束日（不超過 endDate）
            val monthEndDate = if (nextMonthDate > endDate) endDate else nextMonthDate

            // 計算該月天數
            val days = interestCalcHelper.calculateDays(currentDate, monthEndDate)

            // P0-006: 使用固定 begin_date 的利率（V3 fc0 lines 426-429）
            // 所有月份都使用相同利率（begin_date 月的利率）
            val originalRate = firstRateLookup.originalRate
            val adjustedRate = firstRateLookup.adjustedRate

            // 日加權利率累加
            totalRateWeighted = totalRateWeighted.add(adjustedRate.multiply(BigDecimal(days)))
            totalDays += days

            // 記錄月份明細（P1-001: 先不計算 intAmt，最後一次計算）
            monthlyDetails.add(
                MonthlyRateDetail(
                    strDate = currentDate,
                    endDate = monthEndDate,
                    month = interestCalcHelper.formatMonth(currentDate),
                    days = days,
                    iRateOriginal = originalRate,
                    iRate = adjustedRate,
                    intAmt = BigDecimal.ZERO,  // 稍後更新
                    principalAmt = input.principalAmt,
                    description = null
                )
            )

            // 移至下個月
            currentDate = nextMonthDate
        }

        // Step 4: 計算日加權平均利率（V3 lines 471-474）
        val averageRate = if (totalDays > 0) {
            totalRateWeighted.divide(
                BigDecimal(totalDays),
                InterestRateConstants.RATE_SCALE,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }

        // P1-001: 最後一次計算 intAmt（V3 lines 482-486）
        // 使用平均利率 × 總天數（而非逐月計算並累加）
        val totalIntAmt = MathUtils.round(
            input.principalAmt
                .multiply(averageRate)
                .divide(BigDecimal("10000"), InterestRateConstants.AMOUNT_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal(totalDays))
                .divide(BigDecimal(yearDays), InterestRateConstants.AMOUNT_SCALE, RoundingMode.HALF_UP),
            precision
        )

        logger.debug {
            "Free-Look interest calculated: int_rate_type=$intRateType, " +
                "months=$months, totalDays=$totalDays, avgRate=$averageRate, totalIntAmt=$totalIntAmt"
        }

        return InterestRateCalculationResult(
            actualRate = averageRate,
            intAmt = totalIntAmt,
            monthlyDetails = monthlyDetails
        )
    }
}
