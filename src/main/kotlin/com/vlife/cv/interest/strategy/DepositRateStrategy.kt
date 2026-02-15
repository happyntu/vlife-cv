package com.vlife.cv.interest.strategy

import com.vlife.common.util.DateUtils
import com.vlife.common.util.MathUtils
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateConstants
import com.vlife.cv.interest.InterestRateInput
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
 * 現金價值利率計算策略（存款類日加權）
 *
 * V3 對應：
 * - cv210p_rate_calc_C (pk_cv_cv210p.pck lines 1510-1664)
 * - cv210p_rate_calc_G 委派（pk_cv_cv210p.pck lines 1954-1958）
 *
 * **雙重路由（CV210P-R-001）**：
 * - insurance_type_3 IN ('G', 'H') → 委派 AnnuityRateStrategy（rate_calc_G）
 * - insurance_type_3 = 'F' → 直接計算存款類日加權（rate_calc_C）
 *
 * **計算方式**：
 * 1. 適用於投資型保單之現金價值計算
 * 2. 逐月查詢 QIRAT 利率（int_rate_type='5'）
 * 3. 逐月計算利息：int_amt += principal × (rate/10000) × (days/yearDays)
 * 4. 逐月 ROUND（與 LoanRateStrategy 不同，LoanRateStrategy 是累計後一次 ROUND）
 * 5. actualRate = 日加權平均利率
 *
 * **rate_type 說明**：
 * - rate_type 'C'：存款類現金價值利率（insurance_type_3 NOT IN ['G', 'H']）
 * - rate_type 'C' + insurance_type_3 IN ['G', 'H']：委派 AnnuityRateStrategy
 *
 * **捨入時機差異**（CV210P-R-004）：
 * - **DepositRateStrategy (rate_calc_C)**：逐月 ROUND（本實作）
 * - **LoanRateStrategy (f20)**：累計後一次 ROUND
 * - **投資型貸款 (fa0)**：逐月 ROUND
 *
 * @see LoanRateStrategy 貸款利率（累計後一次 ROUND）
 * @see AnnuityRateStrategy 企業年金利率（複利計算）
 */
@Component
class DepositRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper,
    private val annuityRateStrategy: AnnuityRateStrategy  // 雙重路由所需
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(
        RateType.DEPOSIT_RATE
    )

    override fun calculate(
        input: InterestRateInput,
        precision: Int,
        plan: Pldf?,
        planNote: PlanNote?
    ): InterestRateCalculationResult {
        // V3 雙重路由邏輯（lines 1954-1958）
        // insurance_type_3 IN ('G', 'H') → rate_calc_G（AnnuityRateStrategy）
        val insuranceType3 = plan?.insuranceType3
        if (insuranceType3 in listOf("G", "H")) {
            logger.debug { "DepositRateStrategy: insurance_type_3=$insuranceType3, delegating to AnnuityRateStrategy" }
            return annuityRateStrategy.calculate(input, precision, plan, planNote)
        }

        // insurance_type_3 = 'F' → rate_calc_C（存款類日加權）
        return calculateDepositRate(input, precision)
    }

    /**
     * 存款類日加權利息計算（逐月 ROUND）
     *
     * V3 對應：cv210p_rate_calc_C (lines 1510-1664)
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（actualRate 為日加權平均，intAmt 為累計利息）
     */
    private fun calculateDepositRate(
        input: InterestRateInput,
        precision: Int
    ): InterestRateCalculationResult {
        // 邊界檢查
        if (input.beginDate == null || input.endDate == null) {
            logger.debug { "Invalid input: beginDate or endDate is null" }
            return InterestRateCalculationResult.zero()
        }
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!
        if (beginDate >= endDate) {  // V3 lines 1535-1538
            logger.debug { "Invalid input: beginDate >= endDate" }
            return InterestRateCalculationResult.zero()
        }

        // Step 1: 計算年天數（考慮閏年）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算月數
        // var 使用理由：根據邊界條件調整（months <= 0 時 +1）
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        if (months <= 0) {
            months += 1  // V3 lines 1545-1547
        }

        // Step 3: 逐月查詢利率並計算利息（逐月 ROUND）
        // var 使用理由：迴圈中累加利息與日期遞增（遵循 V3 邏輯）
        var totalIntAmt = BigDecimal.ZERO
        var totalRateWeighted = BigDecimal.ZERO
        var totalDays = 0
        var currentDate = beginDate

        val monthlyDetails = mutableListOf<MonthlyRateDetail>()

        for (i in 1..months) {
            // 調整至月初（用於 QIRAT 查詢）
            val monthStartDate = DateUtils.withDay(currentDate, 1) ?: currentDate

            // 下個月的起始日
            val nextMonthDate = DateUtils.addMonths(currentDate, 1) ?: currentDate

            // 計算該月份的結束日（不超過 endDate）
            val monthEndDate = if (nextMonthDate > endDate) endDate else nextMonthDate

            // 計算該月天數
            val days = interestCalcHelper.calculateDays(currentDate, monthEndDate)

            // 查詢該月利率（int_rate_type='5'，投資型/宣告利率，V3 line 1564）
            val rateLookup = qiratRateLookup.lookupRate(input, "5", monthStartDate)
            val originalRate = rateLookup.originalRate
            val adjustedRate = rateLookup.adjustedRate

            // 計算該月利息（逐月 ROUND，V3 lines 1630-1632）
            // 利息 = ROUND(本金 × (利率 / 10000) × (天數 / 年天數), precision)
            val monthlyIntAmt = MathUtils.round(
                input.principalAmt
                    .multiply(adjustedRate)
                    .divide(BigDecimal("10000"), InterestRateConstants.AMOUNT_SCALE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(days))
                    .divide(BigDecimal(yearDays), InterestRateConstants.AMOUNT_SCALE, RoundingMode.HALF_UP),
                precision
            )

            // 累加利息
            totalIntAmt = totalIntAmt.add(monthlyIntAmt)

            // 日加權利率累加
            totalRateWeighted = totalRateWeighted.add(adjustedRate.multiply(BigDecimal(days)))
            totalDays += days

            // 記錄月份明細
            monthlyDetails.add(
                MonthlyRateDetail(
                    strDate = currentDate,
                    endDate = monthEndDate,
                    month = interestCalcHelper.formatMonth(currentDate),
                    days = days,
                    iRateOriginal = originalRate,
                    iRate = adjustedRate,
                    intAmt = monthlyIntAmt,
                    principalAmt = input.principalAmt,
                    description = null
                )
            )

            // 移至下個月
            currentDate = nextMonthDate
        }

        // Step 4: 計算日加權平均利率（V3 lines 1639-1642, 1661-1663）
        val averageRate = if (totalDays > 0) {
            val rawRate = totalRateWeighted.divide(
                BigDecimal(totalDays),
                InterestRateConstants.RATE_SCALE,
                RoundingMode.HALF_UP
            )
            // P1-003: V3 lines 1661-1663 使用 calc_round(p_tot_rate / p_tot_days)
            MathUtils.calcRound(rawRate)
        } else {
            BigDecimal.ZERO
        }

        logger.debug {
            "Deposit rate calculated: int_rate_type=5, " +
                "months=$months, totalDays=$totalDays, avgRate=$averageRate, totalIntAmt=$totalIntAmt"
        }

        return InterestRateCalculationResult(
            actualRate = averageRate,
            intAmt = totalIntAmt,
            monthlyDetails = monthlyDetails
        )
    }
}
