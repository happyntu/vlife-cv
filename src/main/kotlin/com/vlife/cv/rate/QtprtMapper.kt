package com.vlife.cv.rate

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDate

/**
 * QTPRT Quote Rate Mapper
 *
 * MyBatis mapper for CV.QTPRT table operations.
 *
 * Adheres to:
 * - ADR-016: SQL must be in XML, no @Select/@Insert/@Update/@Delete annotations
 * - ADR-017: Mapper naming follows table-oriented convention (QtprtMapper)
 */
@Mapper
interface QtprtMapper {

    // ===== Query Operations =====

    /**
     * Find rate by primary key
     *
     * @param targetPremCode Target premium code
     * @param targetType Target type
     * @param strDate Effective start date
     * @param endDate Effective end date
     * @param rateAge Rate age
     * @param rateSex Rate sex
     * @param qtprtType Quote rate type
     * @return Quote rate record, or null if not found
     */
    fun findByPrimaryKey(
        @Param("targetPremCode") targetPremCode: String,
        @Param("targetType") targetType: String,
        @Param("strDate") strDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("rateAge") rateAge: Int,
        @Param("rateSex") rateSex: String,
        @Param("qtprtType") qtprtType: String
    ): QuoteRate?

    /**
     * Find rate by effective date (most common query)
     *
     * Finds rate where effectiveDate is between strDate and endDate.
     *
     * @param targetPremCode Target premium code
     * @param targetType Target type
     * @param rateAge Rate age
     * @param rateSex Rate sex
     * @param qtprtType Quote rate type
     * @param effectiveDate Effective date (defaults to today)
     * @return Quote rate record, or null if not found
     */
    fun findByEffectiveDate(
        @Param("targetPremCode") targetPremCode: String,
        @Param("targetType") targetType: String,
        @Param("rateAge") rateAge: Int,
        @Param("rateSex") rateSex: String,
        @Param("qtprtType") qtprtType: String,
        @Param("effectiveDate") effectiveDate: LocalDate
    ): QuoteRate?

    /**
     * Find all rates by target premium code
     *
     * @param targetPremCode Target premium code
     * @return List of quote rate records
     */
    fun findByTargetPremCode(
        @Param("targetPremCode") targetPremCode: String
    ): List<QuoteRate>

    // ===== Count Operations =====

    /**
     * Count by primary key
     *
     * @return Number of matching records
     */
    fun countByPrimaryKey(
        @Param("targetPremCode") targetPremCode: String,
        @Param("targetType") targetType: String,
        @Param("strDate") strDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("rateAge") rateAge: Int,
        @Param("rateSex") rateSex: String,
        @Param("qtprtType") qtprtType: String
    ): Int

    // ===== Modification Operations =====

    /**
     * Insert new quote rate record
     *
     * @param entity Quote rate entity
     */
    fun insert(entity: QuoteRate)

    /**
     * Update quote rate record (by 7-field primary key)
     *
     * @param key Primary key
     * @param entity Updated quote rate entity
     */
    fun update(
        @Param("key") key: QuoteRateKey,
        @Param("entity") entity: QuoteRate
    )

    /**
     * Delete quote rate record (by 7-field primary key)
     *
     * @param targetPremCode Target premium code
     * @param targetType Target type
     * @param strDate Effective start date
     * @param endDate Effective end date
     * @param rateAge Rate age
     * @param rateSex Rate sex
     * @param qtprtType Quote rate type
     */
    fun deleteByPrimaryKey(
        @Param("targetPremCode") targetPremCode: String,
        @Param("targetType") targetType: String,
        @Param("strDate") strDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("rateAge") rateAge: Int,
        @Param("rateSex") rateSex: String,
        @Param("qtprtType") qtprtType: String
    )
}
