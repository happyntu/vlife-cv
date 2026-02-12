package com.vlife.cv.dividend

import java.math.BigDecimal
import java.time.LocalDate

/**
 * CVDP (紅利分配保單明細預估檔) Entity
 * 對應 V3.CVDP 表格，共 15 個欄位
 * 與 CVD8 為姊妹表，共享 PK 結構 (SERIAL_YEAR3 + POLICY_NO + COVERAGE_NO)
 */
data class Cvdp(
    // === 主鍵（3 欄位）===
    val serialYear3: String,           // SERIAL_YEAR3 (PK) - 序號年度
    val policyNo: String,              // POLICY_NO (PK) - 保單號碼
    val coverageNo: Int,               // COVERAGE_NO (PK) - 險種序號

    // === 險種識別（2 欄位）===
    val planCode: String?,             // PLAN_CODE - 險種代碼
    val version: String?,              // VERSION - 版本號

    // === 紅利分配比率（3 欄位）===
    val deathRatio: BigDecimal?,       // DEATH_RATIO - 死差比率 NUMBER(7,6)
    val rateRatio: BigDecimal?,        // RATE_RATIO - 費率比率 NUMBER(7,6)
    val loadingRatio: BigDecimal?,     // LOADING_RATIO - 附加費比率 NUMBER(7,6)

    // === 紅利計算值（4 欄位）===
    val deathDivValue: BigDecimal?,    // DEATH_DIV_VALUE - 死差紅利值 NUMBER(16,2)
    val intDivValue: BigDecimal?,      // INT_DIV_VALUE - 利差紅利值 NUMBER(16,2)
    val expenDivValue: BigDecimal?,    // EXPEN_DIV_VALUE - 費差紅利值 NUMBER(16,2)
    val divAmt: BigDecimal?,           // DIV_AMT - 紅利金額 NUMBER(12,2)

    // === 處理資訊（3 欄位）===
    val cvdpCode: String?,             // CVDP_CODE - CVDP 代碼
    val divDate: LocalDate?,           // DIV_DATE - 紅利日期
    val processDate: LocalDate?        // PROCESS_DATE - 處理日期
)
