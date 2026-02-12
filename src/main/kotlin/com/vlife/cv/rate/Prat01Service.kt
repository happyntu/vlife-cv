package com.vlife.cv.rate

import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * PRAT01 (Plan Rate 01) Service
 *
 * 費率檔01 業務邏輯服務，提供費率查詢、存在性檢查與快取管理。
 *
 * V3 Package: PK_LIB_PRAT01PROC
 * V4 Schema: CV.PRAT01
 *
 * 快取策略 (ADR-014):
 * - 快取類型: Caffeine (本地快取，延遲 ~100ns)
 * - TTL: 24 小時
 * - 最大容量: 50,000 筆
 * - Key 格式: "{planCode}:{version}:{rateSex}:{rateSub1}:{rateSub2}:{rateAge}"
 * - 預熱策略: 啟動時依險種分批預載
 *
 * 注意：PRAT01 為唯讀表格，費率由精算系統維護，V4 不提供異動操作
 */
@Service
class Prat01Service(
    private val mapper: Prat01Mapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ===== 查詢方法（帶快取）=====

    /**
     * 依完整主鍵查詢費率
     *
     * V3: f99_get_prat01
     *
     * 六欄位複合主鍵查詢，使用 PlanRate01Key 避免傳參錯誤。
     * 查詢結果會被快取，快取 Key 為六欄位組合。
     *
     * @param key 複合主鍵物件
     * @return 費率記錄，若不存在則回傳 null
     */
    @Cacheable(
        value = ["prat01"],
        key = "#key.planCode + ':' + #key.version + ':' + #key.rateSex + ':' + #key.rateSub1 + ':' + #key.rateSub2 + ':' + #key.rateAge"
    )
    fun findByPrimaryKey(key: PlanRate01Key): PlanRate01? {
        log.debug("Querying PRAT01 by key: {}", key)
        return mapper.findByPrimaryKey(key)
    }

    /**
     * 查詢險種的所有費率
     *
     * V3 無對應功能，V4 新增用於批次處理/快取預熱
     *
     * @param planCode 險種代碼
     * @return 該險種的所有費率記錄
     */
    fun findByPlanCode(planCode: String): List<PlanRate01> {
        log.debug("Querying all rates for plan: {}", planCode)
        return mapper.findByPlanCode(planCode)
    }

    /**
     * 查詢特定險種版本的所有費率
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 該險種版本的所有費率記錄
     */
    fun findByPlanCodeAndVersion(planCode: String, version: String): List<PlanRate01> {
        log.debug("Querying rates for plan: {}, version: {}", planCode, version)
        return mapper.findByPlanCodeAndVersion(planCode, version)
    }

    // ===== 存在性檢查 =====

    /**
     * 存在性檢查
     *
     * @param key 複合主鍵物件
     * @return 若存在則回傳 true
     */
    fun exists(key: PlanRate01Key): Boolean {
        return mapper.existsByPrimaryKey(key)
    }

    // ===== 統計方法 =====

    /**
     * 查詢險種費率筆數
     *
     * @param planCode 險種代碼
     * @return 該險種的費率筆數
     */
    fun countByPlanCode(planCode: String): Int {
        return mapper.countByPlanCode(planCode)
    }
}
