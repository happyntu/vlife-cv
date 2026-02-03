package com.vlife.cv.coverage

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

/**
 * 保單基礎值變化 MyBatis Mapper
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
 */
@Mapper
interface CoverageValueChangeMapper {

    /**
     * 依保單號碼查詢所有承保範圍
     *
     * @param policyNo 保單號碼
     * @return 承保範圍清單
     */
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PLAN_CODE as planCode,
            VERSION as version,
            RATE_SEX as rateSex,
            RATE_AGE as rateAge,
            RATE_SUB_1 as rateSub1,
            RATE_SUB_2 as rateSub2,
            CO_ISSUE_DATE as issueDate,
            CO_STATUS_CODE as statusCode,
            INSURANCE_TYPE_3 as insuranceType3,
            PROCESS_DATE as processDate,
            PROCESS_TYPE as processType,
            POLICY_TYPE as policyType,
            CO_STATUS_CODE2 as statusCode2
        FROM CV.CVCO
        WHERE POLICY_NO = #{policyNo}
        ORDER BY COVERAGE_NO
        FETCH FIRST 100 ROWS ONLY
    """)
    fun findByPolicyNo(@Param("policyNo") policyNo: String): List<CoverageValueChange>

    /**
     * 依主鍵查詢單筆承保範圍
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 承保範圍資料，不存在時回傳 null
     */
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PLAN_CODE as planCode,
            VERSION as version,
            RATE_SEX as rateSex,
            RATE_AGE as rateAge,
            RATE_SUB_1 as rateSub1,
            RATE_SUB_2 as rateSub2,
            CO_ISSUE_DATE as issueDate,
            CO_STATUS_CODE as statusCode,
            INSURANCE_TYPE_3 as insuranceType3,
            PROCESS_DATE as processDate,
            PROCESS_TYPE as processType,
            POLICY_TYPE as policyType,
            CO_STATUS_CODE2 as statusCode2
        FROM CV.CVCO
        WHERE POLICY_NO = #{policyNo} AND COVERAGE_NO = #{coverageNo}
    """)
    fun findById(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): CoverageValueChange?

    /**
     * 依險種代碼查詢承保範圍
     *
     * @param planCode 險種代碼
     * @return 承保範圍清單
     */
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PLAN_CODE as planCode,
            VERSION as version,
            RATE_SEX as rateSex,
            RATE_AGE as rateAge,
            RATE_SUB_1 as rateSub1,
            RATE_SUB_2 as rateSub2,
            CO_ISSUE_DATE as issueDate,
            CO_STATUS_CODE as statusCode,
            INSURANCE_TYPE_3 as insuranceType3,
            PROCESS_DATE as processDate,
            PROCESS_TYPE as processType,
            POLICY_TYPE as policyType,
            CO_STATUS_CODE2 as statusCode2
        FROM CV.CVCO
        WHERE PLAN_CODE = #{planCode}
        ORDER BY POLICY_NO, COVERAGE_NO
        FETCH FIRST 1000 ROWS ONLY
    """)
    fun findByPlanCode(@Param("planCode") planCode: String): List<CoverageValueChange>

    /**
     * 依承保狀態碼查詢承保範圍
     *
     * @param statusCode 承保狀態碼
     * @return 承保範圍清單
     */
    @Select("""
        SELECT
            POLICY_NO as policyNo,
            COVERAGE_NO as coverageNo,
            PLAN_CODE as planCode,
            VERSION as version,
            RATE_SEX as rateSex,
            RATE_AGE as rateAge,
            RATE_SUB_1 as rateSub1,
            RATE_SUB_2 as rateSub2,
            CO_ISSUE_DATE as issueDate,
            CO_STATUS_CODE as statusCode,
            INSURANCE_TYPE_3 as insuranceType3,
            PROCESS_DATE as processDate,
            PROCESS_TYPE as processType,
            POLICY_TYPE as policyType,
            CO_STATUS_CODE2 as statusCode2
        FROM CV.CVCO
        WHERE CO_STATUS_CODE = #{statusCode}
        ORDER BY PROCESS_DATE DESC
        FETCH FIRST 1000 ROWS ONLY
    """)
    fun findByStatusCode(@Param("statusCode") statusCode: String): List<CoverageValueChange>

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    @Select("""
        SELECT DISTINCT PLAN_CODE
        FROM CV.CVCO
        ORDER BY PLAN_CODE
    """)
    fun findAllPlanCodes(): List<String>

    /**
     * 計算指定保單的承保範圍數量
     *
     * @param policyNo 保單號碼
     * @return 承保範圍數量
     */
    @Select("""
        SELECT COUNT(*)
        FROM CV.CVCO
        WHERE POLICY_NO = #{policyNo}
    """)
    fun countByPolicyNo(@Param("policyNo") policyNo: String): Int
}
