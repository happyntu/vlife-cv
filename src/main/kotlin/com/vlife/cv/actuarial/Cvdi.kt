package com.vlife.cv.actuarial

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 紅利分配水準檔 Entity (CV.CVDI)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 對應 V3.CVDI 表格，管理分紅保單的紅利分配參數。
 * 資料量：51,409 筆，單一模組使用。
 *
 * 業務別名：DividendDistributionLevel
 *
 * 使用範例：
 * ```kotlin
 * val levels = cvdiService.findByPlanCode("PLAN1", "1")
 * val level = cvdiService.findByCondition("PLAN1", "1", "01", "0", 30, 1000000, 10000.00, 5, LocalDate.now())
 * ```
 *
 * @property planCode 險種代碼 (PK, 5 碼)
 * @property version 版本號 (PK, 1 碼)
 * @property paidStatus 繳費狀態 (PK, 2 碼): 01=繳費中, 99=繳清
 * @property rateSex 費率性別 (PK, 1 碼): 0=無關, 1=男, 2=女
 * @property ageLimitStart 年齡下限 (PK)
 * @property ageLimitEnd 年齡上限 (PK)
 * @property faceAmtStart 保額下限 (PK)
 * @property faceAmtEnd 保額上限 (PK)
 * @property modePremStart 保費下限 (PK)
 * @property modePremEnd 保費上限 (PK)
 * @property policyYear 保單年度 (PK)
 * @property declareDate 宣告日期 (PK)
 * @property rateRatio 費率比率
 * @property deathRatio 死亡比率
 * @property loadingRatio 附加比率
 * @property rewardRatio 獎勵比率
 * @property death1Ratio 死亡比率1
 * @property death2Ratio 死亡比率2
 * @property ratioFee 比率費用
 * @property fixFee 固定費用
 * @property detBirRate 出生率
 * @property confirmFlag 確認旗標
 * @property confirmOper 確認人員
 * @property confirmDate 確認日期
 * @property averageDiscount 平均折扣
 */
data class Cvdi(
    val planCode: String,
    val version: String,
    val paidStatus: String,
    val rateSex: String,
    val ageLimitStart: Int,
    val ageLimitEnd: Int,
    val faceAmtStart: Long,
    val faceAmtEnd: Long,
    val modePremStart: BigDecimal,
    val modePremEnd: BigDecimal,
    val policyYear: Int,
    val declareDate: LocalDate,
    val rateRatio: BigDecimal?,
    val deathRatio: BigDecimal?,
    val loadingRatio: BigDecimal?,
    val rewardRatio: BigDecimal?,
    val death1Ratio: BigDecimal?,
    val death2Ratio: BigDecimal?,
    val ratioFee: BigDecimal?,
    val fixFee: BigDecimal?,
    val detBirRate: BigDecimal?,
    val confirmFlag: String?,
    val confirmOper: String?,
    val confirmDate: LocalDate?,
    val averageDiscount: BigDecimal?
) {
    init {
        require(planCode.isNotBlank() && planCode.length <= 5) {
            "planCode must be 1-5 characters, got: '$planCode'"
        }
        require(version.length == 1) {
            "version must be exactly 1 character, got: '$version'"
        }
        require(paidStatus.length == 2) {
            "paidStatus must be exactly 2 characters, got: '$paidStatus'"
        }
        require(rateSex.length == 1) {
            "rateSex must be exactly 1 character, got: '$rateSex'"
        }
    }

    /**
     * 複合主鍵 (簡化版，用於常見查詢)
     */
    val planKey: CvdiPlanKey get() = CvdiPlanKey(planCode, version)

    /**
     * 檢查是否為繳費中狀態
     */
    fun isPaying(): Boolean = paidStatus == PaidStatus.PAYING.code

    /**
     * 檢查是否為繳清狀態
     */
    fun isPaidUp(): Boolean = paidStatus == PaidStatus.PAID_UP.code

    /**
     * 取得總比率 (費率比率 + 死亡比率 + 附加比率)
     */
    fun getTotalRatio(): BigDecimal {
        val rate = rateRatio ?: BigDecimal.ZERO
        val death = deathRatio ?: BigDecimal.ZERO
        val loading = loadingRatio ?: BigDecimal.ZERO
        return rate.add(death).add(loading)
    }
}

/**
 * 簡化主鍵 (險種 + 版本)
 */
data class CvdiPlanKey(
    val planCode: String,
    val version: String
) {
    override fun toString(): String = "$planCode:$version"

    companion object {
        fun parse(value: String): CvdiPlanKey {
            val parts = value.split(":")
            require(parts.size == 2) { "Invalid CvdiPlanKey format: $value" }
            return CvdiPlanKey(parts[0], parts[1])
        }
    }
}

/**
 * 繳費狀態列舉
 *
 * 對應 PAID_STATUS 欄位的 Domain Values
 */
enum class PaidStatus(val code: String, val description: String) {
    PAYING("01", "繳費中"),
    PAID_UP("99", "繳清");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: String): PaidStatus? = codeMap[code]

        fun fromCodeOrThrow(code: String): PaidStatus =
            fromCode(code) ?: throw IllegalArgumentException("Unknown PaidStatus: $code")
    }
}

/**
 * 費率性別列舉
 *
 * 對應 RATE_SEX 欄位的 Domain Values
 */
enum class RateSex(val code: String, val description: String) {
    UNISEX("0", "無關"),
    MALE("1", "男"),
    FEMALE("2", "女");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: String): RateSex? = codeMap[code]

        fun fromCodeOrThrow(code: String): RateSex =
            fromCode(code) ?: throw IllegalArgumentException("Unknown RateSex: $code")
    }
}
