package com.vlife.cv.exclusion

import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class XclrsService(
    private val mapper: XclrsMapper
) {
    fun getByXclrsSerial(xclrsSerial: Long): Xclrs? {
        return mapper.findByXclrsSerial(xclrsSerial)
    }

    fun getByClaimReceNoAndClaimTypeAndXclrsCode(
        claimReceNo: String, claimType: String, xclrsCode: String
    ): Xclrs? {
        return mapper.findByClaimReceNoAndClaimTypeAndXclrsCode(claimReceNo, claimType, xclrsCode)
    }

    fun getFirstByClaimReceNoAndClaimType(claimReceNo: String, claimType: String): Xclrs? {
        return mapper.findFirstByClaimReceNoAndClaimType(claimReceNo, claimType)
    }

    fun getByClaimReceNo(claimReceNo: String): List<Xclrs> {
        return mapper.findByClaimReceNo(claimReceNo)
    }

    fun getByEventId(eventId: String): List<Xclrs> {
        return mapper.findByEventId(eventId)
    }

    /**
     * 取得事故確定日
     * 對應 V3: f99_get_xclrs_event_date_s
     * V3 實作為空（NULL），V4 保持一致，始終回傳 null。
     */
    fun getEventDateS(eventId: String, claimType: String): LocalDate? {
        return null
    }

    fun create(entity: Xclrs): Xclrs {
        logger.info { "Creating XCLRS: claimReceNo=${entity.claimReceNo}" }
        mapper.insert(entity)
        return entity
    }

    fun update(entity: Xclrs): Int {
        logger.info { "Updating XCLRS: xclrsSerial=${entity.xclrsSerial}" }
        return mapper.update(entity)
    }

    fun deleteByXclrsSerial(xclrsSerial: Long): Int {
        logger.info { "Deleting XCLRS: xclrsSerial=$xclrsSerial" }
        return mapper.deleteByXclrsSerial(xclrsSerial)
    }

    fun deleteByClaimReceNo(claimReceNo: String): Int {
        logger.info { "Deleting XCLRS by claimReceNo=$claimReceNo" }
        return mapper.deleteByClaimReceNo(claimReceNo)
    }
}
