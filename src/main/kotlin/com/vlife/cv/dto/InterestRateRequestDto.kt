package com.vlife.cv.dto

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 利率計算請求 DTO
 *
 * V3 對應：cv210p_rate_value_calculation 的輸入參數（pk_cv_cv210p.pck lines 1944+）
 *
 * **用途**：L3 REST API 請求封裝
 *
 * @property rateType 利率類型代碼（'0'-'5', '8', 'A'-'F'）
 * @property beginDate 起始日期（計算區間起）
 * @property endDate 結束日期（計算區間迄）
 * @property principalAmt 本金金額
 * @property actualRate 實際利率（萬分率，如已知利率則使用此值）
 * @property rateSub 利率減碼（萬分率）
 * @property rateDisc 利率折扣（百分比，預設 100）
 * @property subAcntPlanCode 子帳戶險種代碼（QIRAT 查詢用）
 * @property ivTargetCode 投資標的代碼（投資型保單用）
 * @property precision 精度（0=台幣, 2=外幣）
 */
data class InterestRateRequestDto(
    val rateType: String,
    val beginDate: LocalDate?,
    val endDate: LocalDate?,
    val principalAmt: BigDecimal = BigDecimal.ZERO,
    val actualRate: BigDecimal = BigDecimal.ZERO,
    val rateSub: BigDecimal = BigDecimal.ZERO,
    val rateDisc: BigDecimal = BigDecimal("100"),
    val subAcntPlanCode: String? = null,
    val ivTargetCode: String? = null,
    val precision: Int = 0
)

/**
 * 批量利率計算請求 DTO
 *
 * @property inputs 利率計算輸入清單
 * @property precision 精度（0=台幣, 2=外幣）
 */
data class InterestRateBatchRequestDto(
    val inputs: List<InterestRateRequestDto>,
    val precision: Int = 0
)
