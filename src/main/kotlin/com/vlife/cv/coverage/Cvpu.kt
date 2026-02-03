package com.vlife.cv.coverage

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 產品單位檔 Entity (CV.CVPU) - 紅利分配
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 對應 V3.CVPU 表格，管理保單紅利分配與增值保額記錄。
 * 資料量：148 筆，跨模組使用 (PC, BL, CV)。
 *
 * 業務別名：ProductUnit
 *
 * 使用範例：
 * ```kotlin
 * val dividends = cvpuService.findByPolicyNo("P000000001")
 * val totalDividend = cvpuService.sumDividendDeclare("P000000001", 1)
 * ```
 *
 * @property policyNo 保單號碼 (PK, 10 碼)
 * @property coverageNo 承保範圍編號 (PK)
 * @property ps06Type PS06類型 (PK, 1 碼)
 * @property cvpuType CVPU類型 (PK, 1 碼)
 * @property lastAnnivDur 週年期間 (PK)
 * @property statusCode CVPU狀態碼 (可空)
 * @property divDeclare 宣告紅利
 * @property divPuaAmt 增值保額紅利
 * @property financialDate 財務日期 (可空)
 * @property pcpoNo 契變單號 (可空)
 * @property programId 程式代碼 (可空)
 * @property processDate 處理日期 (可空)
 * @property policyType 保單類型 (可空)
 * @property approvedDate 核准日期 (可空)
 * @property programIdCvpu CVPU程式代碼 (可空)
 */
data class Cvpu(
    val policyNo: String,
    val coverageNo: Int,
    val ps06Type: String,
    val cvpuType: String,
    val lastAnnivDur: Int,
    val statusCode: String?,
    val divDeclare: BigDecimal,
    val divPuaAmt: BigDecimal,
    val financialDate: LocalDate?,
    val pcpoNo: String?,
    val programId: String?,
    val processDate: LocalDate?,
    val policyType: String?,
    val approvedDate: LocalDate?,
    val programIdCvpu: String?
) {
    init {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be 1-10 characters, got: '$policyNo'"
        }
        require(coverageNo >= 0) {
            "coverageNo must be non-negative, got: $coverageNo"
        }
        require(ps06Type.length == 1) {
            "ps06Type must be exactly 1 character, got: '$ps06Type'"
        }
        require(cvpuType.length == 1) {
            "cvpuType must be exactly 1 character, got: '$cvpuType'"
        }
        require(lastAnnivDur >= 0) {
            "lastAnnivDur must be non-negative, got: $lastAnnivDur"
        }
    }

    /**
     * 複合主鍵
     */
    val id: CvpuId get() = CvpuId(policyNo, coverageNo, ps06Type, cvpuType, lastAnnivDur)

    /**
     * 檢查是否為有效狀態
     */
    fun isActive(): Boolean = statusCode == CvpuStatusCode.ACTIVE.code

    /**
     * 取得總紅利金額 (宣告紅利 + 增值保額紅利)
     */
    fun getTotalDividend(): BigDecimal = divDeclare.add(divPuaAmt)
}

/**
 * 複合主鍵 (CV.CVPU)
 */
data class CvpuId(
    val policyNo: String,
    val coverageNo: Int,
    val ps06Type: String,
    val cvpuType: String,
    val lastAnnivDur: Int
) {
    override fun toString(): String = "$policyNo:$coverageNo:$ps06Type:$cvpuType:$lastAnnivDur"

    companion object {
        fun parse(value: String): CvpuId {
            val parts = value.split(":")
            require(parts.size == 5) { "Invalid CvpuId format: $value" }
            return CvpuId(
                policyNo = parts[0],
                coverageNo = parts[1].toInt(),
                ps06Type = parts[2],
                cvpuType = parts[3],
                lastAnnivDur = parts[4].toInt()
            )
        }
    }
}

/**
 * 產品單位狀態碼列舉
 *
 * 對應 CVPU_STATUS_CODE 欄位的 Domain Values
 */
enum class CvpuStatusCode(val code: String, val description: String) {
    ACTIVE("1", "有效"),
    PROCESSED("2", "已處理");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: String): CvpuStatusCode? = codeMap[code]

        fun fromCodeOrThrow(code: String): CvpuStatusCode =
            fromCode(code) ?: throw IllegalArgumentException("Unknown CvpuStatusCode: $code")
    }
}

/**
 * 紅利摘要
 */
data class DividendSummary(
    val policyNo: String,
    val coverageNo: Int,
    val totalDivDeclare: BigDecimal,
    val totalDivPuaAmt: BigDecimal,
    val recordCount: Int
) {
    val totalDividend: BigDecimal get() = totalDivDeclare.add(totalDivPuaAmt)
}
