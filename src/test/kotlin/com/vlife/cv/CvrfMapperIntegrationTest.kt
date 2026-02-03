package com.vlife.cv

import com.vlife.cv.actuarial.CvrfMapper
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CvrfMapper 整合測試
 *
 * 使用 TestContainers Oracle XE 進行真實資料庫測試。
 * 測試準備金因子檔 (CV.CVRF) 的查詢操作。
 *
 * 注意：CVRF 是跨模組使用的表格（BL, CL, CV）
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("CvrfMapper 整合測試")
class CvrfMapperIntegrationTest {

    companion object {
        private const val ORACLE_IMAGE = "gvenzl/oracle-xe:21-slim"

        /**
         * 使用 withInitScript() 在連線用戶 schema 中建立表格
         * CV 用戶連線後，CV.CVRF 等同於 CVRF（當前 schema）
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
    private lateinit var cvrfMapper: CvrfMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM CV.CVRF")
    }

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return reserve factors for plan code`() {
            // Given - 期間類型：1=終身, 2=定期, 3=其他
            insertTestReserveFactor("PLAN1", "1", 1)  // 終身
            insertTestReserveFactor("PLAN1", "1", 2)  // 定期
            insertTestReserveFactor("PLAN2", "1", 1)  // 另一險種

            // When
            val result = cvrfMapper.findByPlanCode("PLAN1", "1")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.planCode == "PLAN1" })
            println("✓ findByPlanCode: 成功查詢險種 PLAN1 的 ${result.size} 筆準備金因子")
        }

        @Test
        fun `should return empty list for unknown plan code`() {
            // When
            val result = cvrfMapper.findByPlanCode("UNKNOWN", "1")

            // Then
            assertTrue(result.isEmpty())
            println("✓ findByPlanCode: 未知險種正確返回空清單")
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        fun `should return reserve factor when exists`() {
            // Given
            insertTestReserveFactor("PLAN1", "1", 1)

            // When
            val result = cvrfMapper.findById("PLAN1", "1", 1)

            // Then
            assertNotNull(result)
            assertEquals("PLAN1", result.planCode)
            assertEquals("1", result.version)
            assertEquals(1, result.durationType)
            assertTrue(result.isWholeLife())
            println("✓ findById: 成功查詢存在的準備金因子")
        }

        @Test
        fun `should return null when not exists`() {
            // When
            val result = cvrfMapper.findById("PLAN1", "1", 999)

            // Then
            assertNull(result)
            println("✓ findById: 不存在時正確返回 null")
        }
    }

    @Nested
    @DisplayName("findByDurationType")
    inner class FindByDurationType {

        @Test
        fun `should return reserve factors by duration type`() {
            // Given - 期間類型：1=終身, 2=定期, 3=其他
            insertTestReserveFactor("PLAN1", "1", 1)  // 終身
            insertTestReserveFactor("PLAN2", "1", 1)  // 終身
            insertTestReserveFactor("PLAN3", "1", 2)  // 定期

            // When
            val result = cvrfMapper.findByDurationType(1)  // 查詢終身險

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.durationType == 1 })
            println("✓ findByDurationType: 成功查詢期間類型 1 (終身) 的 ${result.size} 筆準備金因子")
        }
    }

    @Nested
    @DisplayName("findAllPlanCodes")
    inner class FindAllPlanCodes {

        @Test
        fun `should return distinct plan codes`() {
            // Given
            insertTestReserveFactor("PLAN1", "1", 1)
            insertTestReserveFactor("PLAN1", "1", 2)
            insertTestReserveFactor("PLAN2", "1", 1)
            insertTestReserveFactor("PLAN3", "1", 1)

            // When
            val result = cvrfMapper.findAllPlanCodes()

            // Then
            assertEquals(3, result.size)
            assertTrue(result.contains("PLAN1"))
            assertTrue(result.contains("PLAN2"))
            assertTrue(result.contains("PLAN3"))
            println("✓ findAllPlanCodes: 成功取得 ${result.size} 個不重複的險種代碼")
        }
    }

    @Nested
    @DisplayName("countByPlanCode")
    inner class CountByPlanCode {

        @Test
        fun `should return correct count`() {
            // Given
            insertTestReserveFactor("PLAN1", "1", 1)
            insertTestReserveFactor("PLAN1", "1", 2)
            insertTestReserveFactor("PLAN1", "1", 3)

            // When
            val count = cvrfMapper.countByPlanCode("PLAN1", "1")

            // Then
            assertEquals(3, count)
            println("✓ countByPlanCode: 正確計算準備金因子數量為 $count")
        }

        @Test
        fun `should return zero for unknown plan code`() {
            // When
            val count = cvrfMapper.countByPlanCode("UNKNOWN", "1")

            // Then
            assertEquals(0, count)
            println("✓ countByPlanCode: 未知險種正確返回 0")
        }
    }

    /**
     * 插入測試準備金因子資料
     *
     * 欄位驗證規則（Cvrf Entity init block）：
     * - planCode: 1-5 字元
     * - version: 必須恰好 1 字元
     * - durationType: 必須是 1, 2, 或 3
     *
     * NOT NULL 欄位：
     * - RETURN_COST_F
     * - MODIFY_RV_IND
     * - RECORD_TYPE_10
     * - MIX_RVF_IND
     */
    private fun insertTestReserveFactor(
        planCode: String,
        version: String,
        durationType: Int
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO CV.CVRF (
                PLAN_CODE, VERSION, DUR_TYPE,
                RETURN_COST_F, MODIFY_RV_IND, RECORD_TYPE_10, MIX_RVF_IND
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            planCode, version, durationType,
            "N",   // RETURN_COST_F: N=否
            "N",   // MODIFY_RV_IND: N=否
            "01",  // RECORD_TYPE_10: 記錄類型 (10 碼，但這裡用 2 碼簡化)
            "N"    // MIX_RVF_IND: N=否
        )
    }
}
