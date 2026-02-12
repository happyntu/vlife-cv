package com.vlife.cv.actuarial

/**
 * 價值區塊查詢條件 DTO
 *
 * 封裝 V3 f99_get_uvalblb 的 11 個參數
 * 核心邏輯：依 planRelationSub 決定查詢 UVALBLB（標準體）或 UVALBLBS（多被保人）
 */
data class ValueBlockQuery(
    val planCode: String,                 // 險種代碼
    val version: String,                  // 版本
    val sex: String,                      // 性別
    val age: Int,                         // 年齡
    val rateSub1: String? = null,         // 費率次代碼1（多被保人用）
    val rateSub2: String? = null,         // 費率次代碼2（多被保人用）
    val uvSub1: String? = null,           // 精算次鍵1（null → '0'）
    val uvSub2: String? = null,           // 精算次鍵2（null → '0'）
    val recordType: String,               // 記錄類型
    val planRelationSub: String? = null   // 計畫關係（null/'0'/'5'=標準體，其他=多被保人）
) {
    /** 是否為標準體查詢 */
    val isStandard: Boolean
        get() = planRelationSub == null ||
                planRelationSub == "0" ||
                planRelationSub == "5"

    /** NVL 邏輯：null → '0' */
    val effectiveUvSub1: String get() = uvSub1 ?: "0"
    val effectiveUvSub2: String get() = uvSub2 ?: "0"

    /** 多被保人性別碼（左補零 2 位） */
    val sexSubPadded: String get() = (rateSub1 ?: "0").padStart(2, '0')

    /** 多被保人年齡碼（左補零 3 位） */
    val ageSubPadded: String get() = (rateSub2 ?: "0").padStart(3, '0')
}
