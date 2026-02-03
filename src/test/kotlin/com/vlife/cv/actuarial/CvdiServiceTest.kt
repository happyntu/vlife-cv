package com.vlife.cv.actuarial

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CvdiService 單元測試
 *
 * 遵循 ADR-17 規範，測試表格導向命名的 Service。
 * 參見 ADR-15 測試策略說明。
 */
@DisplayName("CvdiService 單元測試")
class CvdiServiceTest {

    private lateinit var mapper: CvdiMapper
    private lateinit var service: CvdiService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = CvdiService(mapper)
    }

    private fun createTestCvdi(
        planCode: String = "12RA1",
        version: String = "1",
        paidStatus: String = "01",
        rateSex: String = "0",
        age: Int = 30,
        policyYear: Int = 1,
        declareDate: LocalDate = LocalDate.of(2025, 1, 1),
        rateRatio: BigDecimal = BigDecimal("0.0250")
    ) = Cvdi(
        planCode = planCode,
        version = version,
        paidStatus = paidStatus,
        rateSex = rateSex,
        ageLimitStart = age,
        ageLimitEnd = age,
        faceAmtStart = 0L,
        faceAmtEnd = 999999999L,
        modePremStart = BigDecimal.ZERO,
        modePremEnd = BigDecimal("999999999.99"),
        policyYear = policyYear,
        declareDate = declareDate,
        rateRatio = rateRatio,
        deathRatio = null,
        loadingRatio = null,
        rewardRatio = null,
        death1Ratio = null,
        death2Ratio = null,
        ratioFee = null,
        fixFee = null,
        detBirRate = null,
        confirmFlag = null,
        confirmOper = null,
        confirmDate = null,
        averageDiscount = null
    )

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return dividend levels for given plan code and version`() {
            // Given
            val levels = listOf(
                createTestCvdi(policyYear = 1),
                createTestCvdi(policyYear = 2),
                createTestCvdi(policyYear = 3)
            )
            every { mapper.findByPlanCode("12RA1", "1") } returns levels

            // When
            val result = service.findByPlanCode("12RA1", "1")

            // Then
            assertEquals(3, result.size)
            verify(exactly = 1) { mapper.findByPlanCode("12RA1", "1") }
        }

        @Test
        fun `should return empty list when no data`() {
            // Given
            every { mapper.findByPlanCode("XXXXX", "1") } returns emptyList()

            // When
            val result = service.findByPlanCode("XXXXX", "1")

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("findByPlanCodeAndPaidStatus")
    inner class FindByPlanCodeAndPaidStatus {

        @Test
        fun `should return dividend levels for given paid status`() {
            // Given
            val levels = listOf(
                createTestCvdi(paidStatus = "01"),
                createTestCvdi(paidStatus = "01")
            )
            every { mapper.findByPlanCodeAndPaidStatus("12RA1", "1", "01") } returns levels

            // When
            val result = service.findByPlanCodeAndPaidStatus("12RA1", "1", "01")

            // Then
            assertEquals(2, result.size)
            verify(exactly = 1) { mapper.findByPlanCodeAndPaidStatus("12RA1", "1", "01") }
        }
    }

    @Nested
    @DisplayName("findByCondition")
    inner class FindByCondition {

        @Test
        fun `should return matching dividend level`() {
            // Given
            val level = createTestCvdi(rateRatio = BigDecimal("0.0350"))
            val declareDate = LocalDate.of(2025, 1, 1)
            every {
                mapper.findByCondition(
                    "12RA1", "1", "01", "0",
                    30, 1000000L, BigDecimal("50000"),
                    1, declareDate
                )
            } returns level

            // When
            val result = service.findByCondition(
                planCode = "12RA1",
                version = "1",
                paidStatus = "01",
                rateSex = "0",
                age = 30,
                faceAmt = 1000000L,
                modePrem = BigDecimal("50000"),
                policyYear = 1,
                declareDate = declareDate
            )

            // Then
            assertNotNull(result)
            assertEquals(BigDecimal("0.0350"), result.rateRatio)
        }

        @Test
        fun `should return null when no matching level`() {
            // Given
            val declareDate = LocalDate.of(2025, 1, 1)
            every {
                mapper.findByCondition(
                    "XXXXX", "1", "01", "0",
                    99, 1L, BigDecimal.ONE,
                    99, declareDate
                )
            } returns null

            // When
            val result = service.findByCondition(
                planCode = "XXXXX",
                version = "1",
                paidStatus = "01",
                rateSex = "0",
                age = 99,
                faceAmt = 1L,
                modePrem = BigDecimal.ONE,
                policyYear = 99,
                declareDate = declareDate
            )

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findAllPlanCodes")
    inner class FindAllPlanCodes {

        @Test
        fun `should return all distinct plan codes`() {
            // Given
            every { mapper.findAllPlanCodes() } returns listOf("12RA1", "12RA2", "12RA3")

            // When
            val result = service.findAllPlanCodes()

            // Then
            assertEquals(3, result.size)
            assertEquals("12RA1", result[0])
        }
    }

    @Nested
    @DisplayName("findDeclareDates")
    inner class FindDeclareDates {

        @Test
        fun `should return all declare dates for plan code`() {
            // Given
            val dates = listOf(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2023, 1, 1)
            )
            every { mapper.findDeclareDates("12RA1", "1") } returns dates

            // When
            val result = service.findDeclareDates("12RA1", "1")

            // Then
            assertEquals(3, result.size)
            assertEquals(LocalDate.of(2025, 1, 1), result[0])
        }
    }

    @Nested
    @DisplayName("countByPlanCode")
    inner class CountByPlanCode {

        @Test
        fun `should return count for given plan code`() {
            // Given
            every { mapper.countByPlanCode("12RA1", "1") } returns 150

            // When
            val result = service.countByPlanCode("12RA1", "1")

            // Then
            assertEquals(150, result)
        }
    }

    @Nested
    @DisplayName("existsByPlanCode")
    inner class ExistsByPlanCode {

        @Test
        fun `should return true when exists`() {
            // Given
            every { mapper.countByPlanCode("12RA1", "1") } returns 10

            // When
            val result = service.existsByPlanCode("12RA1", "1")

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when not exists`() {
            // Given
            every { mapper.countByPlanCode("XXXXX", "1") } returns 0

            // When
            val result = service.existsByPlanCode("XXXXX", "1")

            // Then
            assertFalse(result)
        }
    }
}
