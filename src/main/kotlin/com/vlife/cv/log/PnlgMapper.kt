package com.vlife.cv.log

import org.apache.ibatis.annotations.Mapper

/**
 * PNLG - 保單日誌 Mapper
 *
 * 遵循 ADR-016: SQL 必須寫在 XML Mapper
 * 遵循 ADR-017: Mapper 命名 = PnlgMapper (表格導向)
 */
@Mapper
interface PnlgMapper {
    /**
     * 根據日誌序號查詢單筆記錄
     */
    fun findByPnlgSerial(pnlgSerial: Long): PolicyLog?

    /**
     * 根據險種代碼與版本查詢歷史記錄
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 按處理日期倒序排列的變更歷史
     */
    fun findByPlanCodeAndVersion(planCode: String, version: String): List<PolicyLog>

    /**
     * 根據操作人員查詢歷史記錄
     * @param operator 操作人員
     * @return 按處理日期倒序排列
     */
    fun findByOperator(operator: String): List<PolicyLog>

    /**
     * 根據操作類型查詢歷史記錄
     * @param actionType 操作類型 (I/U/D)
     * @return 按處理日期倒序排列
     */
    fun findByActionType(actionType: String): List<PolicyLog>

    /**
     * 查詢全部記錄
     * @return 按處理日期倒序排列
     */
    fun findAll(): List<PolicyLog>
}
