package com.vlife.cv.actuarial

import com.vlife.cv.config.CacheConfig.Companion.CACHE_CVRF_BY_PLAN
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 準備金因子檔 Service (CV.CVRF)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供準備金因子的查詢服務，含快取機制。
 *
 * 業務別名：ReserveFactorService
 *
 * ⚠️ 跨模組使用注意：
 * CVRF 被 BL、CL、CV 三個模組使用，其他模組應透過 API 存取。
 */
@Service
class CvrfService(
    private val mapper: CvrfMapper
) {
    /**
     * 依險種代碼和版本查詢所有準備金因子
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 準備金因子清單
     */
    @Cacheable(value = [CACHE_CVRF_BY_PLAN], key = "#planCode + ':' + #version", cacheManager = CV_CACHE_MANAGER)
    fun findByPlanCode(planCode: String, version: String): List<Cvrf> {
        logger.debug { "Finding CVRF by planCode=$planCode, version=$version" }
        return mapper.findByPlanCode(planCode, version)
    }

    /**
     * 依主鍵查詢單筆準備金因子
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param durationType 期間類型
     * @return 準備金因子
     */
    fun findById(planCode: String, version: String, durationType: Int): Cvrf? {
        logger.debug { "Finding CVRF by planCode=$planCode, version=$version, durationType=$durationType" }
        return mapper.findById(planCode, version, durationType)
    }

    /**
     * 依期間類型查詢準備金因子
     *
     * @param durationType 期間類型
     * @return 準備金因子清單
     */
    fun findByDurationType(durationType: Int): List<Cvrf> {
        logger.debug { "Finding CVRF by durationType=$durationType" }
        return mapper.findByDurationType(durationType)
    }

    /**
     * 查詢終身險的準備金因子
     *
     * @return 終身險準備金因子清單
     */
    fun findWholeLifeFactors(): List<Cvrf> {
        return findByDurationType(DurationType.WHOLE_LIFE.code)
    }

    /**
     * 查詢定期險的準備金因子
     *
     * @return 定期險準備金因子清單
     */
    fun findTermFactors(): List<Cvrf> {
        return findByDurationType(DurationType.TERM.code)
    }

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    fun findAllPlanCodes(): List<String> {
        return mapper.findAllPlanCodes()
    }

    /**
     * 計算指定險種的準備金因子數量
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 資料筆數
     */
    fun countByPlanCode(planCode: String, version: String): Int {
        return mapper.countByPlanCode(planCode, version)
    }

    /**
     * 檢查指定險種是否存在準備金因子
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 是否存在
     */
    fun existsByPlanCode(planCode: String, version: String): Boolean {
        return countByPlanCode(planCode, version) > 0
    }
}
