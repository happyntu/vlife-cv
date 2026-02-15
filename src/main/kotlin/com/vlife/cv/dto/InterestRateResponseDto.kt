package com.vlife.cv.dto

import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.MonthlyRateDetail
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 利率計算回應 DTO
 *
 * V3 對應：cv210p_rate_value_calculation 的輸出（pk_cv_cv210p.pck lines 1944+）
 *
 * **用途**：L3 REST API 回應封裝
 *
 * @property actualRate 實際利率（萬分率）
 * @property intAmt 利息金額
 * @property monthlyDetails 月份明細清單
 */
data class InterestRateResponseDto(
    val actualRate: BigDecimal,
    val intAmt: BigDecimal,
    val monthlyDetails: List<MonthlyRateDetailDto>
) {
    companion object {
        /**
         * 從 Domain Object 轉換為 DTO
         */
        fun from(result: InterestRateCalculationResult): InterestRateResponseDto {
            return InterestRateResponseDto(
                actualRate = result.actualRate,
                intAmt = result.intAmt,
                monthlyDetails = result.monthlyDetails.map { MonthlyRateDetailDto.from(it) }
            )
        }
    }
}

/**
 * 月份利率明細 DTO
 *
 * @property strDate 起始日期（nullable，與 V3 一致）
 * @property endDate 結束日期（nullable，與 V3 一致）
 * @property month 月份（YYYYMM，nullable，與 V3 一致）
 * @property days 天數
 * @property iRateOriginal 原始利率（百分率）
 * @property iRate 調整後利率（百分率）
 * @property intAmt 利息金額（該月）
 * @property principalAmt 本金金額
 */
data class MonthlyRateDetailDto(
    val strDate: LocalDate?,
    val endDate: LocalDate?,
    val month: String?,
    val days: Int,
    val iRateOriginal: BigDecimal,
    val iRate: BigDecimal,
    val intAmt: BigDecimal,
    val principalAmt: BigDecimal
) {
    companion object {
        /**
         * 從 Domain Object 轉換為 DTO
         */
        fun from(detail: MonthlyRateDetail): MonthlyRateDetailDto {
            return MonthlyRateDetailDto(
                strDate = detail.strDate,
                endDate = detail.endDate,
                month = detail.month,
                days = detail.days,
                iRateOriginal = detail.iRateOriginal,
                iRate = detail.iRate,
                intAmt = detail.intAmt,
                principalAmt = detail.principalAmt
            )
        }
    }
}

/**
 * 批量利率計算回應 DTO
 *
 * @property results 計算結果清單
 */
data class InterestRateBatchResponseDto(
    val results: List<InterestRateResponseDto>
) {
    companion object {
        /**
         * 從 Domain Objects 轉換為 DTO
         */
        fun from(results: List<InterestRateCalculationResult>): InterestRateBatchResponseDto {
            return InterestRateBatchResponseDto(
                results = results.map { InterestRateResponseDto.from(it) }
            )
        }
    }
}

/**
 * 支援的利率類型回應 DTO
 *
 * @property rateTypes 支援的利率類型清單
 */
data class SupportedRateTypesResponseDto(
    val rateTypes: List<RateTypeDto>
)

/**
 * 利率類型 DTO
 *
 * @property code 代碼
 * @property description 說明
 */
data class RateTypeDto(
    val code: String,
    val description: String
)
