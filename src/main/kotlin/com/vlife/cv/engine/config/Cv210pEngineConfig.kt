package com.vlife.cv.engine.config

import com.vlife.cv.engine.contract.Cv210pInterestRateCalculator
import com.vlife.cv.engine.stub.StubCv210pInterestRateCalculator
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/**
 * CV210P 引擎配置（L2 Engine Contract 配置）
 *
 * **配置原則**（ADR-027）：
 * - 預設注入 Stub（@ConditionalOnMissingBean）
 * - 實作完成後以 @Primary 取代 Stub
 * - 零修改替換：依賴方無需修改程式碼
 *
 * **替換機制**：
 * 1. 開發階段：
 *    - 未啟用 Cv210pInterestRateCalculatorImpl → 注入 Stub
 *    - 依賴方可正常編譯與測試（回傳零值）
 *
 * 2. 實作完成階段：
 *    - Cv210pInterestRateCalculatorImpl 以 @Primary 啟用
 *    - Spring 優先注入 Impl，Stub 自動被忽略
 *    - 依賴方零修改
 *
 * **使用情境**：
 * - CV208P（年金險精算）注入 Cv210pInterestRateCalculator
 * - CV211P（保單價值）注入 Cv210pInterestRateCalculator
 * - CV218P（紅利計算）注入 Cv210pInterestRateCalculator
 *
 * **測試策略**：
 * - 單元測試：Mock Cv210pInterestRateCalculator
 * - 整合測試（不啟用 Impl）：使用 Stub
 * - 整合測試（啟用 Impl）：使用真實 Impl
 *
 * @see Cv210pInterestRateCalculator
 * @see StubCv210pInterestRateCalculator
 * @see com.vlife.cv.engine.impl.Cv210pInterestRateCalculatorImpl
 */
@Configuration
class Cv210pEngineConfig {

    /**
     * 預設注入 Stub（開發階段）
     *
     * @ConditionalOnMissingBean：
     * - 如果 Cv210pInterestRateCalculatorImpl 未啟用 → 注入 Stub
     * - 如果 Cv210pInterestRateCalculatorImpl 已啟用 → 忽略此 Bean
     */
    @Bean
    @ConditionalOnMissingBean(Cv210pInterestRateCalculator::class)
    fun cv210pInterestRateCalculator(): Cv210pInterestRateCalculator {
        logger.info { "Initializing StubCv210pInterestRateCalculator (development mode)" }
        return StubCv210pInterestRateCalculator()
    }
}
