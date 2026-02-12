package com.vlife.cv.rate

import com.github.benmanes.caffeine.cache.Cache
import com.vlife.cv.TestApplication
import com.vlife.cv.TestConfiguration
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
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
import javax.sql.DataSource

/**
 * PratService Integration Test
 *
 * 驗證 PratService 與 Caffeine Cache 的整合行為。
 *
 * 測試重點：
 * 1. 基本 CRUD 操作
 * 2. Caffeine Cache 行為（命中、失效）
 * 3. 批次查詢
 * 4. 邊界條件
 *
 * 使用 Singleton Container Pattern，確保容器在所有測試類別間共享。
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("PratService 整合測試")
class PratServiceIntegrationTest {

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
    private lateinit var pratService: PratService

    @Autowired
    private lateinit var pratMapper: PratMapper

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dataSource: DataSource

    private lateinit var planRateCache: Cache<Any, Any>

    @BeforeEach
    fun setupTest() {
        // 清理資料庫
        cleanDatabase()

        // 取得 Caffeine Cache 實例
        val caffeineCache = cacheManager.getCache("planRate") as? CaffeineCache
            ?: throw IllegalStateException("Cache 'planRate' not found or not CaffeineCache")
        planRateCache = caffeineCache.nativeCache

        // 清空快取
        planRateCache.invalidateAll()
    }

    private fun cleanDatabase() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                try {
                    stmt.execute("DELETE FROM PRAT")
                } catch (e: Exception) {
                    // 忽略錯誤
                }
            }
        }
    }

    // ===== 基本 CRUD 測試 =====

    @Nested
    @DisplayName("基本 CRUD 操作")
    inner class BasicCrudTests {

        @Test
        fun `should insert and find insurance rate`() {
            // Given
            val entity = createTestPlanRate(
                planCode = "TEST1",
                version = "1",
                rateSex = "1",
                rateSub1 = "00",
                rateSub2 = "01",
                rateAge = 30
            )

            // When
            pratService.create(entity)
            val result = pratService.getRateDirect(
                "TEST1", "1", "1", "00", "01", 30
            )

            // Then
            assertNotNull(result)
            assertEquals("TEST1", result?.planCode)
            assertEquals(0, BigDecimal("125.50").compareTo(result?.annualPrem))
        }

        @Test
        fun `should update existing insurance rate`() {
            // Given
            insertRateViaJdbc("UPD01", "1", "1", "00", "01", 30, 100.00)

            val key = PlanRateKey("UPD01", "1", "1", "00", "01", 30)
            val updatedEntity = createTestPlanRate(
                planCode = "UPD01",
                version = "1",
                rateSex = "1",
                rateSub1 = "00",
                rateSub2 = "01",
                rateAge = 30,
                annualPrem = BigDecimal("200.00")
            )

            // When
            pratService.update(key, updatedEntity)
            val result = pratService.getRateDirect("UPD01", "1", "1", "00", "01", 30)

            // Then
            assertNotNull(result)
            assertEquals(0, BigDecimal("200.00").compareTo(result?.annualPrem))
        }

        @Test
        fun `should delete insurance rate`() {
            // Given
            insertRateViaJdbc("DEL01", "1", "1", "00", "01", 30, 100.00)

            // When
            pratService.delete("DEL01", "1", "1", "00", "01", 30)
            val result = pratService.getRateDirect("DEL01", "1", "1", "00", "01", 30)

            // Then
            assertNull(result)
        }
    }

    // ===== 快取行為測試（核心） =====

    @Nested
    @DisplayName("快取行為驗證")
    inner class CacheBehaviorTests {

        @Test
        fun `should use cache on second query`() {
            // Given
            insertRateViaJdbc("CACHE1", "1", "1", "00", "01", 30, 100.00)

            // When - 第一次查詢（Cache MISS）
            val result1 = pratService.getRateDirect("CACHE1", "1", "1", "00", "01", 30)
            val stats1 = planRateCache.stats()
            val hitCount1 = stats1.hitCount()
            val missCount1 = stats1.missCount()

            // When - 第二次查詢（Cache HIT）
            val result2 = pratService.getRateDirect("CACHE1", "1", "1", "00", "01", 30)
            val stats2 = planRateCache.stats()
            val hitCount2 = stats2.hitCount()
            val missCount2 = stats2.missCount()

            // Then
            assertNotNull(result1)
            assertNotNull(result2)
            assertEquals(result1?.planCode, result2?.planCode)

            // 驗證快取命中
            assertTrue(hitCount2 > hitCount1, "Cache hit count should increase")
            assertEquals(missCount1, missCount2, "Cache miss count should not change")
        }

        @Test
        fun `should invalidate cache after update`() {
            // Given
            insertRateViaJdbc("INVAL1", "1", "1", "00", "01", 30, 100.00)

            // 第一次查詢，建立快取
            pratService.getRateDirect("INVAL1", "1", "1", "00", "01", 30)
            val hitCountBefore = planRateCache.stats().hitCount()

            // When - 更新資料（應清除快取）
            val key = PlanRateKey("INVAL1", "1", "1", "00", "01", 30)
            val updatedEntity = createTestPlanRate(
                planCode = "INVAL1",
                version = "1",
                rateSex = "1",
                rateSub1 = "00",
                rateSub2 = "01",
                rateAge = 30,
                annualPrem = BigDecimal("200.00")
            )
            pratService.update(key, updatedEntity)

            // Then - 再次查詢應重新從資料庫載入
            val result = pratService.getRateDirect("INVAL1", "1", "1", "00", "01", 30)
            val hitCountAfter = planRateCache.stats().hitCount()

            assertNotNull(result)
            assertEquals(0, BigDecimal("200.00").compareTo(result?.annualPrem))

            // 快取命中數應保持不變（因為快取被清除，需重新載入）
            assertEquals(hitCountBefore, hitCountAfter, "Cache should be invalidated after update")
        }

        @Test
        fun `should invalidate cache after delete`() {
            // Given
            insertRateViaJdbc("DELIN1", "1", "1", "00", "01", 30, 100.00)

            // 第一次查詢，建立快取
            pratService.getRateDirect("DELIN1", "1", "1", "00", "01", 30)

            // When - 刪除資料（應清除快取）
            pratService.delete("DELIN1", "1", "1", "00", "01", 30)

            // Then - 再次查詢應返回 null
            val result = pratService.getRateDirect("DELIN1", "1", "1", "00", "01", 30)
            assertNull(result, "Deleted rate should not be found")
        }

        @Test
        fun `should get cache stats`() {
            // Given
            insertRateViaJdbc("STAT01", "1", "1", "00", "01", 30, 100.00)

            // When
            pratService.getRateDirect("STAT01", "1", "1", "00", "01", 30) // MISS
            pratService.getRateDirect("STAT01", "1", "1", "00", "01", 30) // HIT
            val stats = planRateCache.stats()

            // Then
            assertTrue(stats.hitCount() >= 1, "Cache hit count should be at least 1")
            assertTrue(stats.missCount() >= 1, "Cache miss count should be at least 1")
            assertTrue(stats.hitRate() > 0.0, "Cache hit rate should be greater than 0")
        }
    }

    // ===== 批次查詢測試 =====

    @Nested
    @DisplayName("批次查詢測試")
    inner class BatchQueryTests {

        @Test
        fun `should find all rates by plan code`() {
            // Given
            insertRateViaJdbc("BATCH1", "1", "1", "00", "01", 30, 100.00)
            insertRateViaJdbc("BATCH1", "1", "1", "00", "01", 35, 120.00)
            insertRateViaJdbc("BATCH1", "1", "2", "00", "01", 30, 110.00)

            // When - 使用動態查詢計數
            val count = pratService.countDynamic("BATCH1", "1", "1", null, null, null)

            // Then
            assertEquals(2, count, "Should find 2 rates with rateSex=1")
        }

        @Test
        fun `should return empty list for non-existent plan`() {
            // When
            val count = pratService.countDynamic("NOEXIST", "1", null, null, null, null)

            // Then
            assertEquals(0, count)
        }
    }

    // ===== 邊界條件測試 =====

    @Nested
    @DisplayName("邊界條件測試")
    inner class BoundaryConditionTests {

        @Test
        fun `should return null for non-existent rate`() {
            // When
            val result = pratService.getRateDirect("NOEXIST", "1", "1", "00", "01", 99)

            // Then
            assertNull(result)
        }

        @Test
        fun `should handle empty cache on first query`() {
            // Given
            insertRateViaJdbc("EMPTY1", "1", "1", "00", "01", 30, 100.00)
            planRateCache.invalidateAll()

            // When
            val result = pratService.getRateDirect("EMPTY1", "1", "1", "00", "01", 30)

            // Then
            assertNotNull(result)
            assertEquals("EMPTY1", result?.planCode)
        }

        @Test
        fun `should handle rateSex 0 (no sex distinction)`() {
            // Given
            insertRateViaJdbc("SEX0", "1", "0", "00", "01", 30, 100.00)

            // When
            val result = pratService.getRateDirect("SEX0", "1", "0", "00", "01", 30)

            // Then
            assertNotNull(result)
            assertEquals("0", result?.rateSex)
        }

        @Test
        fun `should handle rate age boundary values`() {
            // Given - 年齡 0
            insertRateViaJdbc("AGE0", "1", "1", "00", "01", 0, 50.00)

            // When
            val result0 = pratService.getRateDirect("AGE0", "1", "1", "00", "01", 0)

            // Then
            assertNotNull(result0)
            assertEquals(0, result0?.rateAge)

            // Given - 年齡 99
            insertRateViaJdbc("AGE99", "1", "1", "00", "01", 99, 500.00)

            // When
            val result99 = pratService.getRateDirect("AGE99", "1", "1", "00", "01", 99)

            // Then
            assertNotNull(result99)
            assertEquals(99, result99?.rateAge)
        }
    }

    // ===== Helper Methods =====

    private fun createTestPlanRate(
        planCode: String,
        version: String,
        rateSex: String,
        rateSub1: String,
        rateSub2: String,
        rateAge: Int,
        annualPrem: BigDecimal = BigDecimal("125.50")
    ) = PlanRate(
        planCode = planCode,
        version = version,
        rateSex = rateSex,
        rateSub1 = rateSub1,
        rateSub2 = rateSub2,
        rateAge = rateAge,
        annualPrem = annualPrem,
        annualPrem2 = null,
        employeeDisc = BigDecimal("10.00"),
        loadingRate2 = null
    )

    private fun insertRateViaJdbc(
        planCode: String,
        version: String,
        rateSex: String,
        rateSub1: String,
        rateSub2: String,
        rateAge: Int,
        annualPrem: Double
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO PRAT (
                PLAN_CODE, VERSION, RATE_SEX, RATE_SUB_1, RATE_SUB_2, RATE_AGE,
                ANNUAL_PREM, ANNUAL_PREM2, EMPLOYEE_DISC, LOADING_RATE2
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            planCode, version, rateSex, rateSub1, rateSub2, rateAge,
            annualPrem, null, 10.00, null
        )
    }
}
