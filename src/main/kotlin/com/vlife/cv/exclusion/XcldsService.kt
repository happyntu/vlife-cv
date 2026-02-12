package com.vlife.cv.exclusion

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class XcldsService(
    private val mapper: XcldsMapper
) {
    fun findByKey(policyNo: String, coverageNo: Int, xcldsType: String): Xclds? {
        return mapper.findByPolicyNoAndCoverageNoAndXcldsType(policyNo, coverageNo, xcldsType)
    }

    fun findDeathDate(policyNo: String, coverageNo: Int): LocalDate? {
        return mapper.findByPolicyNoAndCoverageNoAndXcldsType(
            policyNo, coverageNo, Xclds.TYPE_DEATH
        )?.xcldsDate
    }

    fun findByPolicyNo(policyNo: String): List<Xclds> {
        return mapper.findByPolicyNo(policyNo)
    }

    fun findByPolicyNoAndCoverageNo(policyNo: String, coverageNo: Int): List<Xclds> {
        return mapper.findByPolicyNoAndCoverageNo(policyNo, coverageNo)
    }

    fun insert(entity: Xclds): Int {
        logger.info { "Inserting XCLDS: policyNo=${entity.policyNo}, coverageNo=${entity.coverageNo}, type=${entity.xcldsType}" }
        return mapper.insert(entity)
    }

    @Transactional
    fun upsert(entity: Xclds): Int {
        mapper.deleteByPrimaryKey(entity.policyNo, entity.coverageNo, entity.xcldsType)
        return mapper.insert(entity)
    }

    @Transactional
    fun recordDeathDate(policyNo: String, coverageNo: Int, deathDate: LocalDate, claimNo: String): Int {
        val entity = Xclds(
            policyNo = policyNo,
            coverageNo = coverageNo,
            xcldsType = Xclds.TYPE_DEATH,
            xcldsDate = deathDate,
            referenceCode = claimNo
        )
        return upsert(entity)
    }
}
