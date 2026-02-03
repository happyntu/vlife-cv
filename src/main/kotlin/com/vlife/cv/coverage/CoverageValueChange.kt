package com.vlife.cv.coverage

import java.time.LocalDate

/**
 * 保單基礎值變化檔 (CVCO)
 *
 * 對應 V3.CVCO 表格，追蹤保單承保範圍的狀態變化歷史。
 * 資料量：3,800 筆，單一模組使用。
 *
 * 使用範例：
 * ```kotlin
 * val coverages = coverageValueChangeService.findByPolicyNo("P000000001")
 * val coverage = coverageValueChangeService.findById("P000000001", 1)
 * ```
 *
 * @property policyNo 保單號碼 (PK, 10 碼)
 * @property coverageNo 承保範圍編號 (PK)
 * @property planCode 險種代碼 (5 碼)
 * @property version 版本號 (1 碼)
 * @property rateSex 費率性別 (1 碼)
 * @property rateAge 費率年齡
 * @property rateSub1 費率子鍵1 (2 碼)
 * @property rateSub2 費率子鍵2 (3 碼)
 * @property issueDate 承保生效日
 * @property statusCode 承保狀態碼 (1 碼)
 * @property insuranceType3 保險類型3 (1 碼)
 * @property processDate 處理日期
 * @property processType 處理類型 (1 碼)
 * @property policyType 保單類型 (可空)
 * @property statusCode2 承保狀態碼2 (可空)
 */
data class CoverageValueChange(
    val policyNo: String,
    val coverageNo: Int,
    val planCode: String,
    val version: String,
    val rateSex: String,
    val rateAge: Int,
    val rateSub1: String,
    val rateSub2: String,
    val issueDate: LocalDate,
    val statusCode: String,
    val insuranceType3: String,
    val processDate: LocalDate,
    val processType: String,
    val policyType: String?,
    val statusCode2: String?
) {
    init {
        require(policyNo.isNotBlank() && policyNo.length <= 10) {
            "policyNo must be 1-10 characters, got: '$policyNo'"
        }
        require(coverageNo >= 0) {
            "coverageNo must be non-negative, got: $coverageNo"
        }
        require(planCode.isNotBlank() && planCode.length <= 5) {
            "planCode must be 1-5 characters, got: '$planCode'"
        }
        require(version.length == 1) {
            "version must be exactly 1 character, got: '$version'"
        }
        require(rateSex.length == 1) {
            "rateSex must be exactly 1 character, got: '$rateSex'"
        }
        require(statusCode.length == 1) {
            "statusCode must be exactly 1 character, got: '$statusCode'"
        }
    }

    /**
     * 複合主鍵
     */
    val id: CoverageValueChangeId get() = CoverageValueChangeId(policyNo, coverageNo)

    /**
     * 檢查是否為有效狀態
     */
    fun isActive(): Boolean = statusCode == CoverageStatusCode.ACTIVE.code

    /**
     * 檢查是否已滿期
     */
    fun isMatured(): Boolean = statusCode == CoverageStatusCode.MATURED.code

    /**
     * 檢查是否已失效
     */
    fun isLapsed(): Boolean = statusCode == CoverageStatusCode.LAPSED.code
}

/**
 * 複合主鍵
 */
data class CoverageValueChangeId(
    val policyNo: String,
    val coverageNo: Int
) {
    override fun toString(): String = "$policyNo:$coverageNo"

    companion object {
        fun parse(value: String): CoverageValueChangeId {
            val parts = value.split(":")
            require(parts.size == 2) { "Invalid CoverageValueChangeId format: $value" }
            return CoverageValueChangeId(parts[0], parts[1].toInt())
        }
    }
}

/**
 * 承保狀態碼列舉
 *
 * 對應 CO_STATUS_CODE 欄位的 Domain Values
 */
enum class CoverageStatusCode(val code: String, val description: String) {
    ACTIVE("P", "有效"),
    PENDING("A", "待確認"),
    RETURN("R", "還本"),
    MATURED("M", "滿期"),
    WITHDRAWN("W", "放棄/撤回"),
    LAPSED("L", "失效"),
    DEATH("D", "身故"),
    UNKNOWN("U", "未知"),
    INVALID("I", "無效");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: String): CoverageStatusCode? = codeMap[code]

        fun fromCodeOrThrow(code: String): CoverageStatusCode =
            fromCode(code) ?: throw IllegalArgumentException("Unknown CoverageStatusCode: $code")
    }
}

/**
 * 保險類型3列舉
 *
 * 對應 INSURANCE_TYPE_3 欄位的 Domain Values
 */
enum class InsuranceType3(val code: String, val description: String) {
    REGULAR("R", "一般保險"),
    CRITICAL("C", "重大疾病"),
    LIFE("F", "壽險");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: String): InsuranceType3? = codeMap[code]

        fun fromCodeOrThrow(code: String): InsuranceType3 =
            fromCode(code) ?: throw IllegalArgumentException("Unknown InsuranceType3: $code")
    }
}
