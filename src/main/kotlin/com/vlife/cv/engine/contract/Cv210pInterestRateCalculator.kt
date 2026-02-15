package com.vlife.cv.engine.contract

import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PlanNote

/**
 * CV210P 利率計算引擎契約介面（L2 Engine Contract）
 *
 * V3 對應：pk_cv_cv210p.pck（精算利率計算引擎）
 * - cv210p_rate_value_calculation (lines 1944-2091)：主要入口
 * - cv210p_rate_calc (lines 1877-1943)：rate_type 分派
 * - cv210p_f20_select_loan (lines 948-1107)：貸款利率計算
 * - cv210p_f12_select_div (lines 756-864)：計息利率計算
 * - 其他 f0x / rate_calc_x 輔助函式
 *
 * **設計原則**（ADR-027）：
 * - L2 層：同模組不同引擎間的契約介面
 * - 保留 V3 Package 前綴（Cv210p）以利追溯
 * - 透過 @ConditionalOnMissingBean 支援 Stub → Impl 零修改替換
 *
 * **使用場景**：
 * - CV208P（年金險精算）→ CV210P（利率計算）
 * - CV211P（保單價值）→ CV210P（利率計算）
 * - CV218P（紅利計算）→ CV210P（利率計算）
 *
 * **實作替換機制**：
 * 1. 開發階段：StubCv210pInterestRateCalculator（回傳 zero）
 * 2. 實作完成：Cv210pInterestRateCalculatorImpl（注入 InterestRateService）
 *
 * @see com.vlife.cv.engine.stub.StubCv210pInterestRateCalculator
 * @see com.vlife.cv.engine.impl.Cv210pInterestRateCalculatorImpl
 * @see com.vlife.cv.engine.config.Cv210pEngineConfig
 */
interface Cv210pInterestRateCalculator {

    /**
     * 計算利率與利息
     *
     * V3 對應：cv210p_rate_value_calculation (pk_cv_cv210p.pck lines 1944-2091)
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
    ): InterestRateCalculationResult

    /**
     * 批量計算利率
     *
     * V3 對應：多次呼叫 cv210p_rate_value_calculation（無批次 API）
     *
     * @param inputs 利率計算輸入清單
     * @param precision 精度（0=台幣, 2=外幣）
     * @return 計算結果清單
     */
    fun calculateRateBatch(
        inputs: List<InterestRateInput>,
        precision: Int = 0
    ): List<InterestRateCalculationResult>

    /**
     * 檢查是否支援指定的 rate_type
     *
     * V3 對應：cv210p_rate_calc 中的 IF/CASE 邏輯（lines 1877-1943）
     *
     * @param rateType 利率類型
     * @return true 如果支援，否則 false
     */
    fun supportsRateType(rateType: RateType): Boolean

    /**
     * 取得所有支援的 rate_types
     *
     * V3 對應：支援 13 種 rate_type ('0'-'5', '8', 'A'-'F')
     *
     * @return 支援的利率類型集合
     */
    fun getSupportedRateTypes(): Set<RateType>
}
