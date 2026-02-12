package com.vlife.cv.rate

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Quote Rate Entity (CV.QTPRT)
 *
 * Represents insurance quote premium rate data for target premium codes.
 * Used for premium quotation calculations before policy issuance.
 *
 * V3 Table: V3.QTPRT
 * V4 Schema: CV.QTPRT (ADR-009)
 * Owner: vlife-cv
 *
 * Primary Key: (targetPremCode, targetType, strDate, endDate, rateAge, rateSex, qtprtType) - 7 fields composite key
 *
 * @property targetPremCode Target premium code, max 5 chars
 * @property targetType Target type code (A=annual, ...), 1 char
 * @property strDate Effective start date (NOT NULL)
 * @property endDate Effective end date (NOT NULL)
 * @property rateAge Rate age (insured age at quotation)
 * @property rateSex Rate sex code (0=no distinction, 1=male, 2=female), 1 char
 * @property annualPrem Annual premium per 10,000 coverage amount (NOT NULL)
 * @property qtprtType Quote rate type code (1=standard, ...), 1 char
 */
data class QuoteRate(
    // === Primary Key (7 fields) ===
    val targetPremCode: String,        // TARGET_PREM_CODE (PK)
    val targetType: String,            // TARGET_TYPE (PK)
    val strDate: LocalDate,            // STR_DATE (PK)
    val endDate: LocalDate,            // END_DATE (PK)
    val rateAge: Int,                  // RATE_AGE (PK)
    val rateSex: String,               // RATE_SEX (PK)
    val qtprtType: String,             // QTPRT_TYPE (PK)

    // === Rate Field (1 field) ===
    val annualPrem: BigDecimal         // ANNUAL_PREM (NOT NULL)
)

/**
 * Quote Rate composite primary key
 *
 * Used for update and delete operations where the full key is needed.
 */
data class QuoteRateKey(
    val targetPremCode: String,
    val targetType: String,
    val strDate: LocalDate,
    val endDate: LocalDate,
    val rateAge: Int,
    val rateSex: String,
    val qtprtType: String
)

/**
 * Quote Rate query DTO
 *
 * Encapsulates effective date-based query parameters.
 *
 * @property targetPremCode Target premium code
 * @property targetType Target type
 * @property rateAge Rate age
 * @property rateSex Rate sex
 * @property qtprtType Quote rate type
 * @property effectiveDate Effective date for date range query (defaults to today)
 */
data class QuoteRateQuery(
    val targetPremCode: String,
    val targetType: String,
    val rateAge: Int,
    val rateSex: String,
    val qtprtType: String,
    val effectiveDate: LocalDate = LocalDate.now()
)

/**
 * Simplified DTO for REST API response
 *
 * Contains all fields for quote rate.
 */
data class QuoteRateDto(
    val targetPremCode: String,
    val targetType: String,
    val strDate: LocalDate,
    val endDate: LocalDate,
    val rateAge: Int,
    val rateSex: String,
    val qtprtType: String,
    val annualPrem: BigDecimal
)

/**
 * Convert QuoteRate entity to DTO
 */
fun QuoteRate.toDto(): QuoteRateDto = QuoteRateDto(
    targetPremCode = targetPremCode,
    targetType = targetType,
    strDate = strDate,
    endDate = endDate,
    rateAge = rateAge,
    rateSex = rateSex,
    qtprtType = qtprtType,
    annualPrem = annualPrem
)
