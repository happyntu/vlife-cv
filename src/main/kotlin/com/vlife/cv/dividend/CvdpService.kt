package com.vlife.cv.dividend

import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 紅利分配保單明細預估檔 Service (CV.CVDP)
 *
 * 對應 V3 PK_LIB_CVDPPROC 的業務邏輯層。
 * 不使用快取（目前零資料量，且為事務性紅利計算資料）。
 */
@Service
class CvdpService(
    private val mapper: CvdpMapper
) {

    // === 查詢方法 ===

    /**
     * 依主鍵查詢單筆紅利分配預估
     * 對應 V3: f99_get_cvdp
     */
    fun getByPrimaryKey(serialYear3: String, policyNo: String, coverageNo: Int): Cvdp? {
        logger.debug { "Finding CVDP by PK: serialYear3=$serialYear3, policyNo=$policyNo, coverageNo=$coverageNo" }
        return mapper.findByPrimaryKey(serialYear3, policyNo, coverageNo)
    }

    /**
     * 依序號年度+保單號碼查詢（多險種）
     */
    fun getBySerialYear3AndPolicyNo(serialYear3: String, policyNo: String): List<Cvdp> {
        logger.debug { "Finding CVDP by serialYear3=$serialYear3, policyNo=$policyNo" }
        return mapper.findBySerialYear3AndPolicyNo(serialYear3, policyNo)
    }

    // === 異動操作 ===

    /**
     * 新增紅利分配預估記錄
     * 對應 V3: f99_insert_cvdp
     */
    fun create(entity: Cvdp, operator: String): Int {
        logger.info { "Creating CVDP: serialYear3=${entity.serialYear3}, policyNo=${entity.policyNo}, coverageNo=${entity.coverageNo}, operator=$operator" }
        val result = mapper.insert(entity)
        // TODO: 記錄至 AuditLog
        return result
    }

    /**
     * 更新紅利分配預估記錄
     * 對應 V3: f99_update_cvdp
     */
    fun update(entity: Cvdp, operator: String): Int {
        logger.info { "Updating CVDP: serialYear3=${entity.serialYear3}, policyNo=${entity.policyNo}, coverageNo=${entity.coverageNo}, operator=$operator" }
        val result = mapper.update(entity)
        // TODO: 記錄至 AuditLog
        return result
    }

    /**
     * 刪除紅利分配預估記錄
     * 對應 V3: f99_delete_cvdp
     */
    fun delete(serialYear3: String, policyNo: String, coverageNo: Int, operator: String): Int {
        logger.info { "Deleting CVDP: serialYear3=$serialYear3, policyNo=$policyNo, coverageNo=$coverageNo, operator=$operator" }
        val result = mapper.delete(serialYear3, policyNo, coverageNo)
        // TODO: 記錄至 AuditLog
        return result
    }
}
