package com.vlife.cv.actuarial

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * UVALBLB 價值區塊-標準體 Mapper
 *
 * 遵循 ADR-016/ADR-017
 */
@Mapper
interface UvalblbMapper {

    /**
     * 依完整主鍵查詢（含 NVL 處理）
     *
     * V3: f99_get_uvalblb (標準體分支)
     */
    fun findByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String
    ): ValueBlock?

    /**
     * 計數查詢（唯一性檢查）
     */
    fun countByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String
    ): Int

    /**
     * 透過 Object View 查詢完整物件
     *
     * V3: f99_get_uvalblb_1, f99_get_uvalblb_x
     */
    fun findObjectByPrimaryKey(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String
    ): ValueBlock?

    /**
     * 新增
     * V3: f99_insert_uvalblb
     */
    fun insert(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String,
        @Param("values") values: List<Double?>
    ): Int

    /**
     * 更新精算值陣列
     * V3: f99_update_uvalblb (p_type=0 分支)
     */
    fun updateValues(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String,
        @Param("values") values: List<Double?>
    ): Int

    /**
     * 刪除
     * V3: f99_delete_uvalblb
     */
    fun delete(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("sex") sex: String,
        @Param("age") age: Int,
        @Param("uvSub1") uvSub1: String,
        @Param("uvSub2") uvSub2: String,
        @Param("recordType") recordType: String
    ): Int
}
