package com.vlife.cv.interest

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 月份利息明細
 *
 * V3 對應：rec_iri_array (9 欄位, Oracle all_type_attrs 驗證)
 * 用途：記錄各月份的利率與利息計算明細，支援 Parallel Run 比對
 *
 * **設計說明**：
 * - V3 使用 `nt_iri_array` (NESTED TABLE，最多 120 筆 = 10 年)
 * - V4 使用 `List<MonthlyRateDetail>`（無長度限制）
 * - 用於日加權利率計算的逐月明細記錄
 *
 * **Null Safety**：
 * - `strDate`, `endDate`, `month`, `description` 為可空類型（與 V3 初始化一致）
 * - `days`, `iRateOriginal`, `iRate`, `intAmt`, `principalAmt` 有非空預設值（0）
 *
 * @property strDate 月起日（該月份計算起始日）
 * @property endDate 月迄日（該月份計算結束日）
 * @property month 月份標示（格式：YYYY/MM，如 "2025/01"）
 * @property days 天數（該月份計算天數）
 * @property iRateOriginal 優惠前利率（尚未套用 rateSub/rateDisc）
 * @property iRate 優惠後利率（套用 rateSub/rateDisc 後）
 * @property intAmt 利息金額（該月份的利息金額）
 * @property principalAmt 本金（該月份的計算本金）
 * @property description 說明（備註，Parallel Run 比對用）
 *
 * @see InterestRateInput
 * @see InterestRateCalculationResult
 */
data class MonthlyRateDetail(
    /**
     * 月起日
     * V3: rec_iri_array.str_date (DATE, 初始化為 NULL)
     */
    val strDate: LocalDate? = null,

    /**
     * 月迄日
     * V3: rec_iri_array.end_date (DATE, 初始化為 NULL)
     */
    val endDate: LocalDate? = null,

    /**
     * 月份標示
     * V3: rec_iri_array.month (VARCHAR2(7), 初始化為 NULL)
     *
     * 格式：YYYY/MM（如 "2025/01"）
     */
    val month: String? = null,

    /**
     * 天數
     * V3: rec_iri_array.days (SMALLINT, 初始化為 0)
     *
     * 該月份計算天數（可能小於月份實際天數）
     */
    val days: Int = 0,

    /**
     * 優惠前利率（萬分率）
     * V3: rec_iri_array.i_rate_o (FLOAT, 初始化為 0)
     *
     * 尚未套用 rate_sub/rate_disc 的利率（QIRAT 原始值）
     */
    val iRateOriginal: BigDecimal = BigDecimal.ZERO,

    /**
     * 優惠後利率（萬分率）
     * V3: rec_iri_array.i_rate (FLOAT, 初始化為 0)
     *
     * 套用 rate_sub/rate_disc 後的利率
     *
     * 計算公式（pk_cv_cv210p.pck lines 928-933）：
     * 1. 先減碼：rate = rate - rate_sub
     * 2. 後折扣：rate = rate × (rate_disc / 100)
     */
    val iRate: BigDecimal = BigDecimal.ZERO,

    /**
     * 利息金額
     * V3: rec_iri_array.int_amt (NUMBER, 初始化為 0)
     *
     * 該月份的利息金額
     *
     * **注意捨入時機差異**（CV210P-R-004）：
     * - fa0/rate_calc_C：逐月 ROUND
     * - f20：累計後一次 ROUND
     */
    val intAmt: BigDecimal = BigDecimal.ZERO,

    /**
     * 本金
     * V3: rec_iri_array.principal_amt (NUMBER, 初始化為 0)
     *
     * 該月份的計算本金（可能與 InterestRateInput.principalAmt 不同）
     */
    val principalAmt: BigDecimal = BigDecimal.ZERO,

    /**
     * 說明
     * V3: rec_iri_array.desc_1 (VARCHAR2(20), 初始化為 NULL)
     *
     * 備註說明，用於 Parallel Run 比對（CV210P-R-010）
     */
    val description: String? = null
)
