package com.vlife.cv.actuarial

import java.math.BigDecimal

/**
 * 通用價值主表 Entity (UVAL)
 *
 * 對應 V3.UVAL 表格
 * 主鍵：8 欄位複合鍵
 * 存儲死亡率、預定利率等精算基礎參數與單一精算值。
 * Oracle 驗證（2026-02-12）：UVAL_VALUE = NUMBER(20,9)。
 */
data class UniversalValue(
    // === 主鍵 (8 欄位) ===
    val planCode: String,          // PLAN_CODE - 險種代碼
    val version: String,           // VERSION - 版本
    val sex: String,               // SEX - 性別
    val age: Int,                  // AGE - 年齡
    val recordType: String,        // RECORD_TYPE - 記錄類型
    val serialYear: Int,           // SERIAL_YEAR - 序列年度
    val deathRate: String,         // DEATH_RATE - 死亡率代碼
    val uvRvfRate: String,         // UV_RVF_RATE - 解約金利率代碼

    // === 精算值欄位 ===
    val uvalValue: BigDecimal?     // UVAL_VALUE - 單一精算值
)
