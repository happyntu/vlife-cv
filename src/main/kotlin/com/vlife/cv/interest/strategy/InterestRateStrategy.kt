package com.vlife.cv.interest.strategy

import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PlanNote

/**
 * 利率計算策略介面
 *
 * V3 對應：cv210p_rate_value_calculation() 的 rate_type 分派邏輯
 *
 * **設計模式**：Strategy Pattern
 * - 每個 Strategy 對應一個或多個 rate_type
 * - 替代 V3 的 IF/CASE 分派邏輯
 *
 * **實作 Strategy**：
 * - DividendRateStrategy (rate_type '0')
 * - InterestCalcRateStrategy (rate_type '1')
 * - LoanRateStrategy (rate_type '2', '3')
 * - LastMonthRateStrategy (rate_type '4')
 * - FourBankRateStrategy (rate_type '5')
 * - AvgDeclaredRateStrategy (rate_type '8')
 * - FreeLookRateStrategy (rate_type 'A', 'B', 'E')
 * - DepositRateStrategy (rate_type 'C')
 * - AnnuityRateStrategy (rate_type 'D', 'F')
 *
 * @see RateStrategyDispatcher
 */
interface InterestRateStrategy {

    /**
     * 此策略支援的 rate_type 集合
     *
     * 範例：
     * - DividendRateStrategy: setOf(RateType.DIVIDEND_RATE)
     * - LoanRateStrategy: setOf(RateType.LOAN_RATE_MONTHLY, RateType.LOAN_RATE_MONTHLY_V2)
     *
     * @return 支援的 RateType 集合
     */
    fun supportedRateTypes(): Set<RateType>

    /**
     * 執行利率計算
     *
     * V3 對應：各 cv210p_fXX_* procedures
     *
     * **參數說明**：
     * - input: 利率計算輸入（ob_iri）
     * - precision: 精度（0=台幣四捨五入至整數, 2=外幣保留 2 位小數）
     * - plan: 險種定義（ob_pldf），可能為 null
     * - planNote: 投資型擴充（ob_plnt），可能為 null
     *
     * **回傳**：
     * - InterestRateCalculationResult（包含 actualRate, intAmt, monthlyDetails）
     *
     * @param input 利率計算輸入
     * @param precision 精度（0=台幣, 2=外幣）
     * @param plan 險種定義（可能為 null）
     * @param planNote 投資型擴充（可能為 null）
     * @return 計算結果
     */
    fun calculate(
        input: InterestRateInput,
        precision: Int,
        plan: Pldf? = null,
        planNote: PlanNote? = null
    ): InterestRateCalculationResult
}
