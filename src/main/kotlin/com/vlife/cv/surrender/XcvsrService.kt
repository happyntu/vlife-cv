package com.vlife.cv.surrender

import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * XCVSR Service - 跨險種解約金評分服務
 *
 * V3 對應：PK_LIB_XCVSRPROC
 * - 部署於 LIB + CV 雙模組（程式碼相同）
 * - 僅 1 個查詢 routine：f99_get_xcvsr
 *
 * V4 改進：
 * - 使用 Caffeine Cache（TTL 24hr, max 10,000）
 * - 預留完整 CRUD 操作（V3 僅查詢）
 * - 移除 Object View 機制，改用 Kotlin data class
 *
 * 快取策略：
 * - 查詢高頻但資料異動少，適合快取
 * - TTL 24 小時（解約金評分參數不常變更）
 * - 最大容量 10,000 筆
 */
@Service
class XcvsrService(
    private val mapper: XcvsrMapper
) {
    private val logger = LoggerFactory.getLogger(XcvsrService::class.java)

    // === 查詢操作 ===

    /**
     * 查詢保單特定險種的評分單位
     * 對應 V3 的 f99_get_xcvsr
     *
     * @param policyNo 保單號碼
     * @param coverageNo 險種序號
     * @return 查詢結果，查無資料回傳 null
     */
    @Cacheable(value = ["xcvsr"], key = "#policyNo + '_' + #coverageNo")
    fun getByPolicyNoAndCoverageNo(policyNo: String, coverageNo: Int): CrossProductSurrender? {
        logger.debug("查詢 XCVSR: policyNo={}, coverageNo={}", policyNo, coverageNo)
        return mapper.findByPolicyNoAndCoverageNo(policyNo, coverageNo)
    }

    /**
     * 查詢保單所有險種的記錄
     *
     * @param policyNo 保單號碼
     * @return 查詢結果清單，無資料回傳空清單
     */
    @Cacheable(value = ["xcvsrList"], key = "#policyNo")
    fun findAllByPolicyNo(policyNo: String): List<CrossProductSurrender> {
        logger.debug("查詢保單所有 XCVSR: policyNo={}", policyNo)
        return mapper.findAllByPolicyNo(policyNo)
    }

    /**
     * 存在性檢查
     *
     * @param policyNo 保單號碼
     * @param coverageNo 險種序號
     * @return true 表示記錄存在
     */
    fun exists(policyNo: String, coverageNo: Int): Boolean {
        logger.debug("檢查 XCVSR 存在性: policyNo={}, coverageNo={}", policyNo, coverageNo)
        return mapper.exists(policyNo, coverageNo)
    }

    // === 異動操作（預留供管理介面使用）===

    /**
     * 新增記錄
     *
     * @param entity 待新增實體
     * @param operator 操作者（用於稽核追蹤）
     * @return 影響筆數
     */
    @CacheEvict(value = ["xcvsr", "xcvsrList"], key = "#entity.policyNo + '_' + #entity.coverageNo")
    fun create(entity: CrossProductSurrender, operator: String): Int {
        logger.info("新增 XCVSR: policyNo={}, coverageNo={}, operator={}",
            entity.policyNo, entity.coverageNo, operator)

        // 檢查是否已存在
        if (exists(entity.policyNo, entity.coverageNo)) {
            throw IllegalArgumentException("XCVSR 記錄已存在: policyNo=${entity.policyNo}, coverageNo=${entity.coverageNo}")
        }

        val result = mapper.insert(entity)
        logger.debug("新增 XCVSR 完成: 影響筆數={}", result)
        // TODO: 記錄至 AuditLog
        return result
    }

    /**
     * 更新記錄
     *
     * @param entity 待更新實體
     * @param operator 操作者（用於稽核追蹤）
     * @return 影響筆數
     */
    @CacheEvict(value = ["xcvsr", "xcvsrList"], key = "#entity.policyNo + '_' + #entity.coverageNo")
    fun update(entity: CrossProductSurrender, operator: String): Int {
        logger.info("更新 XCVSR: policyNo={}, coverageNo={}, operator={}",
            entity.policyNo, entity.coverageNo, operator)

        val result = mapper.update(entity)
        if (result == 0) {
            throw IllegalArgumentException("XCVSR 記錄不存在，無法更新: policyNo=${entity.policyNo}, coverageNo=${entity.coverageNo}")
        }

        logger.debug("更新 XCVSR 完成: 影響筆數={}", result)
        // TODO: 記錄至 AuditLog
        return result
    }

    /**
     * 刪除記錄
     *
     * @param policyNo 保單號碼
     * @param coverageNo 險種序號
     * @param operator 操作者（用於稽核追蹤）
     * @return 影響筆數
     */
    @CacheEvict(value = ["xcvsr", "xcvsrList"], key = "#policyNo + '_' + #coverageNo")
    fun delete(policyNo: String, coverageNo: Int, operator: String): Int {
        logger.info("刪除 XCVSR: policyNo={}, coverageNo={}, operator={}",
            policyNo, coverageNo, operator)

        val result = mapper.delete(policyNo, coverageNo)
        if (result == 0) {
            throw IllegalArgumentException("XCVSR 記錄不存在，無法刪除: policyNo=${policyNo}, coverageNo=${coverageNo}")
        }

        logger.debug("刪除 XCVSR 完成: 影響筆數={}", result)
        // TODO: 記錄至 AuditLog
        return result
    }
}

/**
 * XCVSR Cache 配置
 * - TTL: 24 小時
 * - 最大容量: 10,000 筆
 */
@Configuration
class XcvsrCacheConfig {
    @Bean
    fun xcvsrCacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager("xcvsr", "xcvsrList")
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(10_000)
        )
        return cacheManager
    }
}
