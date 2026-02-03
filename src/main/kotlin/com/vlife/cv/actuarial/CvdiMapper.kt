package com.vlife.cv.actuarial

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDate

/**
 * 紅利分配水準檔 Mapper (CV.CVDI)
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
 * 遵循 ADR-016 規範，SQL 集中於 XML 管理。
 * 遵循 ADR-017 規範，Mapper 名稱對應資料庫表格名稱。
 *
 * 對應 V3 的 CVDI 表格，儲存分紅保單的紅利分配參數。
 *
 * @see resources/mapper/cv/CvdiMapper.xml
 */
@Mapper
interface CvdiMapper {

    /**
     * 依險種代碼和版本查詢所有紅利分配水準
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 紅利分配水準清單
     */
    fun findByPlanCode(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): List<Cvdi>

    /**
     * 依險種、繳費狀態查詢紅利分配水準
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param paidStatus 繳費狀態
     * @return 紅利分配水準清單
     */
    fun findByPlanCodeAndPaidStatus(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("paidStatus") paidStatus: String
    ): List<Cvdi>

    /**
     * 依條件查詢符合的紅利分配水準
     *
     * 用於查詢特定年齡、保額、保費範圍內的紅利分配參數
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param paidStatus 繳費狀態
     * @param rateSex 費率性別
     * @param age 年齡 (需在 AGE_LIMIT_STR ~ AGE_LIMIT_END 範圍內)
     * @param faceAmt 保額 (需在 FACE_AMT_STR ~ FACE_AMT_END 範圍內)
     * @param modePrem 保費 (需在 MODE_PREM_S ~ MODE_PREM_E 範圍內)
     * @param policyYear 保單年度
     * @param declareDate 宣告日期
     * @return 符合條件的紅利分配水準，不存在時回傳 null
     */
    fun findByCondition(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("paidStatus") paidStatus: String,
        @Param("rateSex") rateSex: String,
        @Param("age") age: Int,
        @Param("faceAmt") faceAmt: Long,
        @Param("modePrem") modePrem: java.math.BigDecimal,
        @Param("policyYear") policyYear: Int,
        @Param("declareDate") declareDate: LocalDate
    ): Cvdi?

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    fun findAllPlanCodes(): List<String>

    /**
     * 依險種查詢所有宣告日期
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 宣告日期清單
     */
    fun findDeclareDates(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): List<LocalDate>

    /**
     * 計算指定險種的紅利分配水準數量
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 資料筆數
     */
    fun countByPlanCode(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): Int
}
