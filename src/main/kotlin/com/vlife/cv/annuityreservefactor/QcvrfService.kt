package com.vlife.cv.annuityreservefactor

import com.vlife.cv.config.CacheConfig.Companion.CACHE_QCVRF
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class QcvrfService(
    private val mapper: QcvrfMapper
) {
    @Cacheable(value = [CACHE_QCVRF], key = "#annyPlanCode", cacheManager = CV_CACHE_MANAGER)
    fun getByAnnyPlanCode(annyPlanCode: String): List<AnnuityReserveFactorDto> {
        logger.debug { "Finding QCVRF by annyPlanCode=$annyPlanCode" }
        return mapper.findByAnnyPlanCode(annyPlanCode).map { it.toDto() }
    }

    fun getByPk(annyPlanCode: String, strDate: LocalDate, endDate: LocalDate): AnnuityReserveFactorDto? {
        logger.debug { "Finding QCVRF by PK: annyPlanCode=$annyPlanCode, strDate=$strDate, endDate=$endDate" }
        return mapper.findByPk(annyPlanCode, strDate, endDate)?.toDto()
    }

    @Transactional
    @CacheEvict(value = [CACHE_QCVRF], key = "#entity.annyPlanCode", cacheManager = CV_CACHE_MANAGER)
    fun create(entity: AnnuityReserveFactor, operator: String): Int {
        logger.info { "Creating QCVRF: annyPlanCode=${entity.annyPlanCode}, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.insert(entity)
    }

    @Transactional
    @CacheEvict(value = [CACHE_QCVRF], key = "#annyPlanCode", cacheManager = CV_CACHE_MANAGER)
    fun update(annyPlanCode: String, strDate: LocalDate, endDate: LocalDate, entity: AnnuityReserveFactor, operator: String): Int {
        logger.info { "Updating QCVRF: annyPlanCode=$annyPlanCode, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.updateByPk(annyPlanCode, strDate, endDate, entity)
    }

    @Transactional
    @CacheEvict(value = [CACHE_QCVRF], key = "#annyPlanCode", cacheManager = CV_CACHE_MANAGER)
    fun delete(annyPlanCode: String, strDate: LocalDate, endDate: LocalDate, operator: String): Int {
        logger.info { "Deleting QCVRF: annyPlanCode=$annyPlanCode, strDate=$strDate, endDate=$endDate, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.deleteByPk(annyPlanCode, strDate, endDate)
    }
}
