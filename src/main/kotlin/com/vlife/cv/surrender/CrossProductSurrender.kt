package com.vlife.cv.surrender

import java.math.BigDecimal

/**
 * XCVSR (Cross Product Cash Value Surrender) - 跨險種解約金評分記錄 Entity
 * 對應 V3 VLIFE_SAS.XCVSR（V4 遷移至 CV.XCVSR），共 3 個欄位
 *
 * 業務說明：
 * - 記錄保單特定險種的解約金評分單位
 * - 主要用於 PS 模組的給付評分計算
 * - 目前無資料（預留未來使用）
 *
 * V3 對應：
 * - 表格：VLIFE_SAS.XCVSR（V3 透過 Synonym V3.XCVSR 存取）
 * - Package：PK_LIB_XCVSRPROC（部署於 LIB + CV 雙模組）
 * - Object View：OV_XCVSR（V4 移除，改用 Kotlin data class）
 *
 * ADR 遵循：
 * - ADR-009：歸屬 CV schema（現金價值/精算範疇）
 * - ADR-012：無 SYS_SERIAL，使用業務主鍵 [POLICY_NO, COVERAGE_NO]
 */
data class CrossProductSurrender(
    /**
     * 保單號碼（主鍵）
     * V3: POLICY_NO VARCHAR2(40) NOT NULL
     */
    val policyNo: String,

    /**
     * 險種序號（主鍵）
     * V3: COVERAGE_NO NUMBER(5) NOT NULL
     */
    val coverageNo: Int,

    /**
     * 評分單位
     * V3: SCORE_RATING_UNIT NUMBER(6,1) NOT NULL
     * 用途：給付評分計算的單位值（PS 模組使用）
     */
    val scoreRatingUnit: BigDecimal
)
