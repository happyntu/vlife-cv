package com.vlife.cv.actuarial

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CvrfService 單元測試
 *
 * 遵循 ADR-017 規範，測試表格導向命名的 Service。
 * 參見 ADR-015 測試策略說明。
 */
@DisplayName("CvrfService 單元測試")
class CvrfServiceTest {

    private lateinit var mapper: CvrfMapper
    private lateinit var service: CvrfService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = CvrfService(mapper)
    }

    private fun createTestCvrf(
        planCode: String = "12RA1",
        version: String = "1",
        durationType: Int = 1
    ) = Cvrf(
        planCode = planCode,
        version = version,
        durationType = durationType,
        durationYear = 20,
        collectionYear = 20,
        payMode = 1,
        insuredType = 1,
        policyReserveDeathFactor = 1,
        policyReserveRate = BigDecimal("0.0200"),
        reserveDeathFactor = 1,
        reserveRate = BigDecimal("0.0200"),
        policyReserveTso = "N",
        reserveTso = "N",
        etReserveRate = null,
        etReserveTsoInd = null,
        rbnType = null,
        sbnType = null,
        i26OrNot = "N",
        i26Rate = null,
        accidentOrNot = "N",
        accidentRate = null,
        returnPremInd = null,
        returnIntType = null,
        returnInt = null,
        returnCostFlag = "N",
        modifyReserveInd = "N",
        recordType10 = "N",
        mixReserveInd = "N",
        returnPremInd2 = null,
        etPolicyReserveRate = null,
        etPolicyReserveTsoInd = null,
        etAccidentInd = null,
        etAccidentRate = null
    )

    @Nested
    @DisplayName("findByPlanCode")
    inner class FindByPlanCode {

        @Test
        fun `should return reserve factors for given plan code and version`() {
            // Given
            val factors = listOf(
                createTestCvrf(durationType = 1),
                createTestCvrf(durationType = 2),
                createTestCvrf(durationType = 3)
            )
            every { mapper.findByPlanCode("12RA1", "1") } returns factors

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
    @DisplayName("findById")
    inner class FindById {

        @Test
        fun `should return reserve factor when exists`() {
            // Given
            val factor = createTestCvrf(durationType = 1)
            every { mapper.findById("12RA1", "1", 1) } returns factor

            // When
            val result = service.findById("12RA1", "1", 1)

            // Then
            assertNotNull(result)
            assertEquals("12RA1", result.planCode)
            assertEquals(1, result.durationType)
        }

        @Test
        fun `should return null when not exists`() {
            // Given
            every { mapper.findById("XXXXX", "1", 3) } returns null

            // When
            val result = service.findById("XXXXX", "1", 3)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findByDurationType")
    inner class FindByDurationType {

        @Test
        fun `should return reserve factors for given duration type`() {
            // Given
            val factors = listOf(
                createTestCvrf(planCode = "12RA1", durationType = 1),
                createTestCvrf(planCode = "12RA2", durationType = 1)
            )
            every { mapper.findByDurationType(1) } returns factors

            // When
            val result = service.findByDurationType(1)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.durationType == 1 })
        }
    }

    @Nested
    @DisplayName("findWholeLifeFactors")
    inner class FindWholeLifeFactors {

        @Test
        fun `should return whole life reserve factors`() {
            // Given
            val factors = listOf(
                createTestCvrf(durationType = DurationType.WHOLE_LIFE.code)
            )
            every { mapper.findByDurationType(DurationType.WHOLE_LIFE.code) } returns factors

            // When
            val result = service.findWholeLifeFactors()

            // Then
            assertEquals(1, result.size)
            assertEquals(DurationType.WHOLE_LIFE.code, result[0].durationType)
        }
    }

    @Nested
    @DisplayName("findTermFactors")
    inner class FindTermFactors {

        @Test
        fun `should return term reserve factors`() {
            // Given
            val factors = listOf(
                createTestCvrf(durationType = DurationType.TERM.code)
            )
            every { mapper.findByDurationType(DurationType.TERM.code) } returns factors

            // When
            val result = service.findTermFactors()

            // Then
            assertEquals(1, result.size)
            assertEquals(DurationType.TERM.code, result[0].durationType)
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
    @DisplayName("countByPlanCode")
    inner class CountByPlanCode {

        @Test
        fun `should return count for given plan code`() {
            // Given
            every { mapper.countByPlanCode("12RA1", "1") } returns 50

            // When
            val result = service.countByPlanCode("12RA1", "1")

            // Then
            assertEquals(50, result)
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
