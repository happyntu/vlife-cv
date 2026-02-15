package com.vlife.cv.interest

/**
 * 利率計算常數
 *
 * 集中管理 CV210P 利率計算相關常數，避免重複定義。
 *
 * **使用模組**：
 * - InterestCalcRateStrategy（計息利率）
 * - LoanRateStrategy（貸款利率）
 * - 其他需要相同精度設定的 Strategy
 *
 * @see InterestCalcRateStrategy
 * @see LoanRateStrategy
 */
object InterestRateConstants {

    /**
     * 利率精度：10 位小數
     *
     * 用於萬分率計算，確保精度足夠不丟失精度。
     *
     * V3 對應：所有 rate 計算均使用 FLOAT (雙精度浮點數)
     */
    const val RATE_SCALE = 10

    /**
     * 金額計算精度：10 位小數
     *
     * 用於中間運算階段，最終 ROUND 至實際精度（台幣 0 位，外幣 2 位）。
     *
     * V3 對應：所有 int_amt 計算均使用 NUMBER (無限精度)
     */
    const val AMOUNT_SCALE = 10
}
