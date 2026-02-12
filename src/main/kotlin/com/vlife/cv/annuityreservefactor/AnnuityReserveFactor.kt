package com.vlife.cv.annuityreservefactor

import java.time.LocalDate

/**
 * 年金準備金因子檔 Entity (QCVRF)
 * V4 Schema: CV.QCVRF (ADR-009)
 * 主鍵：(ANNY_PLAN_CODE, STR_DATE, END_DATE)
 * 欄位數：10
 */
data class AnnuityReserveFactor(
    // === 複合主鍵 (3 欄位) ===
    val annyPlanCode: String,          // ANNY_PLAN_CODE VARCHAR2(5) NOT NULL
    val strDate: LocalDate,            // STR_DATE DATE NOT NULL
    val endDate: LocalDate,            // END_DATE DATE NOT NULL

    // === 期間參數 ===
    val durType: Int,                  // DUR_TYPE NUMBER(5) NOT NULL
    val durYear: Int?,                 // DUR_YEAR NUMBER(5)

    // === 保單準備金因子 ===
    val poRvfDeath: Int,               // PO_RVF_DEATH NUMBER(5) NOT NULL
    val poRvfTso: String,              // PO_RVF_TSO VARCHAR2(5) NOT NULL

    // === 法定準備金因子 ===
    val rvfDeath: Int,                 // RVF_DEATH NUMBER(5) NOT NULL
    val rvfTso: String,                // RVF_TSO VARCHAR2(5) NOT NULL

    // === 利率計畫 ===
    val intPlanCode: String?           // INT_PLAN_CODE VARCHAR2(5)
)

/**
 * 年金準備金因子 DTO
 */
data class AnnuityReserveFactorDto(
    val annyPlanCode: String,
    val strDate: LocalDate,
    val endDate: LocalDate,
    val durType: Int,
    val durYear: Int?,
    val poRvfDeath: Int,
    val poRvfTso: String,
    val rvfDeath: Int,
    val rvfTso: String,
    val intPlanCode: String?
)

fun AnnuityReserveFactor.toDto(): AnnuityReserveFactorDto = AnnuityReserveFactorDto(
    annyPlanCode = annyPlanCode,
    strDate = strDate,
    endDate = endDate,
    durType = durType,
    durYear = durYear,
    poRvfDeath = poRvfDeath,
    poRvfTso = poRvfTso,
    rvfDeath = rvfDeath,
    rvfTso = rvfTso,
    intPlanCode = intPlanCode
)
