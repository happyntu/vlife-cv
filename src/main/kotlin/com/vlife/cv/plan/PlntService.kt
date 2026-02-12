package com.vlife.cv.plan

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * PLNT 險種註記 Service
 *
 * 提供險種註記查詢功能，並實作 Caffeine Cache（遵循 ADR-014）。
 *
 * 快取策略：
 * - TTL: 24 小時（險種註記資料很少變更）
 * - 最大容量: 10,000 筆
 * - Key: "planCode:version"
 */
@Service
class PlntService(
    private val plntMapper: PlntMapper
) {

    private val logger = LoggerFactory.getLogger(PlntService::class.java)

    /**
     * Caffeine Cache 設定
     * - maximumSize: 10,000 筆（涵蓋大部分險種版本）
     * - expireAfterWrite: 24 小時（險種註記資料異動頻率低）
     */
    private val cache: Cache<String, PlanNote> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .recordStats()
        .build()

    /**
     * 依險種代碼與版本查詢險種註記（含快取）
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 險種註記，若不存在則返回 null
     */
    fun findByPlanCodeAndVersion(planCode: String, version: String): PlanNote? {
        val cacheKey = buildCacheKey(planCode, version)

        // 先查詢快取
        val cachedValue = cache.getIfPresent(cacheKey)
        if (cachedValue != null) {
            logger.debug("Cache hit for key: {}", cacheKey)
            return cachedValue
        }

        // Cache miss，查詢資料庫
        logger.debug("Cache miss for key: {}, querying from database", cacheKey)
        val result = plntMapper.findByPlanCodeAndVersion(planCode, version)

        // 只有非 null 結果才放入快取
        if (result != null) {
            cache.put(cacheKey, result)
        }

        return result
    }

    /**
     * 依險種代碼查詢所有版本險種註記（不使用快取）
     *
     * 理由：返回多筆結果，不適合單一 key 快取
     *
     * @param planCode 險種代碼
     * @return 險種註記列表
     */
    fun findByPlanCode(planCode: String): List<PlanNote> {
        logger.debug("Query all versions for planCode: {}", planCode)
        return plntMapper.findByPlanCode(planCode)
    }

    /**
     * 查詢所有險種註記（不使用快取）
     *
     * 理由：返回大量資料，不適合快取
     *
     * @return 所有險種註記列表
     */
    fun findAll(): List<PlanNote> {
        logger.debug("Query all plan notes")
        return plntMapper.findAll()
    }

    /**
     * 清除特定險種版本的快取
     *
     * @param planCode 險種代碼
     * @param version 版本號
     */
    fun evictCache(planCode: String, version: String) {
        val cacheKey = buildCacheKey(planCode, version)
        cache.invalidate(cacheKey)
        logger.info("Cache evicted for key: {}", cacheKey)
    }

    /**
     * 清除所有快取
     */
    fun evictAllCache() {
        cache.invalidateAll()
        logger.info("All cache invalidated")
    }

    /**
     * 取得快取統計資訊
     *
     * @return 快取統計字串
     */
    fun getCacheStats(): String {
        val stats = cache.stats()
        return "CacheStats[hitRate=${stats.hitRate()}, " +
                "hitCount=${stats.hitCount()}, " +
                "missCount=${stats.missCount()}, " +
                "evictionCount=${stats.evictionCount()}]"
    }

    /**
     * 建立快取 Key
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 快取 Key
     */
    private fun buildCacheKey(planCode: String, version: String): String {
        return "$planCode:$version"
    }
}
