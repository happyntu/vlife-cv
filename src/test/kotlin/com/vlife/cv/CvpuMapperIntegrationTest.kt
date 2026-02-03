package com.vlife.cv

import com.vlife.cv.coverage.CvpuMapper
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
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CvpuMapper 整合測試
 *
 * 使用 TestContainers Oracle XE 進行真實資料庫測試。
 * 測試產品單位表 (CV.CVPU) - 紅利分配的 CRUD 操作。
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("CvpuMapper 整合測試")
class CvpuMapperIntegrationTest {

    companion object {
        private const val ORACLE_IMAGE = "gvenzl/oracle-xe:21-slim"

        /**
         * 使用 withInitScript() 在連線用戶 schema 中建立表格
         * CV 用戶連線後，CV.CVPU 等同於 CVPU（當前 schema）
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
    private lateinit var cvpuMapper: CvpuMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val today = LocalDate.now()

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM CV.CVPU")
    }

    @Nested
    @DisplayName("findByPolicyNo")
    inner class FindByPolicyNo {

        @Test
        fun `should return all dividend records for policy`() {
            // Given
            insertTestDividend("P000000001", 1, "1", "1", 1, 1000.00, 500.00)
            insertTestDividend("P000000001", 1, "1", "1", 2, 1100.00, 550.00)
            insertTestDividend("P000000002", 1, "1", "1", 1, 2000.00, 1000.00)

            // When
            val result = cvpuMapper.findByPolicyNo("P000000001")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.policyNo == "P000000001" })
            println("✓ findByPolicyNo: 成功查詢保單 P000000001 的 ${result.size} 筆紅利分配記錄")
        }

        @Test
        fun `should return empty list for unknown policy`() {
            // When
            val result = cvpuMapper.findByPolicyNo("UNKNOWN")

            // Then
            assertTrue(result.isEmpty())
            println("✓ findByPolicyNo: 未知保單正確返回空清單")
        }
    }

    @Nested
    @DisplayName("findByCoverage")
    inner class FindByCoverage {

        @Test
        fun `should return dividend records for coverage`() {
            // Given
            insertTestDividend("P000000001", 1, "1", "1", 1, 1000.00, 500.00)
            insertTestDividend("P000000001", 1, "1", "1", 2, 1100.00, 550.00)
            insertTestDividend("P000000001", 2, "1", "1", 1, 2000.00, 1000.00)

            // When
            val result = cvpuMapper.findByCoverage("P000000001", 1)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.coverageNo == 1 })
            println("✓ findByCoverage: 成功查詢承保範圍 1 的 ${result.size} 筆紅利分配記錄")
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        fun `should return dividend record when exists`() {
            // Given
            insertTestDividend("P000000001", 1, "1", "1", 1, 1000.00, 500.00)

            // When
            val result = cvpuMapper.findById("P000000001", 1, "1", "1", 1)

            // Then
            assertNotNull(result)
            assertEquals("P000000001", result.policyNo)
            assertEquals(1, result.coverageNo)
            // 使用 compareTo 比較 BigDecimal（避免 scale 不同導致 equals 失敗）
            assertTrue(BigDecimal("1000.00").compareTo(result.divDeclare) == 0)
            println("✓ findById: 成功查詢存在的紅利分配記錄")
        }

        @Test
        fun `should return null when not exists`() {
            // When
            val result = cvpuMapper.findById("P000000001", 1, "1", "1", 999)

            // Then
            assertNull(result)
            println("✓ findById: 不存在時正確返回 null")
        }
    }

    @Nested
    @DisplayName("sumDivDeclare")
    inner class SumDivDeclare {

        @Test
        fun `should return sum of declared dividends`() {
            // Given
            insertTestDividend("P000000001", 1, "1", "1", 1, 1000.00, 500.00)
            insertTestDividend("P000000001", 1, "1", "1", 2, 1100.00, 550.00)
            insertTestDividend("P000000001", 1, "1", "1", 3, 1200.00, 600.00)

            // When
            val result = cvpuMapper.sumDivDeclare("P000000001", 1)

            // Then
            assertNotNull(result)
            // 使用 compareTo 比較 BigDecimal（避免 scale 不同導致 equals 失敗）
            assertTrue(BigDecimal("3300.00").compareTo(result) == 0)
            println("✓ sumDivDeclare: 正確計算宣告紅利總和為 $result")
        }

        @Test
        fun `should return null when no records`() {
            // When
            val result = cvpuMapper.sumDivDeclare("P000000001", 999)

            // Then
            assertNull(result)
            println("✓ sumDivDeclare: 無資料時正確返回 null")
        }
    }

    @Nested
    @DisplayName("sumDivPuaAmt")
    inner class SumDivPuaAmt {

        @Test
        fun `should return sum of PUA amounts`() {
            // Given
            insertTestDividend("P000000001", 1, "1", "1", 1, 1000.00, 500.00)
            insertTestDividend("P000000001", 1, "1", "1", 2, 1100.00, 550.00)
            insertTestDividend("P000000001", 1, "1", "1", 3, 1200.00, 600.00)

            // When
            val result = cvpuMapper.sumDivPuaAmt("P000000001", 1)

            // Then
            assertNotNull(result)
            // 使用 compareTo 比較 BigDecimal（避免 scale 不同導致 equals 失敗）
            assertTrue(BigDecimal("1650.00").compareTo(result) == 0)
            println("✓ sumDivPuaAmt: 正確計算增值保額紅利總和為 $result")
        }
    }

    @Nested
    @DisplayName("countByCoverage")
    inner class CountByCoverage {

        @Test
        fun `should return correct count`() {
            // Given
            insertTestDividend("P000000001", 1, "1", "1", 1, 1000.00, 500.00)
            insertTestDividend("P000000001", 1, "1", "1", 2, 1100.00, 550.00)
            insertTestDividend("P000000001", 1, "1", "1", 3, 1200.00, 600.00)

            // When
            val count = cvpuMapper.countByCoverage("P000000001", 1)

            // Then
            assertEquals(3, count)
            println("✓ countByCoverage: 正確計算紅利分配記錄數量為 $count")
        }

        @Test
        fun `should return zero for unknown coverage`() {
            // When
            val count = cvpuMapper.countByCoverage("P000000001", 999)

            // Then
            assertEquals(0, count)
            println("✓ countByCoverage: 未知承保範圍正確返回 0")
        }
    }

    @Nested
    @DisplayName("findLatestByCoverage")
    inner class FindLatestByCoverage {

        @Test
        fun `should return latest dividend record`() {
            // Given
            insertTestDividend("P000000001", 1, "1", "1", 1, 1000.00, 500.00)
            insertTestDividend("P000000001", 1, "1", "1", 2, 1100.00, 550.00)
            insertTestDividend("P000000001", 1, "1", "1", 3, 1200.00, 600.00)

            // When
            val result = cvpuMapper.findLatestByCoverage("P000000001", 1)

            // Then
            assertNotNull(result)
            assertEquals(3, result.lastAnnivDur)
            // 使用 compareTo 比較 BigDecimal（避免 scale 不同導致 equals 失敗）
            assertTrue(BigDecimal("1200.00").compareTo(result.divDeclare) == 0)
            println("✓ findLatestByCoverage: 成功取得最新週年期間 ${result.lastAnnivDur} 的紅利分配記錄")
        }

        @Test
        fun `should return null when no records`() {
            // When
            val result = cvpuMapper.findLatestByCoverage("P000000001", 999)

            // Then
            assertNull(result)
            println("✓ findLatestByCoverage: 無資料時正確返回 null")
        }
    }

    /**
     * 插入測試紅利分配資料
     *
     * 欄位驗證規則（Cvpu Entity init block）：
     * - policyNo: 1-10 字元
     * - coverageNo: >= 0
     * - ps06Type: 必須恰好 1 字元
     * - cvpuType: 必須恰好 1 字元
     * - lastAnnivDur: >= 0
     */
    private fun insertTestDividend(
        policyNo: String,
        coverageNo: Int,
        ps06Type: String,
        cvpuType: String,
        lastAnnivDur: Int,
        divDeclare: Double,
        divPuaAmt: Double
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO CV.CVPU (
                POLICY_NO, COVERAGE_NO, PS06_TYPE, CVPU_TYPE, LAST_ANNIV_DUR,
                CVPU_STATUS_CODE, DIV_DECLARE, DIV_PUA_AMT, FINANCIAL_DATE, PROCESS_DATE
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            policyNo, coverageNo, ps06Type, cvpuType, lastAnnivDur,
            "1",  // CVPU_STATUS_CODE: 1=有效
            BigDecimal.valueOf(divDeclare),
            BigDecimal.valueOf(divPuaAmt),
            java.sql.Date.valueOf(today),
            java.sql.Date.valueOf(today)
        )
    }
}
