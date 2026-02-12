package com.vlife.cv.plnd

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.math.BigDecimal
import java.time.LocalDate

/**
 * PLND Mapper Interface
 * SQL 定義寫在 PlndMapper.xml，禁止使用 @Select Annotation (ADR-016)
 */
@Mapper
interface PlndMapper {
    // === 查詢類 ===

    fun exists(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): Boolean

    fun findByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): List<Plnd>

    fun findByPlanCodeAndVersionAndTargetCode(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("ivTargetCode") ivTargetCode: String
    ): List<Plnd>

    fun findByAllConditions(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("ivTargetCode") ivTargetCode: String,
        @Param("ivApplInd") ivApplInd: String
    ): Plnd?

    fun sumRatioByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): BigDecimal?

    // === 異動類 ===

    fun insert(entity: Plnd): Int

    fun updateByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("entity") entity: Plnd
    ): Int

    fun updateByAllConditions(entity: Plnd): Int

    fun deleteByPlanCodeAndVersion(
        @Param("planCode") planCode: String,
        @Param("version") version: String
    ): Int

    fun deleteByAllConditions(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("ivTargetCode") ivTargetCode: String,
        @Param("ivApplInd") ivApplInd: String
    ): Int

    fun findEffectiveDatesByTargetCode(
        @Param("ivTargetCode") ivTargetCode: String
    ): PlndDateRangeDto?
}

/**
 * 投資標的有效期間 DTO
 */
data class PlndDateRangeDto(
    val minStartDate: LocalDate?,
    val maxEndDate: LocalDate?
)
