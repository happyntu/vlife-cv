package com.vlife.cv.rate

import java.math.BigDecimal

/**
 * PRAT01 (Plan Rate 01) - 費率檔01 Entity
 *
 * 險種費率差異表，儲存各險種、各版本、各性別、各年齡對應的費率差異資料。
 * 保費計算流程：查詢 PRAT01 取得基礎費率差異值 → 結合保額計算年繳保費。
 *
 * V3 Table: V3.PRAT01
 * V4 Schema: CV.PRAT01 (ADR-009)
 * Owner: vlife-cv
 * V3 Package: PK_LIB_PRAT01PROC
 *
 * 資料量: 230,286 筆
 * 快取策略: Caffeine (TTL 24hr, 最大 50,000 筆)
 *
 * Primary Key: (planCode, version, rateSex, rateSub1, rateSub2, rateAge) - 6 fields composite key
 *
 * @property planCode 險種代碼, max 5 chars, NOT NULL
 * @property version 版本號, max 1 char, NOT NULL
 * @property rateSex 費率性別 (0=無關, 1=男, 2=女), max 1 char, NOT NULL
 * @property rateSub1 費率子分類 1 (繳費年期代碼), max 2 chars, NOT NULL
 * @property rateSub2 費率子分類 2 (體況分類), max 3 chars, NOT NULL
 * @property rateAge 費率年齡 (0~120), NOT NULL
 * @property prem6Diff 半年繳保費差額, NUMBER(18,8), NOT NULL
 * @property prem3Diff 季繳保費差額, NUMBER(18,8), NOT NULL
 * @property prem1Diff 月繳保費差額, NUMBER(18,8), NOT NULL
 */
data class PlanRate01(
    // === 複合主鍵（6 欄位）===
    val planCode: String,              // PLAN_CODE (PK) - 險種代碼 VARCHAR2(5)
    val version: String,               // VERSION (PK) - 版本號 VARCHAR2(1)
    val rateSex: String,               // RATE_SEX (PK) - 費率性別 VARCHAR2(1)
    val rateSub1: String,              // RATE_SUB_1 (PK) - 費率子分類 1 VARCHAR2(2)
    val rateSub2: String,              // RATE_SUB_2 (PK) - 費率子分類 2 VARCHAR2(3)
    val rateAge: Int,                  // RATE_AGE (PK) - 費率年齡 NUMBER(5)

    // === 費率差異值（3 欄位）===
    val prem6Diff: BigDecimal,         // PREM6_DIFF - 半年繳保費差額 NUMBER(18,8)
    val prem3Diff: BigDecimal,         // PREM3_DIFF - 季繳保費差額 NUMBER(18,8)
    val prem1Diff: BigDecimal          // PREM1_DIFF - 月繳保費差額 NUMBER(18,8)
)

/**
 * PRAT01 複合主鍵參數物件（避免六欄位主鍵傳參錯誤）
 *
 * 用途：
 * 1. 查詢方法參數封裝
 * 2. 快取 Key 生成
 * 3. 避免傳參順序錯誤
 */
data class PlanRate01Key(
    val planCode: String,
    val version: String,
    val rateSex: String,
    val rateSub1: String,
    val rateSub2: String,
    val rateAge: Int
)
