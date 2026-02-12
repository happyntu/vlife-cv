package com.vlife.cv.exclusion

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface XcldsMapper {

    fun findByPolicyNoAndCoverageNoAndXcldsType(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int,
        @Param("xcldsType") xcldsType: String
    ): Xclds?

    fun findByPolicyNo(@Param("policyNo") policyNo: String): List<Xclds>

    fun findByPolicyNoAndCoverageNo(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): List<Xclds>

    fun insert(entity: Xclds): Int

    fun deleteByPrimaryKey(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int,
        @Param("xcldsType") xcldsType: String
    ): Int
}
