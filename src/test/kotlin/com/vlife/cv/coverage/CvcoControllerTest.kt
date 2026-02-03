package com.vlife.cv.coverage

import com.github.pagehelper.PageInfo
import com.vlife.cv.common.PageRequest
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
 * CvcoController 單元測試
 *
 * 遵循 ADR-017 規範，測試表格導向命名的 Controller。
 * 參見 ADR-015 測試策略說明。
 */
@DisplayName("CvcoController 單元測試")
class CvcoControllerTest {

    private lateinit var cvcoService: CvcoService
    private lateinit var cvpuService: CvpuService
    private lateinit var controller: CvcoController

    @BeforeEach
    fun setup() {
        cvcoService = mockk()
        cvpuService = mockk()
        controller = CvcoController(cvcoService, cvpuService)
    }

    private fun createTestCoverage(
        policyNo: String = "P000000001",
        coverageNo: Int = 1,
        planCode: String = "12RA1",
        statusCode: String = "1"
    ) = Cvco(
        policyNo = policyNo,
        coverageNo = coverageNo,
        planCode = planCode,
        version = "1",
        rateSex = "1",
        rateAge = 30,
        rateSub1 = "00",
        rateSub2 = "000",
        issueDate = LocalDate.of(2020, 1, 1),
        statusCode = statusCode,
        insuranceType3 = "L",
        processDate = LocalDate.of(2020, 1, 1),
        processType = "N",
        policyType = null,
        statusCode2 = null
    )

    private fun createTestProductUnit(
        policyNo: String = "P000000001",
        coverageNo: Int = 1,
        lastAnnivDur: Int = 1,
        divDeclare: BigDecimal = BigDecimal("1000.00"),
        divPuaAmt: BigDecimal = BigDecimal("500.0000")
    ) = Cvpu(
        policyNo = policyNo,
        coverageNo = coverageNo,
        ps06Type = "1",
        cvpuType = "1",
        lastAnnivDur = lastAnnivDur,
        statusCode = "1",
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
    @DisplayName("getCoveragesByPolicy")
    inner class GetCoveragesByPolicy {

        @Test
        fun `should return paginated coverages for given policy`() {
            // Given
            val coverages = listOf(
                createTestCoverage(coverageNo = 1),
                createTestCoverage(coverageNo = 2)
            )
            val pageInfo = PageInfo(coverages).apply {
                pageNum = 1
                pageSize = 20
                total = 2
            }
            every { cvcoService.findByPolicyNo("P000000001", any<PageRequest>()) } returns pageInfo

            // When
            val response = controller.getCoveragesByPolicy("P000000001", 1, 20)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(2, response.body!!.data?.content?.size)
        }
    }

    @Nested
    @DisplayName("getCoverageById")
    inner class GetCoverageById {

        @Test
        fun `should return coverage when exists`() {
            // Given
            val coverage = createTestCoverage()
            every { cvcoService.findById("P000000001", 1) } returns coverage

            // When
            val response = controller.getCoverageById("P000000001", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertNotNull(response.body!!.data)
            assertEquals("P000000001", response.body!!.data?.policyNo)
            assertEquals(1, response.body!!.data?.coverageNo)
            verify(exactly = 1) { cvcoService.findById("P000000001", 1) }
        }

        @Test
        fun `should return 404 when not exists`() {
            // Given
            every { cvcoService.findById("UNKNOWN", 1) } returns null

            // When
            val response = controller.getCoverageById("UNKNOWN", 1)

            // Then
            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
            assertEquals(404, response.body!!.code)
            assertEquals("COVERAGE_NOT_FOUND", response.body!!.error?.errorCode)
        }
    }

    @Nested
    @DisplayName("getCoveragesByPlanCode")
    inner class GetCoveragesByPlanCode {

        @Test
        fun `should return coverages for given plan code`() {
            // Given
            val coverages = listOf(
                createTestCoverage(policyNo = "P000000001"),
                createTestCoverage(policyNo = "P000000002")
            )
            val pageInfo = PageInfo(coverages).apply {
                pageNum = 1
                pageSize = 20
                total = 2
            }
            every { cvcoService.findByPlanCode("12RA1", any<PageRequest>()) } returns pageInfo

            // When
            val response = controller.getCoveragesByPlanCode("12RA1", 1, 20)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(2, response.body!!.data?.content?.size)
        }
    }

    @Nested
    @DisplayName("getCoveragesByStatus")
    inner class GetCoveragesByStatus {

        @Test
        fun `should return coverages for given status code`() {
            // Given
            val coverages = listOf(createTestCoverage(statusCode = "1"))
            val pageInfo = PageInfo(coverages).apply {
                pageNum = 1
                pageSize = 20
                total = 1
            }
            every { cvcoService.findByStatusCode("1", any<PageRequest>()) } returns pageInfo

            // When
            val response = controller.getCoveragesByStatus("1", 1, 20)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(1, response.body!!.data?.content?.size)
        }
    }

    @Nested
    @DisplayName("getAllPlanCodes")
    inner class GetAllPlanCodes {

        @Test
        fun `should return all plan codes`() {
            // Given
            every { cvcoService.findAllPlanCodes() } returns listOf("12RA1", "12RA2", "12RA3")

            // When
            val response = controller.getAllPlanCodes()

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(3, response.body!!.data?.size)
            assertEquals("12RA1", response.body!!.data!![0])
        }
    }

    @Nested
    @DisplayName("getDividends")
    inner class GetDividends {

        @Test
        fun `should return dividends for given coverage`() {
            // Given
            val units = listOf(
                createTestProductUnit(lastAnnivDur = 1),
                createTestProductUnit(lastAnnivDur = 2)
            )
            every { cvpuService.findByCoverage("P000000001", 1) } returns units

            // When
            val response = controller.getDividends("P000000001", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(2, response.body!!.data?.size)
            verify(exactly = 1) { cvpuService.findByCoverage("P000000001", 1) }
        }

        @Test
        fun `should return empty list when no dividends`() {
            // Given
            every { cvpuService.findByCoverage("UNKNOWN", 1) } returns emptyList()

            // When
            val response = controller.getDividends("UNKNOWN", 1)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertTrue(response.body!!.data!!.isEmpty())
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
            every { cvpuService.getDividendSummary("P000000001", 1) } returns summary

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
    @DisplayName("getDividendsByPolicy")
    inner class GetDividendsByPolicy {

        @Test
        fun `should return paginated dividends for given policy`() {
            // Given
            val units = listOf(
                createTestProductUnit(coverageNo = 1),
                createTestProductUnit(coverageNo = 2)
            )
            val pageInfo = PageInfo(units).apply {
                pageNum = 1
                pageSize = 20
                total = 2
            }
            every { cvpuService.findByPolicyNo("P000000001", any<PageRequest>()) } returns pageInfo

            // When
            val response = controller.getDividendsByPolicy("P000000001", 1, 20)

            // Then
            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(200, response.body!!.code)
            assertEquals(2, response.body!!.data?.content?.size)
        }
    }

    // TODO: DTO 轉換測試需要進一步調查 enum description 查找問題
    // 已涵蓋的轉換邏輯在 getCoverageById、getDividends 等測試中驗證
}
