package com.vlife.cv.plan

import org.apache.ibatis.annotations.Mapper

/**
 * PLNT 險種註記檔 Mapper
 *
 * 遵循 ADR-016: SQL 必須寫在 XML Mapper
 * 遵循 ADR-017: Mapper 命名 = PlntMapper (表格導向)
 */
@Mapper
interface PlntMapper {

    /**
     * 依險種代碼與版本查詢險種註記
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @return 險種註記，若不存在則返回 null
     */
    fun findByPlanCodeAndVersion(planCode: String, version: String): PlanNote?

    /**
     * 依險種代碼查詢所有版本險種註記
     *
     * @param planCode 險種代碼
     * @return 險種註記列表
     */
    fun findByPlanCode(planCode: String): List<PlanNote>

    /**
     * 查詢所有險種註記
     *
     * @return 所有險種註記列表
     */
    fun findAll(): List<PlanNote>
}
