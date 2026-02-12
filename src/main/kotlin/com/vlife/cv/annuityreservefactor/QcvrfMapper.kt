package com.vlife.cv.annuityreservefactor

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDate

@Mapper
interface QcvrfMapper {
    fun findByAnnyPlanCode(@Param("annyPlanCode") annyPlanCode: String): List<AnnuityReserveFactor>
    fun findByPk(
        @Param("annyPlanCode") annyPlanCode: String,
        @Param("strDate") strDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): AnnuityReserveFactor?
    fun insert(entity: AnnuityReserveFactor): Int
    fun updateByPk(
        @Param("annyPlanCode") annyPlanCode: String,
        @Param("strDate") strDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("entity") entity: AnnuityReserveFactor
    ): Int
    fun deleteByPk(
        @Param("annyPlanCode") annyPlanCode: String,
        @Param("strDate") strDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): Int
}
