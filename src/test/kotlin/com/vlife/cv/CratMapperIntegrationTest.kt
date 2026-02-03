package com.vlife.cv

import com.vlife.cv.commission.Crat
import com.vlife.cv.commission.CratMapper
import com.vlife.cv.commission.CratUpdateRequest
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

    // ==================== CUD 操作整合測試 (CV004M) ====================

    @Nested
    @DisplayName("nextSerial")
    inner class NextSerial {

        @Test
        fun `should return incrementing serial numbers`() {
            // When
            val serial1 = cratMapper.nextSerial()
            val serial2 = cratMapper.nextSerial()

            // Then
            assertTrue(serial1 > 0)
            assertTrue(serial2 > serial1)
            println("✓ nextSerial: 成功取得遞增序號 $serial1, $serial2")
        }
    }

    @Nested
    @DisplayName("insert")
    inner class Insert {

        @Test
        fun `should insert commission rate successfully`() {
            // Given
            val serial = cratMapper.nextSerial()
            val today = LocalDate.now()
            val crat = Crat(
                serial = serial,
                commClassCode = "TEST1",
                commLineCode = "31",
                cratType = "1",
                projectNo = null,
                startDate = today,
                endDate = today.plusYears(1),
                cratKey1 = "001",
                cratKey2 = "010",
                commStartYear = 1,
                commEndYear = 5,
                commStartAge = 20,
                commEndAge = 65,
                commStartModx = null,
                commEndModx = null,
                commRate = BigDecimal("0.050000"),
                commRateOrg = BigDecimal("0.055000"),
                premLimitStart = null,
                premLimitEnd = null
            )

            // When
            val result = cratMapper.insert(crat)

            // Then
            assertEquals(1, result)

            // Verify inserted data
            val inserted = cratMapper.findBySerial(serial)
            assertNotNull(inserted)
            assertEquals("TEST1", inserted.commClassCode)
            assertEquals("31", inserted.commLineCode)
            assertEquals(BigDecimal("0.050000"), inserted.commRate)
            println("✓ insert: 成功新增佣金率 serial=$serial")
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {

        @Test
        fun `should update commission rate successfully`() {
            // Given
            insertTestCommissionRate(100L, "12RA1", "31", 0.05)

            val updateRequest = CratUpdateRequest(
                commRate = BigDecimal("0.080000"),
                commStartYear = 1,
                commEndYear = 10
            )

            // When
            val result = cratMapper.update(100L, updateRequest)

            // Then
            assertEquals(1, result)

            // Verify updated data
            val updated = cratMapper.findBySerial(100L)
            assertNotNull(updated)
            assertEquals(BigDecimal("0.080000"), updated.commRate)
            assertEquals(1, updated.commStartYear)
            assertEquals(10, updated.commEndYear)
            println("✓ update: 成功更新佣金率 serial=100")
        }

        @Test
        fun `should return 0 when updating non-existent record`() {
            // Given
            val updateRequest = CratUpdateRequest(
                commRate = BigDecimal("0.100000")
            )

            // When
            val result = cratMapper.update(99999L, updateRequest)

            // Then
            assertEquals(0, result)
            println("✓ update: 不存在的記錄正確返回 0")
        }
    }

    @Nested
    @DisplayName("deleteBySerial")
    inner class DeleteBySerial {

        @Test
        fun `should delete commission rate successfully`() {
            // Given
            insertTestCommissionRate(200L, "12RA1", "31", 0.05)

            // Verify exists before delete
            assertNotNull(cratMapper.findBySerial(200L))

            // When
            val result = cratMapper.deleteBySerial(200L)

            // Then
            assertEquals(1, result)
            assertNull(cratMapper.findBySerial(200L))
            println("✓ deleteBySerial: 成功刪除佣金率 serial=200")
        }

        @Test
        fun `should return 0 when deleting non-existent record`() {
            // When
            val result = cratMapper.deleteBySerial(99999L)

            // Then
            assertEquals(0, result)
            println("✓ deleteBySerial: 不存在的記錄正確返回 0")
        }
    }

    @Nested
    @DisplayName("countOverlapping")
    inner class CountOverlapping {

        @Test
        fun `should detect overlapping records`() {
            // Given - 新增一筆日期範圍 2024-01-01 ~ 2024-12-31，key 範圍 001~010
            val today = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 12, 31)
            jdbcTemplate.update(
                """
                INSERT INTO CV.CRAT (CRAT_SERIAL, COMM_CLASS_CODE, COMM_LINE_CODE, CRAT_TYPE, CRAT_KEY1, CRAT_KEY2, STR_DATE, END_DATE, COMM_RATE)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                300L, "OVRLP", "31", "1", "001", "010",
                java.sql.Date.valueOf(today), java.sql.Date.valueOf(endDate), 0.05
            )

            // When - 檢查完全重疊的情況
            val count = cratMapper.countOverlapping(
                commClassCode = "OVRLP",
                commLineCode = "31",
                cratType = "1",
                projectNo = null,
                startDate = LocalDate.of(2024, 6, 1),
                endDate = LocalDate.of(2024, 6, 30),
                cratKey1 = "005",
                cratKey2 = "008"
            )

            // Then
            assertEquals(1, count)
            println("✓ countOverlapping: 成功檢測到 $count 筆重疊記錄")
        }

        @Test
        fun `should not detect non-overlapping records by date`() {
            // Given
            val today = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 6, 30)
            jdbcTemplate.update(
                """
                INSERT INTO CV.CRAT (CRAT_SERIAL, COMM_CLASS_CODE, COMM_LINE_CODE, CRAT_TYPE, CRAT_KEY1, CRAT_KEY2, STR_DATE, END_DATE, COMM_RATE)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                301L, "NOOVR", "31", "1", "001", "010",
                java.sql.Date.valueOf(today), java.sql.Date.valueOf(endDate), 0.05
            )

            // When - 檢查日期不重疊的情況 (2024-07-01 ~ 2024-12-31)
            val count = cratMapper.countOverlapping(
                commClassCode = "NOOVR",
                commLineCode = "31",
                cratType = "1",
                projectNo = null,
                startDate = LocalDate.of(2024, 7, 1),
                endDate = LocalDate.of(2024, 12, 31),
                cratKey1 = "005",
                cratKey2 = "008"
            )

            // Then
            assertEquals(0, count)
            println("✓ countOverlapping: 日期不重疊正確返回 0")
        }

        @Test
        fun `should not detect non-overlapping records by key range`() {
            // Given
            val today = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 12, 31)
            jdbcTemplate.update(
                """
                INSERT INTO CV.CRAT (CRAT_SERIAL, COMM_CLASS_CODE, COMM_LINE_CODE, CRAT_TYPE, CRAT_KEY1, CRAT_KEY2, STR_DATE, END_DATE, COMM_RATE)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                302L, "KEYNO", "31", "1", "001", "010",
                java.sql.Date.valueOf(today), java.sql.Date.valueOf(endDate), 0.05
            )

            // When - 檢查 key 範圍不重疊的情況 (011~020)
            val count = cratMapper.countOverlapping(
                commClassCode = "KEYNO",
                commLineCode = "31",
                cratType = "1",
                projectNo = null,
                startDate = today,
                endDate = endDate,
                cratKey1 = "011",
                cratKey2 = "020"
            )

            // Then
            assertEquals(0, count)
            println("✓ countOverlapping: key 範圍不重疊正確返回 0")
        }

        @Test
        fun `should exclude specified serial when checking overlap`() {
            // Given
            val today = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 12, 31)
            jdbcTemplate.update(
                """
                INSERT INTO CV.CRAT (CRAT_SERIAL, COMM_CLASS_CODE, COMM_LINE_CODE, CRAT_TYPE, CRAT_KEY1, CRAT_KEY2, STR_DATE, END_DATE, COMM_RATE)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                303L, "EXCLD", "31", "1", "001", "010",
                java.sql.Date.valueOf(today), java.sql.Date.valueOf(endDate), 0.05
            )

            // When - 排除自身記錄的重疊檢查（用於更新時）
            val count = cratMapper.countOverlapping(
                commClassCode = "EXCLD",
                commLineCode = "31",
                cratType = "1",
                projectNo = null,
                startDate = today,
                endDate = endDate,
                cratKey1 = "001",
                cratKey2 = "010",
                excludeSerial = 303L
            )

            // Then
            assertEquals(0, count)
            println("✓ countOverlapping: 排除自身記錄後正確返回 0")
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
