package com.vlife.cv.rate

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 分帳利率參照表 (QIRAT) Entity
 * V4 Schema: CV.QIRAT
 * 主鍵：(SUB_ACNT_PLAN_CODE, INT_RATE_TYPE, INT_RATE_DATE_STR)
 */
data class SubAccountInterestRate(
    val subAcntPlanCode: String,           // SUB_ACNT_PLAN_CODE (PK) VARCHAR2(4)
    val intRateType: String,               // INT_RATE_TYPE (PK) VARCHAR2(2)
    val intRateDateStr: LocalDate,         // INT_RATE_DATE_STR (PK) DATE NOT NULL
    val intRateDateEnd: LocalDate?,        // INT_RATE_DATE_END DATE
    val intRate: BigDecimal?               // INT_RATE NUMBER(7,4)
) {
    companion object {
        const val TYPE_ANNIVERSARY = "5"
        const val TYPE_LINKED_BOND = "6"
        val INFINITE_END_DATE: LocalDate = LocalDate.of(9999, 12, 31)
    }
}
