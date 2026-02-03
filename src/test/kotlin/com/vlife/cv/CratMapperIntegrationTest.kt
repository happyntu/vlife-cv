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
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CratMapper 整合測試
 *
 * 使用 TestContainers Oracle XE 進行真實資料庫測試。
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("CratMapper 整合測試")
class CratMapperIntegrationTest {

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
            // Given
            insertTestCommissionRate(1L, "CLASS01", "LINE01", 0.05)

            // When
            val result = cratMapper.findBySerial(1L)

            // Then
            assertNotNull(result)
            assertEquals(1L, result.serial)
            assertEquals("CLASS01", result.commClassCode)
        }

        @Test
        fun `should return null when not exists`() {
            // When
            val result = cratMapper.findBySerial(9999L)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findByClassCode")
    inner class FindByClassCode {

        @Test
        fun `should return rates for class code`() {
            // Given
            insertTestCommissionRate(1L, "CLASS01", "LINE01", 0.05)
            insertTestCommissionRate(2L, "CLASS01", "LINE02", 0.06)
            insertTestCommissionRate(3L, "CLASS02", "LINE01", 0.07)

            // When
            val result = cratMapper.findByClassCode("CLASS01")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.commClassCode == "CLASS01" })
        }

        @Test
        fun `should return empty list for unknown class code`() {
            // When
            val result = cratMapper.findByClassCode("UNKNOWN")

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("findAllLineCodes")
    inner class FindAllLineCodes {

        @Test
        fun `should return distinct line codes`() {
            // Given
            insertTestCommissionRate(1L, "CLASS01", "LINE01", 0.05)
            insertTestCommissionRate(2L, "CLASS01", "LINE02", 0.06)
            insertTestCommissionRate(3L, "CLASS02", "LINE01", 0.07)

            // When
            val result = cratMapper.findAllLineCodes()

            // Then
            assertEquals(2, result.size)
            assertTrue(result.contains("LINE01"))
            assertTrue(result.contains("LINE02"))
        }
    }

    private fun insertTestCommissionRate(serial: Long, classCode: String, lineCode: String, rate: Double) {
        val today = LocalDate.now()
        val endDate = today.plusYears(1)
        jdbcTemplate.update(
            """
            INSERT INTO CV.CRAT (CRAT_SERIAL, COMM_CLASS_CODE, COMM_LINE_CODE, STR_DATE, END_DATE, COMM_RATE)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            serial, classCode, lineCode,
            java.sql.Date.valueOf(today), java.sql.Date.valueOf(endDate), rate
        )
    }
}
