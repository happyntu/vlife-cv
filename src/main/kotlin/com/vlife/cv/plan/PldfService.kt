package com.vlife.cv.plan

import com.github.pagehelper.PageHelper
import com.github.pagehelper.PageInfo
import com.vlife.cv.common.PageRequest
import com.vlife.cv.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 險種描述服務 (CV.PLDF)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 遵循 ADR-022 規範，PLDF 歸屬於 CV 模組。
 *
 * 提供險種描述的 CRUD 功能，對應 V3 的 PK_CV_CV001M 和 PK_LIB_PLANPROC。
 * 整合 Caffeine 快取，減少資料庫存取。
 *
 * V3 PL/SQL 對應：
 * - cv001m_read_pldf → findByPlanCodeAndVersion, search
 * - cv001m_delete_pldf → delete
 * - cv001m_check_cv001m1a → create 時的驗證
 * - cv001m_default_values_pldf → 預設值處理
 * - cv001m_validate_pldf → 驗證邏輯
 *
 * 業務別名：PlanDefinitionService
 *
 * @see PldfMapper Mapper 層
 */
@Service
class PldfService(
    private val mapper: PldfMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ==================== 查詢方法 ====================

    /**
     * 依主鍵查詢險種描述
     *
     * 結果會被快取，TTL 1 小時。
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @return 險種描述，不存在時回傳 null
     */
    @Cacheable(
        value = [CacheConfig.CACHE_PLDF_BY_PK],
        key = "'pc:' + #planCode + ':v:' + #version",
        cacheManager = CacheConfig.CV_CACHE_MANAGER
    )
    fun findByPlanCodeAndVersion(planCode: String, version: String): Pldf? {
        log.debug("Loading plan definition: planCode={}, version={}", planCode, version)
        return mapper.findByPlanCodeAndVersion(planCode, version)
    }

    /**
     * 依險種代碼查詢所有版本
     *
     * 結果會被快取，TTL 1 小時。
     *
     * @param planCode 險種代碼
     * @return 該險種所有版本清單
     */
    @Cacheable(
        value = [CacheConfig.CACHE_PLDF_BY_PLAN_CODE],
        key = "#planCode",
        cacheManager = CacheConfig.CV_CACHE_MANAGER
    )
    fun findByPlanCode(planCode: String): List<Pldf> {
        log.debug("Loading plan definitions for planCode: {}", planCode)
        return mapper.findByPlanCode(planCode)
    }

    /**
     * 查詢指定日期有效（上市中）的險種
     *
     * @param effectiveDate 生效日期
     * @return 有效險種清單
     */
    @Cacheable(
        value = [CacheConfig.CACHE_PLDF_EFFECTIVE],
        key = "#effectiveDate.toString()",
        cacheManager = CacheConfig.CV_CACHE_MANAGER
    )
    fun findEffective(effectiveDate: LocalDate): List<Pldf> {
        log.debug("Loading effective plan definitions for date: {}", effectiveDate)
        return mapper.findEffective(effectiveDate)
    }

    /**
     * 查詢指定日期有效的險種 (摘要)
     *
     * @param effectiveDate 生效日期
     * @return 有效險種摘要清單
     */
    fun findEffectiveSummary(effectiveDate: LocalDate): List<PldfSummary> {
        log.debug("Loading effective plan definition summaries for date: {}", effectiveDate)
        return mapper.findEffectiveSummary(effectiveDate)
    }

    /**
     * 依條件搜尋險種 (分頁)
     *
     * 注意：此方法不使用快取，適用於管理介面的動態查詢。
     *
     * @param query 查詢條件
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun search(query: PldfQuery, pageRequest: PageRequest): PageInfo<Pldf> {
        log.debug("Searching plan definitions with query: {} (page: {})", query, pageRequest.pageNum)
        return PageHelper.startPage<Pldf>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo {
                mapper.search(
                    planCode = query.planCode,
                    version = query.version,
                    primaryRiderInd = query.primaryRiderInd,
                    insuranceType3 = query.insuranceType3,
                    planType = query.planType,
                    effectiveDate = query.effectiveDate,
                    currency = query.currency,
                    loanAvalInd = query.loanAvalInd,
                    divType = query.divType
                )
            }
    }

    /**
     * 依條件搜尋險種 (不分頁)
     *
     * @param query 查詢條件
     * @return 符合條件的險種清單
     */
    fun search(query: PldfQuery): List<Pldf> {
        log.debug("Searching plan definitions with query: {}", query)
        return mapper.search(
            planCode = query.planCode,
            version = query.version,
            primaryRiderInd = query.primaryRiderInd,
            insuranceType3 = query.insuranceType3,
            planType = query.planType,
            effectiveDate = query.effectiveDate,
            currency = query.currency,
            loanAvalInd = query.loanAvalInd,
            divType = query.divType
        )
    }

    /**
     * 依條件搜尋險種摘要 (分頁)
     *
     * @param query 查詢條件
     * @param pageRequest 分頁參數
     * @return 分頁結果
     */
    fun searchSummary(query: PldfQuery, pageRequest: PageRequest): PageInfo<PldfSummary> {
        log.debug("Searching plan definition summaries with query: {} (page: {})", query, pageRequest.pageNum)
        return PageHelper.startPage<PldfSummary>(pageRequest.pageNum, pageRequest.pageSize)
            .doSelectPageInfo {
                mapper.searchSummary(
                    planCode = query.planCode,
                    version = query.version,
                    primaryRiderInd = query.primaryRiderInd,
                    insuranceType3 = query.insuranceType3,
                    planType = query.planType,
                    effectiveDate = query.effectiveDate
                )
            }
    }

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    fun findAllPlanCodes(): List<String> = mapper.findAllPlanCodes()

    /**
     * 查詢所有不重複的保險型態3
     *
     * @return 保險型態3清單
     */
    fun findAllInsuranceType3(): List<String> = mapper.findAllInsuranceType3()

    /**
     * 計算指定險種代碼的版本數量
     *
     * @param planCode 險種代碼
     * @return 版本數量
     */
    fun countByPlanCode(planCode: String): Int = mapper.countByPlanCode(planCode)

    /**
     * 檢查險種是否存在
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @return true 若存在
     */
    fun exists(planCode: String, version: String): Boolean =
        mapper.existsByPlanCodeAndVersion(planCode, version) > 0

    // ==================== CUD 方法 (CV001M) ====================

    /**
     * 新增險種描述
     *
     * 對應 V3 cv001m_insert (透過 PK_LIB_PLANPROC.F99_INSERT_PLAN)
     *
     * @param request 新增請求
     * @return 新建立的險種描述
     * @throws PldfAlreadyExistsException 若險種已存在
     */
    @CacheEvict(
        value = [CacheConfig.CACHE_PLDF_BY_PK, CacheConfig.CACHE_PLDF_BY_PLAN_CODE, CacheConfig.CACHE_PLDF_EFFECTIVE],
        allEntries = true,
        cacheManager = CacheConfig.CV_CACHE_MANAGER
    )
    fun create(request: PldfCreateRequest): Pldf {
        log.info("Creating plan definition: planCode={}, version={}", request.planCode, request.version)

        // 檢查主鍵是否已存在 (對應 cv001m_check_cv001m1a)
        if (exists(request.planCode, request.version)) {
            throw PldfAlreadyExistsException(
                "Plan definition already exists: planCode=${request.planCode}, version=${request.version}"
            )
        }

        // 建立 Entity (設定必填欄位的預設值)
        val pldf = Pldf(
            planCode = request.planCode,
            version = request.version,
            planTitle = request.planTitle,
            planName = request.planName,
            contractedName = request.contractedName,
            lowAge = request.lowAge,
            highAge = request.highAge,
            collectYearInd = request.collectYearInd,
            collectYear = request.collectYear,
            expYearInd = request.expYearInd,
            expYear = request.expYear,
            planStartDate = request.planStartDate,
            planEndDate = request.planEndDate,
            primaryRiderInd = request.primaryRiderInd,
            insuranceType = request.insuranceType,
            insuranceType3 = request.insuranceType3,
            planType = request.planType,
            currency1 = request.currency1,
            planAccountInd = "N",  // 預設值
            divType = request.divType,
            divSwM = "N",  // 預設值
            csvCalcType = request.csvCalcType,
            puaCalcType = request.puaCalcType,
            eteCalcType = request.eteCalcType,
            loanAvalInd = request.loanAvalInd,
            commClassCode = request.commClassCode,
            commClassCodeI = request.commClassCodeI,
            uwPlanCode = request.uwPlanCode,
            uwVersion = request.uwVersion,
            pcPlanCode = request.pcPlanCode,
            pcVersion = request.pcVersion,
            annySw = "N",  // 預設值
            premLackInd = "N",  // 預設值
            persistRewardInd = "N",  // 預設值
            persistPremVal = 0  // 預設值
        )

        // 寫入資料庫
        mapper.insert(pldf)
        log.info("Plan definition created: planCode={}, version={}", request.planCode, request.version)

        return pldf
    }

    /**
     * 更新險種描述
     *
     * 對應 V3 cv001m_update (透過 PK_LIB_PLANPROC.F99_UPDATE_PLAN)
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @param request 更新請求
     * @return 更新後的險種描述
     * @throws PldfNotFoundException 若險種不存在
     */
    @CacheEvict(
        value = [CacheConfig.CACHE_PLDF_BY_PK, CacheConfig.CACHE_PLDF_BY_PLAN_CODE, CacheConfig.CACHE_PLDF_EFFECTIVE],
        allEntries = true,
        cacheManager = CacheConfig.CV_CACHE_MANAGER
    )
    fun update(planCode: String, version: String, request: PldfUpdateRequest): Pldf {
        log.info("Updating plan definition: planCode={}, version={}", planCode, version)

        // 檢查是否存在
        if (!exists(planCode, version)) {
            throw PldfNotFoundException("Plan definition not found: planCode=$planCode, version=$version")
        }

        // 執行更新
        val updated = mapper.update(planCode, version, request)
        if (updated == 0) {
            throw PldfNotFoundException("Plan definition not found: planCode=$planCode, version=$version")
        }

        log.info("Plan definition updated: planCode={}, version={}", planCode, version)

        // 回傳更新後的資料
        return mapper.findByPlanCodeAndVersion(planCode, version)
            ?: throw PldfNotFoundException("Plan definition not found after update: planCode=$planCode, version=$version")
    }

    /**
     * 刪除險種描述
     *
     * 對應 V3 cv001m_delete_pldf
     *
     * @param planCode 險種代碼
     * @param version 版數
     * @throws PldfNotFoundException 若險種不存在
     */
    @CacheEvict(
        value = [CacheConfig.CACHE_PLDF_BY_PK, CacheConfig.CACHE_PLDF_BY_PLAN_CODE, CacheConfig.CACHE_PLDF_EFFECTIVE],
        allEntries = true,
        cacheManager = CacheConfig.CV_CACHE_MANAGER
    )
    fun delete(planCode: String, version: String) {
        log.info("Deleting plan definition: planCode={}, version={}", planCode, version)

        val deleted = mapper.deleteByPlanCodeAndVersion(planCode, version)
        if (deleted == 0) {
            throw PldfNotFoundException("Plan definition not found: planCode=$planCode, version=$version")
        }

        log.info("Plan definition deleted: planCode={}, version={}", planCode, version)
    }

    /**
     * 清除所有快取
     *
     * 用於管理介面強制刷新資料。
     */
    @CacheEvict(
        value = [CacheConfig.CACHE_PLDF_BY_PK, CacheConfig.CACHE_PLDF_BY_PLAN_CODE, CacheConfig.CACHE_PLDF_EFFECTIVE],
        allEntries = true,
        cacheManager = CacheConfig.CV_CACHE_MANAGER
    )
    fun refreshCache() {
        log.info("Plan definition cache cleared")
    }
}

/**
 * 險種描述不存在例外
 */
class PldfNotFoundException(message: String) : RuntimeException(message)

/**
 * 險種描述已存在例外
 */
class PldfAlreadyExistsException(message: String) : RuntimeException(message)
