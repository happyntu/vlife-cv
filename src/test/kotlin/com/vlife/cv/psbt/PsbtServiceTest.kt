package com.vlife.cv.psbt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * PsbtService 單元測試
 *
 * 測試策略：
 * - 使用 MockK mock PsbtMapper
 * - 驗證範圍匹配查詢邏輯
 * - 驗證快取行為（Cache Hit/Miss）
 * - 驗證應用層範圍匹配（findFromCacheByRange）
 */
class PsbtServiceTest {

    private lateinit var psbtMapper: PsbtMapper
    private lateinit var psbtService: PsbtService

    @BeforeEach
    fun setUp() {
        psbtMapper = mockk()
        psbtService = PsbtService(psbtMapper)
    }

    @Test
    fun `findByKeysAndRange - should return Psbt when key is within range`() {
        // Given
        val expectedPsbt = createPsbt(
            planCode = "A0001",
            version = "1",
            rateSex = "0",
            psbtType = "1",
            psbtCode = "0001",
            psbtKey1 = 20,
            psbtKey2 = 30,
            psbtKey3 = 10,
            psbtKey4 = 20,
            psbtValue = BigDecimal("1.5000")
        )

        every {
            psbtMapper.findByKeysAndRange("A0001", "1", "0", "1", "0001", 25, 15)
        } returns expectedPsbt

        // When
        val result = psbtService.findByKeysAndRange("A0001", "1", "0", "1", "0001", 25, 15)

        // Then
        assertNotNull(result)
        assertEquals("A0001", result?.planCode)
        assertEquals(BigDecimal("1.5000"), result?.psbtValue)
        verify(exactly = 1) { psbtMapper.findByKeysAndRange("A0001", "1", "0", "1", "0001", 25, 15) }
    }

    @Test
    fun `findByKeysAndRange - should return null when key is out of range`() {
        // Given
        every {
            psbtMapper.findByKeysAndRange("A0001", "1", "0", "1", "0001", 100, 50)
        } returns null

        // When
        val result = psbtService.findByKeysAndRange("A0001", "1", "0", "1", "0001", 100, 50)

        // Then
        assertNull(result)
        verify(exactly = 1) { psbtMapper.findByKeysAndRange("A0001", "1", "0", "1", "0001", 100, 50) }
    }

    @Test
    fun `findByKeysAndRange - should handle boundary values`() {
        // Given: key1 = 20 (exactly at lower bound), key2 = 20 (exactly at upper bound)
        val expectedPsbt = createPsbt(
            psbtKey1 = 20,
            psbtKey2 = 30,
            psbtKey3 = 10,
            psbtKey4 = 20
        )

        every {
            psbtMapper.findByKeysAndRange(any(), any(), any(), any(), any(), 20, 20)
        } returns expectedPsbt

        // When
        val result = psbtService.findByKeysAndRange("A0001", "1", "0", "1", "0001", 20, 20)

        // Then
        assertNotNull(result)
    }

    @Test
    fun `findAllByKeys - should return list from database and cache`() {
        // Given
        val expectedList = listOf(
            createPsbt(psbtKey1 = 20, psbtKey2 = 30, psbtKey3 = 10, psbtKey4 = 20),
            createPsbt(psbtKey1 = 31, psbtKey2 = 40, psbtKey3 = 10, psbtKey4 = 20)
        )

        every {
            psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001")
        } returns expectedList

        // When: 第一次查詢（應該查資料庫）
        val result1 = psbtService.findAllByKeys("A0001", "1", "0", "1", "0001")

        // When: 第二次查詢（應該從快取取得）
        val result2 = psbtService.findAllByKeys("A0001", "1", "0", "1", "0001")

        // Then
        assertEquals(2, result1.size)
        assertEquals(2, result2.size)
        verify(exactly = 1) { psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001") }
    }

    @Test
    fun `findFromCacheByRange - should find matching record from cached list`() {
        // Given
        val records = listOf(
            createPsbt(
                psbtKey1 = 20, psbtKey2 = 30, psbtKey3 = 10, psbtKey4 = 20,
                psbtValue = BigDecimal("1.5000")
            ),
            createPsbt(
                psbtKey1 = 31, psbtKey2 = 40, psbtKey3 = 10, psbtKey4 = 20,
                psbtValue = BigDecimal("2.0000")
            )
        )

        every {
            psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001")
        } returns records

        // When: 查詢 key1=25, key2=15（應該匹配第一筆）
        val result = psbtService.findFromCacheByRange("A0001", "1", "0", "1", "0001", 25, 15)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("1.5000"), result?.psbtValue)
    }

    @Test
    fun `findFromCacheByRange - should return null when no matching range`() {
        // Given
        val records = listOf(
            createPsbt(psbtKey1 = 20, psbtKey2 = 30, psbtKey3 = 10, psbtKey4 = 20)
        )

        every {
            psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001")
        } returns records

        // When: 查詢 key1=100（超出所有範圍）
        val result = psbtService.findFromCacheByRange("A0001", "1", "0", "1", "0001", 100, 15)

        // Then
        assertNull(result)
    }

    @Test
    fun `findFromCacheByRange - should handle boundary matching correctly`() {
        // Given
        val records = listOf(
            createPsbt(
                psbtKey1 = 20, psbtKey2 = 30, psbtKey3 = 10, psbtKey4 = 20,
                psbtValue = BigDecimal("1.5000")
            )
        )

        every {
            psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001")
        } returns records

        // When: 測試邊界值 key1=20（下限邊界）
        val result1 = psbtService.findFromCacheByRange("A0001", "1", "0", "1", "0001", 20, 15)
        // When: 測試邊界值 key1=30（上限邊界）
        val result2 = psbtService.findFromCacheByRange("A0001", "1", "0", "1", "0001", 30, 15)

        // Then: 都應該匹配
        assertNotNull(result1)
        assertNotNull(result2)
    }

    @Test
    fun `evictCache - should remove specific cache entry`() {
        // Given
        val records = listOf(createPsbt())
        every {
            psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001")
        } returns records

        // When: 建立快取
        psbtService.findAllByKeys("A0001", "1", "0", "1", "0001")

        // 清除快取
        psbtService.evictCache("A0001", "1", "0", "1", "0001")

        // 再次查詢
        psbtService.findAllByKeys("A0001", "1", "0", "1", "0001")

        // Then: Mapper 應該被呼叫兩次
        verify(exactly = 2) { psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001") }
    }

    @Test
    fun `evictAllCache - should remove all cache entries`() {
        // Given
        val records1 = listOf(createPsbt(planCode = "A0001"))
        val records2 = listOf(createPsbt(planCode = "A0002"))

        every { psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001") } returns records1
        every { psbtMapper.findAllByKeys("A0002", "1", "0", "1", "0001") } returns records2

        // When: 建立多筆快取
        psbtService.findAllByKeys("A0001", "1", "0", "1", "0001")
        psbtService.findAllByKeys("A0002", "1", "0", "1", "0001")

        // 清除所有快取
        psbtService.evictAllCache()

        // 再次查詢
        psbtService.findAllByKeys("A0001", "1", "0", "1", "0001")
        psbtService.findAllByKeys("A0002", "1", "0", "1", "0001")

        // Then: 每個查詢應該被呼叫兩次
        verify(exactly = 2) { psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001") }
        verify(exactly = 2) { psbtMapper.findAllByKeys("A0002", "1", "0", "1", "0001") }
    }

    @Test
    fun `getCacheStats - should return cache statistics`() {
        // Given
        every { psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001") } returns listOf(createPsbt())

        // When: 多次查詢以產生統計資料
        psbtService.findAllByKeys("A0001", "1", "0", "1", "0001") // Cache miss
        psbtService.findAllByKeys("A0001", "1", "0", "1", "0001") // Cache hit

        val stats = psbtService.getCacheStats()

        // Then
        assertNotNull(stats)
        assertTrue(stats.contains("hitRate="))
        assertTrue(stats.contains("hitCount="))
        assertTrue(stats.contains("missCount="))
    }

    @Test
    fun `cache key uniqueness - different combinations should not share cache`() {
        // Given
        val records1 = listOf(createPsbt(rateSex = "0", psbtValue = BigDecimal("1.5000")))
        val records2 = listOf(createPsbt(rateSex = "1", psbtValue = BigDecimal("1.8000")))

        every { psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001") } returns records1
        every { psbtMapper.findAllByKeys("A0001", "1", "1", "1", "0001") } returns records2

        // When
        val result1 = psbtService.findAllByKeys("A0001", "1", "0", "1", "0001")
        val result2 = psbtService.findAllByKeys("A0001", "1", "1", "1", "0001")

        // Then: 不同性別應該取得不同資料
        assertEquals(BigDecimal("1.5000"), result1[0].psbtValue)
        assertEquals(BigDecimal("1.8000"), result2[0].psbtValue)
        verify(exactly = 1) { psbtMapper.findAllByKeys("A0001", "1", "0", "1", "0001") }
        verify(exactly = 1) { psbtMapper.findAllByKeys("A0001", "1", "1", "1", "0001") }
    }

    /**
     * 建立測試用 Psbt
     */
    private fun createPsbt(
        planCode: String = "A0001",
        version: String = "1",
        rateSex: String = "0",
        psbtType: String = "1",
        psbtCode: String = "0001",
        psbtKey1: Long = 20,
        psbtKey2: Long = 30,
        psbtKey3: Long = 10,
        psbtKey4: Long = 20,
        psbtValue: BigDecimal? = BigDecimal("1.5000")
    ): Psbt {
        return Psbt(
            planCode = planCode,
            version = version,
            rateSex = rateSex,
            psbtType = psbtType,
            psbtCode = psbtCode,
            psbtKey1 = psbtKey1,
            psbtKey2 = psbtKey2,
            psbtKey3 = psbtKey3,
            psbtKey4 = psbtKey4,
            psbtValue = psbtValue
        )
    }
}
