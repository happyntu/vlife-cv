package com.vlife.cv.engine.impl

import com.vlife.cv.engine.contract.Cv210pInterestRateCalculator
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.InterestRateService
import com.vlife.cv.interest.RateType
import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PlanNote
import mu.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * CV210P 利率計算引擎實作（Production）
 *
 * V3 對應：pk_cv_cv210p.pck（精算利率計算引擎）
 *
 * **設計原則**（ADR-027）：
 * - 實作 Engine Contract 介面
 * - 委派所有計算至 InterestRateService
 * - 使用 @Primary 自動取代 Stub（零修改替換）
 *
 * **架構分層**：
 * ```
 * CV208P Engine (依賴方)
 *   ↓ 注入 Cv210pInterestRateCalculator interface
 * Cv210pInterestRateCalculatorImpl (@Primary)
 *   ↓ 注入 InterestRateService
 * InterestRateService
 *   ↓ 注入 RateStrategyDispatcher
 * 各種 InterestRateStrategy 實作 (LoanRateStrategy, etc.)
 * ```
 *
 * **替換機制**：
 * - Cv210pEngineConfig 預設注入 Stub
 * - 本實作以 @Primary 優先注入
 * - Stub 自動被取代，依賴方零修改
 *
 * @see Cv210pInterestRateCalculator
 * @see InterestRateService
 * @see com.vlife.cv.engine.stub.StubCv210pInterestRateCalculator
 * @see com.vlife.cv.engine.config.Cv210pEngineConfig
 */
@Service
@Primary
class Cv210pInterestRateCalculatorImpl(
    private val interestRateService: InterestRateService
) : Cv210pInterestRateCalculator {

    override fun calculateRate(
        input: InterestRateInput,
        precision: Int,
        plan: Pldf?,
        planNote: PlanNote?
    ): InterestRateCalculationResult {
        logger.debug {
            "Cv210pInterestRateCalculatorImpl.calculateRate() delegating to InterestRateService: " +
                "rateType=${input.rateType?.code}, precision=$precision"
        }

        return interestRateService.calculateRate(input, precision, plan, planNote)
    }

    override fun calculateRateBatch(
        inputs: List<InterestRateInput>,
        precision: Int
    ): List<InterestRateCalculationResult> {
        logger.debug {
            "Cv210pInterestRateCalculatorImpl.calculateRateBatch() delegating to InterestRateService: " +
                "${inputs.size} inputs, precision=$precision"
        }

        return interestRateService.calculateRateBatch(inputs, precision)
    }

    override fun supportsRateType(rateType: RateType): Boolean {
        return interestRateService.supportsRateType(rateType)
    }

    override fun getSupportedRateTypes(): Set<RateType> {
        return interestRateService.getSupportedRateTypes()
    }
}
