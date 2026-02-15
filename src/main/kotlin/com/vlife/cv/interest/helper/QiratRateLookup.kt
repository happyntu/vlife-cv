package com.vlife.cv.interest.helper

import com.vlife.common.util.MathUtils
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.rate.QiratService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * QIRAT 利率查詢 + 減碼/折扣套用
 *
 * V3 對應：cv210p_sel_qirat (pk_cv_cv210p.pck lines 873-938)
 *
 * **核心功能**：
 * 1. 查詢 QIRAT 利率（使用 QiratService）
 * 2. 套用 rate_sub（減碼）
 * 3. 套用 rate_disc（折扣）
 *
 * **套用順序（不可對調）**：
 * 1. 先減碼：rate = rate - rate_sub
 * 2. 後折扣：rate = rate × (rate_disc / 100)
 *
 * V3 證據：pk_cv_cv210p.pck lines 928-933
 */
@Component
class QiratRateLookup(
    private val qiratService: QiratService
) {

    companion object {
        /**
         * 折扣基準：100 = 不折扣，90 = 打 9 折
         */
        private val HUNDRED = BigDecimal("100")
    }

    /**
     * 查詢並套用減碼/折扣的利率
     *
     * V3 對應：cv210p_sel_qirat
     *
     * @param input 利率計算輸入（包含 subAcntPlanCode, rateSub, rateDisc）
     * @param intRateType QIRAT 利率類型（'0', '1', '2', '5', '8', '9'）
     * @param baseDate 基準日期
     * @return RateLookupResult（原始利率、優惠後利率）
     */
    fun lookupRate(
        input: InterestRateInput,
        intRateType: String,
        baseDate: LocalDate
    ): RateLookupResult {
        // Step 1: 查詢 QIRAT 原始利率
        val subAcntPlanCode = input.subAcntPlanCode
            ?: return RateLookupResult.zero()

        val qirat = qiratService.getEffectiveRate(subAcntPlanCode, intRateType, baseDate)
        if (qirat == null) {
            logger.debug { "QIRAT not found: plan=$subAcntPlanCode, type=$intRateType, date=$baseDate" }
            return RateLookupResult.zero()
        }

        val originalRate = qirat.intRate ?: BigDecimal.ZERO
        logger.debug { "QIRAT found: plan=$subAcntPlanCode, type=$intRateType, date=$baseDate, rate=$originalRate" }

        // Step 2: 套用減碼/折扣
        val adjustedRate = applyDiscounts(originalRate, input.rateSub, input.rateDisc)

        return RateLookupResult(
            originalRate = originalRate,
            adjustedRate = adjustedRate
        )
    }

    /**
     * 套用減碼與折扣
     *
     * V3 對應：pk_cv_cv210p.pck lines 928-933
     *
     * **順序（不可對調）**：
     * 1. 先減碼：rate = rate - rate_sub
     * 2. 後折扣：rate = rate × (rate_disc / 100)
     *
     * **實作模式**：不可變鏈式調用（遵循 Kotlin 最佳實踐）
     *
     * @param originalRate 原始利率（萬分率）
     * @param rateSub 利率減碼（萬分率）
     * @param rateDisc 利率折扣百分比（100 = 不折扣，90 = 打 9 折）
     * @return 優惠後利率（萬分率）
     */
    fun applyDiscounts(
        originalRate: BigDecimal,
        rateSub: BigDecimal,
        rateDisc: BigDecimal
    ): BigDecimal {
        return originalRate
            .let { rate ->
                // Step 1: 先減碼
                if (rateSub.compareTo(BigDecimal.ZERO) != 0) {
                    rate.subtract(rateSub)
                } else {
                    rate
                }
            }
            .let { rate ->
                // Step 2: 後折扣
                if (rateDisc.compareTo(BigDecimal.ZERO) != 0 && rateDisc.compareTo(HUNDRED) != 0) {
                    rate.multiply(rateDisc).divide(HUNDRED, MathUtils.RATE_SCALE, java.math.RoundingMode.HALF_UP)
                } else {
                    rate
                }
            }
    }

    /**
     * 利率查詢結果
     *
     * @property originalRate 原始利率（萬分率）
     * @property adjustedRate 優惠後利率（萬分率）
     */
    data class RateLookupResult(
        val originalRate: BigDecimal,
        val adjustedRate: BigDecimal
    ) {
        companion object {
            /**
             * 建立零值結果
             *
             * 用於 QIRAT 查無資料時
             */
            fun zero(): RateLookupResult = RateLookupResult(
                originalRate = BigDecimal.ZERO,
                adjustedRate = BigDecimal.ZERO
            )
        }
    }
}
