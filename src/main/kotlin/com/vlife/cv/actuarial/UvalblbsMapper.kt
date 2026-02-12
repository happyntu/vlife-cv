package com.vlife.cv.actuarial

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * UVALBLBS 價值區塊-多被保人 Mapper
 *
 * 遵循 ADR-016/ADR-017
 */
@Mapper
interface UvalblbsMapper {

    /**
     * 依完整主鍵查詢
     *
     * V3: f99_get_uvalblbs
     */
    fun findByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("sexSub") sexSub: String,
        @Param("ageSub") ageSub: String,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String
    ): ValueBlockSub?

    /**
     * 計數查詢（唯一性檢查）
     */
    fun countByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("sexSub") sexSub: String,
        @Param("ageSub") ageSub: String,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String
    ): Int

    /**
     * 新增
     * V3: f99_insert_uvalblbs
     */
    fun insert(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("sexSub") sexSub: String,
        @Param("ageSub") ageSub: String,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String,
        @Param("values") values: List<Double?>
    ): Int

    /**
     * 刪除
     * V3: f99_delete_uvalblbs
     */
    fun delete(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("sexSub") sexSub: String,
        @Param("ageSub") ageSub: String,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String
    ): Int
}
