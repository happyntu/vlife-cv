package com.vlife.cv.cvet

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * 展期定期保單服務 (CV.CVET)
 *
 * 對應 V3 PK_LIB_CVETPROC 的業務邏輯層。
 * 不使用快取（展期定期為事務性資料，契變後必須立即反映）。
 */
@Service
class CvetService(
    private val cvetMapper: CvetMapper
) {

    // === 查詢方法 ===

    /**
     * 查詢展期定期資料
     * 對應 V3: f99_get_cvet
     */
    fun findByPolicyAndCoverage(policyNo: String, coverageNo: Int): ExtendedTermPolicy? {
        logger.debug { "Finding CVET by policyNo=$policyNo, coverageNo=$coverageNo" }
        return cvetMapper.findByPolicyAndCoverage(policyNo, coverageNo)
    }

    /**
     * 判斷險種是否為展期定期
     * 等價於 V3 的 `IF f99_get_cvet(...) = 1 THEN ...`
     */
    fun isExtendedTerm(policyNo: String, coverageNo: Int): Boolean {
        return cvetMapper.findByPolicyAndCoverage(policyNo, coverageNo) != null
    }

    /**
     * 計算保單的展期定期險種數量
     * 對應 V3: f99_get_cvet_cnt
     */
    fun countByPolicyNo(policyNo: String): Int {
        logger.debug { "Counting CVET by policyNo=$policyNo" }
        return cvetMapper.countByPolicyNo(policyNo)
    }

    // === 異動方法 ===

    /**
     * 新增展期定期記錄
     * 對應 V3: f99_insert_cvet
     */
    @Transactional
    fun insert(cvet: ExtendedTermPolicy) {
        logger.info { "Inserting CVET: policyNo=${cvet.policyNo}, coverageNo=${cvet.coverageNo}" }
        val result = cvetMapper.insert(cvet)
        if (result != 1) {
            throw IllegalStateException(
                "新增展期定期記錄失敗: policyNo=${cvet.policyNo}, coverageNo=${cvet.coverageNo}"
            )
        }
    }

    /**
     * 刪除展期定期記錄
     * 對應 V3: f99_delete_cvet
     */
    @Transactional
    fun deleteByPolicyAndCoverage(policyNo: String, coverageNo: Int) {
        logger.info { "Deleting CVET: policyNo=$policyNo, coverageNo=$coverageNo" }
        val result = cvetMapper.deleteByPolicyAndCoverage(policyNo, coverageNo)
        if (result != 1) {
            throw IllegalStateException(
                "刪除展期定期記錄失敗: policyNo=$policyNo, coverageNo=$coverageNo"
            )
        }
    }
}
