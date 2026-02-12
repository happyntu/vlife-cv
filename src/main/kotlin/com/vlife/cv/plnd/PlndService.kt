package com.vlife.cv.plnd

import com.vlife.cv.config.CacheConfig.Companion.CACHE_PLND
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

/**
 * 險種投資標的配置 Service (CV.PLND)
 *
 * 對應 V3 PK_LIB_PLNDPROC 的業務邏輯層。
 * 提供投資型保險商品的投資標的分配資訊查詢與維護。
 *
 * 快取策略：使用 Caffeine 快取查詢結果（投資標的配置變更頻率低）。
 */
@Service
class PlndService(
    private val mapper: PlndMapper
) {

    // === 查詢方法（帶快取）===

    /**
     * 檢查險種版本是否有 PLND 設定
     */
    fun exists(planCode: String, version: String): Boolean {
        logger.debug { "Checking PLND exists: planCode=$planCode, version=$version" }
        return mapper.exists(planCode, version)
    }

    /**
     * 依險種+版本查詢所有標的配置
     */
    @Cacheable(value = [CACHE_PLND], key = "#planCode + ':' + #version", cacheManager = CV_CACHE_MANAGER)
    fun findByPlanCodeAndVersion(planCode: String, version: String): List<PlndDto> {
        logger.debug { "Finding PLND by planCode=$planCode, version=$version" }
        return mapper.findByPlanCodeAndVersion(planCode, version).map { it.toDto() }
    }

    /**
     * 依險種+版本+標的代碼查詢
     */
    @Cacheable(value = [CACHE_PLND], key = "#planCode + ':' + #version + ':' + #ivTargetCode", cacheManager = CV_CACHE_MANAGER)
    fun findByPlanCodeAndVersionAndTargetCode(
        planCode: String,
        version: String,
        ivTargetCode: String
    ): List<PlndDto> {
        logger.debug { "Finding PLND by planCode=$planCode, version=$version, ivTargetCode=$ivTargetCode" }
        return mapper.findByPlanCodeAndVersionAndTargetCode(planCode, version, ivTargetCode)
            .map { it.toDto() }
    }

    /**
     * 依全部條件查詢單筆記錄
     */
    @Cacheable(value = [CACHE_PLND], key = "#planCode + ':' + #version + ':' + #ivTargetCode + ':' + #ivApplInd", cacheManager = CV_CACHE_MANAGER)
    fun findByAllConditions(
        planCode: String,
        version: String,
        ivTargetCode: String,
        ivApplInd: String
    ): PlndDto? {
        logger.debug { "Finding PLND by all conditions: planCode=$planCode, version=$version, ivTargetCode=$ivTargetCode, ivApplInd=$ivApplInd" }
        return mapper.findByAllConditions(planCode, version, ivTargetCode, ivApplInd)?.toDto()
    }

    /**
     * 計算投資比率總和
     */
    fun sumRatioByPlanCodeAndVersion(planCode: String, version: String): BigDecimal? {
        logger.debug { "Summing PLND ratio by planCode=$planCode, version=$version" }
        return mapper.sumRatioByPlanCodeAndVersion(planCode, version)
    }

    /**
     * 查詢投資標的有效期間
     */
    fun findEffectiveDatesByTargetCode(ivTargetCode: String): PlndDateRangeDto? {
        logger.debug { "Finding PLND effective dates by ivTargetCode=$ivTargetCode" }
        return mapper.findEffectiveDatesByTargetCode(ivTargetCode)
    }

    // === 異動操作（失效快取）===

    /**
     * 新增單筆 PLND 記錄
     */
    @Transactional
    @CacheEvict(value = [CACHE_PLND], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun create(entity: Plnd, operator: String): Int {
        logger.info { "Creating PLND: planCode=${entity.planCode}, version=${entity.version}, ivTargetCode=${entity.ivTargetCode}, operator=$operator" }
        // TODO: 記錄至 AuditLog（取代 V3 PLNDLG）
        return mapper.insert(entity)
    }

    /**
     * 更新依險種+版本
     */
    @Transactional
    @CacheEvict(value = [CACHE_PLND], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun updateByPlanCodeAndVersion(
        planCode: String,
        version: String,
        entity: Plnd,
        operator: String
    ): Int {
        logger.info { "Updating PLND by planCode=$planCode, version=$version, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.updateByPlanCodeAndVersion(planCode, version, entity)
    }

    /**
     * 依全部條件更新單筆記錄
     */
    @Transactional
    @CacheEvict(value = [CACHE_PLND], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun updateByAllConditions(entity: Plnd, operator: String): Int {
        logger.info { "Updating PLND by all conditions: planCode=${entity.planCode}, version=${entity.version}, ivTargetCode=${entity.ivTargetCode}, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.updateByAllConditions(entity)
    }

    /**
     * 刪除依險種+版本
     */
    @Transactional
    @CacheEvict(value = [CACHE_PLND], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteByPlanCodeAndVersion(planCode: String, version: String, operator: String): Int {
        logger.info { "Deleting PLND by planCode=$planCode, version=$version, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.deleteByPlanCodeAndVersion(planCode, version)
    }

    /**
     * 刪除依全部條件
     */
    @Transactional
    @CacheEvict(value = [CACHE_PLND], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteByAllConditions(
        planCode: String,
        version: String,
        ivTargetCode: String,
        ivApplInd: String,
        operator: String
    ): Int {
        logger.info { "Deleting PLND by all conditions: planCode=$planCode, version=$version, ivTargetCode=$ivTargetCode, ivApplInd=$ivApplInd, operator=$operator" }
        // TODO: 記錄至 AuditLog
        return mapper.deleteByAllConditions(planCode, version, ivTargetCode, ivApplInd)
    }

    /**
     * 清除 PLND 快取
     */
    @CacheEvict(value = [CACHE_PLND], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun evictCache() {
        logger.info { "Evicted all PLND cache entries" }
    }
}
