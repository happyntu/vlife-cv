package com.vlife.cv.coverage

import com.github.pagehelper.PageHelper
import com.github.pagehelper.PageInfo
import com.vlife.cv.common.PageRequest
import com.vlife.cv.config.CacheConfig.Companion.CACHE_DIVIDEND_SUMMARY
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * 產品單位 (紅利分配) 服務
 *
 * 提供紅利分配記錄的查詢功能。
 * 資料量小 (148 筆)，部分查詢使用快取。
 *
 * 使用範例：
 * ```kotlin
 * val dividends = productUnitService.findByPolicyNo("P000000001")
 * val summary = productUnitService.getDividendSummary("P000000001", 1)
 * ```
 */
@Service
class ProductUnitService(
    private val mapper: ProductUnitMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 依保單號碼查詢所有紅利分配記錄 (分頁)
     *
     * @param policyNo 保單號碼
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun findByPolicyNo(policyNo: String, pageRequest: PageRequest): PageInfo<ProductUnit> {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        log.debug("Finding product units for policy: {} (page: {})", policyNo, pageRequest.pageNum)
        return PageHelper.startPage<ProductUnit>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo { mapper.findByPolicyNo(policyNo) }
    }

    /**
     * 依保單號碼查詢所有紅利分配記錄 (不分頁)
     *
     * @param policyNo 保單號碼
     * @return 紅利分配清單
     */
    fun findByPolicyNo(policyNo: String): List<ProductUnit> {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
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
    fun findByCoverage(policyNo: String, coverageNo: Int): List<ProductUnit> {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
        log.debug("Finding product units for coverage: {}:{}", policyNo, coverageNo)
        return mapper.findByCoverage(policyNo, coverageNo)
    }

    /**
     * 依主鍵查詢單筆紅利分配記錄
     *
     * @return 紅利分配資料，不存在時回傳 null
     */
    fun findById(id: ProductUnitId): ProductUnit? {
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
    fun findLatestByCoverage(policyNo: String, coverageNo: Int): ProductUnit? {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
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
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
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
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
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
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
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
     * 計算指定承保範圍的紅利分配記錄數量
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 記錄數量
     */
    fun countByCoverage(policyNo: String, coverageNo: Int): Int {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
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
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
        return countByCoverage(policyNo, coverageNo) > 0
    }
}
