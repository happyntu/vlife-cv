package com.vlife.cv

import com.vlife.cv.plan.Pldf
import com.vlife.cv.plan.PldfMapper
import com.vlife.cv.plan.PldfUpdateRequest
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PldfMapper 整合測試
 *
 * 使用 TestContainers Oracle XE 進行真實資料庫測試。
 * 對應 V3 PK_CV_CV001M (險種描述資料維護) 的 CRUD 操作。
 *
 * ADR-022: PLDF 業務歸屬於 CV 模組
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("PldfMapper 整合測試 (CV001M)")
class PldfMapperIntegrationTest {

    companion object {
        private const val ORACLE_IMAGE = "gvenzl/oracle-xe:21-slim"

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
    private lateinit var pldfMapper: PldfMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM CV.PLDF")
    }

    // ==================== 查詢測試 ====================

    @Nested
    @DisplayName("findByPlanCodeAndVersion")
    inner class FindByPlanCodeAndVersion {

        @Test
        fun `should return plan definition when exists`() {
            // Given
            insertTestPlanDefinition("12RA1", "1")

            // When
            val result = pldfMapper.findByPlanCodeAndVersion("12RA1", "1")

            // Then
            assertNotNull(result)
            assertEquals("12RA1", result.planCode)
            assertEquals("1", result.version)
            println("✓ findByPlanCodeAndVersion: 成功查詢存在的險種 12RA1/1")
        }

        @Test
        fun `should return null when not exists`() {
            // When
            val result = pldfMapper.findByPlanCodeAndVersion("XXXXX", "9")

            // Then
            assertNull(result)
            println("✓ findByPlanCodeAndVersion: 不存在時正確返回 null")
        }
    }

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return all versions for plan code`() {
            // Given
            insertTestPlanDefinition("12RA1", "1")
            insertTestPlanDefinition("12RA1", "2")
            insertTestPlanDefinition("12RA2", "1")

            // When
            val result = pldfMapper.findByPlanCode("12RA1")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.planCode == "12RA1" })
            println("✓ findByPlanCode: 成功查詢險種 12RA1 的 ${result.size} 個版本")
        }

        @Test
        fun `should return empty list for unknown plan code`() {
            // When
            val result = pldfMapper.findByPlanCode("XXXXX")

            // Then
            assertTrue(result.isEmpty())
            println("✓ findByPlanCode: 未知險種正確返回空清單")
        }
    }

    @Nested
    @DisplayName("findEffective")
    inner class FindEffective {

        @Test
        fun `should return effective plans for date`() {
            // Given - 有效期 2024-01-01 ~ 2024-12-31
            insertTestPlanDefinition(
                "EFFCT", "1",
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31)
            )
            // 已停賣
            insertTestPlanDefinition(
                "EXPRD", "1",
                startDate = LocalDate.of(2020, 1, 1),
                endDate = LocalDate.of(2023, 12, 31)
            )

            // When - 查詢 2024-06-15 有效的險種
            val result = pldfMapper.findEffective(LocalDate.of(2024, 6, 15))

            // Then
            assertEquals(1, result.size)
            assertEquals("EFFCT", result[0].planCode)
            println("✓ findEffective: 成功查詢有效險種 ${result.size} 筆")
        }
    }

    @Nested
    @DisplayName("search")
    inner class Search {

        @Test
        fun `should search by plan code pattern`() {
            // Given
            insertTestPlanDefinition("12RA1", "1")
            insertTestPlanDefinition("12RA2", "1")
            insertTestPlanDefinition("AN001", "1")

            // When
            val result = pldfMapper.search(planCode = "12RA")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.planCode.startsWith("12RA") })
            println("✓ search: 模糊查詢 '12RA' 找到 ${result.size} 筆")
        }

        @Test
        fun `should search by primary rider indicator`() {
            // Given
            insertTestPlanDefinition("PRIM1", "1", primaryRiderInd = "P")
            insertTestPlanDefinition("RIDR1", "1", primaryRiderInd = "R")
            insertTestPlanDefinition("RIDR2", "1", primaryRiderInd = "R")

            // When
            val result = pldfMapper.search(primaryRiderInd = "R")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.primaryRiderInd == "R" })
            println("✓ search: 查詢附約 (R) 找到 ${result.size} 筆")
        }

        @Test
        fun `should search by insurance type 3`() {
            // Given
            insertTestPlanDefinition("LIFE1", "1", insuranceType3 = "A")
            insertTestPlanDefinition("HLTH1", "1", insuranceType3 = "B")
            insertTestPlanDefinition("INVST", "1", insuranceType3 = "F")

            // When
            val result = pldfMapper.search(insuranceType3 = "A")

            // Then
            assertEquals(1, result.size)
            assertEquals("LIFE1", result[0].planCode)
            println("✓ search: 查詢壽險 (A) 找到 ${result.size} 筆")
        }
    }

    @Nested
    @DisplayName("findAllPlanCodes")
    inner class FindAllPlanCodes {

        @Test
        fun `should return distinct plan codes`() {
            // Given
            insertTestPlanDefinition("12RA1", "1")
            insertTestPlanDefinition("12RA1", "2")
            insertTestPlanDefinition("AN001", "1")

            // When
            val result = pldfMapper.findAllPlanCodes()

            // Then
            assertEquals(2, result.size)
            assertTrue(result.contains("12RA1"))
            assertTrue(result.contains("AN001"))
            println("✓ findAllPlanCodes: 取得 ${result.size} 個不重複的險種代碼")
        }
    }

    @Nested
    @DisplayName("countByPlanCode")
    inner class CountByPlanCode {

        @Test
        fun `should count versions for plan code`() {
            // Given
            insertTestPlanDefinition("12RA1", "1")
            insertTestPlanDefinition("12RA1", "2")
            insertTestPlanDefinition("12RA1", "3")

            // When
            val result = pldfMapper.countByPlanCode("12RA1")

            // Then
            assertEquals(3, result)
            println("✓ countByPlanCode: 險種 12RA1 有 $result 個版本")
        }
    }

    @Nested
    @DisplayName("existsByPlanCodeAndVersion")
    inner class ExistsByPlanCodeAndVersion {

        @Test
        fun `should return 1 when exists`() {
            // Given
            insertTestPlanDefinition("12RA1", "1")

            // When
            val result = pldfMapper.existsByPlanCodeAndVersion("12RA1", "1")

            // Then
            assertEquals(1, result)
            println("✓ existsByPlanCodeAndVersion: 存在時返回 1")
        }

        @Test
        fun `should return 0 when not exists`() {
            // When
            val result = pldfMapper.existsByPlanCodeAndVersion("XXXXX", "9")

            // Then
            assertEquals(0, result)
            println("✓ existsByPlanCodeAndVersion: 不存在時返回 0")
        }
    }

    // ==================== CUD 測試 (CV001M) ====================

    @Nested
    @DisplayName("insert")
    inner class Insert {

        @Test
        fun `should insert plan definition successfully`() {
            // Given
            val today = LocalDate.now()
            val pldf = createTestPldf("NEW01", "1", today)

            // When
            val result = pldfMapper.insert(pldf)

            // Then
            assertEquals(1, result)

            // Verify
            val inserted = pldfMapper.findByPlanCodeAndVersion("NEW01", "1")
            assertNotNull(inserted)
            assertEquals("NEW01", inserted.planCode)
            assertEquals("測試險種", inserted.planTitle)
            assertEquals("TWD", inserted.currency1)
            println("✓ insert: 成功新增險種 NEW01/1")
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {

        @Test
        fun `should update plan definition successfully`() {
            // Given
            insertTestPlanDefinition("UPD01", "1")

            val updateRequest = PldfUpdateRequest(
                planTitle = "更新後名稱",
                primaryRiderInd = "R",
                loanAvalInd = "N"
            )

            // When
            val result = pldfMapper.update("UPD01", "1", updateRequest)

            // Then
            assertEquals(1, result)

            // Verify
            val updated = pldfMapper.findByPlanCodeAndVersion("UPD01", "1")
            assertNotNull(updated)
            assertEquals("更新後名稱", updated.planTitle)
            assertEquals("R", updated.primaryRiderInd)
            assertEquals("N", updated.loanAvalInd)
            println("✓ update: 成功更新險種 UPD01/1")
        }

        @Test
        fun `should return 0 when updating non-existent record`() {
            // Given
            val updateRequest = PldfUpdateRequest(planTitle = "新名稱")

            // When
            val result = pldfMapper.update("XXXXX", "9", updateRequest)

            // Then
            assertEquals(0, result)
            println("✓ update: 不存在的記錄正確返回 0")
        }
    }

    @Nested
    @DisplayName("deleteByPlanCodeAndVersion")
    inner class DeleteByPlanCodeAndVersion {

        @Test
        fun `should delete plan definition successfully`() {
            // Given
            insertTestPlanDefinition("DEL01", "1")
            assertNotNull(pldfMapper.findByPlanCodeAndVersion("DEL01", "1"))

            // When
            val result = pldfMapper.deleteByPlanCodeAndVersion("DEL01", "1")

            // Then
            assertEquals(1, result)
            assertNull(pldfMapper.findByPlanCodeAndVersion("DEL01", "1"))
            println("✓ deleteByPlanCodeAndVersion: 成功刪除險種 DEL01/1")
        }

        @Test
        fun `should return 0 when deleting non-existent record`() {
            // When
            val result = pldfMapper.deleteByPlanCodeAndVersion("XXXXX", "9")

            // Then
            assertEquals(0, result)
            println("✓ deleteByPlanCodeAndVersion: 不存在的記錄正確返回 0")
        }
    }

    // ==================== Helper Methods ====================

    private fun insertTestPlanDefinition(
        planCode: String,
        version: String,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate = LocalDate.now().plusYears(10),
        primaryRiderInd: String = "P",
        insuranceType3: String = "A"
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO CV.PLDF (
                PLAN_CODE, VERSION, PLAN_TITLE, LOW_AGE, HIGH_AGE,
                COLLECT_YEAR_IND, EXP_YEAR_IND, PLAN_START_DATE, PLAN_END_DATE,
                PRIMARY_RIDER_IND, INSURANCE_TYPE_3, CURRENCY_1, PLAN_ACCOUNT_IND,
                DIV_SW_M, CSV_CALC_TYPE, PUA_CALC_TYPE, ETE_CALC_TYPE,
                UW_PLAN_CODE, UW_VERSION, ANNY_SW, PREM_LACK_IND,
                PERSIST_REWARD_IND, PERSIST_PREM_VAL
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            planCode, version, "測試險種", 0, 70,
            "1", "2", java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate),
            primaryRiderInd, insuranceType3, "TWD", "N",
            "N", "001", "0001", "000001",
            planCode, version, "N", "N",
            "N", 0
        )
    }

    private fun createTestPldf(planCode: String, version: String, startDate: LocalDate): Pldf {
        return Pldf(
            planCode = planCode,
            version = version,
            planTitle = "測試險種",
            planName = "測試險種全名",
            contractedName = "測試契約名稱",
            lowAge = 0,
            highAge = 70,
            collectYearInd = "1",
            collectYear = 20,
            expYearInd = "2",
            expYear = null,
            planStartDate = startDate,
            planEndDate = startDate.plusYears(10),
            primaryRiderInd = "P",
            insuranceType = "1",
            insuranceType3 = "A",
            planType = "1",
            currency1 = "TWD",
            planAccountInd = "N",
            divType = "00",
            divSwM = "N",
            csvCalcType = "001",
            puaCalcType = "0001",
            eteCalcType = "000001",
            loanAvalInd = "Y",
            commClassCode = "00001",
            commClassCodeI = "1",
            uwPlanCode = planCode,
            uwVersion = version,
            annySw = "N",
            premLackInd = "N",
            persistRewardInd = "N",
            persistPremVal = 0
        )
    }
}
