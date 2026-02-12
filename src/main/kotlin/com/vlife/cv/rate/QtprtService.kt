package com.vlife.cv.rate

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * QTPRT Quote Rate Service
 *
 * Provides quote rate query and CRUD operations.
 * Cache strategy: Caffeine local cache (ADR-014).
 *
 * Cache Settings:
 * - TTL: 1 hour (quote data may change during business hours)
 * - Max size: 50,000 entries
 * - Key format: "{targetPremCode}:{targetType}:{rateAge}:{rateSex}:{qtprtType}:{effectiveDate}"
 */
@Service
class QtprtService(
    private val qtprtMapper: QtprtMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(QtprtService::class.java)
    }

    // ===== Query Methods =====

    /**
     * Get quote rate by effective date (most common query)
     *
     * Finds rate where effectiveDate is between strDate and endDate.
     *
     * @param query Quote rate query parameters
     * @return Quote rate record, null if not found
     */
    @Cacheable(
        "quoteRate",
        key = "#query.targetPremCode + ':' + #query.targetType + ':' + #query.rateAge + ':' + #query.rateSex + ':' + #query.qtprtType + ':' + #query.effectiveDate"
    )
    fun getQuoteRate(query: QuoteRateQuery): QuoteRateDto? {
        return qtprtMapper.findByEffectiveDate(
            query.targetPremCode,
            query.targetType,
            query.rateAge,
            query.rateSex,
            query.qtprtType,
            query.effectiveDate
        )?.toDto()
    }

    /**
     * Get quote rate by primary key
     *
     * @param targetPremCode Target premium code
     * @param targetType Target type
     * @param strDate Effective start date
     * @param endDate Effective end date
     * @param rateAge Rate age
     * @param rateSex Rate sex
     * @param qtprtType Quote rate type
     * @return Quote rate record, null if not found
     */
    fun getQuoteRateByKey(
        targetPremCode: String,
        targetType: String,
        strDate: LocalDate,
        endDate: LocalDate,
        rateAge: Int,
        rateSex: String,
        qtprtType: String
    ): QuoteRateDto? {
        return qtprtMapper.findByPrimaryKey(
            targetPremCode, targetType, strDate, endDate,
            rateAge, rateSex, qtprtType
        )?.toDto()
    }

    /**
     * Get all quote rates for a target premium code
     *
     * @param targetPremCode Target premium code
     * @return List of quote rates, empty if not found
     */
    fun getQuoteRatesByTargetPremCode(targetPremCode: String): List<QuoteRateDto> {
        return qtprtMapper.findByTargetPremCode(targetPremCode)
            .map { it.toDto() }
    }

    /**
     * Check if quote rate record exists
     *
     * @return true if record exists
     */
    fun exists(
        targetPremCode: String,
        targetType: String,
        strDate: LocalDate,
        endDate: LocalDate,
        rateAge: Int,
        rateSex: String,
        qtprtType: String
    ): Boolean {
        return qtprtMapper.countByPrimaryKey(
            targetPremCode, targetType, strDate, endDate,
            rateAge, rateSex, qtprtType
        ) > 0
    }

    // ===== Modification Methods =====

    /**
     * Create new quote rate record
     *
     * @param entity Quote rate entity
     * @return Created entity
     * @throws IllegalStateException if record already exists
     */
    @Transactional
    @CacheEvict("quoteRate", allEntries = true)
    fun create(entity: QuoteRate): QuoteRate {
        val exists = exists(
            entity.targetPremCode, entity.targetType,
            entity.strDate, entity.endDate,
            entity.rateAge, entity.rateSex, entity.qtprtType
        )
        if (exists) {
            throw IllegalStateException(
                "Quote rate record already exists: " +
                "${entity.targetPremCode}/${entity.targetType}/" +
                "${entity.strDate}/${entity.endDate}/" +
                "${entity.rateAge}/${entity.rateSex}/${entity.qtprtType}"
            )
        }
        qtprtMapper.insert(entity)
        logger.info(
            "Created quote rate record: {}/{}/{}/{}/{}/{}/{}",
            entity.targetPremCode, entity.targetType,
            entity.strDate, entity.endDate,
            entity.rateAge, entity.rateSex, entity.qtprtType
        )
        return entity
    }

    /**
     * Update quote rate record
     *
     * @param key Primary key
     * @param entity Updated entity
     * @return Updated entity
     */
    @Transactional
    @CacheEvict("quoteRate", allEntries = true)
    fun update(key: QuoteRateKey, entity: QuoteRate): QuoteRate {
        qtprtMapper.update(key, entity)
        logger.info(
            "Updated quote rate record: {}/{}/{}/{}/{}/{}/{}",
            key.targetPremCode, key.targetType,
            key.strDate, key.endDate,
            key.rateAge, key.rateSex, key.qtprtType
        )
        return entity
    }

    /**
     * Delete quote rate record
     *
     * @param targetPremCode Target premium code
     * @param targetType Target type
     * @param strDate Effective start date
     * @param endDate Effective end date
     * @param rateAge Rate age
     * @param rateSex Rate sex
     * @param qtprtType Quote rate type
     */
    @Transactional
    @CacheEvict("quoteRate", allEntries = true)
    fun delete(
        targetPremCode: String,
        targetType: String,
        strDate: LocalDate,
        endDate: LocalDate,
        rateAge: Int,
        rateSex: String,
        qtprtType: String
    ) {
        qtprtMapper.deleteByPrimaryKey(
            targetPremCode, targetType, strDate, endDate,
            rateAge, rateSex, qtprtType
        )
        logger.info(
            "Deleted quote rate record: {}/{}/{}/{}/{}/{}/{}",
            targetPremCode, targetType, strDate, endDate,
            rateAge, rateSex, qtprtType
        )
    }
}
