package com.vlife.cv.plan

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 險種描述檔 (CV.PLDF)
 *
 * 對應 V3 表格 PLDF，是險種定義的核心表格。
 * 記錄保險商品的基本規則、費率參數、業務規則等。
 *
 * 複合主鍵：planCode + version
 *
 * 業務說明：
 * - 每個險種可有多個版本，用於處理商品改版
 * - version 通常為 1, 2, 3... 或 A, B, C...
 * - planStartDate/planEndDate 控制商品上市與停賣
 *
 * V3 歸屬說明：
 * - V3 資料字典誤將 PLDF 歸類到 PC 模組（基於 Designer 圖表）
 * - 實際業務擁有者為 CV 模組（CV001M 險種描述資料維護）
 * - 詳見 ADR-022-pldf-ownership-reassignment.md
 *
 * @see PldfMapper Mapper 層
 * @see PldfService Service 層
 */
data class Pldf(
    // ==================== 主鍵 ====================

    /**
     * 險種代碼 (PK)
     * 5 碼英數字，如 "12RA1", "AN006"
     */
    val planCode: String,

    /**
     * 版數 (PK)
     * 1 碼，如 "1", "2", "A"
     */
    val version: String,

    // ==================== 基本資訊 ====================

    /**
     * 險種名稱 (簡稱)
     * V3: PLAN_TITLE
     */
    val planTitle: String? = null,

    /**
     * 險種全名
     * V3: PLAN_NAME
     */
    val planName: String? = null,

    /**
     * 契約名稱
     * V3: CONTRACTED_NAME
     */
    val contractedName: String? = null,

    // ==================== 年齡限制 ====================

    /**
     * 投保最小年齡
     * V3: LOW_AGE
     */
    val lowAge: Int,

    /**
     * 投保最大年齡
     * V3: HIGH_AGE
     */
    val highAge: Int,

    /**
     * 被保險人最小年齡
     * V3: LOW_AGE_SUB
     */
    val lowAgeSub: Int? = null,

    /**
     * 被保險人最大年齡
     * V3: HIGH_AGE_SUB
     */
    val highAgeSub: Int? = null,

    /**
     * 投保最小年齡指示
     * 1=足歲, 2=保險年齡
     * V3: LOW_AGE_IND
     */
    val lowAgeInd: String? = null,

    /**
     * 投保最大年齡指示
     * 1=足歲, 2=保險年齡
     * V3: HIGH_AGE_IND
     */
    val highAgeInd: String? = null,

    /**
     * 續保最高年齡指示
     * V3: REN_HIGH_AGE_IND
     */
    val renHighAgeInd: String? = null,

    /**
     * 續保最高年齡
     * V3: REN_HIGH_AGE
     */
    val renHighAge: Int? = null,

    // ==================== 繳費/保障年期 ====================

    /**
     * 繳費年期指示
     * 1=固定, 2=同保障年期, 3=至某歲, 4=可選
     * V3: COLLECT_YEAR_IND
     */
    val collectYearInd: String,

    /**
     * 繳費年期
     * V3: COLLECT_YEAR
     */
    val collectYear: Int? = null,

    /**
     * 保障年期指示
     * 1=固定, 2=終身, 3=至某歲, 4=可選
     * V3: EXP_YEAR_IND
     */
    val expYearInd: String,

    /**
     * 保障年期
     * V3: EXP_YEAR
     */
    val expYear: Int? = null,

    // ==================== 上市日期 ====================

    /**
     * 上市日期
     * V3: PLAN_START_DATE
     */
    val planStartDate: LocalDate,

    /**
     * 停賣日期
     * V3: PLAN_END_DATE
     */
    val planEndDate: LocalDate,

    // ==================== 險種分類 ====================

    /**
     * 主附約指示
     * P=主約, R=附約
     * V3: PRIMARY_RIDER_IND
     */
    val primaryRiderInd: String? = null,

    /**
     * 附約指示
     * V3: RIDER_IND
     */
    val riderInd: String? = null,

    /**
     * 險種關係
     * V3: PLAN_RELATION
     */
    val planRelation: String? = null,

    /**
     * 被保險人險種關係
     * V3: PLAN_RELATION_SUB
     */
    val planRelationSub: String? = null,

    /**
     * 保險型態1 (核保類型)
     * V3: INSURANCE_TYPE
     */
    val insuranceType: String? = null,

    /**
     * 保險型態2 (再保類型)
     * V3: INSURANCE_TYPE_2
     */
    val insuranceType2: String? = null,

    /**
     * 保險型態3 (準備金類型)
     * A=壽險, B=健康險, C=傷害險, F=投資型壽險, G=投資型年金
     * V3: INSURANCE_TYPE_3
     */
    val insuranceType3: String? = null,

    /**
     * 保險型態4
     * V3: INSURANCE_TYPE_4
     */
    val insuranceType4: String? = null,

    /**
     * 保險型態5
     * V3: INSURANCE_TYPE_5
     */
    val insuranceType5: String? = null,

    /**
     * 險種型態
     * 1=傳統, 2=投資連結
     * V3: PLAN_TYPE
     */
    val planType: String? = null,

    // ==================== 幣別與會計 ====================

    /**
     * 幣別
     * TWD=台幣, USD=美金, CNY=人民幣
     * V3: CURRENCY_1
     */
    val currency1: String,

    /**
     * 會計類別
     * V3: ACNT_TYPE
     */
    val acntType: String? = null,

    /**
     * 險種帳戶指示
     * V3: PLAN_ACCOUNT_IND
     */
    val planAccountInd: String,

    // ==================== 紅利設定 ====================

    /**
     * 紅利類型
     * 00=不分紅, 01=分紅保單
     * V3: DIV_TYPE
     */
    val divType: String? = null,

    /**
     * 紅利起算年度
     * V3: DIV_START_YEAR
     */
    val divStartYear: Int? = null,

    /**
     * 紅利給付項目指示
     * V3: DIV_PAY_ITEM_IND
     */
    val divPayItemInd: String? = null,

    /**
     * 紅利計算項目指示
     * V3: DIV_CALC_ITEM_IND
     */
    val divCalcItemInd: String? = null,

    /**
     * CV 紅利代碼
     * V3: CV_DIV_CODE
     */
    val cvDivCode: String? = null,

    /**
     * 紅利開關 M
     * V3: DIV_SW_M
     */
    val divSwM: String,

    // ==================== 給付/保障 ====================

    /**
     * 身故給付指示
     * V3: DEATH_BENEF_IND
     */
    val deathBenefInd: String? = null,

    /**
     * 解約指示
     * V3: SURRENDER_IND
     */
    val surrenderInd: String? = null,

    /**
     * 生存給付指示
     * V3: LBENF
     */
    val lbenf: String? = null,

    /**
     * 滿期給付指示
     * V3: MBENF
     */
    val mbenf: String? = null,

    /**
     * 身故給付指示
     * V3: DBENF
     */
    val dbenf: String? = null,

    /**
     * 保險金給付指示
     * V3: BBENF
     */
    val bbenf: String? = null,

    /**
     * 受益人指示
     * V3: BENEF_IND
     */
    val benefInd: String? = null,

    // ==================== 計算類型 ====================

    /**
     * 解約金計算類型
     * V3: CSV_CALC_TYPE
     */
    val csvCalcType: String,

    /**
     * 繳清計算類型 (PUA = Paid-Up Addition)
     * V3: PUA_CALC_TYPE
     */
    val puaCalcType: String,

    /**
     * 展期計算類型 (ETE = Extended Term)
     * V3: ETE_CALC_TYPE
     */
    val eteCalcType: String,

    // ==================== 貸款 ====================

    /**
     * 可貸款指示
     * Y=可借款, N=不可借款
     * V3: LOAN_AVAL_IND
     */
    val loanAvalInd: String? = null,

    /**
     * 貸款險種代碼
     * V3: LOAN_PLAN_CODE
     */
    val loanPlanCode: String? = null,

    /**
     * 貸款險種版數
     * V3: LOAN_VERSION
     */
    val loanVersion: String? = null,

    /**
     * 可貸款比例
     * V3: LOAN_AVAL_PERCENT
     */
    val loanAvalPercent: Int? = null,

    // ==================== 佣金 ====================

    /**
     * 佣金類別碼
     * V3: COMM_CLASS_CODE
     */
    val commClassCode: String? = null,

    /**
     * 佣金類別碼指示
     * 1=同主契約, 2=指標, 3=依業務線
     * V3: COMM_CLASS_CODE_I
     */
    val commClassCodeI: String? = null,

    /**
     * 產品類別碼
     * V3: PROD_CLASS_CODE
     */
    val prodClassCode: String? = null,

    // ==================== 關聯險種 ====================

    /**
     * 核保險種代碼
     * V3: UW_PLAN_CODE
     */
    val uwPlanCode: String,

    /**
     * 核保險種版數
     * V3: UW_VERSION
     */
    val uwVersion: String,

    /**
     * CV 險種代碼 (精算)
     * V3: CV_PLAN_CODE
     */
    val cvPlanCode: String? = null,

    /**
     * CV 險種版數
     * V3: CV_VERSION
     */
    val cvVersion: String? = null,

    /**
     * 保全規則險種代碼
     * V3: PC_PLAN_CODE
     */
    val pcPlanCode: String? = null,

    /**
     * 保全規則險種版數
     * V3: PC_VERSION
     */
    val pcVersion: String? = null,

    /**
     * 費率險種代碼
     * V3: RATE_PLAN_CODE
     */
    val ratePlanCode: String? = null,

    /**
     * 費率險種版數
     * V3: RATE_VERSION
     */
    val rateVersion: String? = null,

    /**
     * 再保險契約代碼
     * V3: TREATY_CODE
     */
    val treatyCode: String? = null,

    // ==================== 費率參數 ====================

    /**
     * 費率性別指示
     * V3: RATE_SEX_IND
     */
    val rateSexInd: String? = null,

    /**
     * 費率年齡指示
     * V3: RATE_AGE_IND
     */
    val rateAgeInd: String? = null,

    /**
     * 費率子類別1指示
     * V3: RATE_SUB_1_IND
     */
    val rateSub1Ind: String? = null,

    /**
     * 費率子類別2指示
     * V3: RATE_SUB_2_IND
     */
    val rateSub2Ind: String? = null,

    // ==================== 保費相關 ====================

    /**
     * 保費計算類型
     * V3: PREM_CALC_TYPE
     */
    val premCalcType: String? = null,

    /**
     * 保費值
     * V3: PREM_VALUE
     */
    val premValue: BigDecimal? = null,

    /**
     * 保費不足指示
     * V3: PREM_LACK_IND
     */
    val premLackInd: String,

    /**
     * 保額單位
     * V3: FACE_AMT_UNIT
     */
    val faceAmtUnit: Int? = null,

    /**
     * 最高保額
     * V3: TOP_FACE_AMT
     */
    val topFaceAmt: Int? = null,

    /**
     * 保額類型
     * V3: FACE_AMT_TYPE
     */
    val faceAmtType: String? = null,

    // ==================== 年金相關 ====================

    /**
     * 年金開關
     * V3: ANNY_SW
     */
    val annySw: String,

    // ==================== 其他指示 ====================

    /**
     * 持續獎勵金指示
     * V3: PERSIST_REWARD_IND
     */
    val persistRewardInd: String,

    /**
     * 持續保費值
     * V3: PERSIST_PREM_VAL
     */
    val persistPremVal: Int
) {
    /**
     * 檢查險種是否在指定日期有效（上市中）
     */
    fun isEffective(date: LocalDate): Boolean =
        !date.isBefore(planStartDate) && !date.isAfter(planEndDate)

    /**
     * 檢查年齡是否在投保範圍內
     */
    fun isAgeInRange(age: Int): Boolean = age in lowAge..highAge

    /**
     * 是否為主約
     */
    fun isPrimaryPlan(): Boolean = primaryRiderInd == "P"

    /**
     * 是否為附約
     */
    fun isRider(): Boolean = primaryRiderInd == "R"

    /**
     * 是否為投資型商品
     */
    fun isInvestmentLinked(): Boolean =
        insuranceType3 in listOf("F", "G", "H")

    /**
     * 是否為傳統型商品
     */
    fun isTraditional(): Boolean =
        insuranceType3 in listOf("A", "B", "C", "D", "E")

    /**
     * 是否可借款
     */
    fun isLoanAvailable(): Boolean = loanAvalInd == "Y"

    /**
     * 是否為分紅保單
     */
    fun isDividendPolicy(): Boolean = divType != null && divType != "00"
}

/**
 * 險種描述查詢條件
 */
data class PldfQuery(
    /**
     * 險種代碼（支援模糊查詢）
     */
    val planCode: String? = null,

    /**
     * 版數
     */
    val version: String? = null,

    /**
     * 主附約指示
     */
    val primaryRiderInd: String? = null,

    /**
     * 保險型態3_準備金
     */
    val insuranceType3: String? = null,

    /**
     * 險種型態
     */
    val planType: String? = null,

    /**
     * 生效日期（查詢上市中的商品）
     */
    val effectiveDate: LocalDate? = null,

    /**
     * 幣別
     */
    val currency: String? = null,

    /**
     * 可貸款指示
     */
    val loanAvalInd: String? = null,

    /**
     * 紅利類型
     */
    val divType: String? = null
)

/**
 * 險種描述新增請求
 *
 * 僅包含必填欄位與常用選填欄位，
 * 完整欄位請使用 PldfFullCreateRequest。
 */
data class PldfCreateRequest(
    // 必填
    val planCode: String,
    val version: String,
    val lowAge: Int,
    val highAge: Int,
    val collectYearInd: String,
    val expYearInd: String,
    val planStartDate: LocalDate,
    val planEndDate: LocalDate,
    val currency1: String,
    val csvCalcType: String,
    val puaCalcType: String,
    val eteCalcType: String,
    val uwPlanCode: String,
    val uwVersion: String,

    // 常用選填
    val planTitle: String? = null,
    val planName: String? = null,
    val contractedName: String? = null,
    val primaryRiderInd: String? = null,
    val insuranceType: String? = null,
    val insuranceType3: String? = null,
    val planType: String? = null,
    val divType: String? = null,
    val loanAvalInd: String? = null,
    val commClassCode: String? = null,
    val commClassCodeI: String? = null,
    val pcPlanCode: String? = null,
    val pcVersion: String? = null,
    val collectYear: Int? = null,
    val expYear: Int? = null
) {
    init {
        require(planCode.isNotBlank()) { "planCode 不可為空" }
        require(planCode.length <= 5) { "planCode 長度不可超過 5" }
        require(version.isNotBlank()) { "version 不可為空" }
        require(version.length == 1) { "version 長度必須為 1" }
        require(!planEndDate.isBefore(planStartDate)) { "停賣日期不可早於上市日期" }
        require(lowAge >= 0) { "最低投保年齡不可為負" }
        require(highAge >= lowAge) { "最高投保年齡不可小於最低投保年齡" }
    }
}

/**
 * 險種描述更新請求
 */
data class PldfUpdateRequest(
    val planTitle: String? = null,
    val planName: String? = null,
    val contractedName: String? = null,
    val planStartDate: LocalDate? = null,
    val planEndDate: LocalDate? = null,
    val primaryRiderInd: String? = null,
    val insuranceType: String? = null,
    val insuranceType3: String? = null,
    val planType: String? = null,
    val divType: String? = null,
    val loanAvalInd: String? = null,
    val commClassCode: String? = null,
    val commClassCodeI: String? = null,
    val pcPlanCode: String? = null,
    val pcVersion: String? = null,
    val lowAge: Int? = null,
    val highAge: Int? = null,
    val collectYearInd: String? = null,
    val collectYear: Int? = null,
    val expYearInd: String? = null,
    val expYear: Int? = null
)

/**
 * 險種描述摘要 (列表用)
 */
data class PldfSummary(
    val planCode: String,
    val version: String,
    val planTitle: String?,
    val planStartDate: LocalDate,
    val planEndDate: LocalDate,
    val primaryRiderInd: String?,
    val insuranceType3: String?,
    val currency1: String,
    val isEffective: Boolean
)
