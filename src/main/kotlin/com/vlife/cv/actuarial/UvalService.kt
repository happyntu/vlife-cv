package com.vlife.cv.actuarial

import com.vlife.cv.config.CacheConfig.Companion.CACHE_UVAL
import com.vlife.cv.config.CacheConfig.Companion.CACHE_UVALBLB
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * UVAL/UVALBLB/UVALBLBS 統一精算值服務
 *
 * 管理三張精算值表格的查詢與維護
 * 查詢功能帶 Caffeine 快取（ADR-014）
 * 對外提供 REST API 供跨模組使用（NB/PC/CL/RI/PS）
 */
@Service
class UvalService(
    private val uvalMapper: UvalMapper,
    private val uvalblbMapper: UvalblbMapper,
    private val uvalblbsMapper: UvalblbsMapper
) {

    // ===================================================================
    // UVAL 主表操作
    // ===================================================================

    /**
     * 查詢 UVAL 主表記錄
     *
     * V3: f99_get_uval (回傳 1=找到, 0=未找到)
     * V4: 找到回傳 Entity，找不到回傳 null
     */
    @Cacheable(
        value = [CACHE_UVAL],
        key = "#planCode + ':' + #version + ':' + #sex + ':' + #age + ':' + #recordType + ':' + #serialYear + ':' + #deathRate + ':' + #uvRvfRate",
        cacheManager = CV_CACHE_MANAGER
    )
    fun getUval(
        planCode: String, version: String, sex: String, age: Int,
        recordType: String, serialYear: Int, deathRate: String, uvRvfRate: String
    ): UniversalValue? {
        logger.debug { "Getting UVAL: planCode=$planCode, version=$version, sex=$sex, age=$age, recordType=$recordType, serialYear=$serialYear" }
        return uvalMapper.findByPrimaryKey(
            planCode, version, sex, age, recordType, serialYear, deathRate, uvRvfRate
        )
    }

    /**
     * 依險種代碼計數
     *
     * V3: f99_get_uval_cnt
     */
    fun countByPlanCode(planCode: String): Int {
        require(planCode.isNotBlank()) { "險種代碼不可為空" }
        logger.debug { "Counting UVAL by planCode=$planCode" }
        return uvalMapper.countByPlanCode(planCode)
    }

    /**
     * 依完整 8 欄位主鍵檢查存在性
     *
     * V3: f99_get_uval_cnt_2
     * V4: 回傳 Boolean（V3 回傳 COUNT）
     */
    fun existsByFullKey(
        planCode: String, version: String, sex: String, age: Int,
        recordType: String, serialYear: Int, deathRate: String, uvRvfRate: String
    ): Boolean {
        val count = uvalMapper.countByFullKey(
            planCode, version, sex, age, recordType, serialYear, deathRate, uvRvfRate
        )
        return count > 0
    }

    /**
     * 新增 UVAL 記錄
     * V3: f99_insert_uval
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVAL], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun createUval(entity: UniversalValue): Int {
        logger.debug { "Creating UVAL: planCode=${entity.planCode}, version=${entity.version}" }
        return uvalMapper.insert(entity)
    }

    /**
     * 依 6 欄位鍵更新 UVAL 記錄
     * V3: f99_update_uval
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVAL], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun updateUvalByPartialKey(
        planCode: String, version: String, sex: String, age: Int,
        recordType: String, serialYear: Int, entity: UniversalValue
    ): Int {
        logger.debug { "Updating UVAL by partial key: planCode=$planCode, version=$version" }
        return uvalMapper.updateByPartialKey(
            planCode, version, sex, age, recordType, serialYear, entity
        )
    }

    /**
     * 依完整 8 欄位鍵更新 UVAL 記錄
     * V3: f99_update_uval_2
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVAL], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun updateUvalByFullKey(
        planCode: String, version: String, sex: String, age: Int,
        recordType: String, serialYear: Int, deathRate: String, uvRvfRate: String,
        entity: UniversalValue
    ): Int {
        logger.debug { "Updating UVAL by full key: planCode=$planCode, version=$version" }
        return uvalMapper.updateByFullKey(
            planCode, version, sex, age, recordType, serialYear, deathRate, uvRvfRate, entity
        )
    }

    /**
     * 依 6 欄位鍵刪除 UVAL 記錄
     * V3: f99_delete_uval
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVAL], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteUvalByPartialKey(
        planCode: String, version: String, sex: String, age: Int,
        recordType: String, serialYear: Int
    ): Int {
        logger.debug { "Deleting UVAL by partial key: planCode=$planCode, version=$version" }
        return uvalMapper.deleteByPartialKey(planCode, version, sex, age, recordType, serialYear)
    }

    /**
     * 依 5 欄位鍵刪除 UVAL 記錄（不含 record_type）
     * V3: f99_delete_uval_2
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVAL], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteUvalByReducedKey(
        planCode: String, version: String, sex: String, age: Int, serialYear: Int
    ): Int {
        logger.debug { "Deleting UVAL by reduced key: planCode=$planCode, version=$version" }
        return uvalMapper.deleteByReducedKey(planCode, version, sex, age, serialYear)
    }

    /**
     * 依完整 8 欄位鍵刪除 UVAL 記錄
     * V3: f99_delete_uval_3
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVAL], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteUvalByFullKey(
        planCode: String, version: String, sex: String, age: Int,
        recordType: String, serialYear: Int, deathRate: String, uvRvfRate: String
    ): Int {
        logger.debug { "Deleting UVAL by full key: planCode=$planCode, version=$version" }
        return uvalMapper.deleteByFullKey(
            planCode, version, sex, age, recordType, serialYear, deathRate, uvRvfRate
        )
    }

    // ===================================================================
    // UVALBLB / UVALBLBS 價值區塊操作（核心查詢）
    // ===================================================================

    /**
     * 查詢價值區塊（自動判斷標準體 / 多被保人）
     *
     * V3: f99_get_uvalblb (核心 11 參數查詢)
     * 核心邏輯：
     * - planRelationSub IN ('0','5') OR NULL → 查 UVALBLB（標準體）
     * - 其他值 → 查 UVALBLBS（多被保人），rateSub1/rateSub2 左補零
     *
     * @param query 查詢條件
     * @return 精算值陣列，找不到或多筆時回傳 null
     */
    @Cacheable(
        value = [CACHE_UVALBLB],
        key = "#query.planCode + ':' + #query.version + ':' + #query.sex + ':' + #query.age + ':' + #query.effectiveUvSub1 + ':' + #query.effectiveUvSub2 + ':' + #query.recordType + ':' + (#query.planRelationSub ?: 'std') + ':' + (#query.rateSub1 ?: '0') + ':' + (#query.rateSub2 ?: '0')",
        cacheManager = CV_CACHE_MANAGER
    )
    fun getValueBlock(query: ValueBlockQuery): List<Double?>? {
        logger.debug { "Getting value block: planCode=${query.planCode}, recordType=${query.recordType}, isStandard=${query.isStandard}" }
        return if (query.isStandard) {
            getStandardValueBlock(query)
        } else {
            getSubstandardValueBlock(query)
        }
    }

    /**
     * 查詢價值區塊並轉換為巢狀表格式
     *
     * V3: f99_get_uvalblb_2
     * 將 VARRAY 轉換為逐年度記錄的列表
     *
     * @param query 查詢條件
     * @param policyYear 保單年度（若指定則僅回傳該年度；null 則回傳全部）
     * @return 巢狀表記錄列表
     */
    fun getValueBlockAsTable(
        query: ValueBlockQuery,
        policyYear: Int? = null
    ): List<ValueBlockTableEntry> {
        val values = getValueBlock(query) ?: return emptyList()

        return if (policyYear != null && policyYear in 1..values.size) {
            listOf(
                ValueBlockTableEntry(
                    planCode = query.planCode,
                    version = query.version,
                    age = query.age,
                    sex = query.sex,
                    rateSub1 = query.rateSub1,
                    rateSub2 = query.rateSub2,
                    policyYear = policyYear,
                    value = values[policyYear - 1] ?: 0.0,
                    recordType = query.recordType
                )
            )
        } else {
            values.mapIndexed { index, value ->
                ValueBlockTableEntry(
                    planCode = query.planCode,
                    version = query.version,
                    age = query.age,
                    sex = query.sex,
                    rateSub1 = query.rateSub1,
                    rateSub2 = query.rateSub2,
                    policyYear = index + 1,
                    value = value ?: 0.0,
                    recordType = query.recordType
                )
            }
        }
    }

    /**
     * 查詢價值區塊有效元素數量
     *
     * 等同 V3 f99_get_uvalblb 的回傳值語意
     */
    fun getValueBlockCount(query: ValueBlockQuery): Int {
        val values = getValueBlock(query) ?: return 0
        return values.count { it != null }
    }

    // ===== 私有方法 =====

    private fun getStandardValueBlock(query: ValueBlockQuery): List<Double?>? {
        val count = uvalblbMapper.countByPrimaryKey(
            query.planCode, query.version, query.sex, query.age,
            query.effectiveUvSub1, query.effectiveUvSub2, query.recordType
        )
        if (count != 1) return null

        val block = uvalblbMapper.findByPrimaryKey(
            query.planCode, query.version, query.sex, query.age,
            query.effectiveUvSub1, query.effectiveUvSub2, query.recordType
        )
        return block?.values
    }

    private fun getSubstandardValueBlock(query: ValueBlockQuery): List<Double?>? {
        val count = uvalblbsMapper.countByPrimaryKey(
            query.planCode, query.version, query.sex, query.age,
            query.sexSubPadded, query.ageSubPadded,
            query.effectiveUvSub1, query.effectiveUvSub2, query.recordType
        )
        if (count != 1) return null

        val block = uvalblbsMapper.findByPrimaryKey(
            query.planCode, query.version, query.sex, query.age,
            query.sexSubPadded, query.ageSubPadded,
            query.effectiveUvSub1, query.effectiveUvSub2, query.recordType
        )
        return block?.values
    }

    // ===================================================================
    // UVALBLB 維護操作（CV 內部使用）
    // ===================================================================

    /**
     * 新增 UVALBLB 記錄
     * V3: f99_insert_uvalblb
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVALBLB], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun createValueBlock(
        planCode: String, version: String, sex: String, age: Int,
        uvSub1: String, uvSub2: String, recordType: String,
        values: List<Double?>
    ): Int {
        logger.debug { "Creating value block: planCode=$planCode, version=$version" }
        return uvalblbMapper.insert(planCode, version, sex, age, uvSub1, uvSub2, recordType, values)
    }

    /**
     * 更新 UVALBLB 精算值
     * V3: f99_update_uvalblb (p_type=0)
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVALBLB], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun updateValueBlock(
        planCode: String, version: String, sex: String, age: Int,
        uvSub1: String, uvSub2: String, recordType: String,
        values: List<Double?>
    ): Int {
        logger.debug { "Updating value block: planCode=$planCode, version=$version" }
        return uvalblbMapper.updateValues(planCode, version, sex, age, uvSub1, uvSub2, recordType, values)
    }

    /**
     * 刪除 UVALBLB 記錄
     * V3: f99_delete_uvalblb
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVALBLB], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteValueBlock(
        planCode: String, version: String, sex: String, age: Int,
        uvSub1: String, uvSub2: String, recordType: String
    ): Int {
        logger.debug { "Deleting value block: planCode=$planCode, version=$version" }
        return uvalblbMapper.delete(planCode, version, sex, age, uvSub1, uvSub2, recordType)
    }

    // ===================================================================
    // UVALBLBS 維護操作（CV 內部使用）
    // ===================================================================

    /**
     * 新增 UVALBLBS 記錄
     * V3: f99_insert_uvalblbs
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVALBLB], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun createValueBlockSub(
        planCode: String, version: String, sex: String, age: Int,
        sexSub: String, ageSub: String,
        uvSub1: String, uvSub2: String, recordType: String,
        values: List<Double?>
    ): Int {
        logger.debug { "Creating value block sub: planCode=$planCode, version=$version" }
        return uvalblbsMapper.insert(
            planCode, version, sex, age, sexSub, ageSub, uvSub1, uvSub2, recordType, values
        )
    }

    /**
     * 刪除 UVALBLBS 記錄
     * V3: f99_delete_uvalblbs
     */
    @Transactional
    @CacheEvict(value = [CACHE_UVALBLB], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun deleteValueBlockSub(
        planCode: String, version: String, sex: String, age: Int,
        sexSub: String, ageSub: String,
        uvSub1: String, uvSub2: String, recordType: String
    ): Int {
        logger.debug { "Deleting value block sub: planCode=$planCode, version=$version" }
        return uvalblbsMapper.delete(
            planCode, version, sex, age, sexSub, ageSub, uvSub1, uvSub2, recordType
        )
    }
}
