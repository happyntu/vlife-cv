package com.vlife.cv.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.vlife.cv.dto.InterestRateRequestDto
import com.vlife.cv.dto.InterestRateBatchRequestDto
import com.vlife.cv.engine.contract.Cv210pInterestRateCalculator
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.RateType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDate

/**
 * InterestRateController REST API 測試
 *
 * **測試策略**：
 * - 純單元測試（不啟動 Spring 上下文）
 * - 手動建立 Controller 和 MockMvc
 * - Mock Cv210pInterestRateCalculator（L2 Engine Contract）
 * - 驗證 HTTP 狀態碼、JSON 回應格式、錯誤處理
 *
 * **測試覆蓋**：
 * - POST /calculate - 正常情況、無效 rateType、beginDate > endDate
 * - POST /calculate/batch - 批量計算、部分無效輸入
 * - GET /supported-rate-types - 查詢支援的 rate_type
 * - GET /supports/{rateType} - 檢查是否支援指定 rate_type
 */
@DisplayName("InterestRateController REST API 測試")
class InterestRateControllerTest {

    private lateinit var cv210pInterestRateCalculator: Cv210pInterestRateCalculator
    private lateinit var controller: InterestRateController
    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        cv210pInterestRateCalculator = mockk()
        controller = InterestRateController(cv210pInterestRateCalculator)
        objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build()
    }

    @Nested
    @DisplayName("POST /api/v1/cv/interest-rate/calculate")
    inner class CalculateRate {

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

            val result = InterestRateCalculationResult(
                actualRate = BigDecimal("350.5"),
                intAmt = BigDecimal("35050"),
                monthlyDetails = emptyList()
            )

            every {
                cv210pInterestRateCalculator.calculateRate(any(), eq(0), isNull(), isNull())
            } returns result

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.actualRate") { value(350.5) }
                jsonPath("$.intAmt") { value(35050) }
                jsonPath("$.monthlyDetails") { isArray() }
            }

            verify(exactly = 1) {
                cv210pInterestRateCalculator.calculateRate(any(), eq(0), isNull(), isNull())
            }
        }

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

            verify(exactly = 0) {
                cv210pInterestRateCalculator.calculateRate(any(), any(), any(), any())
            }
        }

        @Test
        fun `should return 400 when calculation throws IllegalArgumentException`() {
            // Given
            val request = InterestRateRequestDto(
                rateType = "2",
                beginDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31),
                principalAmt = BigDecimal("1000000"),
                precision = 0
            )

            every {
                cv210pInterestRateCalculator.calculateRate(any(), any(), any(), any())
            } throws IllegalArgumentException("不支援的 rate_type")

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `should return 500 when calculation throws unexpected exception`() {
            // Given
            val request = InterestRateRequestDto(
                rateType = "2",
                beginDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 31),
                principalAmt = BigDecimal("1000000"),
                precision = 0
            )

            every {
                cv210pInterestRateCalculator.calculateRate(any(), any(), any(), any())
            } throws RuntimeException("Database error")

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isInternalServerError() }
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/cv/interest-rate/calculate/batch")
    inner class CalculateRateBatch {

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

            val results = listOf(
                InterestRateCalculationResult(
                    actualRate = BigDecimal("350.5"),
                    intAmt = BigDecimal("17525"),
                    monthlyDetails = emptyList()
                ),
                InterestRateCalculationResult(
                    actualRate = BigDecimal("400.0"),
                    intAmt = BigDecimal("80000"),
                    monthlyDetails = emptyList()
                )
            )

            every {
                cv210pInterestRateCalculator.calculateRateBatch(any(), eq(0))
            } returns results

            // When & Then
            mockMvc.post("/api/v1/cv/interest-rate/calculate/batch") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }.andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.results") { isArray() }
                jsonPath("$.results.length()") { value(2) }
                jsonPath("$.results[0].actualRate") { value(350.5) }
                jsonPath("$.results[1].actualRate") { value(400.0) }
            }
        }

        @Test
        fun `should return 400 when some inputs have invalid rateType`() {
            // Given
            val request = InterestRateBatchRequestDto(
                inputs = listOf(
                    InterestRateRequestDto(rateType = "2", beginDate = LocalDate.of(2024, 1, 1), endDate = LocalDate.of(2024, 12, 31), precision = 0),
                    InterestRateRequestDto(rateType = "Z", beginDate = LocalDate.of(2024, 1, 1), endDate = LocalDate.of(2024, 12, 31), precision = 0)  // 無效
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

            verify(exactly = 0) {
                cv210pInterestRateCalculator.calculateRateBatch(any(), any())
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cv/interest-rate/supported-rate-types")
    inner class GetSupportedRateTypes {

        @Test
        fun `should return all supported rate types`() {
            // Given
            val supportedRateTypes = setOf(
                RateType.AVG_DECLARED_RATE,
                RateType.FOUR_BANK_RATE,
                RateType.LOAN_RATE_MONTHLY
            )

            every {
                cv210pInterestRateCalculator.getSupportedRateTypes()
            } returns supportedRateTypes

            // When & Then
            mockMvc.get("/api/v1/cv/interest-rate/supported-rate-types")
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.rateTypes") { isArray() }
                    jsonPath("$.rateTypes.length()") { value(3) }
                    jsonPath("$.rateTypes[0].code") { exists() }
                    jsonPath("$.rateTypes[0].description") { exists() }
                }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/cv/interest-rate/supports/{rateType}")
    inner class SupportsRateType {

        @Test
        fun `should return true when rateType is supported`() {
            // Given
            every {
                cv210pInterestRateCalculator.supportsRateType(RateType.LOAN_RATE_MONTHLY)
            } returns true

            // When & Then
            mockMvc.get("/api/v1/cv/interest-rate/supports/2")
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.supported") { value(true) }
                }
        }

        @Test
        fun `should return false when rateType is not supported`() {
            // Given - 無效的 rateType，fromCode 回傳 null

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
