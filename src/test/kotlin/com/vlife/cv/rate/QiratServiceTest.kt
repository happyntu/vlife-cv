package com.vlife.cv.rate

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("QiratService 單元測試")
class QiratServiceTest {

    private lateinit var mapper: QiratMapper
    private lateinit var service: QiratService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = QiratService(mapper)
    }

    private fun createTestRate(
        planCode: String = "LE01",
        rateType: String = "5",
        str: LocalDate = LocalDate.of(2020, 1, 1),
        end: LocalDate? = LocalDate.of(2020, 12, 31),
        rate: BigDecimal? = BigDecimal("250.0000")
    ) = SubAccountInterestRate(
        subAcntPlanCode = planCode,
        intRateType = rateType,
        intRateDateStr = str,
        intRateDateEnd = end,
        intRate = rate
    )

    @Nested
    @DisplayName("getEffectiveRate")
    inner class GetEffectiveRate {
        @Test
        fun `should return direct match`() {
            val rate = createTestRate()
            every { mapper.findByBaseDate("LE01", "5", LocalDate.of(2020, 6, 15)) } returns rate
            val result = service.getEffectiveRate("LE01", "5", LocalDate.of(2020, 6, 15))
            assertNotNull(result)
            assertEquals(BigDecimal("250.0000"), result.intRate)
        }

        @Test
        fun `should fallback to max before date`() {
            val fallback = createTestRate(str = LocalDate.of(2019, 1, 1), end = LocalDate.of(2019, 12, 31))
            every { mapper.findByBaseDate("LE01", "5", LocalDate.of(2020, 6, 15)) } returns null
            every { mapper.findMaxBeforeDate("LE01", "5", LocalDate.of(2020, 6, 15)) } returns fallback
            val result = service.getEffectiveRate("LE01", "5", LocalDate.of(2020, 6, 15))
            assertNotNull(result)
            assertEquals(LocalDate.of(2019, 1, 1), result.intRateDateStr)
        }

        @Test
        fun `should return null when no match and no fallback`() {
            every { mapper.findByBaseDate("LE01", "5", any()) } returns null
            every { mapper.findMaxBeforeDate("LE01", "5", any()) } returns null
            val result = service.getEffectiveRate("LE01", "5", LocalDate.of(2020, 6, 15))
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findByBaseDate")
    inner class FindByBaseDate {
        @Test
        fun `should return rate when found`() {
            val rate = createTestRate()
            every { mapper.findByBaseDate("LE01", "5", LocalDate.of(2020, 6, 15)) } returns rate
            val result = service.findByBaseDate("LE01", "5", LocalDate.of(2020, 6, 15))
            assertNotNull(result)
        }
    }

    @Nested
    @DisplayName("countByPlanAndType")
    inner class CountByPlanAndType {
        @Test
        fun `should return count`() {
            every { mapper.countByPlanAndType("LE01", "5") } returns 10
            assertEquals(10, service.countByPlanAndType("LE01", "5"))
        }

        @Test
        fun `should count all types when null`() {
            every { mapper.countByPlanAndType("LE01", null) } returns 25
            assertEquals(25, service.countByPlanAndType("LE01"))
        }
    }

    @Nested
    @DisplayName("existsByFullKey")
    inner class ExistsByFullKey {
        @Test
        fun `should return true when exists`() {
            every { mapper.existsByFullKey("LE01", "5", LocalDate.of(2020, 1, 1)) } returns true
            assertTrue(service.existsByFullKey("LE01", "5", LocalDate.of(2020, 1, 1)))
        }

        @Test
        fun `should return false when not exists`() {
            every { mapper.existsByFullKey("LE01", "5", LocalDate.of(2099, 1, 1)) } returns false
            assertFalse(service.existsByFullKey("LE01", "5", LocalDate.of(2099, 1, 1)))
        }
    }

    @Nested
    @DisplayName("insertWithDateAdjust")
    inner class InsertWithDateAdjust {
        @Test
        fun `should insert and adjust previous end date`() {
            val rate = createTestRate(str = LocalDate.of(2021, 1, 1), end = SubAccountInterestRate.INFINITE_END_DATE)
            every { mapper.existsByFullKey("LE01", "5", LocalDate.of(2021, 1, 1)) } returns false
            every { mapper.findMaxStartDate("LE01", "5") } returns LocalDate.of(2020, 1, 1)
            every { mapper.updateEndDate("LE01", "5", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)) } returns 1
            every { mapper.insert(rate) } returns 1

            service.insertWithDateAdjust(rate)

            verify(exactly = 1) { mapper.updateEndDate("LE01", "5", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)) }
            verify(exactly = 1) { mapper.insert(rate) }
        }

        @Test
        fun `should throw when duplicate key`() {
            val rate = createTestRate()
            every { mapper.existsByFullKey("LE01", "5", LocalDate.of(2020, 1, 1)) } returns true
            assertThrows<IllegalArgumentException> { service.insertWithDateAdjust(rate) }
        }
    }

    @Nested
    @DisplayName("deleteWithDateAdjust")
    inner class DeleteWithDateAdjust {
        @Test
        fun `should delete and restore previous end date to infinite`() {
            every { mapper.delete("LE01", "5", LocalDate.of(2021, 1, 1), SubAccountInterestRate.INFINITE_END_DATE) } returns 1
            every { mapper.findByDateAfterOrEqual("LE01", "5", LocalDate.of(2021, 1, 1)) } returns null
            every { mapper.findMaxStartDate("LE01", "5") } returns LocalDate.of(2020, 1, 1)
            every { mapper.updateEndDate("LE01", "5", LocalDate.of(2020, 1, 1), SubAccountInterestRate.INFINITE_END_DATE) } returns 1

            service.deleteWithDateAdjust("LE01", "5", LocalDate.of(2021, 1, 1), SubAccountInterestRate.INFINITE_END_DATE)

            verify(exactly = 1) { mapper.updateEndDate("LE01", "5", LocalDate.of(2020, 1, 1), SubAccountInterestRate.INFINITE_END_DATE) }
        }

        @Test
        fun `should throw when no record to delete`() {
            every { mapper.delete("LE01", "5", any(), any()) } returns 0
            assertThrows<IllegalArgumentException> { service.deleteWithDateAdjust("LE01", "5", LocalDate.of(2099, 1, 1), LocalDate.of(2099, 12, 31)) }
        }
    }

    @Nested
    @DisplayName("updateRate")
    inner class UpdateRate {
        @Test
        fun `should update rate`() {
            every { mapper.updateRate("LE01", "5", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), BigDecimal("300.0000")) } returns 1
            val result = service.updateRate("LE01", "5", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31), BigDecimal("300.0000"))
            assertEquals(1, result)
        }
    }
}
