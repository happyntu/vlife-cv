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
 * 佣金率服務 (CV.CRAT)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供佣金率的查詢功能，對應 V3 的 pk_lib_cratproc 功能。
 * 整合 Caffeine 快取，減少資料庫存取。
 *
 * 業務別名：CommissionRateService
 *
 * 使用範例：
 * ```kotlin
 * // 依類別碼查詢
 * val rates = cratService.findByClassCode("12RA1")
 *
 * // 查詢有效佣金率
 * val effectiveRates = cratService.findEffectiveRates(
 *     commClassCode = "12RA1",
 *     commLineCode = "31",
 *     effectiveDate = LocalDate.now()
 * )
 *
 * // 依條件搜尋
 * val results = cratService.search(
 *     CratQuery(
 *         commLineCode = "31",
 *         effectiveDate = LocalDate.now()
 *     )
 * )
 * ```
 *
 * @see CratMapper Mapper 層（ADR-017 表格導向命名）
 */
@Service
class CratService(
    private val mapper: CratMapper
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
    fun findBySerial(serial: Long): Crat? {
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
    fun findByClassCode(commClassCode: String): List<Crat> {
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
    ): List<Crat> {
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
    ): Crat? {
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
    fun search(query: CratQuery, pageRequest: PageRequest): PageInfo<Crat> {
        log.debug("Searching commission rates with query: {} (page: {})", query, pageRequest.pageNum)
        return PageHelper.startPage<Crat>(pageRequest.pageNum, pageRequest.pageSize)
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
    fun search(query: CratQuery): List<Crat> {
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

    // ==================== CUD 操作 (CV004M) ====================

    /**
     * 新增佣金率
     *
     * 對應 V3 cv004m_insert_crat
     * 新增前會檢查 key 值是否重疊。
     *
     * @param request 新增請求
     * @return 新建立的佣金率
     * @throws CratOverlapException 若 key 值重疊
     */
    @CacheEvict(
        value = [
            CACHE_COMMISSION_RATE_BY_CLASS_CODE,
            CACHE_COMMISSION_RATE_EFFECTIVE
        ],
        allEntries = true,
        cacheManager = CV_CACHE_MANAGER
    )
    fun create(request: CratCreateRequest): Crat {
        log.info("Creating commission rate: classCode={}, lineCode={}", request.commClassCode, request.commLineCode)

        // 檢查 key 值重疊 (對應 cv004m_check_crat_key)
        val overlapCount = mapper.countOverlapping(
            commClassCode = request.commClassCode,
            commLineCode = request.commLineCode,
            cratType = request.cratType,
            projectNo = request.projectNo,
            startDate = request.startDate,
            endDate = request.endDate,
            cratKey1 = request.cratKey1,
            cratKey2 = request.cratKey2
        )
        if (overlapCount > 0) {
            throw CratOverlapException(
                "Commission rate key overlap detected: $overlapCount overlapping records"
            )
        }

        // 取得新序號
        val serial = mapper.nextSerial()

        // 建立 Entity
        val crat = Crat(
            serial = serial,
            commClassCode = request.commClassCode,
            commLineCode = request.commLineCode,
            cratType = request.cratType,
            projectNo = request.projectNo,
            startDate = request.startDate,
            endDate = request.endDate,
            cratKey1 = request.cratKey1,
            cratKey2 = request.cratKey2,
            commStartYear = request.commStartYear,
            commEndYear = request.commEndYear,
            commStartAge = request.commStartAge,
            commEndAge = request.commEndAge,
            commStartModx = request.commStartModx,
            commEndModx = request.commEndModx,
            commRate = request.commRate,
            commRateOrg = request.commRateOrg,
            premLimitStart = request.premLimitStart,
            premLimitEnd = request.premLimitEnd
        )

        // 寫入資料庫
        mapper.insert(crat)
        log.info("Commission rate created: serial={}", serial)

        return crat
    }

    /**
     * 更新佣金率
     *
     * 對應 V3 cv004m_update_crat
     *
     * @param serial 序號 (主鍵)
     * @param request 更新請求
     * @return 更新後的佣金率
     * @throws CratNotFoundException 若佣金率不存在
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
    fun update(serial: Long, request: CratUpdateRequest): Crat {
        log.info("Updating commission rate: serial={}", serial)

        // 檢查是否存在
        val existing = mapper.findBySerial(serial)
            ?: throw CratNotFoundException("Commission rate not found: $serial")

        // 若有更新日期或 key，需要檢查重疊
        if (request.startDate != null || request.endDate != null ||
            request.cratKey1 != null || request.cratKey2 != null) {

            val overlapCount = mapper.countOverlapping(
                commClassCode = existing.commClassCode,
                commLineCode = request.commLineCode ?: existing.commLineCode,
                cratType = request.cratType ?: existing.cratType,
                projectNo = request.projectNo ?: existing.projectNo,
                startDate = request.startDate ?: existing.startDate,
                endDate = request.endDate ?: existing.endDate,
                cratKey1 = request.cratKey1 ?: existing.cratKey1,
                cratKey2 = request.cratKey2 ?: existing.cratKey2,
                excludeSerial = serial
            )
            if (overlapCount > 0) {
                throw CratOverlapException(
                    "Commission rate key overlap detected: $overlapCount overlapping records"
                )
            }
        }

        // 執行更新
        val updated = mapper.update(serial, request)
        if (updated == 0) {
            throw CratNotFoundException("Commission rate not found: $serial")
        }

        log.info("Commission rate updated: serial={}", serial)

        // 回傳更新後的資料
        return mapper.findBySerial(serial)
            ?: throw CratNotFoundException("Commission rate not found after update: $serial")
    }

    /**
     * 刪除佣金率
     *
     * 對應 V3 cv004m_delete_crat
     *
     * @param serial 序號 (主鍵)
     * @throws CratNotFoundException 若佣金率不存在
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
    fun delete(serial: Long) {
        log.info("Deleting commission rate: serial={}", serial)

        val deleted = mapper.deleteBySerial(serial)
        if (deleted == 0) {
            throw CratNotFoundException("Commission rate not found: $serial")
        }

        log.info("Commission rate deleted: serial={}", serial)
    }

    /**
     * 檢查 key 值是否重疊
     *
     * 對應 V3 cv004m_check_crat_key
     *
     * @param request 新增請求
     * @return true 若有重疊
     */
    fun hasOverlap(request: CratCreateRequest): Boolean {
        return mapper.countOverlapping(
            commClassCode = request.commClassCode,
            commLineCode = request.commLineCode,
            cratType = request.cratType,
            projectNo = request.projectNo,
            startDate = request.startDate,
            endDate = request.endDate,
            cratKey1 = request.cratKey1,
            cratKey2 = request.cratKey2
        ) > 0
    }
}

/**
 * 佣金率不存在例外
 */
class CratNotFoundException(message: String) : RuntimeException(message)

/**
 * 佣金率 key 值重疊例外
 */
class CratOverlapException(message: String) : RuntimeException(message)
