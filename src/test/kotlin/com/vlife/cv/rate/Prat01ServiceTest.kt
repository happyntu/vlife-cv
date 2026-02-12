package com.vlife.cv.rate

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PRAT01 Service 單元測試
 *
 * 測試策略：
 * - 使用 MockK mock Mapper
 * - 驗證業務邏輯正確性
 * - 快取驗證需在整合測試中執行（@SpringBootTest）
 *
 * 覆蓋率目標: 90%+
 */
class Prat01ServiceTest {

    private val mapper: Prat01Mapper = mockk()
    private val service = Prat01Service(mapper)

    // ===== 查詢測試 =====

    @Test
    fun `查詢費率 - 成功`() {
        // Given
        val key = PlanRate01Key("U0A24", "2", "2", "00", "000", 40)
        val entity = mockPlanRate01(key)
        every { mapper.findByPrimaryKey(key) } returns entity

        // When
        val result = service.findByPrimaryKey(key)

        // Then
        assertNotNull(result)
        assertEquals("U0A24", result.planCode)
        assertEquals("2", result.version)
        assertEquals("2", result.rateSex)
        assertEquals("00", result.rateSub1)
        assertEquals("000", result.rateSub2)
        assertEquals(40, result.rateAge)
        assertEquals(BigDecimal("0.06000000"), result.prem6Diff)
        assertEquals(BigDecimal("0.03000000"), result.prem3Diff)
        assertEquals(BigDecimal("0.04000000"), result.prem1Diff)
        verify(exactly = 1) { mapper.findByPrimaryKey(key) }
    }

    @Test
    fun `查詢費率 - 不存在`() {
        // Given
        val key = PlanRate01Key("XXXXX", "9", "M", "99", "999", 999)
        every { mapper.findByPrimaryKey(key) } returns null

        // When
        val result = service.findByPrimaryKey(key)

        // Then
        assertNull(result)
        verify(exactly = 1) { mapper.findByPrimaryKey(key) }
    }

    @Test
    fun `查詢險種所有費率 - 成功`() {
        // Given
        val entities = listOf(
            mockPlanRate01(PlanRate01Key("U0A24", "2", "2", "00", "000", 40)),
            mockPlanRate01(PlanRate01Key("U0A24", "2", "2", "00", "000", 41)),
            mockPlanRate01(PlanRate01Key("U0A24", "2", "2", "00", "000", 42))
        )
        every { mapper.findByPlanCode("U0A24") } returns entities

        // When
        val result = service.findByPlanCode("U0A24")

        // Then
        assertEquals(3, result.size)
        assertTrue(result.all { it.planCode == "U0A24" })
        verify(exactly = 1) { mapper.findByPlanCode("U0A24") }
    }

    @Test
    fun `查詢險種所有費率 - 無資料`() {
        // Given
        every { mapper.findByPlanCode("XXXXX") } returns emptyList()

        // When
        val result = service.findByPlanCode("XXXXX")

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 1) { mapper.findByPlanCode("XXXXX") }
    }

    @Test
    fun `查詢險種版本費率 - 成功`() {
        // Given
        val entities = listOf(
            mockPlanRate01(PlanRate01Key("U0A24", "2", "2", "00", "000", 40)),
            mockPlanRate01(PlanRate01Key("U0A24", "2", "2", "00", "000", 41))
        )
        every { mapper.findByPlanCodeAndVersion("U0A24", "2") } returns entities

        // When
        val result = service.findByPlanCodeAndVersion("U0A24", "2")

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.planCode == "U0A24" && it.version == "2" })
        verify(exactly = 1) { mapper.findByPlanCodeAndVersion("U0A24", "2") }
    }

    // ===== 存在性檢查測試 =====

    @Test
    fun `存在性檢查 - 存在`() {
        // Given
        val key = PlanRate01Key("U0A24", "2", "2", "00", "000", 40)
        every { mapper.existsByPrimaryKey(key) } returns true

        // When
        val result = service.exists(key)

        // Then
        assertTrue(result)
        verify(exactly = 1) { mapper.existsByPrimaryKey(key) }
    }

    @Test
    fun `存在性檢查 - 不存在`() {
        // Given
        val key = PlanRate01Key("XXXXX", "9", "M", "99", "999", 999)
        every { mapper.existsByPrimaryKey(key) } returns false

        // When
        val result = service.exists(key)

        // Then
        assertFalse(result)
        verify(exactly = 1) { mapper.existsByPrimaryKey(key) }
    }

    // ===== 計數測試 =====

    @Test
    fun `計數 - 有資料`() {
        // Given
        every { mapper.countByPlanCode("U0A24") } returns 150

        // When
        val result = service.countByPlanCode("U0A24")

        // Then
        assertEquals(150, result)
        verify(exactly = 1) { mapper.countByPlanCode("U0A24") }
    }

    @Test
    fun `計數 - 無資料`() {
        // Given
        every { mapper.countByPlanCode("XXXXX") } returns 0

        // When
        val result = service.countByPlanCode("XXXXX")

        // Then
        assertEquals(0, result)
        verify(exactly = 1) { mapper.countByPlanCode("XXXXX") }
    }

    // ===== 邊界條件測試 =====

    @Test
    fun `查詢費率 - 年齡邊界值 0`() {
        // Given
        val key = PlanRate01Key("U0A24", "2", "2", "00", "000", 0)
        val entity = mockPlanRate01(key)
        every { mapper.findByPrimaryKey(key) } returns entity

        // When
        val result = service.findByPrimaryKey(key)

        // Then
        assertNotNull(result)
        assertEquals(0, result.rateAge)
    }

    @Test
    fun `查詢費率 - 年齡邊界值 120`() {
        // Given
        val key = PlanRate01Key("U0A24", "2", "2", "00", "000", 120)
        val entity = mockPlanRate01(key)
        every { mapper.findByPrimaryKey(key) } returns entity

        // When
        val result = service.findByPrimaryKey(key)

        // Then
        assertNotNull(result)
        assertEquals(120, result.rateAge)
    }

    @Test
    fun `查詢費率 - 性別代碼 M`() {
        // Given
        val key = PlanRate01Key("U0A24", "2", "M", "00", "000", 40)
        val entity = mockPlanRate01(key)
        every { mapper.findByPrimaryKey(key) } returns entity

        // When
        val result = service.findByPrimaryKey(key)

        // Then
        assertNotNull(result)
        assertEquals("M", result.rateSex)
    }

    @Test
    fun `查詢費率 - BigDecimal 精度驗證`() {
        // Given
        val key = PlanRate01Key("U0A24", "2", "2", "00", "000", 40)
        val entity = PlanRate01(
            planCode = "U0A24",
            version = "2",
            rateSex = "2",
            rateSub1 = "00",
            rateSub2 = "000",
            rateAge = 40,
            prem6Diff = BigDecimal("0.12345678"),  // 8 位小數精度
            prem3Diff = BigDecimal("0.87654321"),
            prem1Diff = BigDecimal("0.11111111")
        )
        every { mapper.findByPrimaryKey(key) } returns entity

        // When
        val result = service.findByPrimaryKey(key)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("0.12345678"), result.prem6Diff)
        assertEquals(BigDecimal("0.87654321"), result.prem3Diff)
        assertEquals(BigDecimal("0.11111111"), result.prem1Diff)
    }

    // ===== Mock 工具方法 =====

    private fun mockPlanRate01(key: PlanRate01Key) = PlanRate01(
        planCode = key.planCode,
        version = key.version,
        rateSex = key.rateSex,
        rateSub1 = key.rateSub1,
        rateSub2 = key.rateSub2,
        rateAge = key.rateAge,
        prem6Diff = BigDecimal("0.06000000"),
        prem3Diff = BigDecimal("0.03000000"),
        prem1Diff = BigDecimal("0.04000000")
    )
}
