package com.vlife.cv.commission

import com.github.pagehelper.PageHelper
import com.github.pagehelper.PageInfo
import com.vlife.cv.common.PageRequest
import com.vlife.cv.config.CacheConfig.Companion.CACHE_COMMISSION_RATE_BY_CLASS_CODE
import com.vlife.cv.config.CacheConfig.Companion.CACHE_COMMISSION_RATE_BY_SERIAL
import com.vlife.cv.config.CacheConfig.Companion.CACHE_COMMISSION_RATE_EFFECTIVE
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 佣金率服務
 *
 * 提供佣金率的查詢功能，對應 V3 的 pk_lib_cratproc 功能。
 * 整合 Caffeine 快取，減少資料庫存取。
 *
 * 使用範例：
 * ```kotlin
 * // 依類別碼查詢
 * val rates = commissionRateService.findByClassCode("12RA1")
 *
 * // 查詢有效佣金率
 * val effectiveRates = commissionRateService.findEffectiveRates(
 *     commClassCode = "12RA1",
 *     commLineCode = "31",
 *     effectiveDate = LocalDate.now()
 * )
 *
 * // 依條件搜尋
 * val results = commissionRateService.search(
 *     CommissionRateQuery(
 *         commLineCode = "31",
 *         effectiveDate = LocalDate.now()
 *     )
 * )
 * ```
 */
@Service
class CommissionRateService(
    private val mapper: CommissionRateMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 依序號查詢佣金率
     *
     * 結果會被快取，TTL 1 小時。
     *
     * @param serial 序號 (主鍵)
     * @return 佣金率資料，不存在時回傳 null
     */
    @Cacheable(value = [CACHE_COMMISSION_RATE_BY_SERIAL], key = "#serial", cacheManager = CV_CACHE_MANAGER)
    fun findBySerial(serial: Long): CommissionRate? {
        log.debug("Loading commission rate for serial: {}", serial)
        return mapper.findBySerial(serial)
    }

    /**
     * 依佣金類別碼查詢佣金率清單
     *
     * 結果會被快取，TTL 1 小時。
     *
     * @param commClassCode 佣金率類別碼
     * @return 佣金率清單
     */
    @Cacheable(value = [CACHE_COMMISSION_RATE_BY_CLASS_CODE], key = "#commClassCode", cacheManager = CV_CACHE_MANAGER)
    fun findByClassCode(commClassCode: String): List<CommissionRate> {
        log.debug("Loading commission rates for class code: {}", commClassCode)
        return mapper.findByClassCode(commClassCode)
    }

    /**
     * 查詢指定日期有效的佣金率
     *
     * 結果會被快取，TTL 1 小時。
     *
     * @param commClassCode 佣金率類別碼
     * @param commLineCode 業務線代號
     * @param effectiveDate 生效日期
     * @return 有效的佣金率清單
     */
    @Cacheable(
        value = [CACHE_COMMISSION_RATE_EFFECTIVE],
        key = "'cc:' + #commClassCode + ':lc:' + #commLineCode + ':dt:' + #effectiveDate.toString()",
        cacheManager = CV_CACHE_MANAGER
    )
    fun findEffectiveRates(
        commClassCode: String,
        commLineCode: String,
        effectiveDate: LocalDate
    ): List<CommissionRate> {
        log.debug(
            "Loading effective commission rates: classCode={}, lineCode={}, date={}",
            commClassCode, commLineCode, effectiveDate
        )
        return mapper.findEffectiveRates(commClassCode, commLineCode, effectiveDate)
    }

    /**
     * 查詢指定日期、年齡有效的佣金率
     *
     * @param commClassCode 佣金率類別碼
     * @param commLineCode 業務線代號
     * @param effectiveDate 生效日期
     * @param age 年齡
     * @return 符合條件的佣金率，可能為 null
     */
    fun findEffectiveRateForAge(
        commClassCode: String,
        commLineCode: String,
        effectiveDate: LocalDate,
        age: Int
    ): CommissionRate? {
        return findEffectiveRates(commClassCode, commLineCode, effectiveDate)
            .filter { it.isAgeInRange(age) }
            .maxByOrNull { it.commRate ?: java.math.BigDecimal.ZERO }
    }

    /**
     * 依業務線代號查詢所有佣金率類別碼
     *
     * @param commLineCode 業務線代號
     * @return 佣金率類別碼清單
     */
    fun findClassCodesByLineCode(commLineCode: String): List<String> {
        return mapper.findClassCodesByLineCode(commLineCode)
    }

    /**
     * 查詢所有不重複的業務線代號
     *
     * @return 業務線代號清單
     */
    fun findAllLineCodes(): List<String> = mapper.findAllLineCodes()

    /**
     * 查詢所有不重複的佣金率型態
     *
     * @return 佣金率型態清單
     */
    fun findAllCratTypes(): List<String> = mapper.findAllCratTypes()

    /**
     * 依多條件搜尋佣金率 (分頁)
     *
     * 注意：此方法不使用快取，適用於管理介面的動態查詢。
     *
     * @param query 查詢條件
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun search(query: CommissionRateQuery, pageRequest: PageRequest): PageInfo<CommissionRate> {
        log.debug("Searching commission rates with query: {} (page: {})", query, pageRequest.pageNum)
        return PageHelper.startPage<CommissionRate>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo {
                mapper.search(
                    commClassCode = query.commClassCode,
                    commLineCode = query.commLineCode,
                    cratType = query.cratType,
                    effectiveDate = query.effectiveDate
                )
            }
    }

    /**
     * 依多條件搜尋佣金率 (不分頁)
     *
     * 注意：此方法不使用快取，適用於管理介面的動態查詢。
     *
     * @param query 查詢條件
     * @return 符合條件的佣金率清單
     */
    fun search(query: CommissionRateQuery): List<CommissionRate> {
        log.debug("Searching commission rates with query: {}", query)
        return mapper.search(
            commClassCode = query.commClassCode,
            commLineCode = query.commLineCode,
            cratType = query.cratType,
            effectiveDate = query.effectiveDate
        )
    }

    /**
     * 檢查佣金類別碼是否存在
     *
     * @param commClassCode 佣金率類別碼
     * @return true 若存在
     */
    fun existsByClassCode(commClassCode: String): Boolean =
        mapper.countByClassCode(commClassCode) > 0

    /**
     * 計算指定佣金類別碼的資料筆數
     *
     * @param commClassCode 佣金率類別碼
     * @return 資料筆數
     */
    fun countByClassCode(commClassCode: String): Int =
        mapper.countByClassCode(commClassCode)

    /**
     * 清除所有快取
     *
     * 用於管理介面強制刷新資料。
     */
    @CacheEvict(
        value = [
            CACHE_COMMISSION_RATE_BY_SERIAL,
            CACHE_COMMISSION_RATE_BY_CLASS_CODE,
            CACHE_COMMISSION_RATE_EFFECTIVE
        ],
        allEntries = true,
        cacheManager = CV_CACHE_MANAGER
    )
    fun refreshCache() {
        log.info("Commission rate cache cleared")
    }
}
