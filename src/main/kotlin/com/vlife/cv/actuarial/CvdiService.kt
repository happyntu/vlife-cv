package com.vlife.cv.actuarial

import com.vlife.cv.config.CacheConfig.Companion.CACHE_CVDI_BY_PLAN
import com.vlife.cv.config.CacheConfig.Companion.CV_CACHE_MANAGER
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * 紅利分配水準檔 Service (CV.CVDI)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供紅利分配水準的查詢服務，含快取機制。
 *
 * 業務別名：DividendDistributionLevelService
 */
@Service
class CvdiService(
    private val mapper: CvdiMapper
) {
    /**
     * 依險種代碼和版本查詢所有紅利分配水準
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 紅利分配水準清單
     */
    @Cacheable(value = [CACHE_CVDI_BY_PLAN], key = "#planCode + ':' + #version", cacheManager = CV_CACHE_MANAGER)
    fun findByPlanCode(planCode: String, version: String): List<Cvdi> {
        logger.debug { "Finding CVDI by planCode=$planCode, version=$version" }
        return mapper.findByPlanCode(planCode, version)
    }

    /**
     * 依險種、繳費狀態查詢紅利分配水準
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param paidStatus 繳費狀態
     * @return 紅利分配水準清單
     */
    fun findByPlanCodeAndPaidStatus(
        planCode: String,
        version: String,
        paidStatus: String
    ): List<Cvdi> {
        logger.debug { "Finding CVDI by planCode=$planCode, version=$version, paidStatus=$paidStatus" }
        return mapper.findByPlanCodeAndPaidStatus(planCode, version, paidStatus)
    }

    /**
     * 依條件查詢符合的紅利分配水準
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param paidStatus 繳費狀態
     * @param rateSex 費率性別
     * @param age 年齡
     * @param faceAmt 保額
     * @param modePrem 保費
     * @param policyYear 保單年度
     * @param declareDate 宣告日期
     * @return 符合條件的紅利分配水準
     */
    fun findByCondition(
        planCode: String,
        version: String,
        paidStatus: String,
        rateSex: String,
        age: Int,
        faceAmt: Long,
        modePrem: BigDecimal,
        policyYear: Int,
        declareDate: LocalDate
    ): Cvdi? {
        logger.debug {
            "Finding CVDI by condition: planCode=$planCode, version=$version, " +
                "paidStatus=$paidStatus, rateSex=$rateSex, age=$age, faceAmt=$faceAmt, " +
                "modePrem=$modePrem, policyYear=$policyYear, declareDate=$declareDate"
        }
        return mapper.findByCondition(
            planCode, version, paidStatus, rateSex,
            age, faceAmt, modePrem, policyYear, declareDate
        )
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
     * 依險種查詢所有宣告日期
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 宣告日期清單
     */
    fun findDeclareDates(planCode: String, version: String): List<LocalDate> {
        return mapper.findDeclareDates(planCode, version)
    }

    /**
     * 計算指定險種的紅利分配水準數量
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 資料筆數
     */
    fun countByPlanCode(planCode: String, version: String): Int {
        return mapper.countByPlanCode(planCode, version)
    }

    /**
     * 檢查指定險種是否存在紅利分配水準
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 是否存在
     */
    fun existsByPlanCode(planCode: String, version: String): Boolean {
        return countByPlanCode(planCode, version) > 0
    }
}
