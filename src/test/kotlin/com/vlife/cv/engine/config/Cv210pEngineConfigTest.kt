package com.vlife.cv.engine.config

import com.vlife.cv.engine.contract.Cv210pInterestRateCalculator
import com.vlife.cv.engine.stub.StubCv210pInterestRateCalculator
import com.vlife.cv.interest.RateType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Cv210pEngineConfig 單元測試
 *
 * **測試目標**：驗證 ADR-027 零修改替換機制（Stub 注入邏輯）
 *
 * **測試策略**：
 * - 純單元測試（不啟動 Spring 上下文）
 * - 直接呼叫配置方法驗證 Bean 建立邏輯
 * - 驗證 Stub 可正常建立與呼叫
 *
 * **驗證項目**：
 * - Cv210pEngineConfig.cv210pInterestRateCalculator() 可正常建立 Stub
 * - Stub 方法可正常呼叫
 * - Stub 返回預期值（13 種 RateType）
 *
 * **注意**：
 * - Impl 優先注入（@Primary）+ Stub fallback（@ConditionalOnMissingBean）的整合測試
 *   需要完整資料庫環境，留待整合測試階段驗證（使用 TestContainers）
 */
@DisplayName("Cv210pEngineConfig 單元測試")
class Cv210pEngineConfigTest {

    /**
     * 測試：直接呼叫配置方法建立 Stub
     *
     * **驗證邏輯**：
     * - Cv210pEngineConfig.cv210pInterestRateCalculator() 可正常建立 Bean
     * - 返回實例為 StubCv210pInterestRateCalculator
     *
     * **ADR-027 驗證**：配置方法邏輯正確
     */
    @Test
    fun `should create Stub calculator via config method`() {
        // Given
        val config = Cv210pEngineConfig()

        // When
        val calculator = config.cv210pInterestRateCalculator()

        // Then
        assertTrue(
            calculator is StubCv210pInterestRateCalculator,
            "Expected StubCv210pInterestRateCalculator but got ${calculator::class.simpleName}"
        )
    }

    /**
     * 測試：驗證 Stub Calculator 可正常呼叫
     *
     * **驗證邏輯**：
     * - Stub Bean 可正常建立
     * - supportsRateType() 方法可正常呼叫
     * - getSupportedRateTypes() 方法可正常呼叫
     * - 返回預期的 Stub 值（13 種 RateType）
     */
    @Test
    fun `should be able to call Stub calculator methods`() {
        // Given
        val config = Cv210pEngineConfig()
        val calculator = config.cv210pInterestRateCalculator()

        // When
        val supportedRateTypes = calculator.getSupportedRateTypes()
        val supportsLoanRate = calculator.supportsRateType(RateType.LOAN_RATE_MONTHLY)

        // Then
        assertTrue(
            supportedRateTypes.size == 13,
            "Expected 13 supported rate types but got ${supportedRateTypes.size}"
        )
        assertTrue(
            supportsLoanRate,
            "Expected Stub to support LOAN_RATE_MONTHLY"
        )
    }

    /**
     * 測試：驗證所有 13 種 RateType 都被 Stub 支援
     *
     * **驗證邏輯**：
     * - Stub 應支援所有 13 種 RateType（Stub 的預設行為）
     */
    @Test
    fun `Stub should support all 13 rate types`() {
        // Given
        val config = Cv210pEngineConfig()
        val calculator = config.cv210pInterestRateCalculator()

        // When
        val supportedRateTypes = calculator.getSupportedRateTypes()

        // Then
        val allRateTypes = RateType.entries.toSet()
        assertTrue(
            supportedRateTypes == allRateTypes,
            "Expected Stub to support all ${allRateTypes.size} rate types"
        )
    }
}
