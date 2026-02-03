package com.vlife.cv.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * CV 模組快取配置
 *
 * 使用 Caffeine 作為本地快取，用於佣金率等高頻讀取資料。
 *
 * 配置說明：
 * - TTL: 1 小時（佣金率變更頻率低）
 * - 最大項目數: 60,000（CRAT 53,108 筆 + 緩衝）
 * - 記錄統計資訊用於監控
 *
 * 快取名稱：
 * - commissionRateBySerial: 依序號快取單筆佣金率
 * - commissionRateByClassCode: 依類別碼快取佣金率清單
 * - commissionRateEffective: 快取有效佣金率查詢結果
 * - dividendSummary: 紅利摘要快取
 */
@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val CV_CACHE_MANAGER = "cvCacheManager"
        const val CACHE_COMMISSION_RATE_BY_SERIAL = "commissionRateBySerial"
        const val CACHE_COMMISSION_RATE_BY_CLASS_CODE = "commissionRateByClassCode"
        const val CACHE_COMMISSION_RATE_EFFECTIVE = "commissionRateEffective"
        const val CACHE_DIVIDEND_SUMMARY = "dividendSummary"
        const val CACHE_CVDI_BY_PLAN = "cvdiByPlan"
        const val CACHE_CVRF_BY_PLAN = "cvrfByPlan"

        /** 快取 TTL：1 小時（佣金率等資料變更頻率低，每日批次更新） */
        private const val DEFAULT_EXPIRE_HOURS = 1L

        /** 最大快取項目數：60,000（CRAT 53,108 筆 + 其他快取 + 緩衝空間） */
        private const val DEFAULT_MAX_SIZE = 60_000L
    }

    @Bean("cvCacheManager")
    fun cvCacheManager(): CacheManager {
        return CaffeineCacheManager().apply {
            setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS)
                    .maximumSize(DEFAULT_MAX_SIZE)
                    .recordStats()
            )
            setCacheNames(
                listOf(
                    CACHE_COMMISSION_RATE_BY_SERIAL,
                    CACHE_COMMISSION_RATE_BY_CLASS_CODE,
                    CACHE_COMMISSION_RATE_EFFECTIVE,
                    CACHE_DIVIDEND_SUMMARY,
                    CACHE_CVDI_BY_PLAN,
                    CACHE_CVRF_BY_PLAN
                )
            )
        }
    }
}
