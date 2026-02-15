package com.vlife.cv.interest.v3

import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import mu.KotlinLogging
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.math.BigDecimal
import java.time.LocalDate
import javax.sql.DataSource

/**
 * Phase 2C-2: V3StoredProcedureCaller 整合測試
 *
 * **測試目標**：
 * - 驗證 V3StoredProcedureCaller 可正確呼叫 V3 wrapper procedure
 * - 驗證 Oracle JDBC CallableStatement 參數傳遞
 * - 驗證結果映射至 V4 資料結構
 *
 * **前置需求**：
 * - V3 database 已部署 cv210p_wrapper_test procedure
 * - 環境變數：ORACLE_HOST, ORACLE_PORT, ORACLE_SERVICE, ORACLE_USERNAME, ORACLE_PASSWORD
 *
 * **執行方式**：
 * ```bash
 * ./gradlew :modules:vlife-cv:test --tests V3StoredProcedureCallerIntegrationTest
 * ```
 */
@DisplayName("Phase 2C-2: V3StoredProcedureCaller 整合測試")
@Tag("v3comparison")
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V3StoredProcedureCallerIntegrationTest {

    private val logger = KotlinLogging.logger {}
    private var v3DataSource: DataSource? = null
    private var v3Caller: V3StoredProcedureCaller? = null

    @BeforeAll
    fun setUpClass() {
        // Read Oracle connection info from environment variables
        val host = System.getProperty("ORACLE_HOST") ?: System.getenv("ORACLE_HOST")
        val port = System.getProperty("ORACLE_PORT") ?: System.getenv("ORACLE_PORT")
        val service = System.getProperty("ORACLE_SERVICE") ?: System.getenv("ORACLE_SERVICE")
        val username = System.getProperty("ORACLE_USERNAME") ?: System.getenv("ORACLE_USERNAME")
        val password = System.getProperty("ORACLE_PASSWORD") ?: System.getenv("ORACLE_PASSWORD")

        if (password == null || host == null || port == null || service == null || username == null) {
            logger.warn { "Oracle connection info not complete, skipping V3 caller tests" }
            return
        }

        try {
            val url = "jdbc:oracle:thin:@$host:$port/$service"
            v3DataSource = DriverManagerDataSource().apply {
                setDriverClassName("oracle.jdbc.OracleDriver")
                this.url = url
                this.username = username
                setPassword(password)
            }
            v3Caller = V3StoredProcedureCaller(v3DataSource!!)
            logger.info { "V3StoredProcedureCaller initialized successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize V3StoredProcedureCaller" }
        }
    }

    @AfterAll
    fun tearDownClass() {
        v3DataSource = null
        v3Caller = null
    }

    /**
     * Test 1: 呼叫 V3 wrapper procedure（基本功能）
     */
    @Test
    fun `PC-101 should call V3 wrapper procedure successfully`() {
        assumeTrue(v3Caller != null, "V3StoredProcedureCaller not available")

        // Given: V4 InterestRateInput
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000")
        )

        // When: Call V3 stored procedure
        val result = v3Caller!!.callCv210pRateCalc(input, "10A06", "1")

        // Then: Verify result
        Assertions.assertNotNull(result)
        logger.info { "V3 result: actualRate=${result.actualRate}, intAmt=${result.intAmt}" }

        // Verify test mode returns expected values
        assertEquals(BigDecimal("250"), result.actualRate, "Expected test rate 2.5%")
        assertEquals(BigDecimal("25000"), result.intAmt, "Expected 1M × 2.5% = 25000")
    }

    /**
     * Test 2: 測試不同本金額
     */
    @Test
    fun `PC-102 should calculate interest for different principal amounts`() {
        assumeTrue(v3Caller != null, "V3StoredProcedureCaller not available")

        // Given: Different principal amounts
        val testCases = listOf(
            BigDecimal("100000") to BigDecimal("2500"),    // 100K × 2.5%
            BigDecimal("500000") to BigDecimal("12500"),   // 500K × 2.5%
            BigDecimal("2000000") to BigDecimal("50000")   // 2M × 2.5%
        )

        testCases.forEach { (principal, expectedInt) ->
            // When
            val input = InterestRateInput(
                rateType = RateType.COMPOUND_RATE,
                beginDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31),
                principalAmt = principal
            )
            val result = v3Caller!!.callCv210pRateCalc(input, "10A06", "1")

            // Then
            assertEquals(expectedInt, result.intAmt,
                "Principal $principal should yield interest $expectedInt")
        }
    }

    /**
     * Test 3: 測試險種不存在的情況
     */
    @Test
    fun `PC-103 should handle non-existent plan code`() {
        assumeTrue(v3Caller != null, "V3StoredProcedureCaller not available")

        // Given: Non-existent plan code
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000")
        )

        // When: Call with non-existent plan
        val result = v3Caller!!.callCv210pRateCalc(input, "XXXXX", "Z")

        // Then: Should return zero (PLAN_NOT_FOUND status)
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
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
