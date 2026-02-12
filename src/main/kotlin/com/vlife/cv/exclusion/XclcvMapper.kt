package com.vlife.cv.exclusion

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.LocalDate

@Mapper
interface XclcvMapper {

    fun countByPolicyAndCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Int

    fun findByPolicyAndCoverageWithPriority(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int,
        @Param("eventDateS") eventDateS: LocalDate
    ): Xclcv?

    fun findByPolicyCoverageAndInd(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int,
        @Param("clbfRvfInd") clbfRvfInd: String
    ): Xclcv?

    fun findByPolicyCoverageAndStatus(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int,
        @Param("codtStatusCode") codtStatusCode: String,
        @Param("codtStatusCode2") codtStatusCode2: String?
    ): Xclcv?

    fun findByPolicyCoverageAndIndPattern(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int,
        @Param("clbfRvfIndPattern") clbfRvfIndPattern: String
    ): Xclcv?

    fun findLatestByPolicyAndCoverage(
        @Param("policyNo") policyNo: String,
        @Param("coverageNo") coverageNo: Int
    ): Xclcv?

    fun insert(entity: Xclcv): Int

    fun deleteByClaimNo(@Param("claimNo") claimNo: String): Int
}
