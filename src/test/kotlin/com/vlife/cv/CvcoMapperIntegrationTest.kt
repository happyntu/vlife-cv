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

        @Container
        @JvmStatic
        val oracle: OracleContainer = OracleContainer(DockerImageName.parse(ORACLE_IMAGE))
            .withDatabaseName("VLIFE")
            .withUsername("vlife")
            .withPassword("vlife123")
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
            insertTestCoverage("P000000001", 1, "PLAN1", "1")

            // When
            val result = cvcoMapper.findById("P000000001", 1)

            // Then
            assertNotNull(result)
            assertEquals("P000000001", result.policyNo)
            assertEquals(1, result.coverageNo)
            assertEquals("PLAN1", result.planCode)
        }

        @Test
        fun `should return null when not exists`() {
            // When
            val result = cvcoMapper.findById("NOTEXIST", 999)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findByPolicyNo")
    inner class FindByPolicyNo {

        @Test
        fun `should return all coverages for policy`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "1")
            insertTestCoverage("P000000001", 2, "PLAN2", "1")
            insertTestCoverage("P000000002", 1, "PLAN1", "1")

            // When
            val result = cvcoMapper.findByPolicyNo("P000000001")

            // Then
            assertEquals(2, result.size)
            assertEquals(1, result[0].coverageNo)
            assertEquals(2, result[1].coverageNo)
        }

        @Test
        fun `should return empty list for unknown policy`() {
            // When
            val result = cvcoMapper.findByPolicyNo("NOTEXIST")

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return coverages by plan code`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "1")
            insertTestCoverage("P000000002", 1, "PLAN1", "1")
            insertTestCoverage("P000000003", 1, "PLAN2", "1")

            // When
            val result = cvcoMapper.findByPlanCode("PLAN1")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.planCode == "PLAN1" })
        }
    }

    @Nested
    @DisplayName("findByStatusCode")
    inner class FindByStatusCode {

        @Test
        fun `should return coverages by status code`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "1")
            insertTestCoverage("P000000002", 1, "PLAN1", "2")
            insertTestCoverage("P000000003", 1, "PLAN1", "1")

            // When
            val result = cvcoMapper.findByStatusCode("1")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.statusCode == "1" })
        }
    }

    @Nested
    @DisplayName("findAllPlanCodes")
    inner class FindAllPlanCodes {

        @Test
        fun `should return distinct plan codes`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "1")
            insertTestCoverage("P000000002", 1, "PLAN1", "1")
            insertTestCoverage("P000000003", 1, "PLAN2", "1")
            insertTestCoverage("P000000004", 1, "PLAN3", "1")

            // When
            val result = cvcoMapper.findAllPlanCodes()

            // Then
            assertEquals(3, result.size)
            assertTrue(result.contains("PLAN1"))
            assertTrue(result.contains("PLAN2"))
            assertTrue(result.contains("PLAN3"))
        }
    }

    @Nested
    @DisplayName("countByPolicyNo")
    inner class CountByPolicyNo {

        @Test
        fun `should return correct count`() {
            // Given
            insertTestCoverage("P000000001", 1, "PLAN1", "1")
            insertTestCoverage("P000000001", 2, "PLAN2", "1")
            insertTestCoverage("P000000001", 3, "PLAN3", "1")

            // When
            val count = cvcoMapper.countByPolicyNo("P000000001")

            // Then
            assertEquals(3, count)
        }

        @Test
        fun `should return zero for unknown policy`() {
            // When
            val count = cvcoMapper.countByPolicyNo("NOTEXIST")

            // Then
            assertEquals(0, count)
        }
    }

    /**
     * 插入測試承保範圍資料
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
            ) VALUES (?, ?, ?, '001', 'M', 30, 'A', 'B', ?, ?, 'A', ?, 'NB')
            """.trimIndent(),
            policyNo, coverageNo, planCode,
            java.sql.Date.valueOf(today), statusCode, java.sql.Date.valueOf(today)
        )
    }
}
