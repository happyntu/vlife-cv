package com.vlife.cv.exclusion

import java.time.LocalDate

/**
 * XCLRS - 風險資料庫理賠狀況檔 Entity
 * 對應 CV.XCLRS 表格（8 欄位）
 */
data class Xclrs(
    val xclrsSerial: Long,              // XCLRS_SERIAL (PK) - 系統流水號
    val claimReceNo: String,            // CLAIM_RECE_NO - 理賠案號
    val eventId: String,                // EVENT_ID - 事故者ID
    val claimType: String,              // CLAIM_TYPE - 理賠型態
    val claimTypeSub: String? = null,   // CLAIM_TYPE_SUB - 理賠型態2
    val xclrsCode: String? = null,      // XCLRS_CODE - 項目代碼
    val eventDateS: LocalDate,          // EVENT_DATE_S - 事故確定日
    val processDate: LocalDate          // PROCESS_DATE - 處理日期
)
