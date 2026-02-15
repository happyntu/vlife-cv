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
 * 分紅利率計算策略（月加權平均）
 *
 * V3 對應：
 * - cv210p_f10_select_div (pk_cv_cv210p.pck lines 507-641)
 * - cv210p_f10_select_div_G (pk_cv_cv210p.pck lines 643-742)
 *
 * **計算方式**：
 * 1. 逐月查詢 QIRAT 利率（int_rate_type='0' 或 '5'）
 * 2. 累加各月利率
 * 3. actualRate = 總利率 / 月數（月加權平均）
 * 4. **不計算利息**，僅回傳 actualRate
 *
 * **雙重路由**（內部委派）：
 * - insurance_type_3 ≠ 'G' → f10_select_div（int_rate_type='0'）
 * - insurance_type_3 = 'G' → f10_select_div_G（int_rate_type='5'，利變年金）
 *
 * @see InterestCalcRateStrategy 計息利率（日加權 + 計算利息）
 */
@Component
class DividendRateStrategy(
    private val qiratRateLookup: QiratRateLookup,
    private val interestCalcHelper: InterestCalcHelper
) : InterestRateStrategy {

    override fun supportedRateTypes(): Set<RateType> = setOf(RateType.DIVIDEND_RATE)

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

        // 雙重路由：insurance_type_3='G' → 利變年金分紅（int_rate_type='5'）
        val insuranceType3 = plan?.insuranceType3
        val intRateType = if (insuranceType3 == "G") "5" else "0"

        return calculateDividendRate(input, intRateType)
    }

    /**
     * 分紅利率計算（月加權平均）
     *
     * V3 對應：cv210p_f10_select_div / cv210p_f10_select_div_G
     *
     * @param input 利率計算輸入
     * @param intRateType QIRAT 利率類型（'0' 或 '5'）
     * @return 計算結果（actualRate 為月加權平均）
     */
    private fun calculateDividendRate(
        input: InterestRateInput,
        intRateType: String
    ): InterestRateCalculationResult {
        val beginDate = input.beginDate!!
        val endDate = input.endDate!!

        // Step 1: 計算月數
        // var 使用理由：根據邊界條件調整（months <= 0 時 +1）
        var months = interestCalcHelper.calculateMonths(beginDate, endDate)
        if (months <= 0) {
            months += 1  // V3 lines 545-547：月數 <= 0 時加 1
        }

        // Step 2: 逐月查詢利率並累加
        // var 使用理由：迴圈中累加利率與日期遞增（遵循 V3 邏輯）
        var totalRate = BigDecimal.ZERO
        var currentDate = beginDate

        for (i in 1..months) {
            // 調整至月初（用於 QIRAT 查詢）
            val monthStartDate = DateUtils.withDay(currentDate, 1) ?: currentDate

            // 查詢該月利率（已套用減碼/折扣）
            val actualRate = if (input.actualRate.compareTo(BigDecimal.ZERO) != 0) {
                // 如果已知利率，使用已知利率（V3 lines 551-553）
                input.actualRate
            } else {
                // 否則查詢 QIRAT
                val rateLookup = qiratRateLookup.lookupRate(input, intRateType, monthStartDate)
                rateLookup.adjustedRate
            }

            totalRate = totalRate.add(actualRate)

            // 下個月
            currentDate = DateUtils.addMonths(currentDate, 1) ?: currentDate
        }

        // Step 3: 計算月加權平均利率
        val averageRate = totalRate.divide(
            BigDecimal(months),
            10,  // 保留 10 位小數（利率精度）
            RoundingMode.HALF_UP
        )

        logger.debug { "Dividend rate calculated: months=$months, totalRate=$totalRate, avgRate=$averageRate" }

        return InterestRateCalculationResult(
            actualRate = averageRate,
            intAmt = BigDecimal.ZERO,  // 分紅利率不計算利息
            monthlyDetails = emptyList()  // 月加權不回傳明細
        )
    }
}
