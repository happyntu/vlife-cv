package com.vlife.cv.coverage

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 保單基礎值變化服務
 *
 * 提供承保範圍狀態變化的查詢功能。
 * 資料量適中 (3,800 筆)，未使用快取。
 *
 * 使用範例：
 * ```kotlin
 * val coverages = coverageValueChangeService.findByPolicyNo("P000000001")
 * val activeCoverages = coverages.filter { it.isActive() }
 * ```
 */
@Service
class CoverageValueChangeService(
    private val mapper: CoverageValueChangeMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 依保單號碼查詢所有承保範圍
     *
     * @param policyNo 保單號碼
     * @return 承保範圍清單
     */
    fun findByPolicyNo(policyNo: String): List<CoverageValueChange> {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        log.debug("Finding coverages for policy: {}", policyNo)
        return mapper.findByPolicyNo(policyNo)
    }

    /**
     * 依主鍵查詢單筆承保範圍
     *
     * @param policyNo 保單號碼
     * @param coverageNo 承保範圍編號
     * @return 承保範圍資料，不存在時回傳 null
     */
    fun findById(policyNo: String, coverageNo: Int): CoverageValueChange? {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        require(coverageNo >= 0) { "coverageNo must be non-negative" }
        log.debug("Finding coverage: {}:{}", policyNo, coverageNo)
        return mapper.findById(policyNo, coverageNo)
    }

    /**
     * 依險種代碼查詢承保範圍
     *
     * @param planCode 險種代碼
     * @return 承保範圍清單 (最多 1000 筆)
     */
    fun findByPlanCode(planCode: String): List<CoverageValueChange> {
        require(planCode.isNotBlank() && planCode.length <= 5) {
            "planCode must be non-blank and at most 5 characters"
        }
        log.debug("Finding coverages for plan code: {}", planCode)
        return mapper.findByPlanCode(planCode)
    }

    /**
     * 依承保狀態碼查詢承保範圍
     *
     * @param statusCode 承保狀態碼
     * @return 承保範圍清單 (最多 1000 筆)
     */
    fun findByStatusCode(statusCode: String): List<CoverageValueChange> {
        require(statusCode.isNotBlank() && statusCode.length <= 1) {
            "statusCode must be exactly 1 character"
        }
        log.debug("Finding coverages by status: {}", statusCode)
        return mapper.findByStatusCode(statusCode)
    }

    /**
     * 依承保狀態列舉查詢承保範圍
     *
     * @param status 承保狀態列舉
     * @return 承保範圍清單 (最多 1000 筆)
     */
    fun findByStatus(status: CoverageStatusCode): List<CoverageValueChange> =
        findByStatusCode(status.code)

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    fun findAllPlanCodes(): List<String> = mapper.findAllPlanCodes()

    /**
     * 檢查保單是否存在承保範圍
     *
     * @param policyNo 保單號碼
     * @return true 若存在
     */
    fun existsByPolicyNo(policyNo: String): Boolean {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        return mapper.countByPolicyNo(policyNo) > 0
    }

    /**
     * 計算指定保單的承保範圍數量
     *
     * @param policyNo 保單號碼
     * @return 承保範圍數量
     */
    fun countByPolicyNo(policyNo: String): Int {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        return mapper.countByPolicyNo(policyNo)
    }

    /**
     * 查詢指定保單的有效承保範圍
     *
     * @param policyNo 保單號碼
     * @return 有效承保範圍清單
     */
    fun findActiveCoverages(policyNo: String): List<CoverageValueChange> {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be non-blank and at most 10 characters"
        }
        return findByPolicyNo(policyNo).filter { it.isActive() }
    }
}
