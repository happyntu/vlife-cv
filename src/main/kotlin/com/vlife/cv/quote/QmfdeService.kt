package com.vlife.cv.quote

import com.vlife.cv.config.CacheConfig.Companion.CACHE_QMFDE
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class QmfdeService(
    private val mapper: QmfdeMapper
) {
    @Cacheable(value = [CACHE_QMFDE], key = "#ivTargetCode", cacheManager = CV_CACHE_MANAGER)
    fun getByTargetCode(ivTargetCode: String): QmfdeDto? {
        logger.debug { "Finding QMFDE by ivTargetCode=$ivTargetCode" }
        return mapper.findByTargetCode(ivTargetCode)?.toDto()
    }

    @Cacheable(value = [CACHE_QMFDE], key = "'std:' + #ivStandardCode", cacheManager = CV_CACHE_MANAGER)
    fun getByStandardCode(ivStandardCode: String): QmfdeDto? {
        logger.debug { "Finding QMFDE by ivStandardCode=$ivStandardCode" }
        return mapper.findByStandardCode(ivStandardCode)?.toDto()
    }

    fun countByPlanCodeAndVersionAndDate(planCode: String, version: String, date: LocalDate): Int {
        return mapper.countByPlanCodeAndVersionAndDate(planCode, version, date)
    }

    fun countByEntryInd(qmfdeEntryInd: String): Int {
        return mapper.countByEntryInd(qmfdeEntryInd)
    }

    fun exists(ivTargetCode: String): Boolean {
        return mapper.exists(ivTargetCode)
    }

    fun existsByStandardCode(ivStandardCode: String): Boolean {
        return mapper.existsByStandardCode(ivStandardCode)
    }

    @CacheEvict(value = [CACHE_QMFDE], key = "#entity.ivTargetCode", cacheManager = CV_CACHE_MANAGER)
    fun create(entity: Qmfde, operator: String): Int {
        logger.info { "Creating QMFDE: ivTargetCode=${entity.ivTargetCode}, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.insert(entity)
    }

    @CacheEvict(value = [CACHE_QMFDE], key = "#entity.ivTargetCode", cacheManager = CV_CACHE_MANAGER)
    fun update(entity: Qmfde, operator: String): Int {
        logger.info { "Updating QMFDE: ivTargetCode=${entity.ivTargetCode}, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.update(entity)
    }

    @CacheEvict(value = [CACHE_QMFDE], key = "#ivTargetCode", cacheManager = CV_CACHE_MANAGER)
    fun delete(ivTargetCode: String, operator: String): Int {
        logger.info { "Deleting QMFDE: ivTargetCode=$ivTargetCode, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.delete(ivTargetCode)
    }
}
