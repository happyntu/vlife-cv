package com.vlife.cv.rate

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * PRAT01 (Plan Rate 01) Mapper
 *
 * MyBatis mapper for CV.PRAT01 table operations.
 *
 * Adheres to:
 * - ADR-016: SQL must be in XML, no @Select/@Insert/@Update/@Delete annotations
 * - ADR-017: Mapper naming follows table-oriented convention (Prat01Mapper)
 * - ADR-009: Schema = CV.PRAT01 (明確指定 Schema)
 *
 * V3 Package: PK_LIB_PRAT01PROC
 *
 * 注意：PRAT01 為唯讀表格（費率由精算系統維護），V4 不提供異動操作
 */
@Mapper
interface Prat01Mapper {

    // ===== Query Operations =====

    /**
     * 依完整主鍵查詢費率
     *
     * V3: f99_get_prat01
     *
     * @param key 複合主鍵物件
     * @return 費率記錄，若不存在則回傳 null
     */
    fun findByPrimaryKey(key: PlanRate01Key): PlanRate01?

    /**
     * 查詢險種的所有費率
     *
     * V3 無對應功能，V4 新增用於批次處理/快取預熱
     *
     * @param planCode 險種代碼
     * @return 該險種的所有費率記錄
     */
    fun findByPlanCode(planCode: String): List<PlanRate01>

    /**
     * 查詢特定險種版本的所有費率
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 該險種版本的所有費率記錄
     */
    fun findByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): List<PlanRate01>

    // ===== Existence Check Operations =====

    /**
     * 存在性檢查
     *
     * @param key 複合主鍵物件
     * @return 若存在則回傳 true
     */
    fun existsByPrimaryKey(key: PlanRate01Key): Boolean

    // ===== Count Operations =====

    /**
     * 查詢險種費率筆數
     *
     * @param planCode 險種代碼
     * @return 該險種的費率筆數
     */
    fun countByPlanCode(planCode: String): Int

    // ===== Cache Warming Operations =====

    /**
     * 快取預熱：取得所有險種代碼
     *
     * V3 無對應功能，V4 新增用於快取預熱
     * 自足設計：直接查詢 PRAT01 的 DISTINCT PLAN_CODE
     *
     * @return 所有險種代碼清單（排序）
     */
    fun findDistinctPlanCodes(): List<String>
}
