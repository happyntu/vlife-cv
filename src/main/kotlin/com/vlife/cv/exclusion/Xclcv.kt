package com.vlife.cv.exclusion

import java.time.LocalDate

/**
 * XCLCV - 理賠現金價值暫存檔 Entity
 * 對應 CV.XCLCV 表格（10 欄位）
 */
data class Xclcv(
    val id: Long,                        // XCLCV_SERIAL (PK)
    val policyNo: String,                // POLICY_NO
    val claimNo: String,                 // CLAIM_NO
    val coverageNo: Int,                 // COVERAGE_NO
    val benefCode: String,               // BENEF_CODE
    val eventDateS: LocalDate,           // EVENT_DATE_S
    val processDate: LocalDate,          // PROCESS_DATE
    val clbfRvfInd: String,              // CLBF_RVF_IND
    val codtStatusCode: String,          // CODT_STATUS_CODE
    val codtStatusCode2: String?         // CODT_STATUS_CODE2 (唯一可空)
)
