package com.vlife.cv.rate

import com.vlife.cv.config.CacheConfig.Companion.CACHE_QIRAT
import com.vlife.cv.config.CacheConfig.Companion.CACHE_QIRAT_STRICT
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class QiratService(
    private val qiratMapper: QiratMapper
) {
    @Cacheable(value = [CACHE_QIRAT], key = "#subAcntPlanCode + ':' + #intRateType + ':' + #baseDate", unless = "#result == null", cacheManager = CV_CACHE_MANAGER)
    fun getEffectiveRate(subAcntPlanCode: String, intRateType: String, baseDate: LocalDate): SubAccountInterestRate? {
        logger.debug { "Getting effective rate: plan=$subAcntPlanCode, type=$intRateType, date=$baseDate" }
        return qiratMapper.findByBaseDate(subAcntPlanCode, intRateType, baseDate)
            ?: qiratMapper.findMaxBeforeDate(subAcntPlanCode, intRateType, baseDate)
    }

    @Cacheable(value = [CACHE_QIRAT_STRICT], key = "#subAcntPlanCode + ':' + #intRateType + ':' + #baseDate", unless = "#result == null", cacheManager = CV_CACHE_MANAGER)
    fun findByBaseDate(subAcntPlanCode: String, intRateType: String, baseDate: LocalDate): SubAccountInterestRate? {
        logger.debug { "Finding strict rate: plan=$subAcntPlanCode, type=$intRateType, date=$baseDate" }
        return qiratMapper.findByBaseDate(subAcntPlanCode, intRateType, baseDate)
    }

    fun findByProcessYm(subAcntPlanCode: String, intRateType: String, processYm: String): SubAccountInterestRate? {
        logger.debug { "Finding by processYm: plan=$subAcntPlanCode, type=$intRateType, ym=$processYm" }
        return qiratMapper.findByProcessYm(subAcntPlanCode, intRateType, processYm)
    }

    fun findByFullKey(subAcntPlanCode: String, intRateType: String, intRateDateStr: LocalDate?, intRateDateEnd: LocalDate?): SubAccountInterestRate? {
        return qiratMapper.findByFullKey(subAcntPlanCode, intRateType, intRateDateStr, intRateDateEnd)
    }

    fun countByPlanAndType(subAcntPlanCode: String, intRateType: String? = null): Int {
        return qiratMapper.countByPlanAndType(subAcntPlanCode, intRateType)
    }

    fun existsByFullKey(subAcntPlanCode: String, intRateType: String, intRateDateStr: LocalDate): Boolean {
        return qiratMapper.existsByFullKey(subAcntPlanCode, intRateType, intRateDateStr)
    }

    @Transactional
    @CacheEvict(value = [CACHE_QIRAT, CACHE_QIRAT_STRICT], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun insertWithDateAdjust(rate: SubAccountInterestRate) {
        logger.info { "Inserting QIRAT with date adjust: plan=${rate.subAcntPlanCode}, type=${rate.intRateType}, str=${rate.intRateDateStr}" }
        val exists = qiratMapper.existsByFullKey(rate.subAcntPlanCode, rate.intRateType, rate.intRateDateStr)
        require(!exists) { "此利率起日已存在 (${rate.subAcntPlanCode}/${rate.intRateType}/${rate.intRateDateStr})" }

        val maxStartDate = qiratMapper.findMaxStartDate(rate.subAcntPlanCode, rate.intRateType)
        if (maxStartDate != null && rate.intRateDateStr > maxStartDate) {
            val newEndDate = rate.intRateDateStr.minusDays(1)
            qiratMapper.updateEndDate(rate.subAcntPlanCode, rate.intRateType, maxStartDate, newEndDate)
        }
        qiratMapper.insert(rate)
    }

    @Transactional
    @CacheEvict(value = [CACHE_QIRAT, CACHE_QIRAT_STRICT], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteWithDateAdjust(subAcntPlanCode: String, intRateType: String, intRateDateStr: LocalDate, intRateDateEnd: LocalDate) {
        logger.info { "Deleting QIRAT with date adjust: plan=$subAcntPlanCode, type=$intRateType, str=$intRateDateStr" }
        val deleted = qiratMapper.delete(subAcntPlanCode, intRateType, intRateDateStr, intRateDateEnd)
        require(deleted > 0) { "刪除失敗，查無資料 ($subAcntPlanCode/$intRateType/$intRateDateStr)" }

        val hasNext = qiratMapper.findByDateAfterOrEqual(subAcntPlanCode, intRateType, intRateDateStr)
        if (hasNext == null) {
            val maxStartDate = qiratMapper.findMaxStartDate(subAcntPlanCode, intRateType)
            if (maxStartDate != null) {
                qiratMapper.updateEndDate(subAcntPlanCode, intRateType, maxStartDate, SubAccountInterestRate.INFINITE_END_DATE)
            }
        }
    }

    @Transactional
    @CacheEvict(value = [CACHE_QIRAT, CACHE_QIRAT_STRICT], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun updateRate(subAcntPlanCode: String, intRateType: String, intRateDateStr: LocalDate, intRateDateEnd: LocalDate, newRate: BigDecimal): Int {
        logger.info { "Updating QIRAT rate: plan=$subAcntPlanCode, type=$intRateType, newRate=$newRate" }
        return qiratMapper.updateRate(subAcntPlanCode, intRateType, intRateDateStr, intRateDateEnd, newRate)
    }

    @Transactional
    @CacheEvict(value = [CACHE_QIRAT, CACHE_QIRAT_STRICT], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun updateEndDate(subAcntPlanCode: String, intRateType: String, intRateDateStr: LocalDate, intRateDateEnd: LocalDate): Int {
        logger.info { "Updating QIRAT end date: plan=$subAcntPlanCode, type=$intRateType, str=$intRateDateStr, end=$intRateDateEnd" }
        return qiratMapper.updateEndDate(subAcntPlanCode, intRateType, intRateDateStr, intRateDateEnd)
    }
}
