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
 * 計息利率計算策略（日加權 + 計算利息金額）
 *
 * V3 對應：cv210p_f12_select_div (pk_cv_cv210p.pck lines 756-864)
 *
 * **計算方式**：
 * 1. 逐月查詢 QIRAT 利率（int_rate_type='0'）
 * 2. 計算各月天數
 * 3. 日加權平均利率：actualRate = Σ(各月利率 × 各月天數) / 總天數
 * 4. **計算利息金額**：intAmt = ROUND(principal × (actualRate/10000) × (totalDays/yearDays), precision)
 *
 * **與 DividendRateStrategy 差異**：
 * - DividendRateStrategy：月加權平均（簡單平均），**不計算利息**
 * - InterestCalcRateStrategy：日加權平均，**計算利息金額**
 *
 * **捨入時機**：
 * - actualRate 計算時不捨入（保留精度）
 * - intAmt 計算時 ROUND(precision)（台幣 0 位，外幣 2 位）
 *
 * @see DividendRateStrategy 分紅利率（月加權，不計息）
 * @see LoanRateStrategy 貸款利率（日加權，逐月累計）
 */
@Component
class InterestCalcRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(RateType.INTEREST_CALC_RATE)

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
        if (input.beginDate >= input.endDate) {
            // V3 lines 780-785: beginDate >= endDate 時回傳零值
            logger.debug { "Invalid input: beginDate >= endDate" }
            return InterestRateCalculationResult.zero()
        }

        return calculateInterestWithRate(input, precision)
    }

    /**
     * 計息利率與利息金額計算
     *
     * V3 對應：cv210p_f12_select_div (lines 756-864)
     *
     * **計算邏輯**：
     * 1. 計算年天數（考慮閏年）
     * 2. 計算月數（begin_date 至 end_date）
     * 3. 逐月迭代：
     *    - 確定該月的起迄日（月初至下月初-1，或至 end_date）
     *    - 計算該月天數
     *    - 查詢該月利率（QIRAT type='0'）
     *    - 累加 totalRate（利率 × 天數）
     *    - 累加 totalDays
     * 4. actualRate = totalRate / totalDays（日加權平均）
     * 5. intAmt = ROUND(principal × (actualRate/10000) × (totalDays/yearDays), precision)
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（含 actualRate 和 intAmt）
     */
    private fun calculateInterestWithRate(
        input: InterestRateInput,
        precision: Int
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 1: 計算年天數（閏年考慮）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算月數
        // var 使用理由：根據 V3 邏輯調整（+1 包含結束月份部分天數）
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        months += 1  // V3 line 794: 月數 +1 以包含結束月份的部分天數

        // Step 3: 逐月迭代累計利率與天數
        // var 使用理由：迴圈中累加值與日期遞增（遵循 V3 f12 迭代邏輯）
        var totalRate = BigDecimal.ZERO
        var totalDays = 0
        var currentDate = beginDate

        val monthlyDetails = mutableListOf<MonthlyRateDetail>()

        for (i in 1..months) {
            // 計算該月的結束日（下個月的第一天）
            // var 使用理由：根據邊界條件調整（超過 end_date 時截斷）
            var lastDate = DateUtils.addMonths(currentDate, 1) ?: currentDate
            lastDate = DateUtils.withDay(lastDate, 1) ?: lastDate  // 調整至月初

            // 如果超過計算結束日，調整為 endDate
            if (lastDate >= endDate) {
                lastDate = endDate
            }

            // 計算該月天數
            val days = interestCalcHelper.calculateDays(currentDate, lastDate)

            // 調整 currentDate 至月初（用於 QIRAT 查詢）
            val monthStartDate = DateUtils.withDay(currentDate, 1) ?: currentDate

            // 查詢該月利率
            val actualRate = if (input.actualRate.compareTo(BigDecimal.ZERO) != 0) {
                // 如知利率，使用已知利率（V3 lines 812-814）
                input.actualRate
            } else {
                // 否則查詢 QIRAT（int_rate_type='0'）
                val rateLookup = qiratRateLookup.lookupRate(input, "0", monthStartDate)
                rateLookup.adjustedRate
            }

            // 累加日加權利率與天數
            totalRate = totalRate.add(actualRate.multiply(BigDecimal(days)))
            totalDays += days

            // 記錄月份明細
            monthlyDetails.add(
                MonthlyRateDetail(
                    strDate = currentDate,
                    endDate = lastDate,
                    month = interestCalcHelper.formatMonth(monthStartDate),
                    days = days,
                    iRateOriginal = actualRate,  // f12 不需區分 original/adjusted（已套用減碼折扣）
                    iRate = actualRate,
                    intAmt = BigDecimal.ZERO,  // f12 不記錄逐月利息，僅記錄總利息
                    principalAmt = input.principalAmt
                )
            )

            // 調整至月初（下個月）
            currentDate = DateUtils.withDay(currentDate, 1) ?: currentDate
            currentDate = DateUtils.addMonths(currentDate, 1) ?: currentDate
        }

        // Step 4: 計算日加權平均利率
        val averageRate = if (totalDays > 0) {
            totalRate.divide(
                BigDecimal(totalDays),
                MathUtils.RATE_SCALE,  // 保留足夠精度（10 位小數）
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }

        // Step 5: 計算利息金額
        // V3 lines 859-862: intAmt = ROUND(principal × (actualRate/10000) × (totalDays/yearDays), p_num)
        val intAmt = if (input.principalAmt.compareTo(BigDecimal.ZERO) != 0 && totalDays > 0) {
            input.principalAmt
                .multiply(averageRate.divide(BigDecimal("10000"), MathUtils.AMOUNT_SCALE, RoundingMode.HALF_UP))
                .multiply(BigDecimal(totalDays).divide(BigDecimal(yearDays), MathUtils.AMOUNT_SCALE, RoundingMode.HALF_UP))
                .setScale(precision, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        logger.debug {
            "Interest calc rate calculated: months=$months, totalDays=$totalDays, yearDays=$yearDays, " +
                "avgRate=$averageRate, intAmt=$intAmt"
        }

        return InterestRateCalculationResult(
            actualRate = averageRate,
            intAmt = intAmt,
            monthlyDetails = monthlyDetails
        )
    }
}
