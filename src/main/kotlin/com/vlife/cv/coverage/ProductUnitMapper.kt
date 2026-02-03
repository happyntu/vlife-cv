package com.vlife.cv.coverage

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.math.BigDecimal

/**
 * 產品單位 (紅利分配) MyBatis Mapper
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
 * 遵循 ADR-016 規範，SQL 集中於 XML 管理。
 *
 * @see resources/mapper/cv/ProductUnitMapper.xml
 */
@Mapper
interface ProductUnitMapper {

    /**
     * 依保單號碼查詢所有紅利分配記錄
     *
     * 注意：此方法應搭配 PageHelper.startPage() 使用以支援分頁。
     *
     * @param policyNo 保單號碼
     * @return 紅利分配清單
     */
    fun findByPolicyNo(@Param("policyNo") policyNo: String): List<ProductUnit>

    /**
     * 依保單號碼和承保範圍編號查詢紅利分配記錄
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 紅利分配清單
     */
    fun findByCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): List<ProductUnit>

    /**
     * 依主鍵查詢單筆紅利分配記錄
     *
     * @return 紅利分配資料，不存在時回傳 null
     */
    fun findById(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int,
        @Param("ps06Type") ps06Type: String,
        @Param("cvpuType") cvpuType: String,
        @Param("lastAnnivDur") lastAnnivDur: Int
    ): ProductUnit?

    /**
     * 計算指定承保範圍的宣告紅利總和
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 宣告紅利總和，無資料時回傳 null
     */
    fun sumDivDeclare(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): BigDecimal?

    /**
     * 計算指定承保範圍的增值保額紅利總和
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 增值保額紅利總和，無資料時回傳 null
     */
    fun sumDivPuaAmt(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): BigDecimal?

    /**
     * 計算指定承保範圍的紅利分配記錄數量
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 記錄數量
     */
    fun countByCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Int

    /**
     * 查詢最新週年期間的紅利分配記錄
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 最新紅利分配記錄，無資料時回傳 null
     */
    fun findLatestByCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): ProductUnit?
}
