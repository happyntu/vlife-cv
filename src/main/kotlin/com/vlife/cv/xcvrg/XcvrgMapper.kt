package com.vlife.cv.xcvrg

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface XcvrgMapper {
    fun findByPrimaryKey(
        @Param("replaceAgeCode") replaceAgeCode: String,
        @Param("rateSex") rateSex: String,
        @Param("rateAge") rateAge: Int,
        @Param("rateSubAge") rateSubAge: Int
    ): Xcvrg?

    fun countByPrimaryKey(
        @Param("replaceAgeCode") replaceAgeCode: String,
        @Param("rateSex") rateSex: String,
        @Param("rateAge") rateAge: Int,
        @Param("rateSubAge") rateSubAge: Int
    ): Int

    fun insert(xcvrg: Xcvrg): Int

    fun deleteByPrimaryKey(
        @Param("replaceAgeCode") replaceAgeCode: String,
        @Param("rateSex") rateSex: String,
        @Param("rateAge") rateAge: Int,
        @Param("rateSubAge") rateSubAge: Int
    ): Int
}
