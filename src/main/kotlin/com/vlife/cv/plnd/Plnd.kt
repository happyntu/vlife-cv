package com.vlife.cv.plnd

import java.io.Serializable
import java.math.BigDecimal

/**
 * PLND (險種投資標的配置) Entity
 *
 * 對應 V3.PLND 表格
 * 主鍵：4 欄位複合鍵 (PLAN_CODE, VERSION, IV_TARGET_CODE, IV_APPL_IND)
 * 管理投資型保險商品的投資標的分配資訊
 */
data class Plnd(
    val planCode: String,                          // PLAN_CODE (PK+FK) - 險種代碼
    val version: String,                           // VERSION (PK+FK) - 版本號
    val ivTargetCode: String,                      // IV_TARGET_CODE (PK) - 投資標的/紅利代碼
    val ivApplInd: String,                         // IV_APPL_IND (PK) - 適用指示
    val ivPercent: BigDecimal?,                    // IV_PERCENT - 投資比率/配置百分比
    val ivhsCodeC: String?                         // IVHS_CODE_C - 投資標的歷史代碼
)

/**
 * PLND 複合主鍵定義
 * 對應 4 欄位主鍵：(PLAN_CODE, VERSION, IV_TARGET_CODE, IV_APPL_IND)
 */
data class PlndId(
    val planCode: String = "",
    val version: String = "",
    val ivTargetCode: String = "",
    val ivApplInd: String = ""
) : Serializable

/**
 * 簡化 DTO（僅包含高頻查詢欄位）
 * 避免過度傳輸複合主鍵，多數查詢僅需標的資訊
 */
data class PlndDto(
    val planCode: String,
    val version: String,
    val ivTargetCode: String,
    val ivApplInd: String,
    val ivPercent: BigDecimal?,
    val ivhsCodeC: String?
)

fun Plnd.toDto(): PlndDto = PlndDto(
    planCode = planCode,
    version = version,
    ivTargetCode = ivTargetCode,
    ivApplInd = ivApplInd,
    ivPercent = ivPercent,
    ivhsCodeC = ivhsCodeC
)
