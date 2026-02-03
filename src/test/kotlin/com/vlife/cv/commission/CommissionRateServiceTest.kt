package com.vlife.cv.commission

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
 * CommissionRateService 單元測試
 *
 * 注意：PageHelper 分頁功能使用 ThreadLocal 機制，難以在單元測試中模擬。
 * 因此不分頁版本直接調用 Mapper，分頁版本應透過整合測試驗證。
 * 參見 ADR-015 測試策略說明。
 */
@DisplayName("CommissionRateService 單元測試")
class CommissionRateServiceTest {

    private lateinit var mapper: CommissionRateMapper
    private lateinit var service: CommissionRateService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = CommissionRateService(mapper)
    }

    private fun createTestRate(
        serial: Long = 1L,
        commClassCode: String = "12RA1",
        commLineCode: String = "31",
        cratType: String = "1",
        startDate: LocalDate = LocalDate.of(2020, 1, 1),
        endDate: LocalDate = LocalDate.of(2030, 12, 31),
        commRate: BigDecimal? = BigDecimal("5.0000"),
        commStartAge: Int? = 20,
        commEndAge: Int? = 65
    ) = CommissionRate(
        serial = serial,
        commClassCode = commClassCode,
        commLineCode = commLineCode,
        cratType = cratType,
        projectNo = null,
        startDate = startDate,
        endDate = endDate,
        cratKey1 = "030",
        cratKey2 = "030",
        commStartYear = 1,
        commEndYear = 10,
        commStartAge = commStartAge,
        commEndAge = commEndAge,
        commStartModx = null,
        commEndModx = null,
        commRate = commRate,
        commRateOrg = null,
        premLimitStart = null,
        premLimitEnd = null
    )

    @Nested
    @DisplayName("findBySerial")
    inner class FindBySerial {

        @Test
        fun `should return rate when exists`() {
            // Given
            val rate = createTestRate(serial = 100L)
            every { mapper.findBySerial(100L) } returns rate

            // When
            val result = service.findBySerial(100L)

            // Then
            assertEquals(100L, result?.serial)
            assertEquals("12RA1", result?.commClassCode)
            verify(exactly = 1) { mapper.findBySerial(100L) }
        }

        @Test
        fun `should return null when not exists`() {
            // Given
            every { mapper.findBySerial(999L) } returns null

            // When
            val result = service.findBySerial(999L)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findByClassCode")
    inner class FindByClassCode {

        @Test
        fun `should return rates for given class code`() {
            // Given
            val rates = listOf(
                createTestRate(serial = 1L, commLineCode = "31"),
                createTestRate(serial = 2L, commLineCode = "21"),
                createTestRate(serial = 3L, commLineCode = "35")
            )
            every { mapper.findByClassCode("12RA1") } returns rates

            // When
            val result = service.findByClassCode("12RA1")

            // Then
            assertEquals(3, result.size)
            assertEquals("31", result[0].commLineCode)
            verify(exactly = 1) { mapper.findByClassCode("12RA1") }
        }

        @Test
        fun `should return empty list for unknown class code`() {
            // Given
            every { mapper.findByClassCode("XXXXX") } returns emptyList()

            // When
            val result = service.findByClassCode("XXXXX")

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("findEffectiveRates")
    inner class FindEffectiveRates {

        @Test
        fun `should return effective rates for given date`() {
            // Given
            val rates = listOf(
                createTestRate(serial = 1L, cratType = "1"),
                createTestRate(serial = 2L, cratType = "9")
            )
            val effectiveDate = LocalDate.of(2025, 6, 15)
            every { mapper.findEffectiveRates("12RA1", "31", effectiveDate) } returns rates

            // When
            val result = service.findEffectiveRates("12RA1", "31", effectiveDate)

            // Then
            assertEquals(2, result.size)
        }

        @Test
        fun `should return empty list when no effective rates`() {
            // Given
            val effectiveDate = LocalDate.of(1900, 1, 1)
            every { mapper.findEffectiveRates("12RA1", "31", effectiveDate) } returns emptyList()

            // When
            val result = service.findEffectiveRates("12RA1", "31", effectiveDate)

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("findEffectiveRateForAge")
    inner class FindEffectiveRateForAge {

        @Test
        fun `should return rate matching age range`() {
            // Given
            val rates = listOf(
                createTestRate(serial = 1L, commStartAge = 20, commEndAge = 40, commRate = BigDecimal("3.0")),
                createTestRate(serial = 2L, commStartAge = 41, commEndAge = 65, commRate = BigDecimal("5.0"))
            )
            val effectiveDate = LocalDate.of(2025, 6, 15)
            every { mapper.findEffectiveRates("12RA1", "31", effectiveDate) } returns rates

            // When
            val result = service.findEffectiveRateForAge("12RA1", "31", effectiveDate, 30)

            // Then
            assertEquals(1L, result?.serial)
            assertEquals(BigDecimal("3.0"), result?.commRate)
        }

        @Test
        fun `should return null when no rate matches age`() {
            // Given
            val rates = listOf(
                createTestRate(serial = 1L, commStartAge = 20, commEndAge = 40)
            )
            val effectiveDate = LocalDate.of(2025, 6, 15)
            every { mapper.findEffectiveRates("12RA1", "31", effectiveDate) } returns rates

            // When
            val result = service.findEffectiveRateForAge("12RA1", "31", effectiveDate, 50)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("existsByClassCode")
    inner class ExistsByClassCode {

        @Test
        fun `should return true when exists`() {
            // Given
            every { mapper.countByClassCode("12RA1") } returns 10

            // When
            val result = service.existsByClassCode("12RA1")

            // Then
            assertTrue(result)
        }

        @Test
        fun `should return false when not exists`() {
            // Given
            every { mapper.countByClassCode("XXXXX") } returns 0

            // When
            val result = service.existsByClassCode("XXXXX")

            // Then
            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("search")
    inner class Search {

        @Test
        fun `should search with all criteria`() {
            // Given
            val rates = listOf(createTestRate())
            val effectiveDate = LocalDate.of(2025, 6, 15)
            every {
                mapper.search("12RA1", "31", "1", effectiveDate)
            } returns rates

            // When
            val query = CommissionRateQuery(
                commClassCode = "12RA1",
                commLineCode = "31",
                cratType = "1",
                effectiveDate = effectiveDate
            )
            val result = service.search(query)

            // Then
            assertEquals(1, result.size)
            verify(exactly = 1) { mapper.search("12RA1", "31", "1", effectiveDate) }
        }

        @Test
        fun `should search with partial criteria`() {
            // Given
            val rates = listOf(
                createTestRate(serial = 1L),
                createTestRate(serial = 2L)
            )
            every {
                mapper.search(null, "31", null, null)
            } returns rates

            // When
            val query = CommissionRateQuery(commLineCode = "31")
            val result = service.search(query)

            // Then
            assertEquals(2, result.size)
        }
    }
}

@DisplayName("CommissionRate 單元測試")
class CommissionRateTest {

    private fun createTestRate(
        startDate: LocalDate = LocalDate.of(2020, 1, 1),
        endDate: LocalDate = LocalDate.of(2030, 12, 31),
        commStartAge: Int? = 20,
        commEndAge: Int? = 65,
        commStartYear: Int? = 1,
        commEndYear: Int? = 10
    ) = CommissionRate(
        serial = 1L,
        commClassCode = "12RA1",
        commLineCode = "31",
        cratType = "1",
        projectNo = null,
        startDate = startDate,
        endDate = endDate,
        cratKey1 = "030",
        cratKey2 = "030",
        commStartYear = commStartYear,
        commEndYear = commEndYear,
        commStartAge = commStartAge,
        commEndAge = commEndAge,
        commStartModx = null,
        commEndModx = null,
        commRate = BigDecimal("5.0000"),
        commRateOrg = null,
        premLimitStart = null,
        premLimitEnd = null
    )

    @Nested
    @DisplayName("isEffectiveAt")
    inner class IsEffectiveAt {

        @Test
        fun `should return true when date is within range`() {
            val rate = createTestRate()
            assertTrue(rate.isEffectiveAt(LocalDate.of(2025, 6, 15)))
        }

        @Test
        fun `should return true when date equals start date`() {
            val rate = createTestRate()
            assertTrue(rate.isEffectiveAt(LocalDate.of(2020, 1, 1)))
        }

        @Test
        fun `should return true when date equals end date`() {
            val rate = createTestRate()
            assertTrue(rate.isEffectiveAt(LocalDate.of(2030, 12, 31)))
        }

        @Test
        fun `should return false when date is before start`() {
            val rate = createTestRate()
            assertFalse(rate.isEffectiveAt(LocalDate.of(2019, 12, 31)))
        }

        @Test
        fun `should return false when date is after end`() {
            val rate = createTestRate()
            assertFalse(rate.isEffectiveAt(LocalDate.of(2031, 1, 1)))
        }
    }

    @Nested
    @DisplayName("isAgeInRange")
    inner class IsAgeInRange {

        @Test
        fun `should return true when age is within range`() {
            val rate = createTestRate(commStartAge = 20, commEndAge = 65)
            assertTrue(rate.isAgeInRange(30))
        }

        @Test
        fun `should return true when age equals start age`() {
            val rate = createTestRate(commStartAge = 20, commEndAge = 65)
            assertTrue(rate.isAgeInRange(20))
        }

        @Test
        fun `should return true when age equals end age`() {
            val rate = createTestRate(commStartAge = 20, commEndAge = 65)
            assertTrue(rate.isAgeInRange(65))
        }

        @Test
        fun `should return false when age is below range`() {
            val rate = createTestRate(commStartAge = 20, commEndAge = 65)
            assertFalse(rate.isAgeInRange(19))
        }

        @Test
        fun `should return false when age is above range`() {
            val rate = createTestRate(commStartAge = 20, commEndAge = 65)
            assertFalse(rate.isAgeInRange(66))
        }

        @Test
        fun `should return true when age range is null`() {
            val rate = createTestRate(commStartAge = null, commEndAge = null)
            assertTrue(rate.isAgeInRange(100))
        }
    }

    @Nested
    @DisplayName("isYearInRange")
    inner class IsYearInRange {

        @Test
        fun `should return true when year is within range`() {
            val rate = createTestRate(commStartYear = 1, commEndYear = 10)
            assertTrue(rate.isYearInRange(5))
        }

        @Test
        fun `should return false when year is outside range`() {
            val rate = createTestRate(commStartYear = 1, commEndYear = 10)
            assertFalse(rate.isYearInRange(11))
        }

        @Test
        fun `should return true when year range is null`() {
            val rate = createTestRate(commStartYear = null, commEndYear = null)
            assertTrue(rate.isYearInRange(100))
        }
    }

    @Nested
    @DisplayName("validation")
    inner class Validation {

        @Test
        fun `should fail for invalid commClassCode`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                createTestRate().copy(commClassCode = "")
            }
        }

        @Test
        fun `should fail for commClassCode exceeding max length`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                createTestRate().copy(commClassCode = "123456")
            }
        }

        @Test
        fun `should fail for start date after end date`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                createTestRate(
                    startDate = LocalDate.of(2030, 1, 1),
                    endDate = LocalDate.of(2020, 1, 1)
                )
            }
        }
    }
}

@DisplayName("CommissionLineCode 列舉測試")
class CommissionLineCodeTest {

    @Test
    fun `should find code by value`() {
        val result = CommissionLineCode.fromCode("31")
        assertEquals(CommissionLineCode.THREE_TIER_AGENT, result)
        assertEquals("三階業務員", result?.description)
    }

    @Test
    fun `should return null for unknown code`() {
        val result = CommissionLineCode.fromCode("XX")
        assertNull(result)
    }

    @Test
    fun `should throw for unknown code with OrThrow`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            CommissionLineCode.fromCodeOrThrow("XX")
        }
    }
}

@DisplayName("CommissionRateType 列舉測試")
class CommissionRateTypeTest {

    @Test
    fun `should find type by code`() {
        val result = CommissionRateType.fromCode("1")
        assertEquals(CommissionRateType.GENERAL, result)
        assertEquals("一般佣金_折算率", result?.description)
    }

    @Test
    fun `should return null for unknown type`() {
        val result = CommissionRateType.fromCode("X")
        assertNull(result)
    }
}
