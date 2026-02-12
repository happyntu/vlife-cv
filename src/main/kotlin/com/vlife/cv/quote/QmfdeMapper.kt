package com.vlife.cv.quote

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDate

@Mapper
interface QmfdeMapper {
    fun findByTargetCode(@Param("ivTargetCode") ivTargetCode: String): Qmfde?
    fun findByStandardCode(@Param("ivStandardCode") ivStandardCode: String): Qmfde?
    fun countByPlanCodeAndVersionAndDate(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("date") date: LocalDate
    ): Int
    fun countByEntryInd(@Param("qmfdeEntryInd") qmfdeEntryInd: String): Int
    fun exists(@Param("ivTargetCode") ivTargetCode: String): Boolean
    fun existsByStandardCode(@Param("ivStandardCode") ivStandardCode: String): Boolean
    fun insert(entity: Qmfde): Int
    fun update(entity: Qmfde): Int
    fun delete(@Param("ivTargetCode") ivTargetCode: String): Int
}
