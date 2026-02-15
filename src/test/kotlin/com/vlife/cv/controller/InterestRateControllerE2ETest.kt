package com.vlife.cv.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.vlife.cv.TestApplication
import com.vlife.cv.TestConfiguration
import com.vlife.cv.dto.InterestRateBatchRequestDto
import com.vlife.cv.dto.InterestRateRequestDto
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.OracleContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.LocalDate

/**
 * InterestRateController E2E 測試
 *
 * **測試目標**：
 * 1. 驗證完整 API 流程：HTTP → Controller → Service → Strategy → Database
 * 2. 驗證 REST API 規格（路由、狀態碼、JSON 格式）
 * 3. 驗證 Spring Boot 完整上下文啟動（資料庫 + 所有 Bean）
 *
 * **測試策略**：
 * - 使用 @SpringBootTest（完整 Spring 上下文）
 * - 使用 @AutoConfigureMockMvc（模擬 HTTP 請求）
 * - 使用 TestContainers Oracle XE（真實資料庫）
 * - 發送真實的 HTTP 請求，驗證回應
 *
 * **注意**：
 * - 此測試需要 Phase 6 Strategy Mapper XML 實作完成後才能執行
 * - 目前所有測試標記為 @Disabled，避免 BindingException
 * - 測試框架已建立，可作為未來完整 E2E 測試的基礎
 */
@SpringBootTest(
    classes = [TestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class)
@Testcontainers
@DisplayName("InterestRateController E2E 測試")
class InterestRateControllerE2ETest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @org.junit.jupiter.api.BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .build()
    }

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

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    @Nested
    @DisplayName("POST /api/v1/cv/interest-rate/calculate")
    inner class CalculateRate {

        /**
         * 測試 1：驗證正常計算流程
         *
         * **驗證項目**：
         * - HTTP 200 OK
         * - JSON 回應格式正確
         * - actualRate, intAmt 欄位存在且非 null
         *
         * **注意**：需要 Phase 6 Strategy Mapper XML
         */
        @Test
        fun `should return 200 with valid input`() {
            // Given
            val request = InterestRateRequestDto(
                rateType = "2",
                beginDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31),
                principalAmt = BigDecimal("1000000"),
                actualRate = BigDecimal.ZERO,
                rateSub = BigDecimal.ZERO,
                rateDisc = BigDecimal("100"),
                subAcntPlanCode = "ABC123",
                ivTargetCode = null,
                precision = 0
            )

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.actualRate") { exists() }
                jsonPath("$.intAmt") { exists() }
                jsonPath("$.monthlyDetails") { isArray() }
            }
        }

        /**
         * 測試 2：驗證無效 rateType 處理
         *
         * **驗證項目**：
         * - HTTP 400 Bad Request
         * - 錯誤訊息提示無效 rate_type
         */
        @Test
        fun `should return 400 when rateType is invalid`() {
            // Given
            val request = InterestRateRequestDto(
                rateType = "Z",  // 無效的 rate_type
                beginDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31),
                principalAmt = BigDecimal("1000000"),
                precision = 0
            )

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
        }

        /**
         * 測試 3：驗證日期範圍錯誤處理
         *
         * **驗證項目**：
         * - HTTP 200 OK（V3 相容行為：返回零值）
         * - actualRate = 0, intAmt = 0
         */
        @Test
        fun `should return zero when beginDate is after endDate`() {
            // Given
            val request = InterestRateRequestDto(
                rateType = "2",
                beginDate = LocalDate.of(2024, 12, 31),
                endDate = LocalDate.of(2024, 1, 1),  // endDate < beginDate
                principalAmt = BigDecimal("1000000"),
                precision = 0
            )

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                jsonPath("$.actualRate") { value(0) }
                jsonPath("$.intAmt") { value(0) }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/cv/interest-rate/calculate/batch")
    inner class CalculateRateBatch {

        /**
         * 測試 4：驗證批量計算
         *
         * **驗證項目**：
         * - HTTP 200 OK
         * - 返回結果數量與輸入一致
         * - 每個結果都有 actualRate 和 intAmt
         */
        @Test
        fun `should return 200 with valid inputs`() {
            // Given
            val request = InterestRateBatchRequestDto(
                inputs = listOf(
                    InterestRateRequestDto(
                        rateType = "2",
                        beginDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 6, 30),
                        principalAmt = BigDecimal("1000000"),
                        precision = 0
                    ),
                    InterestRateRequestDto(
                        rateType = "3",
                        beginDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 12, 31),
                        principalAmt = BigDecimal("2000000"),
                        precision = 0
                    )
                ),
                precision = 0
            )

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate/batch") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.results") { isArray() }
                jsonPath("$.results.length()") { value(2) }
                jsonPath("$.results[0].actualRate") { exists() }
                jsonPath("$.results[1].actualRate") { exists() }
            }
        }

        /**
         * 測試 5：驗證部分無效輸入處理
         *
         * **驗證項目**：
         * - HTTP 400 Bad Request（第一個驗證錯誤即失敗）
         */
        @Test
        fun `should return 400 when some inputs have invalid rateType`() {
            // Given
            val request = InterestRateBatchRequestDto(
                inputs = listOf(
                    InterestRateRequestDto(
                        rateType = "2",
                        beginDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 12, 31),
                        precision = 0
                    ),
                    InterestRateRequestDto(
                        rateType = "Z",  // 無效
                        beginDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 12, 31),
                        precision = 0
                    )
                ),
                precision = 0
            )

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate/batch") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cv/interest-rate/supported-rate-types")
    inner class GetSupportedRateTypes {

        /**
         * 測試 6：驗證支援的 rate_type 查詢
         *
         * **驗證項目**：
         * - HTTP 200 OK
         * - 返回 13 種 RateType
         * - 每個 RateType 有 code 和 description
         */
        @Test
        fun `should return all supported rate types`() {
            // When & Then
            mockMvc.get("/api/v1/cv/interest-rate/supported-rate-types")
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.rateTypes") { isArray() }
                    jsonPath("$.rateTypes.length()") { value(13) }
                    jsonPath("$.rateTypes[0].code") { exists() }
                    jsonPath("$.rateTypes[0].description") { exists() }
                }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cv/interest-rate/supports/{rateType}")
    inner class SupportsRateType {

        /**
         * 測試 7：驗證支援的 rate_type 檢查（支援）
         *
         * **驗證項目**：
         * - HTTP 200 OK
         * - supported = true
         */
        @Test
        fun `should return true when rateType is supported`() {
            // When & Then
            mockMvc.get("/api/v1/cv/interest-rate/supports/2")
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.supported") { value(true) }
                }
        }

        /**
         * 測試 8：驗證不支援的 rate_type 檢查
         *
         * **驗證項目**：
         * - HTTP 200 OK
         * - supported = false
         */
        @Test
        fun `should return false when rateType is not supported`() {
            // When & Then
            mockMvc.get("/api/v1/cv/interest-rate/supports/Z")
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.supported") { value(false) }
                }
        }
    }
}
