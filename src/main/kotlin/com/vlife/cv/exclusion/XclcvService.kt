package com.vlife.cv.exclusion

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class XclcvService(
    private val mapper: XclcvMapper
) {
    fun countByPolicyAndCoverage(policyNo: String, coverageNo: Int): Int {
        return mapper.countByPolicyAndCoverage(policyNo, coverageNo)
    }

    fun findByPriority(policyNo: String, coverageNo: Int, eventDateS: LocalDate): Xclcv? {
        return mapper.findByPolicyAndCoverageWithPriority(policyNo, coverageNo, eventDateS)
    }

    fun findByInd(policyNo: String, coverageNo: Int, clbfRvfInd: String): Xclcv? {
        return mapper.findByPolicyCoverageAndInd(policyNo, coverageNo, clbfRvfInd)
    }

    fun findByStatus(policyNo: String, coverageNo: Int, codtStatusCode: String, codtStatusCode2: String?): Xclcv? {
        return mapper.findByPolicyCoverageAndStatus(policyNo, coverageNo, codtStatusCode, codtStatusCode2)
    }

    fun findByIndPattern(policyNo: String, coverageNo: Int, clbfRvfIndPattern: String): Xclcv? {
        return mapper.findByPolicyCoverageAndIndPattern(policyNo, coverageNo, clbfRvfIndPattern)
    }

    fun findLatest(policyNo: String, coverageNo: Int): Xclcv? {
        return mapper.findLatestByPolicyAndCoverage(policyNo, coverageNo)
    }

    fun insert(entity: Xclcv): Int {
        logger.info { "Inserting XCLCV: policyNo=${entity.policyNo}, claimNo=${entity.claimNo}" }
        return mapper.insert(entity)
    }

    fun deleteByClaimNo(claimNo: String): Int {
        logger.info { "Deleting XCLCV by claimNo=$claimNo" }
        return mapper.deleteByClaimNo(claimNo)
    }
}
