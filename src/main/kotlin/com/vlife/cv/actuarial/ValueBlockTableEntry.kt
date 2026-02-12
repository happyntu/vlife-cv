package com.vlife.cv.actuarial

/**
 * 價值區塊巢狀表項目 DTO
 *
 * V3 f99_get_uvalblb_2 的輸出格式 (nt_rec_uvalblb_view)
 * 將 VARRAY 轉換為逐年度記錄的表格結構
 */
data class ValueBlockTableEntry(
    val planCode: String,
    val version: String,
    val age: Int,
    val sex: String,
    val rateSub1: String?,
    val rateSub2: String?,
    val policyYear: Int,           // 保單年度（1-based）
    val value: Double,             // 該年度的精算值
    val recordType: String
)
