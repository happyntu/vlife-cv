package com.vlife.cv.coverage

import com.github.pagehelper.PageHelper
import com.github.pagehelper.PageInfo
import com.vlife.cv.common.PageRequest
import com.vlife.cv.config.CacheConfig.Companion.CACHE_DIVIDEND_SUMMARY
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * 產品單位 (紅利分配) 服務 (CV.CVPU)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供紅利分配記錄的查詢功能。
 * 資料量小 (148 筆)，部分查詢使用快取。
 *
 * 業務別名：ProductUnitService
 *
 * 使用範例：
 * ```kotlin
 * val dividends = cvpuService.findByPolicyNo("P000000001")
 * val summary = cvpuService.getDividendSummary("P000000001", 1)
 * ```
 *
 * @see CvpuMapper Mapper 層（ADR-017 表格導向命名）
 */
@Service
class CvpuService(
    private val mapper: CvpuMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // region 驗證方法

    private fun validatePolicyNo(policyNo: String) {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
    }

    private fun validateCoverageNo(coverageNo: Int) {
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
    }

    // endregion

    /**
     * 依保單號碼查詢所有紅利分配記錄 (分頁)
     *
     * @param policyNo 保單號碼
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun findByPolicyNo(policyNo: String, pageRequest: PageRequest): PageInfo<Cvpu> {
        validatePolicyNo(policyNo)
        log.debug("Finding product units for policy: {} (page: {})", policyNo, pageRequest.pageNum)
        return PageHelper.startPage<Cvpu>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo { mapper.findByPolicyNo(policyNo) }
    }

    /**
     * 依保單號碼查詢所有紅利分配記錄 (不分頁)
     *
     * @param policyNo 保單號碼
     * @return 紅利分配清單
     */
    fun findByPolicyNo(policyNo: String): List<Cvpu> {
        validatePolicyNo(policyNo)
        log.debug("Finding product units for policy: {}", policyNo)
        return mapper.findByPolicyNo(policyNo)
    }

    /**
     * 依保單號碼和承保範圍編號查詢紅利分配記錄
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 紅利分配清單
     */
    fun findByCoverage(policyNo: String, coverageNo: Int): List<Cvpu> {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        log.debug("Finding product units for coverage: {}:{}", policyNo, coverageNo)
        return mapper.findByCoverage(policyNo, coverageNo)
    }

    /**
     * 依主鍵查詢單筆紅利分配記錄
     *
     * @return 紅利分配資料，不存在時回傳 null
     */
    fun findById(id: CvpuId): Cvpu? {
        log.debug("Finding product unit: {}", id)
        return mapper.findById(
            policyNo = id.policyNo,
            coverageNo = id.coverageNo,
            ps06Type = id.ps06Type,
            cvpuType = id.cvpuType,
            lastAnnivDur = id.lastAnnivDur
        )
    }

    /**
     * 查詢最新週年期間的紅利分配記錄
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 最新紅利分配記錄，無資料時回傳 null
     */
    fun findLatestByCoverage(policyNo: String, coverageNo: Int): Cvpu? {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        log.debug("Finding latest product unit for coverage: {}:{}", policyNo, coverageNo)
        return mapper.findLatestByCoverage(policyNo, coverageNo)
    }

    /**
     * 計算指定承保範圍的宣告紅利總和
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 宣告紅利總和，無資料時回傳 BigDecimal.ZERO
     */
    fun sumDivDeclare(policyNo: String, coverageNo: Int): BigDecimal {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        return mapper.sumDivDeclare(policyNo, coverageNo) ?: BigDecimal.ZERO
    }

    /**
     * 計算指定承保範圍的增值保額紅利總和
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 增值保額紅利總和，無資料時回傳 BigDecimal.ZERO
     */
    fun sumDivPuaAmt(policyNo: String, coverageNo: Int): BigDecimal {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        return mapper.sumDivPuaAmt(policyNo, coverageNo) ?: BigDecimal.ZERO
    }

    /**
     * 取得紅利摘要
     *
     * 結果會被快取，TTL 1 小時。
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 紅利摘要
     */
    @Cacheable(
        value = [CACHE_DIVIDEND_SUMMARY],
        key = "'pu:' + #policyNo + ':' + #coverageNo",
        cacheManager = CV_CACHE_MANAGER
    )
    fun getDividendSummary(policyNo: String, coverageNo: Int): DividendSummary {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        log.debug("Calculating dividend summary for coverage: {}:{}", policyNo, coverageNo)
        return DividendSummary(
            policyNo = policyNo,
            coverageNo = coverageNo,
            totalDivDeclare = sumDivDeclare(policyNo, coverageNo),
            totalDivPuaAmt = sumDivPuaAmt(policyNo, coverageNo),
            recordCount = mapper.countByCoverage(policyNo, coverageNo)
        )
    }

    /**
     * 清除紅利摘要快取
     *
     * **存取控制**：此方法為管理員專用，API 端點需配置存取限制。
     */
    @CacheEvict(value = [CACHE_DIVIDEND_SUMMARY], allEntries = true, cacheManager = CV_CACHE_MANAGER)
    fun evictDividendSummaryCache() {
        log.info("Evicted all dividend summary cache entries")
    }

    /**
     * 計算指定承保範圍的紅利分配記錄數量
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 記錄數量
     */
    fun countByCoverage(policyNo: String, coverageNo: Int): Int {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        return mapper.countByCoverage(policyNo, coverageNo)
    }

    /**
     * 檢查承保範圍是否有紅利分配記錄
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return true 若存在
     */
    fun existsByCoverage(policyNo: String, coverageNo: Int): Boolean {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        return countByCoverage(policyNo, coverageNo) > 0
    }
}
