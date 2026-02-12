package com.vlife.cv.log

import org.springframework.stereotype.Service

/**
 * PNLG - 保單日誌服務
 *
 * 提供保單定義變更歷史的查詢功能。
 * PNLG 為稽核日誌表，只讀操作，不提供寫入方法。
 *
 * 業務規則:
 * 1. 日誌表為 append-only，不允許修改或刪除
 * 2. 不使用快取（ADR-014: 交易資料，write-intensive）
 * 3. 預設按處理日期倒序排列（最新的在前）
 */
@Service
class PnlgService(
    private val pnlgMapper: PnlgMapper
) {
    /**
     * 根據日誌序號查詢單筆記錄
     *
     * @param pnlgSerial 日誌序號 (主鍵)
     * @return 保單日誌，不存在時返回 null
     */
    fun findByPnlgSerial(pnlgSerial: Long): PolicyLog? {
        return pnlgMapper.findByPnlgSerial(pnlgSerial)
    }

    /**
     * 查詢特定險種的變更歷史
     *
     * 用於追蹤保單定義的所有歷史變更記錄。
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 變更歷史清單，按處理日期倒序排列
     */
    fun findHistoryByPlanCodeAndVersion(planCode: String, version: String): List<PolicyLog> {
        return pnlgMapper.findByPlanCodeAndVersion(planCode, version)
    }

    /**
     * 查詢特定操作人員的變更記錄
     *
     * 用於稽核特定人員的操作歷史。
     *
     * @param operator 操作人員代碼
     * @return 變更記錄清單，按處理日期倒序排列
     */
    fun findByOperator(operator: String): List<PolicyLog> {
        return pnlgMapper.findByOperator(operator)
    }

    /**
     * 查詢特定類型的操作記錄
     *
     * @param actionType 操作類型
     *   - I: INSERT (新增)
     *   - U: UPDATE (修改)
     *   - D: DELETE (刪除)
     * @return 操作記錄清單，按處理日期倒序排列
     */
    fun findByActionType(actionType: String): List<PolicyLog> {
        require(actionType in listOf("I", "U", "D")) {
            "Invalid actionType: $actionType. Must be one of [I, U, D]"
        }
        return pnlgMapper.findByActionType(actionType)
    }

    /**
     * 查詢全部日誌記錄
     *
     * ⚠️ 警告：PNLG 有 351 筆資料，且持續增長，應避免在生產環境使用此方法
     *
     * @return 全部日誌記錄，按處理日期倒序排列
     */
    fun findAll(): List<PolicyLog> {
        return pnlgMapper.findAll()
    }
}
