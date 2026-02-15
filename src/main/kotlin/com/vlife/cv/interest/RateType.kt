package com.vlife.cv.interest

/**
 * 利率類型列舉
 *
 * V3 對應：ob_iri.rate_type (VARCHAR2(1))
 * 支援 13 種利率計算方式，涵蓋傳統型與投資型保單
 *
 * @property code V3 利率類型代碼（單字元）
 * @property description 中文說明
 *
 * @see InterestRateInput
 */
enum class RateType(val code: String, val description: String) {
    /**
     * rate_type '0' - 分紅利率（月加權平均）
     * V3: cv210p_f10_select_div / cv210p_f10_select_div_G
     */
    DIVIDEND_RATE("0", "分紅利率（月加權）"),

    /**
     * rate_type '1' - 計息利率（日加權，包含利息金額計算）
     * V3: cv210p_f12_select_div
     */
    INTEREST_CALC_RATE("1", "計息利率（日加權）"),

    /**
     * rate_type '2' - 貸款利率（各月）
     * V3: cv210p_f20_select_loan / cv210p_fa0_select_loan
     */
    LOAN_RATE_MONTHLY("2", "貸款利率（各月）"),

    /**
     * rate_type '3' - 貸款利率（各月變體）
     * V3: cv210p_f20_select_loan / cv210p_fa0_select_loan
     */
    LOAN_RATE_MONTHLY_V2("3", "貸款利率（各月變體）"),

    /**
     * rate_type '4' - 貸款利率（最後月）
     * V3: cv210p_f30_select_loan_2 / cv210p_fb0_select_loan
     */
    LOAN_RATE_LAST_MONTH("4", "貸款利率（最後月）"),

    /**
     * rate_type '5' - 四行庫利率
     * V3: cv210p_f20_select_loan → cv210p_f40_select_loan (兩步驟)
     */
    FOUR_BANK_RATE("5", "四行庫利率"),

    /**
     * rate_type '8' - 宣告利率 12 月平均
     * V3: cv210p_f50_select_avg_rate（需 insurance_type_3='I'）
     */
    AVG_DECLARED_RATE("8", "宣告利率 12 月平均"),

    /**
     * rate_type 'A' - Free-Look 利息
     * V3: cv210p_rate_calc_A / cv210p_fc0_select_rate
     */
    FREE_LOOK_A("A", "Free-Look 利息"),

    /**
     * rate_type 'B' - Free-Look 利息（變體）
     * V3: cv210p_rate_calc_B / cv210p_fc0_select_rate
     */
    FREE_LOOK_B("B", "Free-Look 利息（變體）"),

    /**
     * rate_type 'C' - 現金價值利率（存款類日加權）
     * V3: cv210p_rate_calc_C / cv210p_rate_calc_G (insurance_type_3 G/H)
     */
    DEPOSIT_RATE("C", "現金價值利率（存款類）"),

    /**
     * rate_type 'D' - 現金價值利率（企業年金）
     * V3: cv210p_rate_calc_G
     */
    ANNUITY_RATE_D("D", "現金價值利率（企業年金）"),

    /**
     * rate_type 'E' - 投資收益利率
     * V3: cv210p_rate_calc_B / cv210p_fc0_select_rate
     */
    FREE_LOOK_E("E", "投資收益利率"),

    /**
     * rate_type 'F' - 複利計算（企業年金）
     * V3: cv210p_rate_calc_G（使用 POWER 複利公式）
     */
    COMPOUND_RATE("F", "複利計算（企業年金）");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        /**
         * 從 V3 rate_type 代碼轉換為 RateType
         *
         * @param code V3 rate_type 代碼（如 "0", "1", "A"）
         * @return 對應的 RateType，若無效代碼則返回 null
         *
         * 注意：L2 內部呼叫回傳 null 以保持 V3 相容行為（CV210P-R-013）。
         * L3 REST API 使用 @Pattern 白名單驗證，無效代碼直接回 400。
         */
        fun fromCode(code: String?): RateType? = code?.let { codeMap[it] }

        /**
         * 投資型適用的 rate_type 集合
         * V3: cv210p_rate_calc 的 regexp_like [234ABCDEF]
         */
        val INVESTMENT_TYPES: Set<RateType> = setOf(
            LOAN_RATE_MONTHLY, LOAN_RATE_MONTHLY_V2, LOAN_RATE_LAST_MONTH,
            FREE_LOOK_A, FREE_LOOK_B, DEPOSIT_RATE,
            ANNUITY_RATE_D, FREE_LOOK_E, COMPOUND_RATE
        )

        /**
         * Free-Look 類型集合
         * V3: cv210p_rate_calc 的 rate_type IN ('A','B','E')
         */
        val FREE_LOOK_TYPES: Set<RateType> = setOf(FREE_LOOK_A, FREE_LOOK_B, FREE_LOOK_E)

        /**
         * 僅限投資型使用的 rate_type 碼（A/B/C/D/E/F）
         *
         * 非投資型（insurance_type_3 NOT IN [FGH]）遇到這些 rate_type 時，
         * V3 會 fallback 至 f20（pk_cv_cv210p.pck lines 2076-2078）
         *
         * 參照：CV210P-R-011
         */
        val INVESTMENT_ONLY_CODES: Set<RateType> = setOf(
            FREE_LOOK_A, FREE_LOOK_B, DEPOSIT_RATE,
            ANNUITY_RATE_D, FREE_LOOK_E, COMPOUND_RATE
        )
    }
}
