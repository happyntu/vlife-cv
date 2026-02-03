package com.vlife.cv.coverage

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CvpuController 單元測試
 *
 * 遵循 ADR-017 規範，測試表格導向命名的 Controller。
 * 參見 ADR-015 測試策略說明。
 */
@DisplayName("CvpuController 單元測試")
class CvpuControllerTest {

    private lateinit var service: CvpuService
    private lateinit var controller: CvpuController

    @BeforeEach
    fun setup() {
        service = mockk()
        controller = CvpuController(service)
    }

    private fun createTestProductUnit(
        policyNo: String = "P000000001",
        coverageNo: Int = 1,
        ps06Type: String = "1",
        cvpuType: String = "1",
        lastAnnivDur: Int = 1,
        statusCode: String = "1",
        divDeclare: BigDecimal = BigDecimal("1000.00"),
        divPuaAmt: BigDecimal = BigDecimal("500.0000")
    ) = Cvpu(
        policyNo = policyNo,
        coverageNo = coverageNo,
        ps06Type = ps06Type,
        cvpuType = cvpuType,
        lastAnnivDur = lastAnnivDur,
        statusCode = statusCode,
        divDeclare = divDeclare,
        divPuaAmt = divPuaAmt,
        financialDate = LocalDate.of(2025, 1, 1),
        pcpoNo = null,
        programId = null,
        processDate = LocalDate.of(2025, 1, 1),
        policyType = null,
        approvedDate = null,
        programIdCvpu = null
    )

    @Nested
    @DisplayName("findByCoverage")
    inner class FindByCoverage {

        @Test
        fun `should return product units for given coverage`() {
            // Given
            val units = listOf(
                createTestProductUnit(lastAnnivDur = 1),
                createTestProductUnit(lastAnnivDur = 2),
                createTestProductUnit(lastAnnivDur = 3)
            )
            every { service.findByCoverage("P000000001", 1) } returns units

            // When
            val response = controller.findByCoverage("P000000001", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
            assertEquals(200, response.body!!.code)
            assertEquals(3, response.body!!.data?.size)
            verify(exactly = 1) { service.findByCoverage("P000000001", 1) }
        }

        @Test
        fun `should return empty list when no data`() {
            // Given
            every { service.findByCoverage("UNKNOWN", 1) } returns emptyList()

            // When
            val response = controller.findByCoverage("UNKNOWN", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertTrue(response.body!!.data!!.isEmpty())
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        fun `should return product unit when exists`() {
            // Given
            val unit = createTestProductUnit()
            val id = CvpuId("P000000001", 1, "1", "1", 1)
            every { service.findById(id) } returns unit

            // When
            val response = controller.findById("P000000001", 1, "1", "1", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertNotNull(response.body!!.data)
            assertEquals("P000000001", response.body!!.data?.policyNo)
            assertEquals(1, response.body!!.data?.coverageNo)
        }

        @Test
        fun `should return 404 when not exists`() {
            // Given
            val id = CvpuId("P000000001", 1, "1", "1", 999)
            every { service.findById(id) } returns null

            // When
            val response = controller.findById("P000000001", 1, "1", "1", 999)

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(404, response.body!!.code)
            assertEquals("CVPU_NOT_FOUND", response.body!!.error?.errorCode)
        }
    }

    @Nested
    @DisplayName("findLatestByCoverage")
    inner class FindLatestByCoverage {

        @Test
        fun `should return latest product unit when exists`() {
            // Given
            val unit = createTestProductUnit(lastAnnivDur = 5)
            every { service.findLatestByCoverage("P000000001", 1) } returns unit

            // When
            val response = controller.findLatestByCoverage("P000000001", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertNotNull(response.body!!.data)
            assertEquals(5, response.body!!.data?.lastAnnivDur)
        }

        @Test
        fun `should return 404 when no data`() {
            // Given
            every { service.findLatestByCoverage("UNKNOWN", 1) } returns null

            // When
            val response = controller.findLatestByCoverage("UNKNOWN", 1)

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(404, response.body!!.code)
            assertNotNull(response.body!!.error)
        }
    }

    @Nested
    @DisplayName("getDividendSummary")
    inner class GetDividendSummary {

        @Test
        fun `should return dividend summary`() {
            // Given
            val summary = DividendSummary(
                policyNo = "P000000001",
                coverageNo = 1,
                totalDivDeclare = BigDecimal("3000.00"),
                totalDivPuaAmt = BigDecimal("1500.0000"),
                recordCount = 3
            )
            every { service.getDividendSummary("P000000001", 1) } returns summary

            // When
            val response = controller.getDividendSummary("P000000001", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertNotNull(response.body!!.data)
            assertEquals("P000000001", response.body!!.data?.policyNo)
            assertEquals(1, response.body!!.data?.coverageNo)
            assertEquals(BigDecimal("3000.00"), response.body!!.data?.totalDivDeclare)
            assertEquals(BigDecimal("1500.0000"), response.body!!.data?.totalDivPuaAmt)
            assertEquals(BigDecimal("4500.0000"), response.body!!.data?.totalDividend)
            assertEquals(3, response.body!!.data?.recordCount)
        }
    }

    @Nested
    @DisplayName("existsByCoverage")
    inner class ExistsByCoverage {

        @Test
        fun `should return true when exists`() {
            // Given
            every { service.existsByCoverage("P000000001", 1) } returns true

            // When
            val response = controller.existsByCoverage("P000000001", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(true, response.body!!.data)
        }

        @Test
        fun `should return false when not exists`() {
            // Given
            every { service.existsByCoverage("UNKNOWN", 1) } returns false

            // When
            val response = controller.existsByCoverage("UNKNOWN", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(false, response.body!!.data)
        }
    }

    @Nested
    @DisplayName("DTO 轉換")
    inner class DtoConversion {

        @Test
        fun `should convert Cvpu to CvpuDto correctly`() {
            // Given
            val unit = createTestProductUnit(
                divDeclare = BigDecimal("1000.00"),
                divPuaAmt = BigDecimal("500.0000")
            )
            every { service.findByCoverage("P000000001", 1) } returns listOf(unit)

            // When
            val response = controller.findByCoverage("P000000001", 1)

            // Then
            val dto = response.body!!.data!![0]
            assertEquals("P000000001", dto.policyNo)
            assertEquals(1, dto.coverageNo)
            assertEquals("1", dto.ps06Type)
            assertEquals("1", dto.cvpuType)
            assertEquals(1, dto.lastAnnivDur)
            assertEquals("1", dto.statusCode)
            assertEquals("有效", dto.statusDesc)
            assertEquals(BigDecimal("1000.00"), dto.divDeclare)
            assertEquals(BigDecimal("500.0000"), dto.divPuaAmt)
            assertEquals(BigDecimal("1500.0000"), dto.totalDividend)
            assertTrue(dto.isActive)
        }
    }
}
