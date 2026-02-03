package com.vlife.cv.commission

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 佣金率檔 Entity (CV.CRAT)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 對應 V3.CRAT 表格，用於佣金計算與業績報表。
 * 資料量：53,108 筆，被 AG、CV 模組使用。
 *
 * 業務別名：CommissionRate
 *
 * 使用範例：
 * ```kotlin
 * val rates = cratService.findByClassCode("12RA1")
 * val rate = cratService.findEffectiveRate(
 *     commClassCode = "12RA1",
 *     commLineCode = "31",
 *     effectiveDate = LocalDate.now()
 * )
 * ```
 *
 * @property serial 序號 (主鍵)
 * @property commClassCode 佣金率類別碼 (5 碼)
 * @property commLineCode 業務線代號 (2 碼)
 * @property cratType 佣金率型態 (1 碼)
 * @property projectNo 專案號碼 (8 碼，可空)
 * @property startDate 生效起日
 * @property endDate 生效迄日
 * @property cratKey1 佣金鍵值1 (3 碼)
 * @property cratKey2 佣金鍵值2 (3 碼)
 * @property commStartYear 佣金起年 (可空)
 * @property commEndYear 佣金迄年 (可空)
 * @property commStartAge 佣金起始年齡 (可空)
 * @property commEndAge 佣金結束年齡 (可空)
 * @property commStartModx 佣金起始繳期 (可空)
 * @property commEndModx 佣金結束繳期 (可空)
 * @property commRate 佣金率 (可空)
 * @property commRateOrg 原始佣金率 (可空)
 * @property premLimitStart 保費下限 (可空)
 * @property premLimitEnd 保費上限 (可空)
 */
data class Crat(
    val serial: Long,
    val commClassCode: String,
    val commLineCode: String,
    val cratType: String,
    val projectNo: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val cratKey1: String,
    val cratKey2: String,
    val commStartYear: Int?,
    val commEndYear: Int?,
    val commStartAge: Int?,
    val commEndAge: Int?,
    val commStartModx: Int?,
    val commEndModx: Int?,
    val commRate: BigDecimal?,
    val commRateOrg: BigDecimal?,
    val premLimitStart: BigDecimal?,
    val premLimitEnd: BigDecimal?
) {
    init {
        require(commClassCode.isNotBlank() && commClassCode.length <= 5) {
            "commClassCode must be 1-5 characters, got: '$commClassCode'"
        }
        require(commLineCode.isNotBlank() && commLineCode.length <= 2) {
            "commLineCode must be 1-2 characters, got: '$commLineCode'"
        }
        require(cratType.isNotBlank() && cratType.length == 1) {
            "cratType must be exactly 1 character, got: '$cratType'"
        }
        require(cratKey1.isNotBlank() && cratKey1.length <= 3) {
            "cratKey1 must be 1-3 characters, got: '$cratKey1'"
        }
        require(cratKey2.isNotBlank() && cratKey2.length <= 3) {
            "cratKey2 must be 1-3 characters, got: '$cratKey2'"
        }
        require(!startDate.isAfter(endDate)) {
            "startDate must not be after endDate: $startDate > $endDate"
        }
    }

    /**
     * 檢查指定日期是否在有效期間內
     */
    fun isEffectiveAt(date: LocalDate): Boolean =
        !date.isBefore(startDate) && !date.isAfter(endDate)

    /**
     * 檢查指定年齡是否在佣金年齡範圍內
     */
    fun isAgeInRange(age: Int): Boolean {
        val start = commStartAge ?: return true
        val end = commEndAge ?: return true
        return age in start..end
    }

    /**
     * 檢查指定年度是否在佣金年度範圍內
     */
    fun isYearInRange(year: Int): Boolean {
        val start = commStartYear ?: return true
        val end = commEndYear ?: return true
        return year in start..end
    }
}

/**
 * 佣金率查詢條件
 */
data class CratQuery(
    val commClassCode: String? = null,
    val commLineCode: String? = null,
    val cratType: String? = null,
    val effectiveDate: LocalDate? = null,
    val age: Int? = null,
    val year: Int? = null
)

/**
 * 業務線代號列舉
 *
 * 對應 COMM_LINE_CODE 欄位的 Domain Values
 */
enum class CommissionLineCode(val code: String, val description: String) {
    THREE_TIER_AGENT("31", "三階業務員"),
    GROUP_ACCIDENT("61", "團意險(三階)"),
    TWO_TIER("21", "二階"),
    THREE_TIER_ADMIN("35", "三階內勤"),
    BANCASSURANCE("1C", "金融保代"),
    TELEMARKETING("5D", "電話行銷(自營)"),
    AIRPORT("4E", "中正機場");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: String): CommissionLineCode? = codeMap[code]

        fun fromCodeOrThrow(code: String): CommissionLineCode =
            fromCode(code) ?: throw IllegalArgumentException("Unknown CommissionLineCode: $code")
    }
}

/**
 * 佣金率型態列舉
 *
 * 對應 CRAT_TYPE 欄位的 Domain Values
 */
enum class CratType(val code: String, val description: String) {
    GENERAL("1", "一般佣金_折算率"),
    EXTENSION_DIFF("9", "展代佣金率差額"),
    TYPE_6("6", "類型6"),
    TARGET_PREMIUM("A", "目標保費佣金率"),
    EXCESS_SINGLE("D", "超額保費佣金率(單投)"),
    EXCESS_REGULAR("B", "超額保費佣金率(定期定額)");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: String): CratType? = codeMap[code]

        fun fromCodeOrThrow(code: String): CratType =
            fromCode(code) ?: throw IllegalArgumentException("Unknown CratType: $code")
    }
}
