package com.vlife.cv.interest

import com.vlife.cv.interest.dispatcher.RateStrategyDispatcher
import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PlanNote
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 利率計算服務
 *
 * V3 對應：cv210p_rate_value_calculation (pk_cv_cv210p.pck lines 1944-2091)
 *
 * **核心職責**：
 * - 統一入口：所有利率計算請求透過本服務處理
 * - 前置檢查：驗證輸入參數、查詢產品資訊（Pldf/PlanNote）
 * - 投資型閘門：檢查 insurance_type_3，非投資型保單不可使用投資型專用 rate_type
 * - 策略分派：依 rate_type 選擇對應的 Strategy 執行計算
 * - 結果封裝：回傳標準化的 InterestRateCalculationResult
 *
 * **分派邏輯（V3 兩層，CV210P-R-011）**：
 * 1. 投資型分派（insurance_type_3 IN [F,G,H]）→ 使用對應 Strategy
 * 2. 非投資型 + 投資型專用 rate_type (A/B/C/D/E/F) → fallback 至 LoanRateStrategy (f20)
 * 3. 傳統型分派（rate_type 0-5, 8）→ 使用對應 Strategy
 *
 * V3 證據：pk_cv_cv210p.pck lines 1877-1943（cv210p_rate_calc）
 * - IF insurance_type_3 IN ('F','G','H') → 投資型路徑
 * - ELSE → 傳統型路徑（rate_type A-F fallback 至 f20）
 *
 * @see RateStrategyDispatcher
 * @see InterestRateStrategy
 */
@Service
class InterestRateService(
    private val rateStrategyDispatcher: RateStrategyDispatcher
) {

    /**
     * 計算利率與利息
     *
     * **邊界條件處理**（與 V3 一致）：
     * - rate_type 無效 → 拋出 IllegalArgumentException
     * - beginDate > endDate → 回傳零值（V3 lines 976-981, 1134-1139）
     * - subAcntPlanCode 為 null → 回傳零值（查無 QIRAT）
     *
     * **精度參數**（p_num）：
     * - 0：台幣（整數）
     * - 2：外幣（小數 2 位）
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @param plan 產品資訊（optional，Strategy 可能需要）
     * @param planNote 產品註記（optional，Strategy 可能需要）
     * @return 利率計算結果
     * @throws IllegalArgumentException 當 rate_type 無效時
     */
    fun calculateRate(
        input: InterestRateInput,
        precision: Int = 0,
        plan: Pldf? = null,
        planNote: PlanNote? = null
    ): InterestRateCalculationResult {
        // Step 1: 驗證 rate_type
        val rateType = input.rateType
            ?: throw IllegalArgumentException("rate_type 不可為 null")

        if (!rateStrategyDispatcher.supports(rateType)) {
            throw IllegalArgumentException(
                "不支援的 rate_type: ${rateType.code} (${rateType.description}). " +
                    "支援的 rate_types: ${rateStrategyDispatcher.supportedRateTypes().map { it.code }}"
            )
        }

        // Step 2: 邊界檢查（beginDate > endDate）
        if (input.beginDate != null && input.endDate != null && input.beginDate > input.endDate) {
            logger.debug { "Invalid date range: beginDate=${input.beginDate} > endDate=${input.endDate}, returning zero" }
            return InterestRateCalculationResult.zero()
        }

        // Step 3: 投資型閘門（CV210P-R-011）
        // V3 兩層分派：非投資型保單不可使用投資型專用 rate_type (A/B/C/D/E/F)
        // V3 證據：pk_cv_cv210p.pck lines 1877-1943
        //   IF insurance_type_3 IN ('F','G','H') → 投資型路徑
        //   ELSE → rate_type A-F fallback 至 f20 (LoanRateStrategy)
        val effectiveRateType = resolveInvestmentGate(rateType, plan)

        // Step 4: 分派策略執行計算
        logger.debug {
            "Calculating rate: rateType=${effectiveRateType.code}, " +
                "beginDate=${input.beginDate}, endDate=${input.endDate}, " +
                "precision=$precision, planCode=${plan?.planCode}"
        }

        val strategy = rateStrategyDispatcher.dispatch(effectiveRateType)
        val result = strategy.calculate(input, precision, plan, planNote)

        logger.debug {
            "Calculation completed: actualRate=${result.actualRate}, intAmt=${result.intAmt}, " +
                "monthlyDetails=${result.monthlyDetails.size} records"
        }

        return result
    }

    /**
     * 批量計算利率
     *
     * 適用於需要計算多組利率的場景（例如：多個保單、多個計算期間）
     *
     * @param inputs 利率計算輸入清單
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果清單
     */
    fun calculateRateBatch(
        inputs: List<InterestRateInput>,
        precision: Int = 0
    ): List<InterestRateCalculationResult> {
        logger.debug { "Batch calculating ${inputs.size} rates" }

        return inputs.map { input ->
            try {
                calculateRate(input, precision)
            } catch (e: Exception) {
                logger.warn(e) { "Batch calculation failed for input: $input" }
                InterestRateCalculationResult.zero()
            }
        }
    }

    /**
     * 檢查是否支援指定的 rate_type
     *
     * @param rateType 利率類型
     * @return true 如果支援，否則 false
     */
    fun supportsRateType(rateType: RateType): Boolean {
        return rateStrategyDispatcher.supports(rateType)
    }

    /**
     * 取得所有支援的 rate_types
     *
     * @return 支援的利率類型集合
     */
    fun getSupportedRateTypes(): Set<RateType> {
        return rateStrategyDispatcher.supportedRateTypes()
    }

    /**
     * 投資型閘門邏輯
     *
     * V3 對應：cv210p_rate_calc (pk_cv_cv210p.pck lines 1877-1943)
     *
     * **規則**（CV210P-R-011）：
     * - 投資型保單（insurance_type_3 IN [F,G,H]）：使用原 rateType
     * - 非投資型保單 + 投資型專用 rate_type (A/B/C/D/E/F)：fallback 至 LOAN_RATE_MONTHLY (f20)
     * - plan 為 null 時無法判斷，使用原 rateType（呼叫者應盡量提供 plan）
     *
     * @param rateType 原始 rate_type
     * @param plan 產品資訊（用於讀取 insurance_type_3）
     * @return 經閘門調整後的 rate_type
     */
    private fun resolveInvestmentGate(rateType: RateType, plan: Pldf?): RateType {
        // 如果不是投資型專用 rate_type，直接通過
        if (rateType !in RateType.INVESTMENT_ONLY_CODES) {
            return rateType
        }

        // 如果 plan 為 null，無法判斷 insurance_type_3，使用原 rateType
        val insuranceType3 = plan?.insuranceType3
        if (insuranceType3 == null) {
            logger.warn {
                "Investment gate: plan is null for investment-only rateType=${rateType.code}, " +
                    "cannot determine insurance_type_3. Using original rateType."
            }
            return rateType
        }

        // 投資型保單（F/G/H）：使用原 rateType
        if (insuranceType3 in INVESTMENT_INSURANCE_TYPES) {
            return rateType
        }

        // 非投資型保單 + 投資型專用 rate_type → fallback 至 LOAN_RATE_MONTHLY (f20)
        logger.info {
            "Investment gate: non-investment policy (insurance_type_3=$insuranceType3) " +
                "with investment-only rateType=${rateType.code}, fallback to LOAN_RATE_MONTHLY (f20)"
        }
        return RateType.LOAN_RATE_MONTHLY
    }

    companion object {
        /**
         * 投資型保險的 insurance_type_3 值
         * V3 證據：pk_cv_cv210p.pck lines 1879-1880
         */
        private val INVESTMENT_INSURANCE_TYPES = setOf("F", "G", "H")
    }
}
