package com.vlife.cv.psbt

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * PSBT 險種給付參數子表 Service
 *
 * 提供險種給付參數查詢功能，並實作 Caffeine Cache（遵循 ADR-014）。
 *
 * V3 equivalent: PK_LIB_PSBTPROC
 *
 * 快取策略：
 * - TTL: 24 小時（精算參數變動頻率低）
 * - 最大容量: 10,000 筆
 * - Key: "planCode|version|rateSex|psbtType|psbtCode"
 */
@Service
class PsbtService(
    private val psbtMapper: PsbtMapper
) {

    private val logger = LoggerFactory.getLogger(PsbtService::class.java)

    /**
     * Caffeine Cache 設定（供 findAllByKeys 使用）
     * - maximumSize: 10,000 筆
     * - expireAfterWrite: 24 小時
     */
    private val cache: Cache<String, List<Psbt>> = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .recordStats()
        .build()

    /**
     * 依險種+版本+性別+類型+代碼 + KEY 範圍匹配查詢
     *
     * V3: PK_LIB_PSBTPROC.f99_get_psbt
     *
     * 此為直接資料庫查詢（無快取），適用低頻或首次查詢場景。
     * 高頻計算場景建議使用 findAllByKeys + 應用層過濾。
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param rateSex 性別費率
     * @param psbtType 參數類型
     * @param psbtCode 參數代碼
     * @param key1 範圍鍵1
     * @param key2 範圍鍵2
     * @return PSBT 記錄，若不存在則返回 null
     */
    fun findByKeysAndRange(
        planCode: String,
        version: String,
        rateSex: String,
        psbtType: String,
        psbtCode: String,
        key1: Long,
        key2: Long
    ): Psbt? {
        logger.debug(
            "Querying PSBT with planCode={}, version={}, rateSex={}, psbtType={}, psbtCode={}, key1={}, key2={}",
            planCode, version, rateSex, psbtType, psbtCode, key1, key2
        )
        return psbtMapper.findByKeysAndRange(planCode, version, rateSex, psbtType, psbtCode, key1, key2)
    }

    /**
     * 查詢指定組合的所有記錄（含快取）
     *
     * 用於批次查詢或快取預載，呼叫端可在應用層進行範圍匹配。
     * 適用高頻精算計算場景。
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param rateSex 性別費率
     * @param psbtType 參數類型
     * @param psbtCode 參數代碼
     * @return PSBT 記錄列表
     */
    fun findAllByKeys(
        planCode: String,
        version: String,
        rateSex: String,
        psbtType: String,
        psbtCode: String
    ): List<Psbt> {
        val cacheKey = buildCacheKey(planCode, version, rateSex, psbtType, psbtCode)

        // 使用原子載入避免併發 cache miss 重複查詢 DB
        return cache.get(cacheKey) {
            logger.debug("Cache miss for key: {}, querying from database", cacheKey)
            psbtMapper.findAllByKeys(planCode, version, rateSex, psbtType, psbtCode)
        } ?: emptyList()
    }

    /**
     * 從快取載入的列表中進行範圍匹配
     *
     * 此為應用層範圍匹配實作，適用高頻計算場景。
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param rateSex 性別費率
     * @param psbtType 參數類型
     * @param psbtCode 參數代碼
     * @param key1 範圍鍵1
     * @param key2 範圍鍵2
     * @return 匹配的 PSBT 記錄，若不存在則返回 null
     */
    fun findFromCacheByRange(
        planCode: String,
        version: String,
        rateSex: String,
        psbtType: String,
        psbtCode: String,
        key1: Long,
        key2: Long
    ): Psbt? {
        val allRecords = findAllByKeys(planCode, version, rateSex, psbtType, psbtCode)

        return allRecords.find { record ->
            record.psbtKey1 <= key1 && record.psbtKey2 >= key1 &&
            record.psbtKey3 <= key2 && record.psbtKey4 >= key2
        }
    }

    /**
     * 清除特定組合的快取
     */
    fun evictCache(planCode: String, version: String, rateSex: String, psbtType: String, psbtCode: String) {
        val cacheKey = buildCacheKey(planCode, version, rateSex, psbtType, psbtCode)
        cache.invalidate(cacheKey)
        logger.info("Cache evicted for key: {}", cacheKey)
    }

    /**
     * 清除所有快取
     */
    fun evictAllCache() {
        cache.invalidateAll()
        logger.info("All PSBT cache invalidated")
    }

    /**
     * 取得快取統計資訊
     */
    fun getCacheStats(): String {
        val stats = cache.stats()
        return "PsbtCacheStats[hitRate=${stats.hitRate()}, " +
                "hitCount=${stats.hitCount()}, " +
                "missCount=${stats.missCount()}, " +
                "evictionCount=${stats.evictionCount()}]"
    }

    /**
     * 建立快取 Key
     */
    private fun buildCacheKey(
        planCode: String,
        version: String,
        rateSex: String,
        psbtType: String,
        psbtCode: String
    ): String {
        return "$planCode|$version|$rateSex|$psbtType|$psbtCode"
    }
}
