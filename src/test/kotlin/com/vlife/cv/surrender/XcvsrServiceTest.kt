package com.vlife.cv.surrender

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

/**
 * XcvsrService 單元測試
 *
 * 測試策略：
 * - 使用 MockK 模擬 Mapper
 * - 目標覆蓋率：90%+
 * - 測試範圍：查詢、存在性檢查、CRUD、邊界條件
 */
@ExtendWith(MockKExtension::class)
class XcvsrServiceTest {

    @MockK
    private lateinit var mapper: XcvsrMapper

    private lateinit var service: XcvsrService

    @BeforeEach
    fun setUp() {
        service = XcvsrService(mapper)
    }

    // === 查詢測試 ===

    @Test
    fun `查詢 - 記錄存在`() {
        // Given
        val entity = CrossProductSurrender(
            policyNo = "L123456789",
            coverageNo = 1,
            scoreRatingUnit = BigDecimal("100.5")
        )
        every { mapper.findByPolicyNoAndCoverageNo("L123456789", 1) } returns entity

        // When
        val result = service.getByPolicyNoAndCoverageNo("L123456789", 1)

        // Then
        assertNotNull(result)
        assertEquals("L123456789", result?.policyNo)
        assertEquals(1, result?.coverageNo)
        assertEquals(BigDecimal("100.5"), result?.scoreRatingUnit)
        verify { mapper.findByPolicyNoAndCoverageNo("L123456789", 1) }
    }

    @Test
    fun `查詢 - 記錄不存在回傳 null`() {
        // Given
        every { mapper.findByPolicyNoAndCoverageNo(any(), any()) } returns null

        // When
        val result = service.getByPolicyNoAndCoverageNo("INVALID", 99)

        // Then
        assertNull(result)
        verify { mapper.findByPolicyNoAndCoverageNo("INVALID", 99) }
    }

    @Test
    fun `查詢保單所有險種 - 有資料`() {
        // Given
        val records = listOf(
            CrossProductSurrender("L123456789", 1, BigDecimal("100.5")),
            CrossProductSurrender("L123456789", 2, BigDecimal("50.0")),
            CrossProductSurrender("L123456789", 3, BigDecimal("75.25"))
        )
        every { mapper.findAllByPolicyNo("L123456789") } returns records

        // When
        val result = service.findAllByPolicyNo("L123456789")

        // Then
        assertEquals(3, result.size)
        assertEquals(1, result[0].coverageNo)
        assertEquals(2, result[1].coverageNo)
        assertEquals(3, result[2].coverageNo)
        verify { mapper.findAllByPolicyNo("L123456789") }
    }

    @Test
    fun `查詢保單所有險種 - 無資料回傳空清單`() {
        // Given
        every { mapper.findAllByPolicyNo(any()) } returns emptyList()

        // When
        val result = service.findAllByPolicyNo("NOPOLICY")

        // Then
        assertTrue(result.isEmpty())
        verify { mapper.findAllByPolicyNo("NOPOLICY") }
    }

    // === 存在性檢查測試 ===

    @Test
    fun `存在性檢查 - 記錄存在`() {
        // Given
        every { mapper.exists("L123456789", 1) } returns true

        // When
        val result = service.exists("L123456789", 1)

        // Then
        assertTrue(result)
        verify { mapper.exists("L123456789", 1) }
    }

    @Test
    fun `存在性檢查 - 記錄不存在`() {
        // Given
        every { mapper.exists("INVALID", 99) } returns false

        // When
        val result = service.exists("INVALID", 99)

        // Then
        assertFalse(result)
        verify { mapper.exists("INVALID", 99) }
    }

    // === 新增測試 ===

    @Test
    fun `新增記錄 - 成功`() {
        // Given
        val entity = CrossProductSurrender("L123456789", 1, BigDecimal("100.5"))
        every { mapper.exists("L123456789", 1) } returns false
        every { mapper.insert(entity) } returns 1

        // When
        val result = service.create(entity, "SYSTEM")

        // Then
        assertEquals(1, result)
        verify { mapper.exists("L123456789", 1) }
        verify { mapper.insert(entity) }
    }

    @Test
    fun `新增記錄 - 失敗（記錄已存在）`() {
        // Given
        val entity = CrossProductSurrender("L123456789", 1, BigDecimal("100.5"))
        every { mapper.exists("L123456789", 1) } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            service.create(entity, "SYSTEM")
        }
        assertTrue(exception.message!!.contains("XCVSR 記錄已存在"))
        verify { mapper.exists("L123456789", 1) }
        verify(exactly = 0) { mapper.insert(any()) }
    }

    // === 更新測試 ===

    @Test
    fun `更新記錄 - 成功`() {
        // Given
        val entity = CrossProductSurrender("L123456789", 1, BigDecimal("200.0"))
        every { mapper.update(entity) } returns 1

        // When
        val result = service.update(entity, "ADMIN")

        // Then
        assertEquals(1, result)
        verify { mapper.update(entity) }
    }

    @Test
    fun `更新記錄 - 失敗（記錄不存在）`() {
        // Given
        val entity = CrossProductSurrender("INVALID", 99, BigDecimal("100.0"))
        every { mapper.update(entity) } returns 0

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            service.update(entity, "ADMIN")
        }
        assertTrue(exception.message!!.contains("XCVSR 記錄不存在，無法更新"))
        verify { mapper.update(entity) }
    }

    // === 刪除測試 ===

    @Test
    fun `刪除記錄 - 成功`() {
        // Given
        every { mapper.delete("L123456789", 1) } returns 1

        // When
        val result = service.delete("L123456789", 1, "ADMIN")

        // Then
        assertEquals(1, result)
        verify { mapper.delete("L123456789", 1) }
    }

    @Test
    fun `刪除記錄 - 失敗（記錄不存在）`() {
        // Given
        every { mapper.delete("INVALID", 99) } returns 0

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            service.delete("INVALID", 99, "ADMIN")
        }
        assertTrue(exception.message!!.contains("XCVSR 記錄不存在，無法刪除"))
        verify { mapper.delete("INVALID", 99) }
    }

    // === 邊界條件測試 ===

    @Test
    fun `邊界條件 - 保單號碼空字串`() {
        // Given
        every { mapper.findByPolicyNoAndCoverageNo("", 1) } returns null

        // When
        val result = service.getByPolicyNoAndCoverageNo("", 1)

        // Then
        assertNull(result)
    }

    @Test
    fun `邊界條件 - 險種序號為 0`() {
        // Given
        every { mapper.findByPolicyNoAndCoverageNo("L123456789", 0) } returns null

        // When
        val result = service.getByPolicyNoAndCoverageNo("L123456789", 0)

        // Then
        assertNull(result)
    }

    @Test
    fun `邊界條件 - 極大評分單位值`() {
        // Given
        val entity = CrossProductSurrender("L123456789", 1, BigDecimal("999999.9"))
        every { mapper.findByPolicyNoAndCoverageNo("L123456789", 1) } returns entity

        // When
        val result = service.getByPolicyNoAndCoverageNo("L123456789", 1)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("999999.9"), result?.scoreRatingUnit)
    }

    @Test
    fun `邊界條件 - 負數評分單位值`() {
        // Given
        val entity = CrossProductSurrender("L123456789", 1, BigDecimal("-100.5"))
        every { mapper.findByPolicyNoAndCoverageNo("L123456789", 1) } returns entity

        // When
        val result = service.getByPolicyNoAndCoverageNo("L123456789", 1)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("-100.5"), result?.scoreRatingUnit)
    }
}
