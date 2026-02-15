package com.vlife.cv.interest

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 利率計算輸入/輸出容器
 *
 * V3 對應：ob_iri (10 欄位, Oracle all_type_attrs 驗證)
 * 用途：作為 CV210P 利率計算的輸入參數與輸出結果容器
 *
 * **Null Safety**：
 * - `beginDate`, `endDate`, `rateType`, `subAcntPlanCode`, `ivTargetCode` 為可空類型（與 V3 初始化邏輯一致）
 * - `actualRate`, `rateDisc`, `rateSub`, `principalAmt`, `intAmt` 有非空預設值
 *
 * **預設值**：
 * - 預設值遵循 V3 cv210p_init_iri 邏輯（pk_cv_cv210p.pck lines 40-48）
 * - `rateDisc` 預設 100（不折扣），這是唯一的非零預設值
 *
 * @property beginDate 起算日（計算開始日期）
 * @property endDate 迄日（計算結束日期）
 * @property rateType 利率類型（決定使用哪種計算方式）
 * @property actualRate 實際利率（萬分率），計算結果或輸入已知利率
 * @property rateDisc 利率折扣百分比（100 = 不折扣，90 = 打 9 折）
 * @property rateSub 利率減碼值（萬分率）
 * @property principalAmt 本金金額
 * @property subAcntPlanCode 子帳戶代碼（QIRAT 查詢用）
 * @property intAmt 利息金額（計算結果）
 * @property ivTargetCode 投資標的代碼（投資型保單用）
 *
 * @see RateType
 * @see MonthlyRateDetail
 */
data class InterestRateInput(
    /**
     * 起算日
     * V3: ob_iri.begin_date (DATE, 初始化為 NULL)
     */
    val beginDate: LocalDate? = null,

    /**
     * 迄日
     * V3: ob_iri.end_date (DATE, 初始化為 NULL)
     */
    val endDate: LocalDate? = null,

    /**
     * 利率類型
     * V3: ob_iri.rate_type (VARCHAR2(1), 初始化為 NULL)
     */
    val rateType: RateType? = null,

    /**
     * 實際利率（萬分率）
     * V3: ob_iri.actual_rate (NUMBER, 初始化為 0)
     *
     * 輸入模式：已知利率（actualRate > 0），跳過 QIRAT 查詢
     * 輸出模式：計算完成後的加權平均利率
     */
    val actualRate: BigDecimal = BigDecimal.ZERO,

    /**
     * 利率折扣百分比
     * V3: ob_iri.rate_disc (NUMBER, 初始化為 100)
     *
     * 預設 100 = 不折扣
     * 90 = 打 9 折（利率 × 0.9）
     *
     * 套用順序：先 rateSub 減碼，後 rateDisc 折扣（pk_cv_cv210p.pck lines 928-933）
     */
    val rateDisc: BigDecimal = BigDecimal("100"),

    /**
     * 利率減碼值（萬分率）
     * V3: ob_iri.rate_sub (NUMBER, 初始化為 0)
     *
     * 範例：rate_sub=50 → 利率減少 0.5%
     *
     * 套用順序：先 rateSub 減碼，後 rateDisc 折扣
     */
    val rateSub: BigDecimal = BigDecimal.ZERO,

    /**
     * 本金金額
     * V3: ob_iri.principal_amt (NUMBER, 初始化為 0)
     *
     * 用於利息計算：int_amt = principal_amt × (actualRate / 10000) × (days / yearDays)
     */
    val principalAmt: BigDecimal = BigDecimal.ZERO,

    /**
     * 子帳戶代碼
     * V3: ob_iri.sub_acnt_plan_code (VARCHAR2(5), 初始化為 NULL)
     *
     * QIRAT 查詢用的利率代碼，通常為險種代碼或特定利率計畫代碼
     */
    val subAcntPlanCode: String? = null,

    /**
     * 利息金額（計算結果）
     * V3: ob_iri.int_amt (NUMBER, 初始化為 0)
     *
     * 輸出：計算完成後的利息金額
     */
    val intAmt: BigDecimal = BigDecimal.ZERO,

    /**
     * 投資標的代碼
     * V3: ob_iri.iv_target_code (VARCHAR2(10), 初始化為 NULL)
     *
     * 投資型保單的目標代碼（Design Spec §2.3 第 10 個欄位）
     */
    val ivTargetCode: String? = null
)
