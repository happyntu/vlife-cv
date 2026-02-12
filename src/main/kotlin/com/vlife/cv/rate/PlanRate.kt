package com.vlife.cv.rate

import java.math.BigDecimal

/**
 * Plan Rate Entity (CV.PRAT)
 *
 * Represents insurance premium rate data for different plan codes, versions,
 * and demographic attributes. This is a core table for premium calculation
 * across the VLIFE system.
 *
 * V3 Table: V3.PRAT
 * V4 Schema: CV.PRAT (ADR-009)
 * Owner: vlife-cv
 *
 * Primary Key: (planCode, version, rateSex, rateSub1, rateSub2, rateAge) - 6 fields composite key
 *
 * @property planCode Plan code (insurance product identifier), max 20 chars
 * @property version Version code, max 4 chars
 * @property rateSex Rate sex code (0=no distinction, 1=male, 2=female), max 4 chars
 * @property rateSub1 Rate sub-key 1 (e.g., smoking status), max 8 chars
 * @property rateSub2 Rate sub-key 2 (e.g., occupation class), max 12 chars
 * @property rateAge Rate age (insured age), no precision limit in Oracle
 * @property annualPrem Annual premium per 10,000 coverage amount (NOT NULL)
 * @property annualPrem2 Second annual premium rate (optional)
 * @property employeeDisc Employee discount rate percentage (optional)
 * @property loadingRate2 Loading rate 2 for additional premium (optional)
 */
data class PlanRate(
    // === Primary Key (6 fields) ===
    val planCode: String,              // PLAN_CODE (PK)
    val version: String,               // VERSION (PK)
    val rateSex: String,               // RATE_SEX (PK)
    val rateSub1: String,              // RATE_SUB_1 (PK)
    val rateSub2: String,              // RATE_SUB_2 (PK)
    val rateAge: Int,                  // RATE_AGE (PK)

    // === Rate Fields (4 fields) ===
    val annualPrem: BigDecimal,        // ANNUAL_PREM (NOT NULL)
    val annualPrem2: BigDecimal?,      // ANNUAL_PREM2 (nullable)
    val employeeDisc: BigDecimal?,     // EMPLOYEE_DISC (nullable)
    val loadingRate2: BigDecimal?      // LOADING_RATE2 (nullable)
)

/**
 * Plan Rate composite primary key
 *
 * Used for update and delete operations where the full key is needed.
 */
data class PlanRateKey(
    val planCode: String,
    val version: String,
    val rateSex: String,
    val rateSub1: String,
    val rateSub2: String,
    val rateAge: Int
)

/**
 * Plan Rate query DTO
 *
 * Encapsulates V3 f99_get_prat query parameters.
 * contractNo and rateSexInd are used for group rate switching logic (handled in Service layer).
 *
 * @property planCode Plan code
 * @property version Version
 * @property rateSex Rate sex (original value, may be overridden by rateSexInd)
 * @property rateSub1 Rate sub-key 1
 * @property rateSub2 Rate sub-key 2
 * @property rateAge Rate age
 * @property contractNo Contract number (for group rate determination)
 * @property rateSexInd Plan's rate sex indicator from PLDF (2=no sex distinction)
 */
data class PlanRateQuery(
    val planCode: String,
    val version: String,
    val rateSex: String,
    val rateSub1: String,
    val rateSub2: String,
    val rateAge: Int,
    val contractNo: String? = null,
    val rateSexInd: Int? = null
)

/**
 * Simplified DTO for REST API response
 *
 * Contains only frequently queried fields.
 */
data class PlanRateDto(
    val planCode: String,
    val version: String,
    val rateSex: String,
    val rateSub1: String,
    val rateSub2: String,
    val rateAge: Int,
    val annualPrem: BigDecimal,
    val annualPrem2: BigDecimal?,
    val employeeDisc: BigDecimal?,
    val loadingRate2: BigDecimal?
)

/**
 * Convert PlanRate entity to DTO
 */
fun PlanRate.toDto(): PlanRateDto = PlanRateDto(
    planCode = planCode,
    version = version,
    rateSex = rateSex,
    rateSub1 = rateSub1,
    rateSub2 = rateSub2,
    rateAge = rateAge,
    annualPrem = annualPrem,
    annualPrem2 = annualPrem2,
    employeeDisc = employeeDisc,
    loadingRate2 = loadingRate2
)
