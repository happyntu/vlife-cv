package com.vlife.cv.interest.strategy

import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import javax.sql.DataSource

/**
 * Phase 2C-0: V3 Database 連線測試（簡化版本，不使用 Spring Boot context）
 *
 * **測試目標**：
 * - 驗證 V3 Oracle database 可連線
 * - 驗證可查詢基礎表格（PLDF, QIRAT）
 * - 為後續 V3 vs V4 比對測試建立基礎
 *
 * **實作策略**：
 * - 不使用 @SpringBootTest（避免依賴問題）
 * - 直接建立 DataSource 和 JdbcTemplate
 * - 簡單的連線與查詢測試
 *
 * **前置需求**：
 * - V3 Oracle database 可連線（140.112.94.208:11522）
 * - 環境變數：ORACLE_PASSWORD（.env 檔案）
 *
 * **執行方式**：
 * ```bash
 * source .env
 * ./gradlew :modules:vlife-cv:test --tests V3DatabaseConnectionTest -DORACLE_PASSWORD="\$ORACLE_PASSWORD"
 * ```
 */
@DisplayName("Phase 2C-0: V3 Database 連線測試")
@Tag("v3comparison")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V3DatabaseConnectionTest {

    private val logger = KotlinLogging.logger {}
    private var v3DataSource: DataSource? = null
    private var v3JdbcTemplate: JdbcTemplate? = null

    @BeforeAll
    fun setUpClass() {
        // Read Oracle connection info from environment variables (.env)
        val host = System.getProperty("ORACLE_HOST") ?: System.getenv("ORACLE_HOST")
        val port = System.getProperty("ORACLE_PORT") ?: System.getenv("ORACLE_PORT")
        val service = System.getProperty("ORACLE_SERVICE") ?: System.getenv("ORACLE_SERVICE")
        val username = System.getProperty("ORACLE_USERNAME") ?: System.getenv("ORACLE_USERNAME")
        val password = System.getProperty("ORACLE_PASSWORD") ?: System.getenv("ORACLE_PASSWORD")

        if (password == null || host == null || port == null || service == null || username == null) {
            logger.warn { "Oracle connection info not complete, skipping V3 connection tests" }
            logger.warn { "Required: ORACLE_HOST, ORACLE_PORT, ORACLE_SERVICE, ORACLE_USERNAME, ORACLE_PASSWORD" }
            return
        }

        try {
            val url = "jdbc:oracle:thin:@$host:$port/$service"
            logger.info { "Connecting to V3 database: $url (user: $username)" }

            v3DataSource = DriverManagerDataSource().apply {
                setDriverClassName("oracle.jdbc.OracleDriver")
                this.url = url
                this.username = username
                setPassword(password)
            }
            v3JdbcTemplate = JdbcTemplate(v3DataSource!!)
            logger.info { "V3 DataSource created successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to create V3 DataSource" }
            v3DataSource = null
            v3JdbcTemplate = null
        }
    }

    @AfterAll
    fun tearDownClass() {
        v3DataSource = null
        v3JdbcTemplate = null
    }

    /**
     * Phase 2C-0-1: 驗證 V3 database 連線
     */
    @Test
    fun `PC-000 should connect to V3 database`() {
        assumeTrue(v3JdbcTemplate != null, "V3 DataSource not available")

        // Query V3 database version
        val version = v3JdbcTemplate!!.queryForObject(
            "SELECT BANNER FROM V\$VERSION WHERE ROWNUM = 1",
            String::class.java
        )

        logger.info { "V3 Oracle version: $version" }
        Assertions.assertNotNull(version)
        assertTrue(version!!.contains("Oracle"), "Expected Oracle database, got: $version")
    }

    /**
     * Phase 2C-0-2: 查詢 V3 PLDF 表格
     */
    @Test
    fun `PC-001 should query V3 PLDF table`() {
        assumeTrue(v3JdbcTemplate != null, "V3 DataSource not available")

        // Query PLDF count
        val count = v3JdbcTemplate!!.queryForObject(
            "SELECT COUNT(*) FROM V3.PLDF",
            Long::class.java
        )

        logger.info { "V3 PLDF table has $count records" }
        Assertions.assertNotNull(count)
        assertTrue(count!! > 0, "PLDF table should not be empty")
    }

    /**
     * Phase 2C-0-3: 查詢特定險種資料
     */
    @Test
    fun `PC-002 should query specific plan from V3 PLDF`() {
        assumeTrue(v3JdbcTemplate != null, "V3 DataSource not available")

        // Query first available plan
        val planCode = v3JdbcTemplate!!.queryForObject(
            """
            SELECT PLAN_CODE
            FROM V3.PLDF
            WHERE ROWNUM = 1
            ORDER BY PLAN_CODE
            """,
            String::class.java
        )

        logger.info { "Found plan in V3: $planCode" }
        Assertions.assertNotNull(planCode)
        assertFalse(planCode!!.isBlank())
    }

    /**
     * Phase 2C-0-4: 查詢 V3 QIRAT 費率資料
     */
    @Test
    fun `PC-003 should query V3 QIRAT rate table`() {
        assumeTrue(v3JdbcTemplate != null, "V3 DataSource not available")

        // Query QIRAT count
        val count = v3JdbcTemplate!!.queryForObject(
            "SELECT COUNT(*) FROM V3.QIRAT",
            Long::class.java
        )

        logger.info { "V3 QIRAT table has $count records" }
        Assertions.assertNotNull(count)
        assertTrue(count!! > 0, "QIRAT table should not be empty")
    }

    /**
     * Phase 2C-0-5: 查詢 V3 PLND 企業年金定義
     */
    @Test
    fun `PC-004 should query V3 PLND table`() {
        assumeTrue(v3JdbcTemplate != null, "V3 DataSource not available")

        // Query PLND count
        val count = v3JdbcTemplate!!.queryForObject(
            "SELECT COUNT(*) FROM V3.PLND",
            Long::class.java
        )

        logger.info { "V3 PLND table has $count records" }
        Assertions.assertNotNull(count)
        assertTrue(count!! >= 0, "PLND table query should succeed")
    }

    /**
     * Phase 2C-0-6: 查詢 V3 QMFDE 企業年金參數
     */
    @Test
    fun `PC-005 should query V3 QMFDE table`() {
        assumeTrue(v3JdbcTemplate != null, "V3 DataSource not available")

        // Query QMFDE count
        val count = v3JdbcTemplate!!.queryForObject(
            "SELECT COUNT(*) FROM V3.QMFDE",
            Long::class.java
        )

        logger.info { "V3 QMFDE table has $count records" }
        Assertions.assertNotNull(count)
        assertTrue(count!! >= 0, "QMFDE table query should succeed")
    }

    /**
     * Helper: 檢查前置條件
     */
    private fun assumeTrue(condition: Boolean, message: String) {
        if (!condition) {
            logger.warn { "Test skipped: $message" }
            Assumptions.assumeTrue(false, message)
        }
    }
}
