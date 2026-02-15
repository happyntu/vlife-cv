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
import com.vlife.cv.plnd.PlndService
import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PlanNote
import com.vlife.cv.quote.QmfdeService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

private val logger = KotlinLogging.logger {}

/** 複利計算內部精度（小數位數），對應 Oracle NUMBER ~38 位有效數字 */
private const val POWER_SCALE = 20

/**
 * 企業年金利率計算策略（現金價值 + 複利計算）
 *
 * V3 對應：
 * - cv210p_rate_calc_G (pk_cv_cv210p.pck lines 1676-1914)
 * - insurance_type_3='G' (利變年金)
 * - insurance_type_3='H' (企業年金)
 *
 * **計算方式**：
 * 1. 適用於企業年金（insurance_type_3='G' 或 'H'）的現金價值計算
 * 2. rate_type 'D'：線性利率計算（單利）
 * 3. rate_type 'F'：複利計算（使用 POWER 複利公式）
 * 4. 逐月查詢利率，根據 rate_type 選擇單利或複利
 * 5. actualRate = 日加權平均利率（單利）或複利加權平均
 *
 * **rate_type 說明**：
 * - rate_type 'D'：現金價值利率（企業年金，線性計算，int_rate_type='8'）
 * - rate_type 'F'：複利計算（企業年金，使用 POWER 公式，int_rate_type='5'）
 *
 * **複利公式（rate_type='F'）**（Design Spec §2.7 公式 4）：
 * - rate_factor = POWER(1 + rate/10000, days/yearDays)
 * - 利息 = ROUND(本金 × (累積 rate_factor - 1), precision)
 * - 多月複利時，逐月迭代累乘
 *
 * **捨入時機**：
 * - rate_type 'D'（線性）：逐月 ROUND（與 DepositRateStrategy 相同）
 * - rate_type 'F'（複利）：逐月累乘，最後 ROUND
 *
 * **特殊邏輯**：
 * - insurance_type_3='G' 時，分紅利率查詢使用 int_rate_type='5'（利變年金特定利率）
 * - insurance_type_3='H' 時，採用固定或浮動利率組合
 * - 支援本息和計算（複利場景）
 *
 * @see DividendRateStrategy 分紅利率（當 insurance_type_3='G' 時）
 * @see DepositRateStrategy 現金價值利率（存款類）
 */
@Component
class AnnuityRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper,
    private val plndService: PlndService,
    private val qmfdeService: QmfdeService
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(
        RateType.ANNUITY_RATE_D,
        RateType.COMPOUND_RATE
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
        if (input.beginDate >= input.endDate) {  // V3 lines 1715-1718
            logger.debug { "Invalid input: beginDate >= endDate" }
            return InterestRateCalculationResult.zero()
        }

        // 根據 rate_type 選擇計算方式
        return when (input.rateType) {
            RateType.COMPOUND_RATE -> calculateCompoundInterest(input, precision, plan)  // rate_type 'F'
            RateType.ANNUITY_RATE_D -> calculateLinearInterest(input, precision)   // rate_type 'D'
            else -> {
                logger.warn { "Unexpected rateType: ${input.rateType}" }
                InterestRateCalculationResult.zero()
            }
        }
    }

    /**
     * P0-003: 計算第一個保單週年日（V3 lines 1747-1760）
     *
     * **邏輯**：
     * 1. 如果 begin_date 的月/日 >= po_issue_date 的月/日：
     *    → p_rate_date 為 begin_date 年份的 po_issue_date 月/日
     * 2. 如果 begin_date 的月/日 < po_issue_date 的月/日：
     *    → p_rate_date 為 begin_date 年份 -1 的 po_issue_date 月/日
     * 3. 保護：p_rate_date 不早於 po_issue_date
     *
     * @param beginDate 利息計算起始日
     * @param poIssueDate 保單發行日
     * @return 第一個保單週年日
     */
    private fun calculateFirstAnniversary(beginDate: java.time.LocalDate, poIssueDate: java.time.LocalDate): java.time.LocalDate {
        val beginMMDD = beginDate.monthValue * 100 + beginDate.dayOfMonth
        val issueMMDD = poIssueDate.monthValue * 100 + poIssueDate.dayOfMonth

        val anniversaryDate = if (beginMMDD >= issueMMDD) {
            // begin_date 的月/日 >= po_issue_date 的月/日
            java.time.LocalDate.of(beginDate.year, poIssueDate.month, poIssueDate.dayOfMonth)
        } else {
            // begin_date 的月/日 < po_issue_date 的月/日 → 前一年
            java.time.LocalDate.of(beginDate.year - 1, poIssueDate.month, poIssueDate.dayOfMonth)
        }

        // 保護：不早於發行日
        return if (anniversaryDate < poIssueDate) poIssueDate else anniversaryDate
    }

    /**
     * 複利計算（rate_type='F'）
     *
     * V3 對應：cv210p_rate_calc_G with POWER 公式（lines 1845-1863）
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（actualRate 為複利加權平均，intAmt 為複利利息）
     */
    private fun calculateCompoundInterest(
        input: InterestRateInput,
        precision: Int,
        plan: Pldf? = null
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 0: P0-001 PLND/QMFDE 查詢（企業年金險種）
        val insuranceType3 = plan?.insuranceType3
        val (intApplyYrInd, intApplyYr, rateG) = if (insuranceType3 in listOf("G", "H")) {
            try {
                val plndList = plndService.findByPlanCodeAndVersion(plan!!.planCode, plan.version)
                if (plndList.isEmpty()) {
                    logger.debug { "No PLND found for planCode=${plan.planCode}, version=${plan.version}" }
                    return InterestRateCalculationResult.zero()
                }

                val qmfdeDto = qmfdeService.getByTargetCode(plndList.first().ivTargetCode)
                    ?: run {
                        logger.debug { "No QMFDE found for ivTargetCode=${plndList.first().ivTargetCode}" }
                        return InterestRateCalculationResult.zero()
                    }

                // Query issue date rate (if needed)
                val issueDate = input.beginDate!!
                val issueRateLookup = qiratRateLookup.lookupRate(input, "5", issueDate)

                Triple(qmfdeDto.intApplyYrInd ?: "0", qmfdeDto.intApplyYr ?: 0, issueRateLookup.adjustedRate)
            } catch (e: Exception) {
                logger.debug { "Error querying PLND/QMFDE: ${e.message}" }
                return InterestRateCalculationResult.zero()
            }
        } else {
            Triple("0", 0, BigDecimal.ZERO)
        }

        // Step 1: 計算年天數（考慮閏年）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算月數
        // var 使用理由：根據邊界條件調整（months <= 0 時 +1）
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        if (months <= 0) {
            months += 1  // V3 lines 1727-1729
        }

        // Step 3: 逐月查詢利率並計算複利因子（逐月累乘）
        // var 使用理由：複利計算需要累乘 rate_factor（遵循 V3 邏輯）
        var cumulativeRateFactor = BigDecimal.ONE  // 複利累積因子（BigDecimal 精確計算）
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

            // P1-004: 每次迭代重算年天數（處理跨閏年）
            val currentYearDays = interestCalcHelper.calculateYearDays(currentDate)

            // 查詢該月利率（int_rate_type='5'，投資型/宣告利率，V3 line 1800）
            val rateLookup = qiratRateLookup.lookupRate(input, "5", monthStartDate)
            val originalRate = rateLookup.originalRate
            val adjustedRate = rateLookup.adjustedRate

            // P0-007: BigDecimal 精確複利因子計算（V3 line 1855）
            // rate_factor = POWER(1 + rate/10000, days/yearDays)
            val base = BigDecimal.ONE.add(
                adjustedRate.divide(BigDecimal("10000"), POWER_SCALE, RoundingMode.HALF_UP)
            )
            val exponent = BigDecimal(days).divide(
                BigDecimal(currentYearDays), POWER_SCALE, RoundingMode.HALF_UP
            )
            val rateFactor = MathUtils.bigDecimalPow(base, exponent, POWER_SCALE)

            // 累乘複利因子（BigDecimal 精確累乘）
            cumulativeRateFactor = cumulativeRateFactor.multiply(rateFactor)
                .setScale(POWER_SCALE, RoundingMode.HALF_UP)

            // 日加權利率累加（用於 actualRate 計算）
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
                    intAmt = BigDecimal.ZERO,  // 複利利息最後計算
                    principalAmt = input.principalAmt,
                    description = "Compound"
                )
            )

            // 移至下個月
            currentDate = nextMonthDate
        }

        // Step 4: 計算複利利息（最後 ROUND，V3 lines 1862-1863）
        // 利息 = ROUND(本金 × (累積 rate_factor - 1), precision)
        val totalIntAmt = MathUtils.round(
            input.principalAmt.multiply(cumulativeRateFactor.subtract(BigDecimal.ONE)),
            precision
        )

        // Step 5: 計算日加權平均利率
        val averageRate = if (totalDays > 0) {
            totalRateWeighted.divide(
                BigDecimal(totalDays),
                InterestRateConstants.RATE_SCALE,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }

        // P0-004: 複利 actualRate 使用複利公式 (Π - 1) × 10000，而非日加權平均
        val compoundActualRate = (cumulativeRateFactor.subtract(BigDecimal.ONE))
            .multiply(BigDecimal("10000"))
            .setScale(InterestRateConstants.RATE_SCALE, RoundingMode.HALF_UP)

        logger.debug {
            "Compound interest calculated: int_rate_type=5, " +
                "months=$months, totalDays=$totalDays, rateFactor=$cumulativeRateFactor, " +
                "compoundRate=$compoundActualRate, totalIntAmt=$totalIntAmt"
        }

        return InterestRateCalculationResult(
            actualRate = compoundActualRate,
            intAmt = totalIntAmt,
            monthlyDetails = monthlyDetails
        )
    }

    /**
     * 線性利率計算（rate_type='D'，單利，逐月 ROUND）
     *
     * V3 對應：cv210p_rate_calc_G 非 POWER 路徑（lines 1826-1840）
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果（actualRate 為日加權平均，intAmt 為累計利息）
     */
    private fun calculateLinearInterest(
        input: InterestRateInput,
        precision: Int
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 1: 計算年天數（考慮閏年）
        val yearDays = interestCalcHelper.calculateYearDays(beginDate)

        // Step 2: 計算月數
        // var 使用理由：根據邊界條件調整（months <= 0 時 +1）
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        if (months <= 0) {
            months += 1  // V3 lines 1727-1729
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

            // 查詢該月利率（int_rate_type='8'，企業年金利率，V3 line 1787）
            val rateLookup = qiratRateLookup.lookupRate(input, "8", monthStartDate)
            val originalRate = rateLookup.originalRate
            val adjustedRate = rateLookup.adjustedRate

            // 計算該月利息（逐月 ROUND，V3 lines 1832-1834）
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
                    description = "Linear"
                )
            )

            // 移至下個月
            currentDate = nextMonthDate
        }

        // Step 4: 計算日加權平均利率（V3 lines 1837-1840）
        val averageRate = if (totalDays > 0) {
            totalRateWeighted.divide(
                BigDecimal(totalDays),
                InterestRateConstants.RATE_SCALE,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }

        logger.debug {
            "Linear interest calculated: int_rate_type=8, " +
                "months=$months, totalDays=$totalDays, avgRate=$averageRate, totalIntAmt=$totalIntAmt"
        }

        return InterestRateCalculationResult(
            actualRate = averageRate,
            intAmt = totalIntAmt,
            monthlyDetails = monthlyDetails
        )
    }
}
