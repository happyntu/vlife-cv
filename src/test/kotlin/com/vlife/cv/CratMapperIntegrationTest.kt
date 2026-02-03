package com.vlife.cv

import com.vlife.cv.commission.CratMapper
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
import org.testcontainers.utility.MountableFile
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CratMapper 整合測試
 *
 * 使用 TestContainers Oracle XE 進行真實資料庫測試。
 *
 * 重要：使用 withCopyFileToContainer() 將初始化腳本放到
 * /container-entrypoint-initdb.d/ 目錄，確保以 SYSDBA 身份執行，
 * 才能正確建立 CV Schema 並授權。
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("CratMapper 整合測試")
class CratMapperIntegrationTest {

    companion object {
        private const val ORACLE_IMAGE = "gvenzl/oracle-xe:21-slim"

        /**
         * 使用 withInitScript() 在連線用戶 schema 中建立表格
         * CV 用戶連線後，CV.CRAT 等同於 CRAT（當前 schema）
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
    private lateinit var cratMapper: CratMapper

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("DELETE FROM CV.CRAT")
    }

    @Nested
    @DisplayName("findBySerial")
    inner class FindBySerial {

        @Test
        fun `should return commission rate when exists`() {
            // Given - 使用符合 Crat 驗證規則的測試值
            // commClassCode: 1-5 chars, commLineCode: 1-2 chars
            insertTestCommissionRate(1L, "12RA1", "31", 0.05)

            // When
            val result = cratMapper.findBySerial(1L)

            // Then
            assertNotNull(result)
            assertEquals(1L, result.serial)
            assertEquals("12RA1", result.commClassCode)
            println("✓ findBySerial: 成功查詢存在的佣金率")
        }

        @Test
        fun `should return null when not exists`() {
            // When
            val result = cratMapper.findBySerial(9999L)

            // Then
            assertNull(result)
            println("✓ findBySerial: 不存在時正確返回 null")
        }
    }

    @Nested
    @DisplayName("findByClassCode")
    inner class FindByClassCode {

        @Test
        fun `should return rates for class code`() {
            // Given - 使用符合 Crat 驗證規則的測試值
            insertTestCommissionRate(1L, "12RA1", "31", 0.05)
            insertTestCommissionRate(2L, "12RA1", "21", 0.06)
            insertTestCommissionRate(3L, "12RA2", "31", 0.07)

            // When
            val result = cratMapper.findByClassCode("12RA1")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.commClassCode == "12RA1" })
            println("✓ findByClassCode: 成功查詢類別碼 12RA1 的 ${result.size} 筆資料")
        }

        @Test
        fun `should return empty list for unknown class code`() {
            // When
            val result = cratMapper.findByClassCode("XXXXX")

            // Then
            assertTrue(result.isEmpty())
            println("✓ findByClassCode: 未知類別碼正確返回空清單")
        }
    }

    @Nested
    @DisplayName("findAllLineCodes")
    inner class FindAllLineCodes {

        @Test
        fun `should return distinct line codes`() {
            // Given - 使用符合 Crat 驗證規則的測試值
            insertTestCommissionRate(1L, "12RA1", "31", 0.05)
            insertTestCommissionRate(2L, "12RA1", "21", 0.06)
            insertTestCommissionRate(3L, "12RA2", "31", 0.07)

            // When
            val result = cratMapper.findAllLineCodes()

            // Then
            assertEquals(2, result.size)
            assertTrue(result.contains("31"))
            assertTrue(result.contains("21"))
            println("✓ findAllLineCodes: 成功取得 ${result.size} 個不重複的業務線代號")
        }
    }

    private fun insertTestCommissionRate(serial: Long, classCode: String, lineCode: String, rate: Double) {
        val today = LocalDate.now()
        val endDate = today.plusYears(1)
        jdbcTemplate.update(
            """
            INSERT INTO CV.CRAT (CRAT_SERIAL, COMM_CLASS_CODE, COMM_LINE_CODE, CRAT_TYPE, CRAT_KEY1, CRAT_KEY2, STR_DATE, END_DATE, COMM_RATE)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            serial, classCode, lineCode, "1", "K1", "K2",
            java.sql.Date.valueOf(today), java.sql.Date.valueOf(endDate), rate
        )
    }
}
