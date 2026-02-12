package com.vlife.cv.rate

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Unit tests for QtprtService
 *
 * Coverage target: >= 80%
 */
class QtprtServiceTest {

    private lateinit var qtprtMapper: QtprtMapper
    private lateinit var qtprtService: QtprtService

    @BeforeEach
    fun setup() {
        qtprtMapper = mockk()
        qtprtService = QtprtService(qtprtMapper)
    }

    // ===== Query Tests =====

    @Test
    fun `getQuoteRate - successful query`() {
        // Given
        val rate = createMockQuoteRate()
        val query = QuoteRateQuery(
            targetPremCode = "TPQL4",
            targetType = "A",
            rateAge = 21,
            rateSex = "1",
            qtprtType = "1",
            effectiveDate = LocalDate.of(2007, 1, 15)
        )
        every {
            qtprtMapper.findByEffectiveDate(
                "TPQL4", "A", 21, "1", "1", LocalDate.of(2007, 1, 15)
            )
        } returns rate

        // When
        val result = qtprtService.getQuoteRate(query)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("260"), result?.annualPrem)
        assertEquals("TPQL4", result?.targetPremCode)
    }

    @Test
    fun `getQuoteRate - not found returns null`() {
        // Given
        val query = QuoteRateQuery(
            targetPremCode = "XXXXX",
            targetType = "A",
            rateAge = 99,
            rateSex = "1",
            qtprtType = "1",
            effectiveDate = LocalDate.now()
        )
        every {
            qtprtMapper.findByEffectiveDate(any(), any(), any(), any(), any(), any())
        } returns null

        // When
        val result = qtprtService.getQuoteRate(query)

        // Then
        assertNull(result)
    }

    @Test
    fun `getQuoteRateByKey - successful query`() {
        // Given
        val rate = createMockQuoteRate()
        every {
            qtprtMapper.findByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                21, "1", "1"
            )
        } returns rate

        // When
        val result = qtprtService.getQuoteRateByKey(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            21, "1", "1"
        )

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("260"), result?.annualPrem)
    }

    @Test
    fun `getQuoteRatesByTargetPremCode - returns list`() {
        // Given
        val rates = listOf(
            createMockQuoteRate(rateAge = 21),
            createMockQuoteRate(rateAge = 22)
        )
        every {
            qtprtMapper.findByTargetPremCode("TPQL4")
        } returns rates

        // When
        val result = qtprtService.getQuoteRatesByTargetPremCode("TPQL4")

        // Then
        assertEquals(2, result.size)
        assertEquals(21, result[0].rateAge)
        assertEquals(22, result[1].rateAge)
    }

    @Test
    fun `getQuoteRatesByTargetPremCode - empty list when not found`() {
        // Given
        every {
            qtprtMapper.findByTargetPremCode("XXXXX")
        } returns emptyList()

        // When
        val result = qtprtService.getQuoteRatesByTargetPremCode("XXXXX")

        // Then
        assertTrue(result.isEmpty())
    }

    // ===== Count Tests =====

    @Test
    fun `exists - record exists`() {
        // Given
        every {
            qtprtMapper.countByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                21, "1", "1"
            )
        } returns 1

        // When
        val result = qtprtService.exists(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            21, "1", "1"
        )

        // Then
        assertTrue(result)
    }

    @Test
    fun `exists - record not exists`() {
        // Given
        every {
            qtprtMapper.countByPrimaryKey(any(), any(), any(), any(), any(), any(), any())
        } returns 0

        // When
        val result = qtprtService.exists(
            "XXXXX", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            99, "1", "1"
        )

        // Then
        assertFalse(result)
    }

    // ===== Modification Tests =====

    @Test
    fun `create - successful insert`() {
        // Given
        val entity = createMockQuoteRate()
        every {
            qtprtMapper.countByPrimaryKey(any(), any(), any(), any(), any(), any(), any())
        } returns 0
        every { qtprtMapper.insert(entity) } just runs

        // When
        val result = qtprtService.create(entity)

        // Then
        assertEquals(entity, result)
        verify { qtprtMapper.insert(entity) }
    }

    @Test
    fun `create - duplicate insert throws exception`() {
        // Given
        val entity = createMockQuoteRate()
        every {
            qtprtMapper.countByPrimaryKey(any(), any(), any(), any(), any(), any(), any())
        } returns 1

        // When & Then
        assertThrows<IllegalStateException> {
            qtprtService.create(entity)
        }
    }

    @Test
    fun `update - successful update`() {
        // Given
        val key = QuoteRateKey(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            21, "1", "1"
        )
        val entity = createMockQuoteRate()
        every { qtprtMapper.update(key, entity) } just runs

        // When
        val result = qtprtService.update(key, entity)

        // Then
        assertEquals(entity, result)
        verify { qtprtMapper.update(key, entity) }
    }

    @Test
    fun `delete - successful deletion`() {
        // Given
        every {
            qtprtMapper.deleteByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                21, "1", "1"
            )
        } just runs

        // When (no exception)
        qtprtService.delete(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            21, "1", "1"
        )

        // Then
        verify {
            qtprtMapper.deleteByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                21, "1", "1"
            )
        }
    }

    // ===== Boundary Condition Tests =====

    @Test
    fun `rate with rateSex=0 (no sex distinction)`() {
        // Given
        val rate = createMockQuoteRate(rateSex = "0")
        every {
            qtprtMapper.findByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                21, "0", "1"
            )
        } returns rate

        // When
        val result = qtprtService.getQuoteRateByKey(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            21, "0", "1"
        )

        // Then
        assertNotNull(result)
        assertEquals("0", result?.rateSex)
    }

    @Test
    fun `rate with different target types`() {
        // Test target type A
        val rateA = createMockQuoteRate(targetType = "A")
        every {
            qtprtMapper.findByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                21, "1", "1"
            )
        } returns rateA

        val resultA = qtprtService.getQuoteRateByKey(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            21, "1", "1"
        )
        assertNotNull(resultA)
        assertEquals("A", resultA?.targetType)

        // Test target type B
        val rateB = createMockQuoteRate(targetType = "B")
        every {
            qtprtMapper.findByPrimaryKey(
                "TPQL4", "B",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                21, "1", "1"
            )
        } returns rateB

        val resultB = qtprtService.getQuoteRateByKey(
            "TPQL4", "B",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            21, "1", "1"
        )
        assertNotNull(resultB)
        assertEquals("B", resultB?.targetType)
    }

    @Test
    fun `rate with rateAge boundary values`() {
        // Test age = 0
        val rateAge0 = createMockQuoteRate(rateAge = 0)
        every {
            qtprtMapper.findByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                0, "1", "1"
            )
        } returns rateAge0

        val result0 = qtprtService.getQuoteRateByKey(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            0, "1", "1"
        )
        assertNotNull(result0)
        assertEquals(0, result0?.rateAge)

        // Test age = 99
        val rateAge99 = createMockQuoteRate(rateAge = 99)
        every {
            qtprtMapper.findByPrimaryKey(
                "TPQL4", "A",
                LocalDate.of(2006, 12, 11),
                LocalDate.of(2010, 12, 31),
                99, "1", "1"
            )
        } returns rateAge99

        val result99 = qtprtService.getQuoteRateByKey(
            "TPQL4", "A",
            LocalDate.of(2006, 12, 11),
            LocalDate.of(2010, 12, 31),
            99, "1", "1"
        )
        assertNotNull(result99)
        assertEquals(99, result99?.rateAge)
    }

    @Test
    fun `query with default effective date`() {
        // Given
        val rate = createMockQuoteRate()
        val today = LocalDate.now()
        val query = QuoteRateQuery(
            targetPremCode = "TPQL4",
            targetType = "A",
            rateAge = 21,
            rateSex = "1",
            qtprtType = "1"
            // effectiveDate defaults to today
        )
        every {
            qtprtMapper.findByEffectiveDate(
                "TPQL4", "A", 21, "1", "1", today
            )
        } returns rate

        // When
        val result = qtprtService.getQuoteRate(query)

        // Then
        assertNotNull(result)
        verify { qtprtMapper.findByEffectiveDate("TPQL4", "A", 21, "1", "1", today) }
    }

    // ===== Helper Methods =====

    private fun createMockQuoteRate(
        targetPremCode: String = "TPQL4",
        targetType: String = "A",
        strDate: LocalDate = LocalDate.of(2006, 12, 11),
        endDate: LocalDate = LocalDate.of(2010, 12, 31),
        rateAge: Int = 21,
        rateSex: String = "1",
        qtprtType: String = "1"
    ) = QuoteRate(
        targetPremCode = targetPremCode,
        targetType = targetType,
        strDate = strDate,
        endDate = endDate,
        rateAge = rateAge,
        rateSex = rateSex,
        qtprtType = qtprtType,
        annualPrem = BigDecimal("260")
    )
}
