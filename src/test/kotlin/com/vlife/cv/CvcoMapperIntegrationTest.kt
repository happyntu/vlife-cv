package com.vlife.cv

import com.vlife.cv.coverage.CvcoMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.OracleContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CvcoMapper 整合測試
 *
 * 使用 TestContainers Oracle XE 進行真實資料庫測試。
 * 驗證 MyBatis Mapper 與 Oracle 的整合。
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("CvcoMapper 整合測試")
class CvcoMapperIntegrationTest {

    companion object {
        private const val ORACLE_IMAGE = "gvenzl/oracle-xe:21-slim"

        /**
         * 使用 withInitScript() 在連線用戶 schema 中建立表格
         * CV 用戶連線後，CV.CVCO 等同於 CVCO（當前 schema）
         */
        @Container
        @JvmStatic
        val oracle: OracleContainer = OracleContainer(DockerImageName.parse(ORACLE_IMAGE))
            .withDatabaseName("VLIFE")
            .withUsername("CV")
            .withPassword("cv123")
            .withInitScript("init-cv-schema.sql")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { oracle.jdbcUrl }
            registry.add("spring.datasource.username") { oracle.username }
            registry.add("spring.datasource.password") { oracle.password }
            registry.add("spring.datasource.driver-class-name") { "oracle.jdbc.OracleDriver" }
        }
    }

    @Autowired
    private lateinit var cvcoMapper: CvcoMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        // 清理測試資料
        jdbcTemplate.execute("DELETE FROM CV.CVCO")
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        fun `should return coverage when exists`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "P")

            // When
            val result = cvcoMapper.findById("P000000001", 1)

            // Then
            assertNotNull(result)
            assertEquals("P000000001", result.policyNo)
            assertEquals(1, result.coverageNo)
            assertEquals("PLAN1", result.planCode)
            println("✓ findById: 成功查詢存在的承保範圍")
        }

        @Test
        fun `should return null when not exists`() {
            // When
            val result = cvcoMapper.findById("NOTEXIST", 999)

            // Then
            assertNull(result)
            println("✓ findById: 不存在時正確返回 null")
        }
    }

    @Nested
    @DisplayName("findByPolicyNo")
    inner class FindByPolicyNo {

        @Test
        fun `should return all coverages for policy`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "P")
            insertTestCoverage("P000000001", 2, "PLAN2", "P")
            insertTestCoverage("P000000002", 1, "PLAN1", "P")

            // When
            val result = cvcoMapper.findByPolicyNo("P000000001")

            // Then
            assertEquals(2, result.size)
            assertEquals(1, result[0].coverageNo)
            assertEquals(2, result[1].coverageNo)
            println("✓ findByPolicyNo: 成功查詢保單 P000000001 的 ${result.size} 筆承保範圍")
        }

        @Test
        fun `should return empty list for unknown policy`() {
            // When
            val result = cvcoMapper.findByPolicyNo("NOTEXIST")

            // Then
            assertTrue(result.isEmpty())
            println("✓ findByPolicyNo: 未知保單正確返回空清單")
        }
    }

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return coverages by plan code`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "P")
            insertTestCoverage("P000000002", 1, "PLAN1", "P")
            insertTestCoverage("P000000003", 1, "PLAN2", "P")

            // When
            val result = cvcoMapper.findByPlanCode("PLAN1")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.planCode == "PLAN1" })
            println("✓ findByPlanCode: 成功查詢險種 PLAN1 的 ${result.size} 筆承保範圍")
        }
    }

    @Nested
    @DisplayName("findByStatusCode")
    inner class FindByStatusCode {

        @Test
        fun `should return coverages by status code`() {
            // Given - 使用 CoverageStatusCode enum 定義的狀態碼
            // P=有效, M=滿期, L=失效
            insertTestCoverage("P000000001", 1, "PLAN1", "P")  // 有效
            insertTestCoverage("P000000002", 1, "PLAN1", "M")  // 滿期
            insertTestCoverage("P000000003", 1, "PLAN1", "P")  // 有效

            // When
            val result = cvcoMapper.findByStatusCode("P")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.statusCode == "P" })
            println("✓ findByStatusCode: 成功查詢狀態碼 P (有效) 的 ${result.size} 筆承保範圍")
        }
    }

    @Nested
    @DisplayName("findAllPlanCodes")
    inner class FindAllPlanCodes {

        @Test
        fun `should return distinct plan codes`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "P")
            insertTestCoverage("P000000002", 1, "PLAN1", "P")
            insertTestCoverage("P000000003", 1, "PLAN2", "P")
            insertTestCoverage("P000000004", 1, "PLAN3", "P")

            // When
            val result = cvcoMapper.findAllPlanCodes()

            // Then
            assertEquals(3, result.size)
            assertTrue(result.contains("PLAN1"))
            assertTrue(result.contains("PLAN2"))
            assertTrue(result.contains("PLAN3"))
            println("✓ findAllPlanCodes: 成功取得 ${result.size} 個不重複的險種代碼")
        }
    }

    @Nested
    @DisplayName("countByPolicyNo")
    inner class CountByPolicyNo {

        @Test
        fun `should return correct count`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "P")
            insertTestCoverage("P000000001", 2, "PLAN2", "P")
            insertTestCoverage("P000000001", 3, "PLAN3", "P")

            // When
            val count = cvcoMapper.countByPolicyNo("P000000001")

            // Then
            assertEquals(3, count)
            println("✓ countByPolicyNo: 正確計算保單承保範圍數量為 $count")
        }

        @Test
        fun `should return zero for unknown policy`() {
            // When
            val count = cvcoMapper.countByPolicyNo("NOTEXIST")

            // Then
            assertEquals(0, count)
            println("✓ countByPolicyNo: 未知保單正確返回 0")
        }
    }

    /**
     * 插入測試承保範圍資料
     *
     * 欄位驗證規則（Cvco Entity init block）：
     * - policyNo: 1-10 字元
     * - planCode: 1-5 字元
     * - version: 必須恰好 1 字元
     * - rateSex: 必須恰好 1 字元
     * - statusCode: 必須恰好 1 字元
     */
    private fun insertTestCoverage(
        policyNo: String,
        coverageNo: Int,
        planCode: String,
        statusCode: String
    ) {
        val today = LocalDate.now()
        jdbcTemplate.update(
            """
            INSERT INTO CV.CVCO (
                POLICY_NO, COVERAGE_NO, PLAN_CODE, VERSION, RATE_SEX, RATE_AGE,
                RATE_SUB_1, RATE_SUB_2, CO_ISSUE_DATE, CO_STATUS_CODE,
                INSURANCE_TYPE_3, PROCESS_DATE, PROCESS_TYPE
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            policyNo, coverageNo, planCode,
            "1",     // VERSION: 1 字元
            "1",     // RATE_SEX: 1=男
            30,      // RATE_AGE
            "01",    // RATE_SUB_1: 2 碼
            "001",   // RATE_SUB_2: 3 碼
            java.sql.Date.valueOf(today),  // CO_ISSUE_DATE
            statusCode,                     // CO_STATUS_CODE: P=有效, M=滿期
            "R",     // INSURANCE_TYPE_3: R=一般保險
            java.sql.Date.valueOf(today),  // PROCESS_DATE
            "01"     // PROCESS_TYPE: 2 碼
        )
    }
}
