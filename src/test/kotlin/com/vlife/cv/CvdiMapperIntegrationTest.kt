package com.vlife.cv

import com.vlife.cv.actuarial.CvdiMapper
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
 * CvdiMapper 整合測試
 *
 * 使用 TestContainers Oracle XE 進行真實資料庫測試。
 * 測試紅利分配水準檔 (CV.CVDI) 的查詢操作。
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("CvdiMapper 整合測試")
class CvdiMapperIntegrationTest {

    companion object {
        private const val ORACLE_IMAGE = "gvenzl/oracle-xe:21-slim"

        /**
         * 使用 withInitScript() 在連線用戶 schema 中建立表格
         * CV 用戶連線後，CV.CVDI 等同於 CVDI（當前 schema）
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
    private lateinit var cvdiMapper: CvdiMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val today = LocalDate.now()

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM CV.CVDI")
    }

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return dividend levels for plan code`() {
            // Given
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, today)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 2, today)
            insertTestDividendLevel("PLAN2", "1", "01", "0", 1, today)

            // When
            val result = cvdiMapper.findByPlanCode("PLAN1", "1")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.planCode == "PLAN1" })
            println("✓ findByPlanCode: 成功查詢險種 PLAN1 的 ${result.size} 筆紅利分配水準")
        }

        @Test
        fun `should return empty list for unknown plan code`() {
            // When
            val result = cvdiMapper.findByPlanCode("UNKNOWN", "1")

            // Then
            assertTrue(result.isEmpty())
            println("✓ findByPlanCode: 未知險種正確返回空清單")
        }
    }

    @Nested
    @DisplayName("findByPlanCodeAndPaidStatus")
    inner class FindByPlanCodeAndPaidStatus {

        @Test
        fun `should return dividend levels by paid status`() {
            // Given - 01=繳費中, 99=繳清
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, today)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 2, today)
            insertTestDividendLevel("PLAN1", "1", "99", "0", 1, today)

            // When
            val result = cvdiMapper.findByPlanCodeAndPaidStatus("PLAN1", "1", "01")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.paidStatus == "01" })
            println("✓ findByPlanCodeAndPaidStatus: 成功查詢繳費中 (01) 的 ${result.size} 筆紅利分配水準")
        }
    }

    @Nested
    @DisplayName("findByCondition")
    inner class FindByCondition {

        @Test
        fun `should return dividend level matching condition`() {
            // Given - 設定適合查詢的範圍
            insertTestDividendLevel("PLAN1", "1", "01", "0", 5, today, 20, 50, 0, 999999999, 0.00, 999999.99)

            // When - 查詢年齡 30，在 20-50 範圍內
            val result = cvdiMapper.findByCondition(
                planCode = "PLAN1",
                version = "1",
                paidStatus = "01",
                rateSex = "0",
                age = 30,
                faceAmt = 1000000,
                modePrem = BigDecimal("50000.00"),
                policyYear = 5,
                declareDate = today
            )

            // Then
            assertNotNull(result)
            assertEquals("PLAN1", result.planCode)
            assertEquals(5, result.policyYear)
            println("✓ findByCondition: 成功查詢符合條件的紅利分配水準")
        }

        @Test
        fun `should return null when no match`() {
            // Given
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, today)

            // When - 查詢不存在的保單年度
            val result = cvdiMapper.findByCondition(
                planCode = "PLAN1",
                version = "1",
                paidStatus = "01",
                rateSex = "0",
                age = 30,
                faceAmt = 1000000,
                modePrem = BigDecimal("50000.00"),
                policyYear = 999,
                declareDate = today
            )

            // Then
            assertNull(result)
            println("✓ findByCondition: 無符合條件時正確返回 null")
        }
    }

    @Nested
    @DisplayName("findAllPlanCodes")
    inner class FindAllPlanCodes {

        @Test
        fun `should return distinct plan codes`() {
            // Given
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, today)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 2, today)
            insertTestDividendLevel("PLAN2", "1", "01", "0", 1, today)
            insertTestDividendLevel("PLAN3", "1", "01", "0", 1, today)

            // When
            val result = cvdiMapper.findAllPlanCodes()

            // Then
            assertEquals(3, result.size)
            assertTrue(result.contains("PLAN1"))
            assertTrue(result.contains("PLAN2"))
            assertTrue(result.contains("PLAN3"))
            println("✓ findAllPlanCodes: 成功取得 ${result.size} 個不重複的險種代碼")
        }
    }

    @Nested
    @DisplayName("findDeclareDates")
    inner class FindDeclareDates {

        @Test
        fun `should return distinct declare dates`() {
            // Given
            val date1 = today
            val date2 = today.minusMonths(1)
            val date3 = today.minusMonths(2)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, date1)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 2, date1)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, date2)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, date3)

            // When
            val result = cvdiMapper.findDeclareDates("PLAN1", "1")

            // Then
            assertEquals(3, result.size)
            println("✓ findDeclareDates: 成功取得 ${result.size} 個不重複的宣告日期")
        }
    }

    @Nested
    @DisplayName("countByPlanCode")
    inner class CountByPlanCode {

        @Test
        fun `should return correct count`() {
            // Given
            insertTestDividendLevel("PLAN1", "1", "01", "0", 1, today)
            insertTestDividendLevel("PLAN1", "1", "01", "0", 2, today)
            insertTestDividendLevel("PLAN1", "1", "99", "0", 1, today)

            // When
            val count = cvdiMapper.countByPlanCode("PLAN1", "1")

            // Then
            assertEquals(3, count)
            println("✓ countByPlanCode: 正確計算紅利分配水準數量為 $count")
        }

        @Test
        fun `should return zero for unknown plan code`() {
            // When
            val count = cvdiMapper.countByPlanCode("UNKNOWN", "1")

            // Then
            assertEquals(0, count)
            println("✓ countByPlanCode: 未知險種正確返回 0")
        }
    }

    /**
     * 插入測試紅利分配水準資料
     *
     * 欄位驗證規則（Cvdi Entity init block）：
     * - planCode: 1-5 字元
     * - version: 必須恰好 1 字元
     * - paidStatus: 必須恰好 2 字元
     * - rateSex: 必須恰好 1 字元
     */
    private fun insertTestDividendLevel(
        planCode: String,
        version: String,
        paidStatus: String,
        rateSex: String,
        policyYear: Int,
        declareDate: LocalDate,
        ageLimitStart: Int = 0,
        ageLimitEnd: Int = 999,
        faceAmtStart: Long = 0,
        faceAmtEnd: Long = 999999999,
        modePremStart: Double = 0.00,
        modePremEnd: Double = 999999.99
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO CV.CVDI (
                PLAN_CODE, VERSION, PAID_STATUS, RATE_SEX,
                AGE_LIMIT_STR, AGE_LIMIT_END, FACE_AMT_STR, FACE_AMT_END,
                MODE_PREM_S, MODE_PREM_E, POLICY_YEAR, DECL_DATE,
                RATE_RATIO, DEATH_RATIO, LOADING_RATIO
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            planCode, version, paidStatus, rateSex,
            ageLimitStart, ageLimitEnd, faceAmtStart, faceAmtEnd,
            BigDecimal.valueOf(modePremStart), BigDecimal.valueOf(modePremEnd),
            policyYear, java.sql.Date.valueOf(declareDate),
            BigDecimal("0.05"),  // RATE_RATIO
            BigDecimal("0.01"),  // DEATH_RATIO
            BigDecimal("0.02")   // LOADING_RATIO
        )
    }
}
