package com.vlife.cv.actuarial

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * 準備金因子檔 Mapper (CV.CVRF)
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
 * 遵循 ADR-016 規範，SQL 集中於 XML 管理。
 * 遵循 ADR-017 規範，Mapper 名稱對應資料庫表格名稱。
 *
 * 對應 V3 的 CVRF 表格，儲存保單準備金計算因子。
 *
 * @see resources/mapper/cv/CvrfMapper.xml
 */
@Mapper
interface CvrfMapper {

    /**
     * 依險種代碼和版本查詢所有準備金因子
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 準備金因子清單
     */
    fun findByPlanCode(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): List<Cvrf>

    /**
     * 依主鍵查詢單筆準備金因子
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param durationType 期間類型
     * @return 準備金因子，不存在時回傳 null
     */
    fun findById(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("durationType") durationType: Int
    ): Cvrf?

    /**
     * 依期間類型查詢準備金因子
     *
     * @param durationType 期間類型 (1=終身, 2=定期, 3=其他)
     * @return 準備金因子清單
     */
    fun findByDurationType(@Param("durationType") durationType: Int): List<Cvrf>

    /**
     * 查詢所有不重複的險種代碼
     *
     * @return 險種代碼清單
     */
    fun findAllPlanCodes(): List<String>

    /**
     * 計算指定險種的準備金因子數量
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 資料筆數
     */
    fun countByPlanCode(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): Int
}
