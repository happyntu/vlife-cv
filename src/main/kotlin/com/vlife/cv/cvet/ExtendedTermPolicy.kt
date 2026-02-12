package com.vlife.cv.cvet

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 展期定期保單資料 Entity
 *
 * 對應 V3.CVET 表格。
 * V4 主鍵改為業務主鍵 (POLICY_NO, COVERAGE_NO)，移除 V3 的 SYS_SERIAL (ADR-012 B 類)。
 */
data class ExtendedTermPolicy(
    // === 主鍵 (2 欄) ===
    val policyNo: String,                      // POLICY_NO (PK) - 保單號碼
    val coverageNo: Int,                       // COVERAGE_NO (PK) - 險種序號 (1=主約, 2+=附約)

    // === 展期基本資訊 (3 欄) ===
    val etTso: String? = null,                 // ET_TSO - 展期定期選項代碼
    val etFaceAmt: BigDecimal? = null,         // ET_FACE_AMT - 展期後保額
    val etEffectDate: LocalDate? = null,       // ET_EFFECT_DATE - 展期生效日

    // === 展期期間資訊 (2 欄) ===
    val etExpiredDate: LocalDate? = null,       // ET_EXPIRED_DATE - 展期到期日
    val etPuaDate: LocalDate? = null,          // ET_PUA_DATE - 增額繳清到期日

    // === 金額資訊 (4 欄) ===
    val etPuaValue: BigDecimal? = null,        // ET_PUA_VALUE - 增額繳清價值
    val etLoanAmt: BigDecimal? = null,         // ET_LOAN_AMT - 展期時保單借款餘額
    val etAplAmt: BigDecimal? = null,          // ET_APL_AMT - 展期時自動墊繳餘額
    val etDisabBenef: BigDecimal? = null,      // ET_DISAB_BENEF - 展期時豁免保費價值

    // === 保單類型 (1 欄) ===
    val policyType: String? = null             // POLICY_TYPE - 保單類型
) {
    companion object {
        const val MAIN_COVERAGE = 1
    }
}
