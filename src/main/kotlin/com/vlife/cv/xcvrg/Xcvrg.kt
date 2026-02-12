package com.vlife.cv.xcvrg

/**
 * XCVRG - 跨保障年齡替換表
 * 對應 CV.XCVRG 表格（5 欄位）
 */
data class Xcvrg(
    val replaceAgeCode: String,    // REPLACE_AGE_CODE (PK)
    val rateSex: String,           // RATE_SEX (PK)
    val rateAge: Int,              // RATE_AGE (PK)
    val rateSubAge: Int,           // RATE_SUB_AGE (PK)
    val rateAgeChgInd: Int         // RATE_AGE_CHG_IND
)
