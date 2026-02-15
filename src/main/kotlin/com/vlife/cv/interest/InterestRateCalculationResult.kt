package com.vlife.cv.interest

import java.math.BigDecimal

/**
 * 利率計算結果
 *
 * 封裝 CV210P 利率計算的輸出結果，包含最終利率、利息金額，以及逐月明細（如適用）。
 *
 * **用途**：
 * - 作為 InterestRateService.calculateRate() 的返回值
 * - 提供給 L2 Engine Contract 和 L3 REST API 使用
 *
 * **明細使用場景**：
 * - 日加權利率計算（f12, f20, rate_calc_C 等）：返回逐月明細
 * - 月加權利率計算（f10, f10_G）：僅返回 actualRate，monthlyDetails 為空
 * - 最後月利率計算（f30, fb0）：僅返回 actualRate 與 intAmt，monthlyDetails 為空
 *
 * @property actualRate 加權平均利率（萬分率）
 * @property intAmt 利息金額（計算結果）
 * @property monthlyDetails 月份明細（日加權計算時提供，否則為空列表）
 *
 * @see InterestRateInput
 * @see MonthlyRateDetail
 */
data class InterestRateCalculationResult(
    /**
     * 加權平均利率（萬分率）
     *
     * - 月加權（rate_type '0'）：各月利率的簡單平均
     * - 日加權（rate_type '1', '2', '3' 等）：各月利率依天數加權平均
     * - 12 月平均（rate_type '8'）：前 12 個月宣告利率的簡單平均
     *
     * V3 對應：g_iri.actual_rate（計算完成後）
     */
    val actualRate: BigDecimal,

    /**
     * 利息金額
     *
     * - rate_type '1'（計息利率）：直接計算利息金額
     * - 其他類型：可能為 0（僅計算利率）或依需求計算
     *
     * V3 對應：g_iri.int_amt（計算完成後）
     */
    val intAmt: BigDecimal,

    /**
     * 月份明細
     *
     * - 日加權計算（f12, f20, fa0, rate_calc_C, rate_calc_G 等）：提供逐月明細
     * - 月加權計算（f10, f10_G）：空列表
     * - 最後月利率（f30, fb0）：空列表
     * - 12 月平均（f50）：空列表
     *
     * V3 對應：g_iri_array（計算完成後）
     */
    val monthlyDetails: List<MonthlyRateDetail> = emptyList()
) {
    companion object {
        /**
         * 建立零值結果
         *
         * 用於邊界條件處理（如 begin_date > end_date, plan_code 查無資料等）
         *
         * @return 利率 0、利息 0、無月份明細的結果
         */
        fun zero(): InterestRateCalculationResult =
            InterestRateCalculationResult(
                actualRate = BigDecimal.ZERO,
                intAmt = BigDecimal.ZERO,
                monthlyDetails = emptyList()
            )

        /**
         * 從 InterestRateInput 建立結果
         *
         * 用於將計算完成的 InterestRateInput 轉換為 Result（L2 Impl 使用）
         *
         * @param input 計算完成的 InterestRateInput（含 actualRate 和 intAmt）
         * @param monthlyDetails 月份明細（如有）
         * @return 計算結果
         */
        fun fromInput(
            input: InterestRateInput,
            monthlyDetails: List<MonthlyRateDetail> = emptyList()
        ): InterestRateCalculationResult =
            InterestRateCalculationResult(
                actualRate = input.actualRate,
                intAmt = input.intAmt,
                monthlyDetails = monthlyDetails
            )
    }
}
