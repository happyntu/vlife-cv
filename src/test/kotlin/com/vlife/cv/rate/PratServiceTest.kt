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

/**
 * Unit tests for PratService
 *
 * Coverage target: >= 80%
 */
class PratServiceTest {

    private lateinit var pratMapper: PratMapper
    private lateinit var pratService: PratService

    @BeforeEach
    fun setup() {
        pratMapper = mockk()
        pratService = PratService(pratMapper)
    }

    // ===== Query Tests =====

    @Test
    fun `getRateDirect - successful query`() {
        // Given
        val rate = createMockPlanRate()
        every {
            pratMapper.findByPrimaryKey("6AA01", "1", "1", "00", "01", 30)
        } returns rate

        // When
        val result = pratService.getRateDirect("6AA01", "1", "1", "00", "01", 30)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("125.50"), result?.annualPrem)
        assertEquals("6AA01", result?.planCode)
    }

    @Test
    fun `getRateDirect - not found returns null`() {
        // Given
        every {
            pratMapper.findByPrimaryKey(any(), any(), any(), any(), any(), any())
        } returns null

        // When
        val result = pratService.getRateDirect("XXXXX", "1", "1", "00", "01", 99)

        // Then
        assertNull(result)
    }

    @Test
    fun `getRate - non-group contract queries PRAT`() {
        // Given
        val rate = createMockPlanRate()
        every {
            pratMapper.findByPrimaryKey(any(), any(), any(), any(), any(), any())
        } returns rate

        val query = PlanRateQuery(
            planCode = "6AA01", version = "1", rateSex = "1",
            rateSub1 = "00", rateSub2 = "01", rateAge = 30,
            contractNo = "C001", rateSexInd = 1
        )

        // When
        val result = pratService.getRate(query)

        // Then
        assertNotNull(result)
        verify { pratMapper.findByPrimaryKey("6AA01", "1", "1", "00", "01", 30) }
    }

    @Test
    fun `getRate - rateSexInd=2 forces rateSex to 0`() {
        // Given
        val rate = createMockPlanRate()
        every {
            pratMapper.findByPrimaryKey(any(), any(), eq("0"), any(), any(), any())
        } returns rate

        val query = PlanRateQuery(
            planCode = "6AA01", version = "1", rateSex = "1",
            rateSub1 = "00", rateSub2 = "01", rateAge = 30,
            contractNo = null, rateSexInd = 2  // No sex distinction
        )

        // When
        val result = pratService.getRate(query)

        // Then
        assertNotNull(result)
        verify { pratMapper.findByPrimaryKey("6AA01", "1", "0", "00", "01", 30) }
    }

    @Test
    fun `getRate - contractNo null does not trigger BIDC query`() {
        // Given
        val rate = createMockPlanRate()
        every {
            pratMapper.findByPrimaryKey(any(), any(), any(), any(), any(), any())
        } returns rate

        val query = PlanRateQuery(
            planCode = "6AA01", version = "1", rateSex = "1",
            rateSub1 = "00", rateSub2 = "01", rateAge = 30,
            contractNo = null, rateSexInd = 1
        )

        // When
        pratService.getRate(query)

        // Then (no BIDC service call, directly queries PRAT)
        verify { pratMapper.findByPrimaryKey("6AA01", "1", "1", "00", "01", 30) }
    }

    // ===== Count Tests =====

    @Test
    fun `exists - record exists`() {
        // Given
        every {
            pratMapper.countByPrimaryKey("6AA01", "1", "1", "00", "01", 30)
        } returns 1

        // When
        val result = pratService.exists("6AA01", "1", "1", "00", "01", 30)

        // Then
        assertTrue(result)
    }

    @Test
    fun `exists - record not exists`() {
        // Given
        every {
            pratMapper.countByPrimaryKey(any(), any(), any(), any(), any(), any())
        } returns 0

        // When
        val result = pratService.exists("XXXXX", "1", "1", "00", "01", 99)

        // Then
        assertFalse(result)
    }

    @Test
    fun `countDynamic - partial conditions with nulls`() {
        // Given
        every {
            pratMapper.countDynamic("6AA01", "1", null, null, null, null)
        } returns 50

        // When
        val result = pratService.countDynamic("6AA01", "1", null, null, null, null)

        // Then
        assertEquals(50, result)
    }

    @Test
    fun `countDynamic - all conditions specified`() {
        // Given
        every {
            pratMapper.countDynamic("6AA01", "1", "1", "00", "01", 30)
        } returns 1

        // When
        val result = pratService.countDynamic("6AA01", "1", "1", "00", "01", 30)

        // Then
        assertEquals(1, result)
    }

    @Test
    fun `countDynamic - blank planCode throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            pratService.countDynamic("", "1", null, null, null, null)
        }
    }

    @Test
    fun `countDynamic - blank version throws exception`() {
        // When & Then
        assertThrows<IllegalArgumentException> {
            pratService.countDynamic("6AA01", "", null, null, null, null)
        }
    }

    // ===== Modification Tests =====

    @Test
    fun `create - successful insert`() {
        // Given
        val entity = createMockPlanRate()
        every {
            pratMapper.countByPrimaryKey(any(), any(), any(), any(), any(), any())
        } returns 0
        every { pratMapper.insert(entity) } just runs

        // When
        val result = pratService.create(entity)

        // Then
        assertEquals(entity, result)
        verify { pratMapper.insert(entity) }
    }

    @Test
    fun `create - duplicate insert throws exception`() {
        // Given
        val entity = createMockPlanRate()
        every {
            pratMapper.countByPrimaryKey(any(), any(), any(), any(), any(), any())
        } returns 1

        // When & Then
        assertThrows<IllegalStateException> {
            pratService.create(entity)
        }
    }

    @Test
    fun `update - successful update`() {
        // Given
        val key = PlanRateKey("6AA01", "1", "1", "00", "01", 30)
        val entity = createMockPlanRate()
        every { pratMapper.update(key, entity) } just runs

        // When
        val result = pratService.update(key, entity)

        // Then
        assertEquals(entity, result)
        verify { pratMapper.update(key, entity) }
    }

    @Test
    fun `delete - successful deletion`() {
        // Given
        every {
            pratMapper.deleteByPrimaryKey("6AA01", "1", "1", "00", "01", 30)
        } just runs

        // When (no exception)
        pratService.delete("6AA01", "1", "1", "00", "01", 30)

        // Then
        verify { pratMapper.deleteByPrimaryKey("6AA01", "1", "1", "00", "01", 30) }
    }

    // ===== Boundary Condition Tests =====

    @Test
    fun `rate with all nullable fields as null`() {
        // Given
        val rate = PlanRate(
            planCode = "6AA01", version = "1", rateSex = "1",
            rateSub1 = "00", rateSub2 = "01", rateAge = 30,
            annualPrem = BigDecimal("125.50"),
            annualPrem2 = null, employeeDisc = null, loadingRate2 = null
        )
        every {
            pratMapper.findByPrimaryKey("6AA01", "1", "1", "00", "01", 30)
        } returns rate

        // When
        val result = pratService.getRateDirect("6AA01", "1", "1", "00", "01", 30)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("125.50"), result?.annualPrem)
        assertNull(result?.annualPrem2)
        assertNull(result?.employeeDisc)
        assertNull(result?.loadingRate2)
    }

    @Test
    fun `rate with rateSex=0 (no sex distinction)`() {
        // Given
        val rate = createMockPlanRate(rateSex = "0")
        every {
            pratMapper.findByPrimaryKey("6AA01", "1", "0", "00", "01", 30)
        } returns rate

        // When
        val result = pratService.getRateDirect("6AA01", "1", "0", "00", "01", 30)

        // Then
        assertNotNull(result)
        assertEquals("0", result?.rateSex)
    }

    @Test
    fun `rate with rateAge boundary values`() {
        // Test age = 0
        val rateAge0 = createMockPlanRate(rateAge = 0)
        every {
            pratMapper.findByPrimaryKey("6AA01", "1", "1", "00", "01", 0)
        } returns rateAge0

        val result0 = pratService.getRateDirect("6AA01", "1", "1", "00", "01", 0)
        assertNotNull(result0)
        assertEquals(0, result0?.rateAge)

        // Test age = 99
        val rateAge99 = createMockPlanRate(rateAge = 99)
        every {
            pratMapper.findByPrimaryKey("6AA01", "1", "1", "00", "01", 99)
        } returns rateAge99

        val result99 = pratService.getRateDirect("6AA01", "1", "1", "00", "01", 99)
        assertNotNull(result99)
        assertEquals(99, result99?.rateAge)
    }

    // ===== Helper Methods =====

    private fun createMockPlanRate(
        planCode: String = "6AA01",
        version: String = "1",
        rateSex: String = "1",
        rateSub1: String = "00",
        rateSub2: String = "01",
        rateAge: Int = 30
    ) = PlanRate(
        planCode = planCode,
        version = version,
        rateSex = rateSex,
        rateSub1 = rateSub1,
        rateSub2 = rateSub2,
        rateAge = rateAge,
        annualPrem = BigDecimal("125.50"),
        annualPrem2 = null,
        employeeDisc = BigDecimal("10.00"),
        loadingRate2 = null
    )
}
