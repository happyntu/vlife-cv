package com.vlife.cv.commission

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDate

/**
 * 佣金率表 Mapper (CV.CRAT)
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
 * 遵循 ADR-016 規範，SQL 集中於 XML 管理。
 * 遵循 ADR-017 規範，Mapper 名稱對應資料庫表格名稱。
 *
 * @see resources/mapper/cv/CratMapper.xml
 */
@Mapper
interface CratMapper {

    /**
     * 依序號查詢佣金率
     *
     * @param serial 序號 (主鍵)
     * @return 佣金率資料，不存在時回傳 null
     */
    fun findBySerial(@Param("serial") serial: Long): Crat?

    /**
     * 依佣金類別碼查詢佣金率清單
     *
     * @param commClassCode 佣金率類別碼
     * @return 佣金率清單
     */
    fun findByClassCode(@Param("commClassCode") commClassCode: String): List<Crat>

    /**
     * 查詢指定日期有效的佣金率
     *
     * @param commClassCode 佣金率類別碼
     * @param commLineCode 業務線代號
     * @param effectiveDate 生效日期
     * @return 有效的佣金率清單
     */
    fun findEffectiveRates(
        @Param("commClassCode") commClassCode: String,
        @Param("commLineCode") commLineCode: String,
        @Param("effectiveDate") effectiveDate: LocalDate
    ): List<Crat>

    /**
     * 依業務線代號查詢所有佣金率類別碼
     *
     * @param commLineCode 業務線代號
     * @return 佣金率類別碼清單
     */
    fun findClassCodesByLineCode(@Param("commLineCode") commLineCode: String): List<String>

    /**
     * 查詢所有不重複的業務線代號
     *
     * @return 業務線代號清單
     */
    fun findAllLineCodes(): List<String>

    /**
     * 查詢所有不重複的佣金率型態
     *
     * @return 佣金率型態清單
     */
    fun findAllCratTypes(): List<String>

    /**
     * 依多條件查詢佣金率
     *
     * 注意：此方法應搭配 PageHelper.startPage() 使用以支援分頁。
     *
     * @param commClassCode 佣金率類別碼 (可選)
     * @param commLineCode 業務線代號 (可選)
     * @param cratType 佣金率型態 (可選)
     * @param effectiveDate 生效日期 (可選)
     * @return 符合條件的佣金率清單
     */
    fun search(
        @Param("commClassCode") commClassCode: String?,
        @Param("commLineCode") commLineCode: String?,
        @Param("cratType") cratType: String?,
        @Param("effectiveDate") effectiveDate: LocalDate?
    ): List<Crat>

    /**
     * 計算指定佣金類別碼的資料筆數
     *
     * @param commClassCode 佣金率類別碼
     * @return 資料筆數
     */
    fun countByClassCode(@Param("commClassCode") commClassCode: String): Int

    // ==================== CUD 操作 (CV004M) ====================

    /**
     * 取得下一個序號 (從序列產生)
     *
     * @return 新的序號
     */
    fun nextSerial(): Long

    /**
     * 新增佣金率
     *
     * 對應 V3 cv004m_insert_crat
     *
     * @param crat 佣金率資料
     * @return 影響的資料筆數
     */
    fun insert(crat: Crat): Int

    /**
     * 更新佣金率
     *
     * 對應 V3 cv004m_update_crat
     *
     * @param serial 序號 (主鍵)
     * @param request 更新請求
     * @return 影響的資料筆數
     */
    fun update(@Param("serial") serial: Long, @Param("req") request: CratUpdateRequest): Int

    /**
     * 刪除佣金率
     *
     * 對應 V3 cv004m_delete_crat
     *
     * @param serial 序號 (主鍵)
     * @return 影響的資料筆數
     */
    fun deleteBySerial(@Param("serial") serial: Long): Int

    /**
     * 檢查 key 值重疊
     *
     * 對應 V3 cv004m_check_crat_key
     * 用於新增/更新前檢查是否有重疊的佣金率設定
     *
     * @param commClassCode 佣金率類別碼
     * @param commLineCode 業務線代號
     * @param cratType 佣金率型態
     * @param projectNo 專案號碼
     * @param startDate 生效起日
     * @param endDate 生效迄日
     * @param cratKey1 佣金鍵值1
     * @param cratKey2 佣金鍵值2
     * @param excludeSerial 排除的序號 (更新時排除自己)
     * @return 重疊的資料筆數
     */
    fun countOverlapping(
        @Param("commClassCode") commClassCode: String,
        @Param("commLineCode") commLineCode: String,
        @Param("cratType") cratType: String,
        @Param("projectNo") projectNo: String?,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("cratKey1") cratKey1: String,
        @Param("cratKey2") cratKey2: String,
        @Param("excludeSerial") excludeSerial: Long? = null
    ): Int
}
