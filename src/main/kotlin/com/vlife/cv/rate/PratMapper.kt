package com.vlife.cv.rate

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * PRAT Plan Rate Mapper
 *
 * MyBatis mapper for CV.PRAT table operations.
 *
 * Adheres to:
 * - ADR-016: SQL must be in XML, no @Select/@Insert/@Update/@Delete annotations
 * - ADR-017: Mapper naming follows table-oriented convention (PratMapper)
 *
 * V3 Package: PK_LIB_PRATPROC
 */
@Mapper
interface PratMapper {

    // ===== Query Operations =====

    /**
     * Find rate by primary key (pure PRAT query)
     *
     * V3: f99_get_prat_1
     *
     * @param planCode Plan code
     * @param version Version
     * @param rateSex Rate sex
     * @param rateSub1 Rate sub-key 1
     * @param rateSub2 Rate sub-key 2
     * @param rateAge Rate age
     * @return Plan rate record, or null if not found
     */
    fun findByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("rateSex") rateSex: String,
        @Param("rateSub1") rateSub1: String,
        @Param("rateSub2") rateSub2: String,
        @Param("rateAge") rateAge: Int
    ): PlanRate?

    // ===== Count Operations =====

    /**
     * Count by primary key (fixed conditions)
     *
     * V3: f99_get_prat_cnt
     *
     * @return Number of matching records
     */
    fun countByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("rateSex") rateSex: String,
        @Param("rateSub1") rateSub1: String,
        @Param("rateSub2") rateSub2: String,
        @Param("rateAge") rateAge: Int
    ): Int

    /**
     * Dynamic condition count (handles NULL values)
     *
     * V3: f99_get_prat_cnt_2
     * V3 uses EXECUTE IMMEDIATE dynamic SQL.
     * V4 uses MyBatis dynamic XML syntax to eliminate SQL injection risk.
     *
     * @param planCode Plan code (required)
     * @param version Version (required)
     * @param rateSex Rate sex (nullable)
     * @param rateSub1 Rate sub-key 1 (nullable)
     * @param rateSub2 Rate sub-key 2 (nullable)
     * @param rateAge Rate age (nullable, V3 uses 999 for "no limit", V4 uses null)
     * @return Number of matching records
     */
    fun countDynamic(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("rateSex") rateSex: String?,
        @Param("rateSub1") rateSub1: String?,
        @Param("rateSub2") rateSub2: String?,
        @Param("rateAge") rateAge: Int?
    ): Int

    // ===== Modification Operations =====

    /**
     * Insert new rate record
     *
     * V3: f99_insert_prat (via Object View ov_prat)
     *
     * @param entity Plan rate entity
     */
    fun insert(entity: PlanRate)

    /**
     * Update rate record (by 6-field primary key)
     *
     * V3: f99_update_prat
     *
     * @param key Primary key
     * @param entity Updated plan rate entity
     */
    fun update(
        @Param("key") key: PlanRateKey,
        @Param("entity") entity: PlanRate
    )

    /**
     * Delete rate record (by 6-field primary key)
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
    fun deleteByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("rateSex") rateSex: String,
        @Param("rateSub1") rateSub1: String,
        @Param("rateSub2") rateSub2: String,
        @Param("rateAge") rateAge: Int
    )
}
