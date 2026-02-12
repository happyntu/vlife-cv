package com.vlife.cv.actuarial

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * UVAL 通用價值主表 Mapper
 *
 * 遵循 ADR-016/ADR-017
 */
@Mapper
interface UvalMapper {

    // ===== 查詢 =====

    /**
     * 依完整 8 欄位主鍵查詢
     *
     * V3: f99_get_uval (透過 ov_uval Object View)
     */
    fun findByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("recordType") recordType: String,
        @Param("serialYear") serialYear: Int,
        @Param("deathRate") deathRate: String,
        @Param("uvRvfRate") uvRvfRate: String
    ): UniversalValue?

    /**
     * 依險種代碼計數
     *
     * V3: f99_get_uval_cnt
     */
    fun countByPlanCode(@Param("planCode") planCode: String): Int

    /**
     * 依完整 8 欄位主鍵檢查存在性
     *
     * V3: f99_get_uval_cnt_2
     */
    fun countByFullKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("recordType") recordType: String,
        @Param("serialYear") serialYear: Int,
        @Param("deathRate") deathRate: String,
        @Param("uvRvfRate") uvRvfRate: String
    ): Int

    // ===== 異動 =====

    /**
     * 新增
     * V3: f99_insert_uval (透過 ov_uval Object View)
     */
    fun insert(entity: UniversalValue): Int

    /**
     * 依 6 欄位鍵更新
     * V3: f99_update_uval
     */
    fun updateByPartialKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("recordType") recordType: String,
        @Param("serialYear") serialYear: Int,
        @Param("entity") entity: UniversalValue
    ): Int

    /**
     * 依完整 8 欄位鍵更新
     * V3: f99_update_uval_2
     */
    fun updateByFullKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("recordType") recordType: String,
        @Param("serialYear") serialYear: Int,
        @Param("deathRate") deathRate: String,
        @Param("uvRvfRate") uvRvfRate: String,
        @Param("entity") entity: UniversalValue
    ): Int

    /**
     * 依 6 欄位鍵刪除
     * V3: f99_delete_uval
     */
    fun deleteByPartialKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("recordType") recordType: String,
        @Param("serialYear") serialYear: Int
    ): Int

    /**
     * 依 5 欄位鍵刪除（不含 record_type）
     * V3: f99_delete_uval_2
     */
    fun deleteByReducedKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("serialYear") serialYear: Int
    ): Int

    /**
     * 依完整 8 欄位鍵刪除
     * V3: f99_delete_uval_3
     */
    fun deleteByFullKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("recordType") recordType: String,
        @Param("serialYear") serialYear: Int,
        @Param("deathRate") deathRate: String,
        @Param("uvRvfRate") uvRvfRate: String
    ): Int
}
