package com.vlife.cv.interest.strategy

import com.vlife.common.util.DateUtils
import com.vlife.common.util.MathUtils
import com.vlife.cv.interest.InterestRateCalculationResult
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
 * 四行庫利率計算策略
 *
 * V3 對應：
 * - cv210p_f40_select_loan (pk_cv_cv210p.pck lines 1231-1374)
 * - **前置**：rate_value_calculation 先呼叫 f20（lines 2076-2078）
 *
 * **⚠️ 前置執行契約（必須遵守）**：
 * - **呼叫者職責**：在呼叫 FourBankRateStrategy 前，必須先呼叫 LoanRateStrategy (f20) 並將結果設定至 `input.actualRate`
 * - **V3 邏輯**：rate_value_calculation 在執行 f40 前先呼叫 f20（lines 2076-2078），將 f20 計算的利率存入 g_iri.actual_rate
 * - **V4 實作**：InterestRateService 或其他呼叫者應先執行 LoanRateStrategy.calculate()，並將 result.actualRate 設定至 input.actualRate
 * - **Fallback**：如果 input.actualRate == 0，本 Strategy 會使用 QIRAT type='2' 作為 fallback（但應視為異常狀況）
 *
 * **計算方式**：
 * 1. **前置執行 f20 邏輯**：計算 g_iri.actual_rate（作為 f40 利息計算的利率來源）
 * 2. 逐月查詢 QIRAT 四行庫利率（int_rate_type='0'）
 * 3. **利息計算使用 f20 設定的 actual_rate**（非 f40 查詢的利率）
 * 4. 累加各月利息（使用 f20 的 actual_rate）
 * 5. 最終 actual_rate 為 f40 查詢的四行庫利率日加權平均
 *
 * **特殊行為**（CV210P-R-012）：
 * - `int_amt` 計算使用 **f20 設定的 actual_rate**，非 f40 本身查詢的 p_actual_rate
 * - `actualRate` 最終值為 **f40 的日加權平均**（p_tot_rate / p_tot_days）
 * - V3 證據：pk_cv_cv210p.pck lines 1356-1357（int_amt 使用 g_iri.actual_rate），line 1361（actual_rate 使用 p_actual_rate 加權）
 *
 * **與 LoanRateStrategy 差異**：
 * - LoanRateStrategy：QIRAT type='2'，利息使用查詢的利率
 * - FourBankRateStrategy：QIRAT type='0'，利息使用前置 f20 設定的 actual_rate
 *
 * @see LoanRateStrategy 貸款利率（f20 邏輯，必須在 f40 前執行）
 */
@Component
class FourBankRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(RateType.FOUR_BANK_RATE)

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
            logger.debug { "Invalid input: beginDate > endDate" }
            return InterestRateCalculationResult.zero()
        }

        return calculateFourBankRate(input, precision)
    }

    /**
     * 四行庫利率計算
     *
     * V3 對應：cv210p_f40_select_loan (lines 1231-1374)
     *
     * **前置條件**（CV210P-R-012）：
     * - V3 在 rate_value_calculation 中先執行 f20（lines 2076-2078），設定 g_iri.actual_rate
     * - V4 假設 input.actualRate 已由外部（InterestRateService）呼叫 LoanRateStrategy 設定
     * - 如果 input.actualRate == 0，則使用 QIRAT type='2' 作為 fallback
     *
     * **計算邏輯**：
     * 1. 計算年天數（考慮閏年）
     * 2. 計算月數（begin_date 至 end_date，最多 120 個月）
     * 3. 逐月迭代：
     *    - 確定該月的起迄日（月底調整至 31 日，或至 end_date）
     *    - 計算該月天數（非最後月 +1）
     *    - 查詢該月四行庫利率（QIRAT type='0'）
     *    - **利息計算使用 input.actualRate（f20 設定的值）**
     *    - 累加 totalRate（使用 p_actual_rate，type='0'）
     *    - 累加 totalDays
     * 4. intAmt = Σ 各月利息
     * 5. actualRate = totalRate / totalDays（f40 的四行庫利率日加權平均）
     *
     * @param input 利率計算輸入（actualRate 應已由 f20 前置設定）
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（actualRate 為 f40 加權平均，intAmt 使用 f20 利率）
     */
    private fun calculateFourBankRate(
        input: InterestRateInput,
        precision: Int
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 1: 計算年天數（閏年考慮）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算月數（最多 120 個月）
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        months += 1
        if (months > 120) {
            months = 120  // V3 lines 1272-1274（已驗證：使用 > 而非 >=）
        }

        // Step 3: 前置利率（f20 設定的 actual_rate，用於利息計算）
        // V3 假設 rate_value_calculation 已執行 f20 並設定 g_iri.actual_rate
        // V4 假設外部已執行 LoanRateStrategy 並設定 input.actualRate
        // 如果 actualRate == 0，使用 QIRAT type='2' 作為 fallback
        val f20Rate = if (input.actualRate.compareTo(BigDecimal.ZERO) != 0) {
            input.actualRate
        } else {
            // Fallback：查詢 QIRAT type='2'（與 f20 相同）
            val fallbackRateDate = interestCalcHelper.toMonthStart(beginDate)
            qiratRateLookup.lookupRate(input, "2", fallbackRateDate).adjustedRate
        }

        // Step 4: 逐月迭代累計四行庫利率與利息
        var totalRate = BigDecimal.ZERO
        var totalDays = 0
        var totalIntAmt = BigDecimal.ZERO
        var currentDate = beginDate

        val monthlyDetails = mutableListOf<MonthlyRateDetail>()

        for (i in 1..months) {
            // 計算該月的結束日（月底 31 日或 end_date）
            var monthEndDate = DateUtils.withDay(currentDate, 31) ?: currentDate
            if (monthEndDate > endDate) {
                monthEndDate = endDate
            }

            // 計算該月天數
            var days = interestCalcHelper.calculateDays(currentDate, monthEndDate)
            if (i != months) {
                days += 1
            }

            // 調整至月初（用於 QIRAT 查詢）
            val rateDate = interestCalcHelper.toMonthStart(currentDate)

            // P1-002: V3 lines 1290-1292：當 actualRate != 0 時，直接使用 actualRate 作為四行庫利率
            // V3 lines 1293-1330：當 actualRate = 0 或 NULL 時，查詢 QIRAT type='0'
            val fourBankRate = if (input.actualRate.compareTo(BigDecimal.ZERO) != 0) {
                // 已知利率：直接使用（V3 lines 1290-1292）
                input.actualRate
            } else {
                // 未知利率：查詢 QIRAT type='0'（V3 lines 1295-1330）
                qiratRateLookup.lookupRate(input, "0", rateDate).adjustedRate
            }

            // **利息計算使用 f20 設定的 actual_rate**（CV210P-R-012）
            // V3 line 1356-1357: int_amt := round(principal × (g_iri.actual_rate / 10000 × days / yearDays), p_num)
            val monthlyInterest = input.principalAmt
                .multiply(f20Rate.divide(BigDecimal("10000"), 10, RoundingMode.HALF_UP))
                .multiply(BigDecimal(days).divide(BigDecimal(yearDays), 10, RoundingMode.HALF_UP))
                .setScale(precision, RoundingMode.HALF_UP)

            totalIntAmt = totalIntAmt.add(monthlyInterest)

            // **actual_rate 加權使用 f40 查詢的四行庫利率**（V3 line 1361）
            totalRate = totalRate.add(fourBankRate.multiply(BigDecimal(days)))
            totalDays += days

            // 記錄月份明細
            monthlyDetails.add(
                MonthlyRateDetail(
                    strDate = currentDate,
                    endDate = monthEndDate,
                    month = interestCalcHelper.formatMonth(rateDate),
                    days = days,
                    iRateOriginal = f20Rate.divide(BigDecimal("10000"), 10, RoundingMode.HALF_UP),  // f20 利率
                    iRate = f20Rate.divide(BigDecimal("10000"), 10, RoundingMode.HALF_UP),  // 利息計算使用 f20 利率
                    intAmt = monthlyInterest,
                    principalAmt = input.principalAmt
                )
            )

            // 下個月（調整至月初）
            currentDate = DateUtils.addMonths(beginDate, i) ?: beginDate
            currentDate = interestCalcHelper.toMonthStart(currentDate)
        }

        // Step 5: 最終 actual_rate 為四行庫利率日加權平均
        val finalActualRate = if (totalDays > 0) {
            // V3 lines 1370-1371: calc_round(totalRate / totalDays)
            MathUtils.calcRound(totalRate.divide(BigDecimal(totalDays), MathUtils.RATE_SCALE, RoundingMode.HALF_UP))
        } else {
            BigDecimal.ZERO
        }

        logger.debug {
            "Four bank rate calculated: months=$months, totalDays=$totalDays, yearDays=$yearDays, " +
                "f20Rate=$f20Rate, f40AvgRate=$finalActualRate, intAmt=$totalIntAmt"
        }

        return InterestRateCalculationResult(
            actualRate = finalActualRate,  // f40 的四行庫利率加權平均
            intAmt = totalIntAmt,  // 使用 f20 利率計算的利息
            monthlyDetails = monthlyDetails
        )
    }
}
