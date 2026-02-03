package com.vlife.cv.actuarial

import java.math.BigDecimal

/**
 * 準備金因子檔 Entity (CV.CVRF)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 對應 V3.CVRF 表格，管理保單準備金計算所需的因子參數。
 * 資料量：3,368 筆，跨模組使用 (BL, CL, CV)。
 *
 * 業務別名：ReserveFactor
 *
 * 使用範例：
 * ```kotlin
 * val factors = cvrfService.findByPlanCode("PLAN1", "1")
 * val factor = cvrfService.findById("PLAN1", "1", 1)
 * ```
 *
 * @property planCode 險種代碼 (PK, 5 碼)
 * @property version 版本號 (PK, 1 碼)
 * @property durationType 期間類型 (PK): 1=終身, 2=定期, 3=其他
 * @property durationYear 期間年數
 * @property collectionYear 繳費年數
 * @property payMode 繳費方式
 * @property insuredType 被保險人類型
 * @property policyReserveDeathFactor 保單準備金死亡因子
 * @property policyReserveRate 保單準備金利率
 * @property reserveDeathFactor 準備金死亡因子
 * @property reserveRate 準備金利率
 * @property policyReserveTso 保單準備金TSO
 * @property reserveTso 準備金TSO
 * @property etReserveRate ET準備金利率
 * @property etReserveTsoInd ET準備金TSO指示
 * @property rbnType RBN類型
 * @property sbnType SBN類型
 * @property i26OrNot I2_6標記
 * @property i26Rate I2_6比率
 * @property accidentOrNot 意外險標記
 * @property accidentRate 意外險比率
 * @property returnPremInd 還本保費指示
 * @property returnIntType 還本利息類型
 * @property returnInt 還本利息
 * @property returnCostFlag 還本成本旗標 (NOT NULL)
 * @property modifyReserveInd 修改準備金指示 (NOT NULL)
 * @property recordType10 記錄類型10 (NOT NULL)
 * @property mixReserveInd 混合準備金指示 (NOT NULL)
 * @property returnPremInd2 還本保費指示2
 * @property etPolicyReserveRate ET保單準備金利率
 * @property etPolicyReserveTsoInd ET保單準備金TSO指示
 * @property etAccidentInd ET意外險指示
 * @property etAccidentRate ET意外險利率
 */
data class Cvrf(
    val planCode: String,
    val version: String,
    val durationType: Int,
    val durationYear: Int?,
    val collectionYear: Int?,
    val payMode: Int?,
    val insuredType: Int?,
    val policyReserveDeathFactor: Int?,
    val policyReserveRate: BigDecimal?,
    val reserveDeathFactor: Int?,
    val reserveRate: BigDecimal?,
    val policyReserveTso: String?,
    val reserveTso: String?,
    val etReserveRate: BigDecimal?,
    val etReserveTsoInd: String?,
    val rbnType: String?,
    val sbnType: String?,
    val i26OrNot: String?,
    val i26Rate: BigDecimal?,
    val accidentOrNot: String?,
    val accidentRate: BigDecimal?,
    val returnPremInd: String?,
    val returnIntType: String?,
    val returnInt: BigDecimal?,
    val returnCostFlag: String,
    val modifyReserveInd: String,
    val recordType10: String,
    val mixReserveInd: String,
    val returnPremInd2: String?,
    val etPolicyReserveRate: BigDecimal?,
    val etPolicyReserveTsoInd: String?,
    val etAccidentInd: String?,
    val etAccidentRate: BigDecimal?
) {
    init {
        require(planCode.isNotBlank() && planCode.length <= 5) {
            "planCode must be 1-5 characters, got: '$planCode'"
        }
        require(version.length == 1) {
            "version must be exactly 1 character, got: '$version'"
        }
        require(durationType in 1..3) {
            "durationType must be 1, 2, or 3, got: $durationType"
        }
    }

    /**
     * 複合主鍵
     */
    val id: CvrfId get() = CvrfId(planCode, version, durationType)

    /**
     * 檢查是否為終身險
     */
    fun isWholeLife(): Boolean = durationType == DurationType.WHOLE_LIFE.code

    /**
     * 檢查是否為定期險
     */
    fun isTerm(): Boolean = durationType == DurationType.TERM.code

    /**
     * 取得期間類型列舉
     */
    fun getDurationTypeEnum(): DurationType? = DurationType.fromCode(durationType)
}

/**
 * 複合主鍵 (CV.CVRF)
 */
data class CvrfId(
    val planCode: String,
    val version: String,
    val durationType: Int
) {
    override fun toString(): String = "$planCode:$version:$durationType"

    companion object {
        fun parse(value: String): CvrfId {
            val parts = value.split(":")
            require(parts.size == 3) { "Invalid CvrfId format: $value" }
            return CvrfId(parts[0], parts[1], parts[2].toInt())
        }
    }
}

/**
 * 期間類型列舉
 *
 * 對應 DUR_TYPE 欄位的 Domain Values
 */
enum class DurationType(val code: Int, val description: String) {
    WHOLE_LIFE(1, "終身"),
    TERM(2, "定期"),
    OTHER(3, "其他");

    companion object {
        private val codeMap = entries.associateBy { it.code }

        fun fromCode(code: Int): DurationType? = codeMap[code]

        fun fromCodeOrThrow(code: Int): DurationType =
            fromCode(code) ?: throw IllegalArgumentException("Unknown DurationType: $code")
    }
}
