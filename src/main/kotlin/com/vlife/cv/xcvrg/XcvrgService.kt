package com.vlife.cv.xcvrg

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
class XcvrgService(
    private val mapper: XcvrgMapper
) {
    fun findByPrimaryKey(replaceAgeCode: String, rateSex: String, rateAge: Int, rateSubAge: Int): Xcvrg? {
        return mapper.findByPrimaryKey(replaceAgeCode, rateSex, rateAge, rateSubAge)
    }

    fun existsByPrimaryKey(replaceAgeCode: String, rateSex: String, rateAge: Int, rateSubAge: Int): Boolean {
        return mapper.countByPrimaryKey(replaceAgeCode, rateSex, rateAge, rateSubAge) > 0
    }

    @Transactional
    fun create(xcvrg: Xcvrg): Int {
        logger.info { "Creating XCVRG: replaceAgeCode=${xcvrg.replaceAgeCode}, rateSex=${xcvrg.rateSex}, rateAge=${xcvrg.rateAge}" }
        return mapper.insert(xcvrg)
    }

    @Transactional
    fun delete(replaceAgeCode: String, rateSex: String, rateAge: Int, rateSubAge: Int): Int {
        logger.info { "Deleting XCVRG: replaceAgeCode=$replaceAgeCode, rateSex=$rateSex, rateAge=$rateAge" }
        return mapper.deleteByPrimaryKey(replaceAgeCode, rateSex, rateAge, rateSubAge)
    }
}
