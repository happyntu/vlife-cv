package com.vlife.cv.plan

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDate

/**
 * 險種描述檔 Mapper (CV.PLDF)
 *
 * 遵循 ADR-016 規範，使用 XML 定義 SQL 查詢。
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 遵循 ADR-022 規範，PLDF 歸屬於 CV 模組。
 *
 * V3 PL/SQL 對應：
 * - PK_LIB_PLANPROC.F99_GET_PLDF → findByPlanCodeAndVersion
 * - PK_LIB_PLANPROC.F99_GET_PLDF_CNT → countByPlanCode
 * - PK_LIB_PLANPROC.F99_INSERT_PLAN → insert
 * - PK_LIB_PLANPROC.F99_UPDATE_PLAN → update
 * - PK_LIB_PLANPROC.F99_DELETE_PLDF → deleteByPlanCodeAndVersion
 *
 * @see Pldf Entity 類別
 * @see PldfService Service 層
 */
@Mapper
interface PldfMapper {

    // ==================== 查詢方法 ====================

    /**
     * 依主鍵查詢險種描述
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @return 險種描述，不存在時回傳 null
     */
    fun findByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): Pldf?

    /**
     * 依險種代碼查詢所有版本
     *
     * @param planCode 險種代碼
     * @return 該險種所有版本清單
     */
    fun findByPlanCode(@Param("planCode") planCode: String): List<Pldf>

    /**
     * 查詢指定日期有效（上市中）的險種
     *
     * @param effectiveDate 生效日期
     * @return 有效險種清單
     */
    fun findEffective(@Param("effectiveDate") effectiveDate: LocalDate): List<Pldf>

    /**
     * 查詢指定日期有效的險種 (摘要)
     *
     * @param effectiveDate 生效日期
     * @return 有效險種摘要清單
     */
    fun findEffectiveSummary(@Param("effectiveDate") effectiveDate: LocalDate): List<PldfSummary>

    /**
     * 依條件搜尋險種
     *
     * 對應 V3 PK_CV_CV001M.cv001m_sql_statement 動態 SQL
     *
     * @param planCode 險種代碼（支援模糊查詢）
     * @param version 版數
     * @param primaryRiderInd 主附約指示
     * @param insuranceType3 保險型態3
     * @param planType 險種型態
     * @param effectiveDate 生效日期
     * @param currency 幣別
     * @param loanAvalInd 可貸款指示
     * @param divType 紅利類型
     * @return 符合條件的險種清單
     */
    fun search(
        @Param("planCode") planCode: String? = null,
        @Param("version") version: String? = null,
        @Param("primaryRiderInd") primaryRiderInd: String? = null,
        @Param("insuranceType3") insuranceType3: String? = null,
        @Param("planType") planType: String? = null,
        @Param("effectiveDate") effectiveDate: LocalDate? = null,
        @Param("currency") currency: String? = null,
        @Param("loanAvalInd") loanAvalInd: String? = null,
        @Param("divType") divType: String? = null
    ): List<Pldf>

    /**
     * 依條件搜尋險種 (摘要)
     *
     * @param planCode 險種代碼（支援模糊查詢）
     * @param version 版數
     * @param primaryRiderInd 主附約指示
     * @param insuranceType3 保險型態3
     * @param planType 險種型態
     * @param effectiveDate 生效日期
     * @return 符合條件的險種摘要清單
     */
    fun searchSummary(
        @Param("planCode") planCode: String? = null,
        @Param("version") version: String? = null,
        @Param("primaryRiderInd") primaryRiderInd: String? = null,
        @Param("insuranceType3") insuranceType3: String? = null,
        @Param("planType") planType: String? = null,
        @Param("effectiveDate") effectiveDate: LocalDate? = null
    ): List<PldfSummary>

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    fun findAllPlanCodes(): List<String>

    /**
     * 查詢所有不重複的保險型態3
     *
     * @return 保險型態3清單
     */
    fun findAllInsuranceType3(): List<String>

    /**
     * 計算指定險種代碼的版本數量
     *
     * @param planCode 險種代碼
     * @return 版本數量
     */
    fun countByPlanCode(@Param("planCode") planCode: String): Int

    // ==================== CUD 方法 (CV001M) ====================

    /**
     * 新增險種描述
     *
     * @param pldf 險種描述資料
     * @return 影響的列數
     */
    fun insert(pldf: Pldf): Int

    /**
     * 更新險種描述
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @param request 更新請求
     * @return 影響的列數
     */
    fun update(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("request") request: PldfUpdateRequest
    ): Int

    /**
     * 刪除險種描述
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @return 影響的列數
     */
    fun deleteByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): Int

    /**
     * 檢查主鍵是否存在
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @return 存在返回 1，不存在返回 0
     */
    fun existsByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): Int
}
