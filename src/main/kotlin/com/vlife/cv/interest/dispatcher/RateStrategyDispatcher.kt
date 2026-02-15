package com.vlife.cv.interest.dispatcher

import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.strategy.InterestRateStrategy
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * 利率策略分派器
 *
 * V3 對應：cv210p_rate_value_calculation + cv210p_rate_calc 的 IF/CASE 邏輯
 *
 * **分派邏輯**：
 * - 依據 `RateType` 選擇對應的 `InterestRateStrategy`
 * - 使用 Spring 自動注入的 Strategy 清單建立映射表
 * - 支援 13 種 rate_type（'0'-'5', '8', 'A'-'F'）
 *
 * **V3 兩層分派邏輯**：
 * 1. 投資型分派（insurance_type_3 IN [F,G,H]）→ 投資型 Strategies
 * 2. 傳統型分派 → 傳統型 Strategies
 *
 * **V4 簡化設計**：
 * - Dispatcher 保持簡單的平面映射（Map<RateType, Strategy>）
 * - 複雜的上下文判斷（insurance_type_3）由 Strategy 內部處理
 * - 例如：DepositRateStrategy 內部判斷是否委派給 AnnuityRateStrategy
 *
 * @see InterestRateStrategy
 */
@Component
class RateStrategyDispatcher(
    private val strategies: List<InterestRateStrategy>
) {

    /**
     * RateType → Strategy 映射表
     *
     * 由 Spring 注入的所有 InterestRateStrategy 實作自動建立。
     * 使用 lazy 初始化以確保所有 Strategy beans 已完成注入。
     */
    private val strategyMap: Map<RateType, InterestRateStrategy> by lazy {
        strategies.flatMap { strategy ->
            strategy.supportedRateTypes().map { rateType -> rateType to strategy }
        }.toMap().also { map ->
            logger.info { "RateStrategyDispatcher initialized with ${map.size} rate types: ${map.keys.map { it.code }}" }
        }
    }

    /**
     * 分派利率計算策略
     *
     * @param rateType 利率類型
     * @return 對應的利率計算策略
     * @throws IllegalArgumentException 當 rateType 無對應策略時
     */
    fun dispatch(rateType: RateType): InterestRateStrategy {
        return strategyMap[rateType]
            ?: throw IllegalArgumentException(
                "不支援的 rate_type: ${rateType.code} (${rateType.description}). " +
                    "已註冊的 rate_types: ${strategyMap.keys.map { it.code }}"
            )
    }

    /**
     * 檢查是否支援指定的 rate_type
     *
     * @param rateType 利率類型
     * @return true 如果有對應策略，否則 false
     */
    fun supports(rateType: RateType): Boolean {
        return strategyMap.containsKey(rateType)
    }

    /**
     * 取得所有已註冊的 rate_types
     *
     * @return 支援的利率類型集合
     */
    fun supportedRateTypes(): Set<RateType> {
        return strategyMap.keys
    }
}
