package com.vlife.cv.exclusion

import java.time.LocalDate

/**
 * XCLDS - 理賠給付特殊日期記錄檔 Entity
 * 對應 CV.XCLDS 表格（5 欄位，全部 NOT NULL）
 */
data class Xclds(
    val policyNo: String,          // POLICY_NO (PK)
    val coverageNo: Int,           // COVERAGE_NO (PK)
    val xcldsType: String,         // XCLDS_TYPE (PK)
    val xcldsDate: LocalDate,      // XCLDS_DATE
    val referenceCode: String      // REFERENCE_CODE
) {
    companion object {
        const val TYPE_DEATH = "2"
        const val TYPE_ACCIDENT = "1"
        const val TYPE_DISABILITY = "3"
    }
}
