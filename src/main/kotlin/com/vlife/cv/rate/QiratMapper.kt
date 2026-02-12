package com.vlife.cv.rate

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.math.BigDecimal
import java.time.LocalDate

@Mapper
interface QiratMapper {
    fun findByBaseDate(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("baseDate") baseDate: LocalDate
    ): SubAccountInterestRate?

    fun findMaxBeforeDate(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("baseDate") baseDate: LocalDate
    ): SubAccountInterestRate?

    fun existsByPlanAndType(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String
    ): Boolean

    fun existsByFullKey(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("intRateDateStr") intRateDateStr: LocalDate
    ): Boolean

    fun findByDateAfterOrEqual(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("intRateDateStr") intRateDateStr: LocalDate
    ): SubAccountInterestRate?

    fun findMaxStartDate(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String
    ): LocalDate?

    fun findByProcessYm(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("processYm") processYm: String
    ): SubAccountInterestRate?

    fun findByFullKey(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("intRateDateStr") intRateDateStr: LocalDate?,
        @Param("intRateDateEnd") intRateDateEnd: LocalDate?
    ): SubAccountInterestRate?

    fun countByPlanAndType(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String?
    ): Int

    fun insert(@Param("rate") rate: SubAccountInterestRate): Int

    fun updateEndDate(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("intRateDateStr") intRateDateStr: LocalDate,
        @Param("intRateDateEnd") intRateDateEnd: LocalDate
    ): Int

    fun updateStartDate(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("intRateDateEnd") intRateDateEnd: LocalDate,
        @Param("intRateDateStr") intRateDateStr: LocalDate
    ): Int

    fun updateRate(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("intRateDateStr") intRateDateStr: LocalDate,
        @Param("intRateDateEnd") intRateDateEnd: LocalDate,
        @Param("intRate") intRate: BigDecimal
    ): Int

    fun delete(
        @Param("subAcntPlanCode") subAcntPlanCode: String,
        @Param("intRateType") intRateType: String,
        @Param("intRateDateStr") intRateDateStr: LocalDate,
        @Param("intRateDateEnd") intRateDateEnd: LocalDate
    ): Int
}
