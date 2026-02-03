package com.vlife.cv.coverage

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import java.math.BigDecimal

/**
 * 產品單位 (紅利分配) MyBatis Mapper
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
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
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PS06_TYPE as ps06Type,
            CVPU_TYPE as cvpuType,
            LAST_ANNIV_DUR as lastAnnivDur,
            CVPU_STATUS_CODE as statusCode,
            DIV_DECLARE as divDeclare,
            DIV_PUA_AMT as divPuaAmt,
            FINANCIAL_DATE as financialDate,
            PCPO_NO as pcpoNo,
            PROGRAM_ID as programId,
            PROCESS_DATE as processDate,
            POLICY_TYPE as policyType,
            CVPU_APPROVED_DATE as approvedDate,
            PROGRAM_ID_CVPU as programIdCvpu
        FROM CV.CVPU
        WHERE POLICY_NO = #{policyNo}
        ORDER BY COVERAGE_NO, LAST_ANNIV_DUR
    """)
    fun findByPolicyNo(@Param("policyNo") policyNo: String): List<ProductUnit>

    /**
     * 依保單號碼和承保範圍編號查詢紅利分配記錄
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 紅利分配清單
     */
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PS06_TYPE as ps06Type,
            CVPU_TYPE as cvpuType,
            LAST_ANNIV_DUR as lastAnnivDur,
            CVPU_STATUS_CODE as statusCode,
            DIV_DECLARE as divDeclare,
            DIV_PUA_AMT as divPuaAmt,
            FINANCIAL_DATE as financialDate,
            PCPO_NO as pcpoNo,
            PROGRAM_ID as programId,
            PROCESS_DATE as processDate,
            POLICY_TYPE as policyType,
            CVPU_APPROVED_DATE as approvedDate,
            PROGRAM_ID_CVPU as programIdCvpu
        FROM CV.CVPU
        WHERE POLICY_NO = #{policyNo} AND COVERAGE_NO = #{coverageNo}
        ORDER BY LAST_ANNIV_DUR
    """)
    fun findByCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): List<ProductUnit>

    /**
     * 依主鍵查詢單筆紅利分配記錄
     *
     * @return 紅利分配資料，不存在時回傳 null
     */
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PS06_TYPE as ps06Type,
            CVPU_TYPE as cvpuType,
            LAST_ANNIV_DUR as lastAnnivDur,
            CVPU_STATUS_CODE as statusCode,
            DIV_DECLARE as divDeclare,
            DIV_PUA_AMT as divPuaAmt,
            FINANCIAL_DATE as financialDate,
            PCPO_NO as pcpoNo,
            PROGRAM_ID as programId,
            PROCESS_DATE as processDate,
            POLICY_TYPE as policyType,
            CVPU_APPROVED_DATE as approvedDate,
            PROGRAM_ID_CVPU as programIdCvpu
        FROM CV.CVPU
        WHERE POLICY_NO = #{policyNo}
          AND COVERAGE_NO = #{coverageNo}
          AND PS06_TYPE = #{ps06Type}
          AND CVPU_TYPE = #{cvpuType}
          AND LAST_ANNIV_DUR = #{lastAnnivDur}
    """)
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
    @Select("""
        SELECT SUM(DIV_DECLARE)
        FROM CV.CVPU
        WHERE POLICY_NO = #{policyNo} AND COVERAGE_NO = #{coverageNo}
    """)
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
    @Select("""
        SELECT SUM(DIV_PUA_AMT)
        FROM CV.CVPU
        WHERE POLICY_NO = #{policyNo} AND COVERAGE_NO = #{coverageNo}
    """)
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
    @Select("""
        SELECT COUNT(*)
        FROM CV.CVPU
        WHERE POLICY_NO = #{policyNo} AND COVERAGE_NO = #{coverageNo}
    """)
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
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PS06_TYPE as ps06Type,
            CVPU_TYPE as cvpuType,
            LAST_ANNIV_DUR as lastAnnivDur,
            CVPU_STATUS_CODE as statusCode,
            DIV_DECLARE as divDeclare,
            DIV_PUA_AMT as divPuaAmt,
            FINANCIAL_DATE as financialDate,
            PCPO_NO as pcpoNo,
            PROGRAM_ID as programId,
            PROCESS_DATE as processDate,
            POLICY_TYPE as policyType,
            CVPU_APPROVED_DATE as approvedDate,
            PROGRAM_ID_CVPU as programIdCvpu
        FROM CV.CVPU
        WHERE POLICY_NO = #{policyNo} AND COVERAGE_NO = #{coverageNo}
        ORDER BY LAST_ANNIV_DUR DESC
        FETCH FIRST 1 ROW ONLY
    """)
    fun findLatestByCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): ProductUnit?
}
