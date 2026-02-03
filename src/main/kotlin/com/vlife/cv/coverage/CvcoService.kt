package com.vlife.cv.coverage

import com.github.pagehelper.PageHelper
import com.github.pagehelper.PageInfo
import com.vlife.cv.common.PageRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 保單基礎值變化服務 (CV.CVCO)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供承保範圍狀態變化的查詢功能。
 * 資料量適中 (3,800 筆)，未使用快取。
 *
 * 業務別名：CoverageValueChangeService
 *
 * 使用範例：
 * ```kotlin
 * val coverages = cvcoService.findByPolicyNo("P000000001")
 * val activeCoverages = coverages.filter { it.isActive() }
 * ```
 *
 * @see CvcoMapper Mapper 層（ADR-017 表格導向命名）
 */
@Service
class CvcoService(
    private val mapper: CvcoMapper
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

    private fun validatePlanCode(planCode: String) {
        require(planCode.isNotBlank() && planCode.length <= 5) {
            "planCode must be non-blank and at most 5 characters"
        }
    }

    private fun validateStatusCode(statusCode: String) {
        require(statusCode.isNotBlank() && statusCode.length <= 1) {
            "statusCode must be exactly 1 character"
        }
    }

    // endregion

    /**
     * 依保單號碼查詢所有承保範圍 (分頁)
     *
     * @param policyNo 保單號碼
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun findByPolicyNo(policyNo: String, pageRequest: PageRequest): PageInfo<Cvco> {
        validatePolicyNo(policyNo)
        log.debug("Finding coverages for policy: {} (page: {})", policyNo, pageRequest.pageNum)
        return PageHelper.startPage<Cvco>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo { mapper.findByPolicyNo(policyNo) }
    }

    /**
     * 依保單號碼查詢所有承保範圍 (不分頁)
     *
     * @param policyNo 保單號碼
     * @return 承保範圍清單
     */
    fun findByPolicyNo(policyNo: String): List<Cvco> {
        validatePolicyNo(policyNo)
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
    fun findById(policyNo: String, coverageNo: Int): Cvco? {
        validatePolicyNo(policyNo)
        validateCoverageNo(coverageNo)
        log.debug("Finding coverage: {}:{}", policyNo, coverageNo)
        return mapper.findById(policyNo, coverageNo)
    }

    /**
     * 依險種代碼查詢承保範圍 (分頁)
     *
     * @param planCode 險種代碼
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun findByPlanCode(planCode: String, pageRequest: PageRequest): PageInfo<Cvco> {
        validatePlanCode(planCode)
        log.debug("Finding coverages for plan code: {} (page: {})", planCode, pageRequest.pageNum)
        return PageHelper.startPage<Cvco>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo { mapper.findByPlanCode(planCode) }
    }

    /**
     * 依險種代碼查詢承保範圍 (不分頁)
     *
     * @param planCode 險種代碼
     * @return 承保範圍清單
     */
    fun findByPlanCode(planCode: String): List<Cvco> {
        validatePlanCode(planCode)
        log.debug("Finding coverages for plan code: {}", planCode)
        return mapper.findByPlanCode(planCode)
    }

    /**
     * 依承保狀態碼查詢承保範圍 (分頁)
     *
     * @param statusCode 承保狀態碼
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun findByStatusCode(statusCode: String, pageRequest: PageRequest): PageInfo<Cvco> {
        validateStatusCode(statusCode)
        log.debug("Finding coverages by status: {} (page: {})", statusCode, pageRequest.pageNum)
        return PageHelper.startPage<Cvco>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo { mapper.findByStatusCode(statusCode) }
    }

    /**
     * 依承保狀態碼查詢承保範圍 (不分頁)
     *
     * @param statusCode 承保狀態碼
     * @return 承保範圍清單
     */
    fun findByStatusCode(statusCode: String): List<Cvco> {
        validateStatusCode(statusCode)
        log.debug("Finding coverages by status: {}", statusCode)
        return mapper.findByStatusCode(statusCode)
    }

    /**
     * 依承保狀態列舉查詢承保範圍 (分頁)
     *
     * @param status 承保狀態列舉
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun findByStatus(status: CoverageStatusCode, pageRequest: PageRequest): PageInfo<Cvco> =
        findByStatusCode(status.code, pageRequest)

    /**
     * 依承保狀態列舉查詢承保範圍 (不分頁)
     *
     * @param status 承保狀態列舉
     * @return 承保範圍清單
     */
    fun findByStatus(status: CoverageStatusCode): List<Cvco> =
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
        validatePolicyNo(policyNo)
        return mapper.countByPolicyNo(policyNo) > 0
    }

    /**
     * 計算指定保單的承保範圍數量
     *
     * @param policyNo 保單號碼
     * @return 承保範圍數量
     */
    fun countByPolicyNo(policyNo: String): Int {
        validatePolicyNo(policyNo)
        return mapper.countByPolicyNo(policyNo)
    }

    /**
     * 查詢指定保單的有效承保範圍
     *
     * @param policyNo 保單號碼
     * @return 有效承保範圍清單
     */
    fun findActiveCoverages(policyNo: String): List<Cvco> {
        validatePolicyNo(policyNo)
        return findByPolicyNo(policyNo).filter { it.isActive() }
    }
}
