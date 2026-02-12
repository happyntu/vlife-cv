package com.vlife.cv.dividend

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * CVDP Mapper (ADR-017: 表格導向命名)
 *
 * 對應 V3 PK_LIB_CVDPPROC 的 CRUD 功能。
 * 所有 SQL 定義於 CvdpMapper.xml (ADR-016)。
 */
@Mapper
interface CvdpMapper {

    fun findByPrimaryKey(
        @Param("serialYear3") serialYear3: String,
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Cvdp?

    fun findBySerialYear3AndPolicyNo(
        @Param("serialYear3") serialYear3: String,
        @Param("policyNo") policyNo: String
    ): List<Cvdp>

    fun insert(entity: Cvdp): Int

    fun update(entity: Cvdp): Int

    fun delete(
        @Param("serialYear3") serialYear3: String,
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Int
}
