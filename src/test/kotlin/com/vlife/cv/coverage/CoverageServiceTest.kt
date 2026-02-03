package com.vlife.cv.coverage

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CoverageValueChangeService 單元測試
 *
 * 注意：PageHelper 分頁功能使用 ThreadLocal 機制，難以在單元測試中模擬。
 * 因此不分頁版本直接調用 Mapper，分頁版本應透過整合測試驗證。
 * 參見 ADR-015 測試策略說明。
 */
@DisplayName("CoverageValueChangeService 單元測試")
class CoverageValueChangeServiceTest {

    private lateinit var mapper: CvcoMapper
    private lateinit var service: CoverageValueChangeService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = CoverageValueChangeService(mapper)
    }

    private fun createTestCoverage(
        policyNo: String = "P000000001",
        coverageNo: Int = 1,
        planCode: String = "A1001",
        statusCode: String = "P"
    ) = CoverageValueChange(
        policyNo = policyNo,
        coverageNo = coverageNo,
        planCode = planCode,
        version = "1",
        rateSex = "1",
        rateAge = 30,
        rateSub1 = "01",
        rateSub2 = "001",
        issueDate = LocalDate.of(2020, 1, 1),
        statusCode = statusCode,
        insuranceType3 = "R",
        processDate = LocalDate.of(2020, 1, 1),
        processType = "O",
        policyType = null,
        statusCode2 = null
    )

    @Nested
    @DisplayName("findByPolicyNo")
    inner class FindByPolicyNo {

        @Test
        fun `should return coverages for given policy`() {
            // Given
            val coverages = listOf(
                createTestCoverage(coverageNo = 1),
                createTestCoverage(coverageNo = 2)
            )
            every { mapper.findByPolicyNo("P000000001") } returns coverages

            // When
            val result = service.findByPolicyNo("P000000001")

            // Then
            assertEquals(2, result.size)
            assertEquals(1, result[0].coverageNo)
            assertEquals(2, result[1].coverageNo)
            verify(exactly = 1) { mapper.findByPolicyNo("P000000001") }
        }

        @Test
        fun `should return empty list for unknown policy`() {
            // Given
            every { mapper.findByPolicyNo("UNKNOWN") } returns emptyList()

            // When
            val result = service.findByPolicyNo("UNKNOWN")

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {

        @Test
        fun `should return coverage when exists`() {
            // Given
            val coverage = createTestCoverage()
            every { mapper.findById("P000000001", 1) } returns coverage

            // When
            val result = service.findById("P000000001", 1)

            // Then
            assertEquals("P000000001", result?.policyNo)
            assertEquals(1, result?.coverageNo)
        }

        @Test
        fun `should return null when not exists`() {
            // Given
            every { mapper.findById("P000000001", 999) } returns null

            // When
            val result = service.findById("P000000001", 999)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return coverages for given plan code`() {
            // Given
            val coverages = listOf(
                createTestCoverage(policyNo = "P000000001"),
                createTestCoverage(policyNo = "P000000002")
            )
            every { mapper.findByPlanCode("A1001") } returns coverages

            // When
            val result = service.findByPlanCode("A1001")

            // Then
            assertEquals(2, result.size)
            verify(exactly = 1) { mapper.findByPlanCode("A1001") }
        }
    }

    @Nested
    @DisplayName("findByStatusCode")
    inner class FindByStatusCode {

        @Test
        fun `should return coverages for given status`() {
            // Given
            val coverages = listOf(
                createTestCoverage(statusCode = "P"),
                createTestCoverage(statusCode = "P")
            )
            every { mapper.findByStatusCode("P") } returns coverages

            // When
            val result = service.findByStatusCode("P")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.statusCode == "P" })
            verify(exactly = 1) { mapper.findByStatusCode("P") }
        }
    }

    @Nested
    @DisplayName("findActiveCoverages")
    inner class FindActiveCoverages {

        @Test
        fun `should return only active coverages`() {
            // Given
            val coverages = listOf(
                createTestCoverage(coverageNo = 1, statusCode = "P"),  // Active
                createTestCoverage(coverageNo = 2, statusCode = "M"),  // Matured
                createTestCoverage(coverageNo = 3, statusCode = "P")   // Active
            )
            every { mapper.findByPolicyNo("P000000001") } returns coverages

            // When
            val result = service.findActiveCoverages("P000000001")

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.statusCode == "P" })
        }
    }

    @Nested
    @DisplayName("existsByPolicyNo")
    inner class ExistsByPolicyNo {

        @Test
        fun `should return true when exists`() {
            // Given
            every { mapper.countByPolicyNo("P000000001") } returns 3

            // When
            val result = service.existsByPolicyNo("P000000001")

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when not exists`() {
            // Given
            every { mapper.countByPolicyNo("UNKNOWN") } returns 0

            // When
            val result = service.existsByPolicyNo("UNKNOWN")

            // Then
            assertFalse(result)
        }
    }
}

/**
 * ProductUnitService 單元測試
 *
 * 注意：PageHelper 分頁功能使用 ThreadLocal 機制，難以在單元測試中模擬。
 * 因此不分頁版本直接調用 Mapper，分頁版本應透過整合測試驗證。
 * 參見 ADR-015 測試策略說明。
 */
@DisplayName("ProductUnitService 單元測試")
class ProductUnitServiceTest {

    private lateinit var mapper: CvpuMapper
    private lateinit var service: ProductUnitService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = ProductUnitService(mapper)
    }

    private fun createTestProductUnit(
        policyNo: String = "P000000001",
        coverageNo: Int = 1,
        lastAnnivDur: Int = 1,
        divDeclare: BigDecimal = BigDecimal("1000.00"),
        divPuaAmt: BigDecimal = BigDecimal("500.0000")
    ) = ProductUnit(
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
    @DisplayName("findByPolicyNo")
    inner class FindByPolicyNo {

        @Test
        fun `should return product units for given policy`() {
            // Given
            val units = listOf(
                createTestProductUnit(lastAnnivDur = 1),
                createTestProductUnit(lastAnnivDur = 2),
                createTestProductUnit(lastAnnivDur = 3)
            )
            every { mapper.findByPolicyNo("P000000001") } returns units

            // When
            val result = service.findByPolicyNo("P000000001")

            // Then
            assertEquals(3, result.size)
            verify(exactly = 1) { mapper.findByPolicyNo("P000000001") }
        }
    }

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
            every { mapper.findByCoverage("P000000001", 1) } returns units

            // When
            val result = service.findByCoverage("P000000001", 1)

            // Then
            assertEquals(3, result.size)
        }
    }

    @Nested
    @DisplayName("sumDivDeclare")
    inner class SumDivDeclare {

        @Test
        fun `should return sum of dividends`() {
            // Given
            every { mapper.sumDivDeclare("P000000001", 1) } returns BigDecimal("3000.00")

            // When
            val result = service.sumDivDeclare("P000000001", 1)

            // Then
            assertEquals(BigDecimal("3000.00"), result)
        }

        @Test
        fun `should return zero when no data`() {
            // Given
            every { mapper.sumDivDeclare("UNKNOWN", 1) } returns null

            // When
            val result = service.sumDivDeclare("UNKNOWN", 1)

            // Then
            assertEquals(BigDecimal.ZERO, result)
        }
    }

    @Nested
    @DisplayName("getDividendSummary")
    inner class GetDividendSummary {

        @Test
        fun `should return dividend summary`() {
            // Given
            every { mapper.sumDivDeclare("P000000001", 1) } returns BigDecimal("3000.00")
            every { mapper.sumDivPuaAmt("P000000001", 1) } returns BigDecimal("1500.0000")
            every { mapper.countByCoverage("P000000001", 1) } returns 3

            // When
            val result = service.getDividendSummary("P000000001", 1)

            // Then
            assertEquals("P000000001", result.policyNo)
            assertEquals(1, result.coverageNo)
            assertEquals(BigDecimal("3000.00"), result.totalDivDeclare)
            assertEquals(BigDecimal("1500.0000"), result.totalDivPuaAmt)
            assertEquals(3, result.recordCount)
        }
    }
}

@DisplayName("CoverageValueChange 單元測試")
class CoverageValueChangeTest {

    private fun createTestCoverage(
        statusCode: String = "P"
    ) = CoverageValueChange(
        policyNo = "P000000001",
        coverageNo = 1,
        planCode = "A1001",
        version = "1",
        rateSex = "1",
        rateAge = 30,
        rateSub1 = "01",
        rateSub2 = "001",
        issueDate = LocalDate.of(2020, 1, 1),
        statusCode = statusCode,
        insuranceType3 = "R",
        processDate = LocalDate.of(2020, 1, 1),
        processType = "O",
        policyType = null,
        statusCode2 = null
    )

    @Nested
    @DisplayName("isActive")
    inner class IsActive {

        @Test
        fun `should return true for active status`() {
            val coverage = createTestCoverage(statusCode = "P")
            assertTrue(coverage.isActive())
        }

        @Test
        fun `should return false for matured status`() {
            val coverage = createTestCoverage(statusCode = "M")
            assertFalse(coverage.isActive())
        }
    }

    @Nested
    @DisplayName("isMatured")
    inner class IsMatured {

        @Test
        fun `should return true for matured status`() {
            val coverage = createTestCoverage(statusCode = "M")
            assertTrue(coverage.isMatured())
        }
    }

    @Nested
    @DisplayName("isLapsed")
    inner class IsLapsed {

        @Test
        fun `should return true for lapsed status`() {
            val coverage = createTestCoverage(statusCode = "L")
            assertTrue(coverage.isLapsed())
        }
    }

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        fun `should fail for invalid policyNo`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                createTestCoverage().copy(policyNo = "")
            }
        }

        @Test
        fun `should fail for policyNo exceeding max length`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                createTestCoverage().copy(policyNo = "P0000000001") // 11 chars
            }
        }

        @Test
        fun `should fail for negative coverageNo`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                createTestCoverage().copy(coverageNo = -1)
            }
        }
    }
}

@DisplayName("ProductUnit 單元測試")
class ProductUnitTest {

    private fun createTestProductUnit() = ProductUnit(
        policyNo = "P000000001",
        coverageNo = 1,
        ps06Type = "1",
        cvpuType = "1",
        lastAnnivDur = 1,
        statusCode = "1",
        divDeclare = BigDecimal("1000.00"),
        divPuaAmt = BigDecimal("500.0000"),
        financialDate = LocalDate.of(2025, 1, 1),
        pcpoNo = null,
        programId = null,
        processDate = LocalDate.of(2025, 1, 1),
        policyType = null,
        approvedDate = null,
        programIdCvpu = null
    )

    @Test
    fun `should calculate total dividend correctly`() {
        val unit = createTestProductUnit()
        assertEquals(BigDecimal("1500.0000"), unit.getTotalDividend())
    }

    @Test
    fun `should return true for active status`() {
        val unit = createTestProductUnit()
        assertTrue(unit.isActive())
    }

    @Test
    fun `should generate correct id`() {
        val unit = createTestProductUnit()
        val id = unit.id
        assertEquals("P000000001", id.policyNo)
        assertEquals(1, id.coverageNo)
        assertEquals("1", id.ps06Type)
        assertEquals("1", id.cvpuType)
        assertEquals(1, id.lastAnnivDur)
    }
}

@DisplayName("CoverageStatusCode 列舉測試")
class CoverageStatusCodeTest {

    @Test
    fun `should find code by value`() {
        val result = CoverageStatusCode.fromCode("P")
        assertEquals(CoverageStatusCode.ACTIVE, result)
        assertEquals("有效", result?.description)
    }

    @Test
    fun `should return null for unknown code`() {
        val result = CoverageStatusCode.fromCode("X")
        assertNull(result)
    }
}

@DisplayName("InsuranceType3 列舉測試")
class InsuranceType3Test {

    @Test
    fun `should find type by code`() {
        val result = InsuranceType3.fromCode("R")
        assertEquals(InsuranceType3.REGULAR, result)
        assertEquals("一般保險", result?.description)
    }
}

@DisplayName("ProductUnitStatusCode 列舉測試")
class ProductUnitStatusCodeTest {

    @Test
    fun `should find status by code`() {
        val result = ProductUnitStatusCode.fromCode("1")
        assertEquals(ProductUnitStatusCode.ACTIVE, result)
        assertEquals("有效", result?.description)
    }
}
