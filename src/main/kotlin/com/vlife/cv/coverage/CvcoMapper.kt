package com.vlife.cv.coverage

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * 保單基礎值變化表 Mapper (CV.CVCO)
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
 * 遵循 ADR-016 規範，SQL 集中於 XML 管理。
 * 遵循 ADR-017 規範，Mapper 名稱對應資料庫表格名稱。
 *
 * @see resources/mapper/cv/CvcoMapper.xml
 */
@Mapper
interface CvcoMapper {

    /**
     * 依保單號碼查詢所有承保範圍
     *
     * 注意：此方法應搭配 PageHelper.startPage() 使用以支援分頁。
     *
     * @param policyNo 保單號碼
     * @return 承保範圍清單
     */
    fun findByPolicyNo(@Param("policyNo") policyNo: String): List<Cvco>

    /**
     * 依主鍵查詢單筆承保範圍
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 承保範圍資料，不存在時回傳 null
     */
    fun findById(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Cvco?

    /**
     * 依險種代碼查詢承保範圍
     *
     * 注意：此方法應搭配 PageHelper.startPage() 使用以支援分頁。
     *
     * @param planCode 險種代碼
     * @return 承保範圍清單
     */
    fun findByPlanCode(@Param("planCode") planCode: String): List<Cvco>

    /**
     * 依承保狀態碼查詢承保範圍
     *
     * 注意：此方法應搭配 PageHelper.startPage() 使用以支援分頁。
     *
     * @param statusCode 承保狀態碼
     * @return 承保範圍清單
     */
    fun findByStatusCode(@Param("statusCode") statusCode: String): List<Cvco>

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    fun findAllPlanCodes(): List<String>

    /**
     * 計算指定保單的承保範圍數量
     *
     * @param policyNo 保單號碼
     * @return 承保範圍數量
     */
    fun countByPolicyNo(@Param("policyNo") policyNo: String): Int
}
