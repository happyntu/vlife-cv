package com.vlife.cv.plan

/**
 * 險種註記檔 (PLNT)
 *
 * 記錄險種的各項特性與計算設定，包含：
 * - 自由審視期設定
 * - 投資型基金設定
 * - 死亡給付設定
 * - 費用與佣金設定
 * - 滿期與年金設定
 *
 * V3 Table: V3.PLNT
 * V4 Schema: CV.PLNT
 *
 * @property planCode 險種代碼
 * @property version 版本號
 * @property insuranceType6 保險類別6
 * @property freeLookInd 自由審視期指標
 * @property freeLookDurInd 自由審視期間指標
 * @property effPeriod 生效期間
 * @property freeLookRateCode 自由審視期費率代碼
 * @property premToFundInd 保費入基金指標
 * @property premToFundDur 保費入基金期間
 * @property deathBenefOpt 死亡給付選項
 * @property deathPlanCode 死亡險種代碼
 * @property deathPlanAgeInd 死亡險種年齡指標
 * @property ivFundsetInd 投資型基金組合指標
 * @property fundInd 基金指標
 * @property fundBalanceInd 基金餘額指標
 * @property premPlanCode 保費險種代碼
 * @property premPreInd 保費預付指標
 * @property extraModxSw 額外MODX開關
 * @property extraInvSw 額外投資開關
 * @property loanAcntInd 貸款帳戶指標
 * @property loanPlanCode 貸款險種代碼
 * @property expenPlanCode 費用險種代碼
 * @property allocPlanCode 配置險種代碼
 * @property coiCalcInd COI計算指標
 * @property coiPlanCode COI險種代碼
 * @property coiPlanAgeInd COI險種年齡指標
 * @property coiExpnInd COI費用指標
 * @property withdrawTypeInd 提領類型指標
 * @property withdrawIvAlloc 提領投資配置
 * @property withdrawDbInd 提領DB指標
 * @property ivChgPerInd 投資變更週期指標
 * @property ivChgCntInd 投資變更次數指標
 * @property felCommPremInd FEL佣金保費指標
 * @property felCommPremCal FEL佣金保費計算
 * @property tpCalcInd TP計算指標
 * @property tpCalcInd2 TP計算指標2
 * @property targetPremCode 目標保費代碼
 * @property tpAgeInd TP年齡指標
 * @property billInformInd 帳單通知指標
 * @property billInformFreq 帳單通知頻率
 * @property cvCalcFreq 保單價值計算頻率
 * @property returnSeqInd 返還順序指標
 * @property returnSeqInd2 返還順序指標2
 * @property matureDurInd 滿期期間指標
 * @property matureDur 滿期期間
 * @property matureDurInd2 滿期期間指標2
 * @property matureValInd 滿期值指標
 * @property matureDivInd 滿期紅利指標
 * @property matureOptInd 滿期選項指標
 * @property matureDefInd 滿期預設指標
 * @property annyInd 年金指標
 * @property annyAdjFactor 年金調整因子
 * @property payOption 給付選項
 * @property annyPlanCode 年金險種代碼
 * @property annyAgeMax 年金最大年齡
 * @property annyStartInd 年金開始指標
 * @property annyDeferedDur 年金遞延期間
 * @property annyPrePayInd 年金預付指標
 * @property annyPrePayYear 年金預付年期
 * @property annyGarInd 年金保證指標
 * @property annyGarPeriod 年金保證期間
 * @property annyAgeMin 年金最小年齡
 * @property annyStrInd 年金起始指標
 * @property annyStrVal 年金起始值
 * @property matureReValInd 滿期重新評價指標
 * @property matureNonInd 滿期不計入指標
 * @property nbdtPlanCode NBDT險種代碼
 * @property pcDurPlan PC期間險種
 * @property businessNo 業務編號
 * @property riderPremInd 附約保費指標
 * @property ivCompanyCode2 投資公司代碼2
 * @property rvfInd RVF指標
 * @property corridorSw 走廊開關
 */
data class PlanNote(
    val planCode: String,
    val version: String,
    val insuranceType6: String? = null,
    val freeLookInd: String? = null,
    val freeLookDurInd: String? = null,
    val effPeriod: Int? = null,
    val freeLookRateCode: String? = null,
    val premToFundInd: String? = null,
    val premToFundDur: Int? = null,
    val deathBenefOpt: String? = null,
    val deathPlanCode: String? = null,
    val deathPlanAgeInd: String? = null,
    val ivFundsetInd: String? = null,
    val fundInd: String? = null,
    val fundBalanceInd: String? = null,
    val premPlanCode: String? = null,
    val premPreInd: String? = null,
    val extraModxSw: String? = null,
    val extraInvSw: String? = null,
    val loanAcntInd: String? = null,
    val loanPlanCode: String? = null,
    val expenPlanCode: String? = null,
    val allocPlanCode: String? = null,
    val coiCalcInd: String? = null,
    val coiPlanCode: String? = null,
    val coiPlanAgeInd: String? = null,
    val coiExpnInd: String? = null,
    val withdrawTypeInd: String? = null,
    val withdrawIvAlloc: String? = null,
    val withdrawDbInd: String? = null,
    val ivChgPerInd: String? = null,
    val ivChgCntInd: String? = null,
    val felCommPremInd: String? = null,
    val felCommPremCal: String? = null,
    val tpCalcInd: String? = null,
    val tpCalcInd2: String? = null,
    val targetPremCode: String? = null,
    val tpAgeInd: String? = null,
    val billInformInd: String? = null,
    val billInformFreq: Int? = null,
    val cvCalcFreq: Int? = null,
    val returnSeqInd: String? = null,
    val returnSeqInd2: String? = null,
    val matureDurInd: String? = null,
    val matureDur: Int? = null,
    val matureDurInd2: String? = null,
    val matureValInd: String? = null,
    val matureDivInd: String? = null,
    val matureOptInd: String? = null,
    val matureDefInd: String? = null,
    val annyInd: String? = null,
    val annyAdjFactor: String? = null,
    val payOption: String? = null,
    val annyPlanCode: String? = null,
    val annyAgeMax: Int? = null,
    val annyStartInd: String? = null,
    val annyDeferedDur: Int? = null,
    val annyPrePayInd: String? = null,
    val annyPrePayYear: Int? = null,
    val annyGarInd: String? = null,
    val annyGarPeriod: Int? = null,
    val annyAgeMin: Int? = null,
    val annyStrInd: String? = null,
    val annyStrVal: Int? = null,
    val matureReValInd: String? = null,
    val matureNonInd: String? = null,
    val nbdtPlanCode: String? = null,
    val pcDurPlan: String? = null,
    val businessNo: String? = null,
    val riderPremInd: String? = null,
    val ivCompanyCode2: String? = null,
    val rvfInd: String? = null,
    val corridorSw: String
)
