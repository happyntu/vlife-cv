package com.vlife.cv.interest.strategy

import com.vlife.common.util.DateUtils
import com.vlife.common.util.MathUtils
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateConstants.AMOUNT_SCALE
import com.vlife.cv.interest.InterestRateConstants.RATE_SCALE
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
 * 貸款利率計算策略(日加權明細）
 *
 * V3 對應：cv210p_f20_select_loan (pk_cv_cv210p.pck lines 948-1107)
 *
 * **計算方式**：
 * 1. 逐月查詢 QIRAT 利率（int_rate_type='2'）
 * 2. 計算各月天數（月底調整至 31 日）
 * 3. 逐月計算利息：int_amt += principal × (rate/10000) × (days/yearDays)（未捨入）
 * 4. **累計後一次 ROUND**：intAmt = ROUND(累計未捨入利息, precision)
 * 5. 日加權平均利率：actualRate = Σ(各月利率 × 各月天數) / 總天數
 *
 * **捨入時機差異**（CV210P-R-004）：
 * - **LoanRateStrategy (f20)**：累計後一次 ROUND（本實作）
 * - fa0 (投資型貸款)：逐月 ROUND（未在本實作中，屬 rate_calc_A）
 * - rate_calc_C (存款類)：逐月 ROUND（DepositRateStrategy）
 *
 * **rate_type 差異**：
 * - rate_type '2'：標準貸款利率
 * - rate_type '3'：貸款利率變體（計算邏輯相同）
 *
 * @see InterestCalcRateStrategy 計息利率（日加權 + 立即計算利息）
 * @see LastMonthRateStrategy 最後月利率
 */
@Component
class LoanRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(
        RateType.LOAN_RATE_MONTHLY,
        RateType.LOAN_RATE_MONTHLY_V2
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
        if (input.beginDate > input.endDate) {
            // V3 lines 976-981: beginDate > endDate 時回傳零值
            logger.debug { "Invalid input: beginDate > endDate" }
            return InterestRateCalculationResult.zero()
        }

        return calculateLoanRate(input, precision)
    }

    /**
     * 貸款利率與利息計算
     *
     * V3 對應：cv210p_f20_select_loan (lines 948-1107)
     *
     * **計算邏輯**：
     * 1. 計算年天數（考慮閏年）
     * 2. 計算月數（begin_date 至 end_date）
     * 3. 逐月迭代：
     *    - 確定該月的起迄日（月底調整至 31 日，或至 end_date）
     *    - 計算該月天數（非最後月 +1）
     *    - 查詢該月利率（QIRAT type='2'）
     *    - 累加未捨入利息：intAmtAccum += principal × (rate/10000) × (days/yearDays)
     *    - 累加 totalRate（利率 × 天數）
     *    - 累加 totalDays
     * 4. **累計後一次 ROUND**：intAmt = ROUND(intAmtAccum, precision)
     * 5. actualRate = totalRate / totalDays（日加權平均）
     *
     * **月底調整邏輯**（V3 lines 1000-1008）：
     * - 使用 pk_sub_chkdaydd.check_day_dd(p_date1, '31', p_date2)
     * - 將日期調整至該月 31 日（如該月無 31 日，調整至月底）
     * - 非最後月時，days += 1（V3 lines 1006-1008）
     *
     * **捨入時機**（V3 lines 1070-1072, 1094）：
     * - 逐月累加**未捨入**利息（與 fa0 不同）
     * - 最終一次 ROUND(intAmtAccum, p_num)
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（含 actualRate, intAmt, monthlyDetails）
     */
    private fun calculateLoanRate(
        input: InterestRateInput,
        precision: Int
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 1: 計算年天數（閏年考慮）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算月數
        // var 使用理由：根據 V3 邏輯調整（+1 包含結束月份）
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        months += 1  // V3 line 990: 月數 +1

        // Step 3: 逐月迭代累計利率、天數與利息
        // var 使用理由：迴圈中累加值與日期遞增（遵循 V3 f20 迭代邏輯）
        var totalRate = BigDecimal.ZERO
        var totalDays = 0
        var intAmtAccum = BigDecimal.ZERO  // 累計未捨入利息
        var currentDate = beginDate

        val monthlyDetails = mutableListOf<MonthlyRateDetail>()

        for (i in 1..months) {
            // 計算該月的結束日（月底 31 日或 end_date）
            // V3 lines 1000-1003: pk_sub_chkdaydd.check_day_dd(p_date1, '31', p_date2)
            // var 使用理由：根據邊界條件調整（超過 end_date 時截斷）
            var monthEndDate = DateUtils.withDay(currentDate, 31) ?: currentDate  // 調整至 31 日（或月底）
            if (monthEndDate > endDate) {
                monthEndDate = endDate
            }

            // 計算該月天數
            // var 使用理由：非最後月時需 +1（V3 lines 1006-1008）
            var days = interestCalcHelper.calculateDays(currentDate, monthEndDate)
            if (i != months) {
                // 非最後月時，days += 1（V3 lines 1006-1008）
                days += 1
            }

            // 調整至月初（用於 QIRAT 查詢）
            val rateDate = interestCalcHelper.toMonthStart(currentDate)

            // 查詢該月利率
            val (originalRate, adjustedRate) = if (input.actualRate.compareTo(BigDecimal.ZERO) != 0) {
                // 如果已知利率，使用已知利率（V3 lines 1015-1025）
                val rate = input.actualRate
                val adjustedRate = if (input.rateSub.compareTo(BigDecimal.ZERO) != 0 || input.rateDisc.compareTo(BigDecimal("100")) != 0) {
                    qiratRateLookup.applyDiscounts(rate, input.rateSub, input.rateDisc)
                } else {
                    rate
                }
                rate to adjustedRate
            } else {
                // 否則查詢 QIRAT（int_rate_type='2'）
                // V3 lines 1049-1053: cv210p_sel_qirat(p_rate_date, '2', ...)
                val rateLookup = qiratRateLookup.lookupRate(input, "2", rateDate)
                rateLookup.originalRate to rateLookup.adjustedRate
            }

            // 檢查負利率（V3 lines 1056-1058）
            val finalRate = if (adjustedRate < BigDecimal.ZERO) {
                BigDecimal.ZERO
            } else {
                adjustedRate
            }

            // 計算該月利息（未捨入）
            // V3 lines 1070-1072: int_amt = principal × (rate/10000 / yearDays × days)
            val monthlyInterest = input.principalAmt
                .multiply(finalRate.divide(BigDecimal("10000"), AMOUNT_SCALE, RoundingMode.HALF_UP))
                .multiply(BigDecimal(days).divide(BigDecimal(yearDays), AMOUNT_SCALE, RoundingMode.HALF_UP))

            // 累加未捨入利息
            intAmtAccum = intAmtAccum.add(monthlyInterest)

            // 累加日加權利率與天數
            totalRate = totalRate.add(finalRate.multiply(BigDecimal(days)))
            totalDays += days

            // 記錄月份明細
            monthlyDetails.add(
                MonthlyRateDetail(
                    strDate = currentDate,
                    endDate = monthEndDate,
                    month = interestCalcHelper.formatMonth(rateDate),
                    days = days,
                    iRateOriginal = originalRate.divide(BigDecimal("10000"), RATE_SCALE, RoundingMode.HALF_UP),
                    iRate = finalRate.divide(BigDecimal("10000"), RATE_SCALE, RoundingMode.HALF_UP),
                    intAmt = monthlyInterest,  // 記錄未捨入利息
                    principalAmt = input.principalAmt
                )
            )

            // 下個月（調整至月初）
            currentDate = DateUtils.addMonths(beginDate, i) ?: beginDate  // 安全處理 nullable
            currentDate = interestCalcHelper.toMonthStart(currentDate)
        }

        // Step 4: 累計後一次 ROUND（V3 line 1094）
        val finalIntAmt = intAmtAccum.setScale(precision, RoundingMode.HALF_UP)

        // Step 5: 計算日加權平均利率
        val averageRate = if (totalDays > 0) {
            // V3 lines 1103-1104: calc_round(totalRate / totalDays)
            MathUtils.calcRound(totalRate.divide(BigDecimal(totalDays), RATE_SCALE, RoundingMode.HALF_UP))
        } else {
            BigDecimal.ZERO
        }

        logger.debug {
            "Loan rate calculated: months=$months, totalDays=$totalDays, yearDays=$yearDays, " +
                "avgRate=$averageRate, intAmt=$finalIntAmt (accum=$intAmtAccum)"
        }

        return InterestRateCalculationResult(
            actualRate = averageRate,
            intAmt = finalIntAmt,
            monthlyDetails = monthlyDetails
        )
    }
}
