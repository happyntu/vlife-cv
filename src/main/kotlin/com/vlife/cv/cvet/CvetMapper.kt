package com.vlife.cv.cvet

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * CVET Mapper (ADR-017: 表格導向命名)
 *
 * 對應 V3 PK_LIB_CVETPROC 的 CRUD 功能。
 * 所有 SQL 定義於 CvetMapper.xml (ADR-016)。
 */
@Mapper
interface CvetMapper {

    fun findByPolicyAndCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): ExtendedTermPolicy?

    fun countByPolicyNo(
        @Param("policyNo") policyNo: String
    ): Int

    fun insert(@Param("cvet") cvet: ExtendedTermPolicy): Int

    fun deleteByPolicyAndCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Int
}
