package com.vlife.cv.psbt

import java.math.BigDecimal

/**
 * PSBT - 險種給付參數子表 (Product Sub-Benefit Table)
 *
 * 儲存險種給付相關參數的查閱表格。以險種代碼、版本、性別費率、參數類型、參數代碼為查詢鍵，
 * 透過 KEY1/KEY2 範圍匹配取得對應的給付參數值。
 *
 * V3 表格: V3.PSBT
 * V4 Schema: CV
 *
 * @property planCode 險種代碼 (VARCHAR2(5) NOT NULL, PK1)
 * @property version 版本號 (VARCHAR2(1) NOT NULL, PK2)
 * @property rateSex 性別費率 (VARCHAR2(1) NOT NULL, PK3)
 * @property psbtType 參數類型 (VARCHAR2(1) NOT NULL, PK4)
 * @property psbtCode 參數代碼 (VARCHAR2(4) NOT NULL, PK5)
 * @property psbtKey1 範圍鍵1 下限 (NUMBER(10,0) NOT NULL, PK6)
 * @property psbtKey2 範圍鍵1 上限 (NUMBER(10,0) NOT NULL, PK7)
 * @property psbtKey3 範圍鍵2 下限 (NUMBER(10,0) NOT NULL, PK8)
 * @property psbtKey4 範圍鍵2 上限 (NUMBER(10,0) NOT NULL, PK9)
 * @property psbtValue 參數值 (NUMBER(10,4), nullable)
 */
data class Psbt(
    // === 險種識別（PK 部分） ===
    val planCode: String,
    val version: String,
    val rateSex: String,

    // === 參數識別（PK 部分） ===
    val psbtType: String,
    val psbtCode: String,

    // === 範圍鍵（PK 部分） ===
    val psbtKey1: Long,
    val psbtKey2: Long,
    val psbtKey3: Long,
    val psbtKey4: Long,

    // === 值欄位 ===
    val psbtValue: BigDecimal?
)
