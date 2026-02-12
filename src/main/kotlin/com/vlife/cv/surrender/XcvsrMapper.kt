package com.vlife.cv.surrender

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * XCVSR Mapper Interface（MyBatis）
 *
 * V3 對應：PK_LIB_XCVSRPROC
 * - f99_get_xcvsr → findByPolicyNoAndCoverageNo
 *
 * ADR 遵循：
 * - ADR-016：SQL 必須寫在 XML，禁止使用 @Select Annotation
 * - ADR-017：Mapper 名稱 = XcvsrMapper（表格導向命名）
 */
@Mapper
interface XcvsrMapper {

    // === 基本查詢 ===

    /**
     * 精確查詢：依保單號碼 + 險種序號查詢
     * 對應 V3 f99_get_xcvsr
     *
     * @param policyNo 保單號碼
     * @param coverageNo 險種序號
     * @return 查詢結果，查無資料回傳 null
     */
    fun findByPolicyNoAndCoverageNo(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): CrossProductSurrender?

    /**
     * 依保單號碼查詢所有險種記錄
     *
     * @param policyNo 保單號碼
     * @return 查詢結果清單，無資料回傳空清單
     */
    fun findAllByPolicyNo(@Param("policyNo") policyNo: String): List<CrossProductSurrender>

    /**
     * 存在性檢查
     *
     * @param policyNo 保單號碼
     * @param coverageNo 險種序號
     * @return true 表示記錄存在
     */
    fun exists(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Boolean

    // === 異動操作（預留）===

    /**
     * 新增記錄
     * V3 無此操作，V4 預留供管理介面使用
     *
     * @param entity 待新增實體
     * @return 影響筆數
     */
    fun insert(entity: CrossProductSurrender): Int

    /**
     * 更新記錄
     * V3 無此操作，V4 預留供管理介面使用
     *
     * @param entity 待更新實體
     * @return 影響筆數
     */
    fun update(entity: CrossProductSurrender): Int

    /**
     * 刪除記錄
     * V3 無此操作，V4 預留供管理介面使用
     *
     * @param policyNo 保單號碼
     * @param coverageNo 險種序號
     * @return 影響筆數
     */
    fun delete(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Int
}
