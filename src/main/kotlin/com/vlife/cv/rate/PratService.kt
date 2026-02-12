package com.vlife.cv.rate

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * PRAT Plan Rate Service
 *
 * Provides rate query (including BIDC group rate switching logic) and CRUD operations.
 * Cache strategy: Caffeine local cache (ADR-014).
 *
 * V3 Package: PK_LIB_PRATPROC
 */
@Service
class PratService(
    private val pratMapper: PratMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PratService::class.java)
    }

    // ===== Query Methods =====

    /**
     * Rate query (with BIDC group rate switching logic)
     *
     * V3: f99_get_prat
     *
     * Logic:
     * 1. If rateSexInd = 2 → force rateSex := '0' (plan has no sex distinction)
     * 2. Query PRAT with effective rateSex
     *
     * Note: Full BIDC group rate switching (contractNo → BIDC.bidc_ind = '2' → query BIPT)
     * is deferred until BIPT table implementation is complete.
     * Currently returns PRAT query result only.
     *
     * @param query Rate query conditions
     * @return Rate record (may come from PRAT or BIPT in future), null if not found
     */
    fun getRate(query: PlanRateQuery): PlanRateDto? {
        // 1. Handle sex indicator
        val effectiveSex = if (query.rateSexInd == 2) "0" else query.rateSex

        // 2. Group rate determination (TODO: implement after BIPT spec is ready)
        // val isGroupRate = query.contractNo?.let { contractNo ->
        //     val bidc = bidcService.getByContractNo(contractNo)
        //     bidc?.bidcInd == "2"
        // } ?: false

        // 3. Query corresponding table based on result
        // For now, always query PRAT until BIPT is implemented
        return getRateDirect(
            query.planCode, query.version, effectiveSex,
            query.rateSub1, query.rateSub2, query.rateAge
        )
    }

    /**
     * Pure PRAT query (without BIDC logic)
     *
     * V3: f99_get_prat_1
     *
     * @return Rate record, null if not found
     */
    @Cacheable("planRate", key = "#planCode + ':' + #version + ':' + #rateSex + ':' + #rateSub1 + ':' + #rateSub2 + ':' + #rateAge")
    fun getRateDirect(
        planCode: String,
        version: String,
        rateSex: String,
        rateSub1: String,
        rateSub2: String,
        rateAge: Int
    ): PlanRateDto? {
        return pratMapper.findByPrimaryKey(planCode, version, rateSex, rateSub1, rateSub2, rateAge)
            ?.toDto()
    }

    /**
     * Check if rate record exists
     *
     * V3: f99_get_prat_cnt (returns COUNT, V3 callers use it for pre-insert validation)
     * V4: Returns Boolean for clearer semantics
     *
     * @return true if record exists
     */
    fun exists(
        planCode: String,
        version: String,
        rateSex: String,
        rateSub1: String,
        rateSub2: String,
        rateAge: Int
    ): Boolean {
        return pratMapper.countByPrimaryKey(planCode, version, rateSex, rateSub1, rateSub2, rateAge) > 0
    }

    /**
     * Dynamic condition count
     *
     * V3: f99_get_prat_cnt_2
     * V3 logic: rateAge = 999 means "no age limit".
     * V4 change: Use null for no limit, caller must convert 999 to null.
     *
     * @param planCode Plan code (required, not blank)
     * @param version Version (required, not blank)
     * @param rateSex Rate sex (nullable)
     * @param rateSub1 Rate sub-key 1 (nullable)
     * @param rateSub2 Rate sub-key 2 (nullable)
     * @param rateAge Rate age (null means no age limit, V3's 999 semantic)
     * @return Number of matching records
     */
    fun countDynamic(
        planCode: String,
        version: String,
        rateSex: String?,
        rateSub1: String?,
        rateSub2: String?,
        rateAge: Int?
    ): Int {
        require(planCode.isNotBlank()) { "Plan code must not be blank" }
        require(version.isNotBlank()) { "Version must not be blank" }
        return pratMapper.countDynamic(planCode, version, rateSex, rateSub1, rateSub2, rateAge)
    }

    // ===== Modification Methods =====

    /**
     * Create new rate record
     *
     * V3: f99_insert_prat
     *
     * @param entity Plan rate entity
     * @return Created entity
     * @throws IllegalStateException if record already exists
     */
    @Transactional
    @CacheEvict("planRate", key = "#entity.planCode + ':' + #entity.version + ':' + #entity.rateSex + ':' + #entity.rateSub1 + ':' + #entity.rateSub2 + ':' + #entity.rateAge")
    fun create(entity: PlanRate): PlanRate {
        val exists = exists(
            entity.planCode, entity.version, entity.rateSex,
            entity.rateSub1, entity.rateSub2, entity.rateAge
        )
        if (exists) {
            throw IllegalStateException(
                "Rate record already exists: ${entity.planCode}/${entity.version}/${entity.rateSex}/${entity.rateSub1}/${entity.rateSub2}/${entity.rateAge}"
            )
        }
        pratMapper.insert(entity)
        logger.info("Created rate record: {}/{}/{}/{}/{}/{}",
            entity.planCode, entity.version, entity.rateSex,
            entity.rateSub1, entity.rateSub2, entity.rateAge)
        return entity
    }

    /**
     * Update rate record
     *
     * V3: f99_update_prat
     *
     * @param key Primary key
     * @param entity Updated entity
     * @return Updated entity
     */
    @Transactional
    @CacheEvict("planRate", key = "#key.planCode + ':' + #key.version + ':' + #key.rateSex + ':' + #key.rateSub1 + ':' + #key.rateSub2 + ':' + #key.rateAge")
    fun update(key: PlanRateKey, entity: PlanRate): PlanRate {
        pratMapper.update(key, entity)
        logger.info("Updated rate record: {}/{}/{}/{}/{}/{}",
            key.planCode, key.version, key.rateSex,
            key.rateSub1, key.rateSub2, key.rateAge)
        return entity
    }

    /**
     * Delete rate record
     *
     * V3: f99_delete_prat
     *
     * @param planCode Plan code
     * @param version Version
     * @param rateSex Rate sex
     * @param rateSub1 Rate sub-key 1
     * @param rateSub2 Rate sub-key 2
     * @param rateAge Rate age
     */
    @Transactional
    @CacheEvict("planRate", key = "#planCode + ':' + #version + ':' + #rateSex + ':' + #rateSub1 + ':' + #rateSub2 + ':' + #rateAge")
    fun delete(
        planCode: String,
        version: String,
        rateSex: String,
        rateSub1: String,
        rateSub2: String,
        rateAge: Int
    ) {
        pratMapper.deleteByPrimaryKey(planCode, version, rateSex, rateSub1, rateSub2, rateAge)
        logger.info("Deleted rate record: {}/{}/{}/{}/{}/{}",
            planCode, version, rateSex, rateSub1, rateSub2, rateAge)
    }
}
