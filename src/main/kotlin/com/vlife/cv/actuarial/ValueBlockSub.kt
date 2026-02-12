package com.vlife.cv.actuarial

/**
 * 價值區塊-多被保人 Entity (UVALBLBS)
 *
 * 對應 V3.UVALBLBS 表格 (574,300 筆)
 * 主鍵：9 欄位複合鍵
 * 存儲多被保人（次標準體）的精算值區塊
 * 相比 UVALBLB 額外包含 sex_sub 和 age_sub 欄位
 */
data class ValueBlockSub(
    // === 主鍵 (9 欄位) ===
    val planCode: String,          // PLAN_CODE - 險種代碼
    val version: String,           // VERSION - 版本
    val sex: String,               // SEX - 性別
    val age: Int,                  // AGE - 年齡
    val sexSub: String,            // SEX_SUB - 次被保人性別碼 (左補零2位)
    val ageSub: String,            // AGE_SUB - 次被保人年齡碼 (左補零3位)
    val uvSub1: String,            // UV_SUB_1 - 精算次鍵1
    val uvSub2: String,            // UV_SUB_2 - 精算次鍵2
    val recordType: String,        // RECORD_TYPE - 記錄類型

    // === 精算值陣列 ===
    val values: List<Double?>      // VALUE1 - 精算值陣列（最多 116 個元素）
)
