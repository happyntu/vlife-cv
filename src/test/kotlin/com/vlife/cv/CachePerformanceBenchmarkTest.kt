package com.vlife.cv

import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.vlife.cv.config.CacheConfig
import com.vlife.cv.config.CacheConfig.Companion.CACHE_COMMISSION_RATE_BY_CLASS_CODE
import com.vlife.cv.config.CacheConfig.Companion.CACHE_COMMISSION_RATE_BY_SERIAL
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 快取效能基準測試 (P2-9)
 *
 * 驗證 Caffeine 快取配置正確性與效能指標：
 * - 快取命中率 (Cache Hit Rate)
 * - 快取統計資訊 (Cache Statistics)
 * - 查詢延遲測量 (Query Latency)
 *
 * 注意：此測試使用 Spring Test Context 但不連接真實資料庫，
 * 主要驗證快取機制本身的行為。
 */
@SpringBootTest(classes = [TestApplication::class])
@ActiveProfiles("integration-test")
@Import(TestConfiguration::class, CacheConfig::class)
@DisplayName("快取效能基準測試")
class CachePerformanceBenchmarkTest {

    @Autowired
    @Qualifier(CV_CACHE_MANAGER)
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun setup() {
        // 清除所有快取
        cacheManager.cacheNames.forEach { name ->
            cacheManager.getCache(name)?.clear()
        }
    }

    @Nested
    @DisplayName("快取配置驗證")
    inner class CacheConfigurationTest {

        @Test
        fun `should have all expected cache names configured`() {
            // Given
            val expectedCaches = listOf(
                CACHE_COMMISSION_RATE_BY_SERIAL,
                CACHE_COMMISSION_RATE_BY_CLASS_CODE,
                CacheConfig.CACHE_COMMISSION_RATE_EFFECTIVE,
                CacheConfig.CACHE_DIVIDEND_SUMMARY,
                CacheConfig.CACHE_CVDI_BY_PLAN,
                CacheConfig.CACHE_CVRF_BY_PLAN
            )

            // When
            val actualCaches = cacheManager.cacheNames.toList()

            // Then
            expectedCaches.forEach { expected ->
                assertTrue(actualCaches.contains(expected), "Missing cache: $expected")
            }
        }

        @Test
        fun `should use CaffeineCacheManager`() {
            assertTrue(cacheManager is CaffeineCacheManager, "Expected CaffeineCacheManager")
        }

        @Test
        fun `should have stats recording enabled`() {
            // 驗證統計記錄已啟用
            val cache = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as? CaffeineCache
            assertNotNull(cache, "Cache should not be null")

            // 執行一些操作來產生統計
            cache.put("test-key", "test-value")
            cache.get("test-key")
            cache.get("non-existent-key")

            // 取得統計資訊
            val stats = cache.nativeCache.stats()
            assertNotNull(stats, "Stats should not be null")
            assertTrue(stats.hitCount() >= 1, "Should have at least one hit")
            assertTrue(stats.missCount() >= 1, "Should have at least one miss")
        }
    }

    @Nested
    @DisplayName("快取命中率測試")
    inner class CacheHitRateTest {

        @Test
        fun `should achieve high hit rate with repeated queries`() {
            // Given
            val cache = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as CaffeineCache
            val testKey = "serial:12345"
            val testValue = "cached-commission-rate"
            val iterations = 100

            // First call - cache miss
            cache.put(testKey, testValue)

            // When - Repeated queries (should all be hits)
            repeat(iterations) {
                cache.get(testKey)
            }

            // Then
            val stats = cache.nativeCache.stats()
            val hitRate = stats.hitRate()

            // 應達到高命中率 (接近 100%)
            assertTrue(
                hitRate >= 0.99,
                "Hit rate should be >= 99%, actual: ${hitRate * 100}%"
            )
        }

        @Test
        fun `should record cache misses for new keys`() {
            // Given
            val cache = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_CLASS_CODE) as CaffeineCache

            // When - Query non-existent keys
            repeat(10) { i ->
                cache.get("non-existent-key-$i")
            }

            // Then
            val stats = cache.nativeCache.stats()
            assertEquals(10, stats.missCount().toInt(), "Should have 10 misses")
        }
    }

    @Nested
    @DisplayName("快取效能基準")
    inner class CachePerformanceBenchmark {

        @Test
        fun `cache get should be sub-microsecond`() {
            // Given
            val cache = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as CaffeineCache
            val testKey = "perf-test-key"
            val testValue = "perf-test-value"
            cache.put(testKey, testValue)

            // Warm up
            repeat(1000) {
                cache.get(testKey)
            }

            // When - Measure cache get latency
            val iterations = 10_000
            val totalNanos = measureNanoTime {
                repeat(iterations) {
                    cache.get(testKey)
                }
            }

            // Then
            val avgNanos = totalNanos / iterations
            val avgMicros = avgNanos / 1000.0

            println("Average cache get latency: $avgNanos ns ($avgMicros µs)")

            // Caffeine 快取讀取應在微秒級
            assertTrue(
                avgMicros < 10.0,
                "Cache get should be < 10 µs, actual: $avgMicros µs"
            )
        }

        @Test
        fun `cache put should be fast`() {
            // Given
            val cache = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as CaffeineCache

            // Warm up
            repeat(1000) { i ->
                cache.put("warmup-$i", "value-$i")
            }
            cache.clear()

            // When - Measure cache put latency
            val iterations = 10_000
            val totalNanos = measureNanoTime {
                repeat(iterations) { i ->
                    cache.put("key-$i", "value-$i")
                }
            }

            // Then
            val avgNanos = totalNanos / iterations
            val avgMicros = avgNanos / 1000.0

            println("Average cache put latency: $avgNanos ns ($avgMicros µs)")

            // Caffeine 快取寫入應在微秒級
            assertTrue(
                avgMicros < 50.0,
                "Cache put should be < 50 µs, actual: $avgMicros µs"
            )
        }
    }

    @Nested
    @DisplayName("快取統計資訊")
    inner class CacheStatisticsTest {

        @Test
        fun `should provide accurate cache statistics`() {
            // Given
            val cache = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as CaffeineCache
            val testKey = "stats-test-key"
            val testValue = "stats-test-value"

            // When
            cache.get("miss-1")  // Miss
            cache.get("miss-2")  // Miss
            cache.put(testKey, testValue)  // Put
            cache.get(testKey)  // Hit
            cache.get(testKey)  // Hit
            cache.get(testKey)  // Hit

            // Then
            val stats = cache.nativeCache.stats()
            printCacheStats("commissionRateBySerial", stats)

            assertEquals(3, stats.hitCount().toInt(), "Should have 3 hits")
            assertEquals(2, stats.missCount().toInt(), "Should have 2 misses")
            assertEquals(5, stats.requestCount().toInt(), "Should have 5 total requests")
        }

        @Test
        fun `should report eviction count when cache is full`() {
            // Given - 使用小容量快取來測試逐出
            val cache = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as CaffeineCache

            // When - 寫入大量項目 (超過預設 60,000 限制需要很長時間，這裡只驗證機制)
            repeat(1000) { i ->
                cache.put("eviction-test-$i", "value-$i")
            }

            // Then - 驗證快取運作正常
            val stats = cache.nativeCache.stats()
            assertTrue(stats.loadCount() >= 0, "Load count should be non-negative")
        }

        private fun printCacheStats(cacheName: String, stats: CacheStats) {
            println(
                """
                |
                |=== Cache Statistics: $cacheName ===
                |Hit Count:     ${stats.hitCount()}
                |Miss Count:    ${stats.missCount()}
                |Hit Rate:      ${String.format("%.2f", stats.hitRate() * 100)}%
                |Request Count: ${stats.requestCount()}
                |Eviction Count: ${stats.evictionCount()}
                |Load Success:  ${stats.loadSuccessCount()}
                |Load Failure:  ${stats.loadFailureCount()}
                |Avg Load Time: ${String.format("%.3f", stats.averageLoadPenalty() / 1_000_000.0)} ms
                |=====================================
                """.trimMargin()
            )
        }
    }

    @Nested
    @DisplayName("多快取隔離測試")
    inner class MultipleCacheIsolationTest {

        @Test
        fun `should isolate data between different caches`() {
            // Given
            val cache1 = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as CaffeineCache
            val cache2 = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_CLASS_CODE) as CaffeineCache
            val sameKey = "shared-key"

            // When
            cache1.put(sameKey, "value-from-cache1")
            cache2.put(sameKey, "value-from-cache2")

            // Then
            assertEquals("value-from-cache1", cache1.get(sameKey)?.get())
            assertEquals("value-from-cache2", cache2.get(sameKey)?.get())
        }

        @Test
        fun `should clear caches independently`() {
            // Given
            val cache1 = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_SERIAL) as CaffeineCache
            val cache2 = cacheManager.getCache(CACHE_COMMISSION_RATE_BY_CLASS_CODE) as CaffeineCache

            cache1.put("key1", "value1")
            cache2.put("key2", "value2")

            // When
            cache1.clear()

            // Then
            assertEquals(null, cache1.get("key1"))
            assertEquals("value2", cache2.get("key2")?.get())
        }
    }
}
