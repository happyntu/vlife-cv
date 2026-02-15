package com.vlife.cv.engine.stub

import com.vlife.cv.engine.contract.Cv210pInterestRateCalculator
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PlanNote
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * CV210P 利率計算引擎 Stub 實作（開發階段）
 *
 * **用途**（ADR-027）：
 * - 開發階段替身：在 CV210P 引擎實作完成前，提供預設行為
 * - 解除時序依賴：依賴 CV210P 的其他引擎（如 CV208P）可先行開發
 * - 整合測試：不需要 Oracle DB 即可進行服務層測試
 *
 * **替換機制**：
 * 1. Cv210pEngineConfig 預設注入 Stub（@ConditionalOnMissingBean）
 * 2. Cv210pInterestRateCalculatorImpl 實作完成後，以 @Primary 自動取代
 * 3. 零修改替換：依賴方無需修改程式碼
 *
 * **Stub 行為**：
 * - calculateRate(): 回傳零值 (zero result)
 * - calculateRateBatch(): 回傳零值清單
 * - supportsRateType(): 回傳 true（接受所有 rate_type）
 * - getSupportedRateTypes(): 回傳 RateType.values()
 *
 * @see Cv210pInterestRateCalculator
 * @see com.vlife.cv.engine.impl.Cv210pInterestRateCalculatorImpl
 * @see com.vlife.cv.engine.config.Cv210pEngineConfig
 */
class StubCv210pInterestRateCalculator : Cv210pInterestRateCalculator {

    override fun calculateRate(
        input: InterestRateInput,
        precision: Int,
        plan: Pldf?,
        planNote: PlanNote?
    ): InterestRateCalculationResult {
        logger.debug {
            "[STUB] Cv210pInterestRateCalculator.calculateRate() called with rateType=${input.rateType?.code}, " +
                "returning zero result"
        }
        return InterestRateCalculationResult.zero()
    }

    override fun calculateRateBatch(
        inputs: List<InterestRateInput>,
        precision: Int
    ): List<InterestRateCalculationResult> {
        logger.debug {
            "[STUB] Cv210pInterestRateCalculator.calculateRateBatch() called with ${inputs.size} inputs, " +
                "returning zero results"
        }
        return inputs.map { InterestRateCalculationResult.zero() }
    }

    override fun supportsRateType(rateType: RateType): Boolean {
        logger.debug { "[STUB] Cv210pInterestRateCalculator.supportsRateType(${rateType.code}) → true" }
        return true  // Stub 接受所有 rate_type
    }

    override fun getSupportedRateTypes(): Set<RateType> {
        logger.debug { "[STUB] Cv210pInterestRateCalculator.getSupportedRateTypes() → all RateTypes" }
        return RateType.values().toSet()
    }
}
