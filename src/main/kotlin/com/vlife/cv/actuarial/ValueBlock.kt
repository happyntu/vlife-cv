package com.vlife.cv.actuarial

/**
 * 價值區塊-標準體 Entity (UVALBLB)
 *
 * 對應 V3.UVALBLB 表格 (5,875,400 筆)
 * 主鍵：7 欄位複合鍵
 * 存儲標準體被保人的精算值區塊（各年度解約金係數、死亡率等）
 */
data class ValueBlock(
    // === 主鍵 (7 欄位) ===
    val planCode: String,          // PLAN_CODE - 險種代碼
    val version: String,           // VERSION - 版本
    val sex: String,               // SEX - 性別
    val age: Int,                  // AGE - 年齡
    val uvSub1: String,            // UV_SUB_1 - 精算次鍵1
    val uvSub2: String,            // UV_SUB_2 - 精算次鍵2
    val recordType: String,        // RECORD_TYPE - 記錄類型

    // === 精算值陣列 ===
    val values: List<Double?>      // VALUE1 - 精算值陣列（最多 116 個元素）
)
