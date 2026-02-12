package com.vlife.cv.quote

import java.math.BigDecimal
import java.time.LocalDate

/**
 * QMFDE (Quote Master File Detail) - 報價主檔明細 Entity
 * 對應 V3.QMFDE 表格，共 47 個欄位
 * V4 Schema: CV.QMFDE
 * 主鍵：IV_TARGET_CODE
 */
data class Qmfde(
    // === 主鍵 ===
    val ivTargetCode: String,            // IV_TARGET_CODE (PK)
    // === 基本資訊（4 欄位）===
    val ivTargetTitle: String,           // IV_TARGET_TITLE NOT NULL
    val ivCompanyCode: String?,          // IV_COMPANY_CODE
    val ivCurrency: String,              // IV_CURRENCY NOT NULL
    val ivStandardCode: String,          // IV_STANDARD_CODE NOT NULL
    // === 子帳戶設定（4 欄位）===
    val subAcntType: String,             // SUB_ACNT_TYPE NOT NULL
    val subAcntPlanCode: String?,        // SUB_ACNT_PLAN_CODE
    val subAcntCalcType: String?,        // SUB_ACNT_CALC_TYPE
    val subAcntType2: String,            // SUB_ACNT_TYPE_2 NOT NULL
    // === 投資類型與計算（4 欄位）===
    val intCalcProc: String,             // INT_CALC_PROC NOT NULL
    val ivType: String,                  // IV_TYPE NOT NULL
    val ivMinAmt: Long?,                 // IV_MIN_AMT
    val fundMinAmt: Long?,               // FUND_MIN_AMT
    // === 單位與成本（4 欄位）===
    val ivUnit: BigDecimal?,             // IV_UNIT NUMBER(18,8)
    val ivCostPrice: BigDecimal?,        // IV_COST_PRICE NUMBER(10,4)
    val ivCostExrt: BigDecimal?,         // IV_COST_EXRT NUMBER(10,6)
    val ivCostVal: BigDecimal?,          // IV_COST_VAL NUMBER(12,6)
    // === 更新與庫存（3 欄位）===
    val updateDate: LocalDate?,          // UPDATE_DATE
    val inventoryQty: Long?,             // INVENTORY_QTY
    val principleFreq: Int,              // PRINCIPLE_FREQ NOT NULL
    // === 關聯設定（3 欄位）===
    val bundleInvInd: String?,           // BUNDLE_INV_IND
    val nbdtPlanCode: String?,           // NBDT_PLAN_CODE
    val pcDurPlan: String?,              // PC_DUR_PLAN
    // === 有效期間（4 欄位）===
    val startDate: LocalDate?,           // START_DATE
    val expiredDate: LocalDate?,         // EXPIRED_DATE
    val qmfdeStrDate: LocalDate?,        // QMFDE_STR_DATE
    val qmfdeEndDate: LocalDate?,        // QMFDE_END_DATE
    // === 進件/報酬/費率（5 欄位）===
    val qmfdeEntryInd: String?,          // QMFDE_ENTRY_IND
    val matureValRate: Int?,             // MATURE_VAL_RATE
    val ivTargetYield: BigDecimal?,      // IV_TARGET_YIELD NUMBER(7,2)
    val prodRate1: BigDecimal?,          // PROD_RATE1 NUMBER(5,2)
    val ivProfitRate: BigDecimal?,       // IV_PROFIT_RATE NUMBER(5,2)
    // === 投資比例（4 欄位）===
    val ivPercentType: String?,          // IV_PERCENT_TYPE
    val ivPercentN: Int?,                // IV_PERCENT_N
    val ivPercentStr: BigDecimal?,       // IV_PERCENT_STR NUMBER(5,2)
    val ivPercentEnd: BigDecimal?,       // IV_PERCENT_END NUMBER(5,2)
    // === 適用年期（2 欄位）===
    val intApplyYrInd: String?,          // INT_APPLY_YR_IND
    val intApplyYr: Int?,                // INT_APPLY_YR
    // === 銷售（4 欄位）===
    val ivSalesEndDate: LocalDate?,      // IV_SALES_END_DATE
    val qmfdeAmt: Long?,                // QMFDE_AMT
    val invest2ndDate: LocalDate?,       // INVEST_2ND_DATE
    val invest3thDate: LocalDate?,       // INVEST_3TH_DATE
    // === 利率與獎勵（5 欄位）===
    val intPlanCode: String?,            // INT_PLAN_CODE
    val lowestIvestReward: BigDecimal?,  // LOWEST_IVEST_REWARD NUMBER(5,2)
    val fyFixedReward: BigDecimal?,      // FY_FIXED_REWARD NUMBER(5,2)
    val lowestGuaranteeReward: BigDecimal?, // LOWEST_GUARANTEE_REWARD NUMBER(5,2)
    val standardReward: BigDecimal?      // STANDARD_REWARD NUMBER(5,2)
)

data class QmfdeDto(
    val ivTargetCode: String,
    val ivTargetTitle: String,
    val ivCompanyCode: String?,
    val ivCurrency: String,
    val ivStandardCode: String,
    val subAcntType: String,
    val ivType: String,
    val ivMinAmt: Long?,
    val startDate: LocalDate?,
    val expiredDate: LocalDate?,
    val qmfdeEntryInd: String?,
    val ivTargetYield: BigDecimal?,
    val bundleInvInd: String?
)

fun Qmfde.toDto(): QmfdeDto = QmfdeDto(
    ivTargetCode = ivTargetCode,
    ivTargetTitle = ivTargetTitle,
    ivCompanyCode = ivCompanyCode,
    ivCurrency = ivCurrency,
    ivStandardCode = ivStandardCode,
    subAcntType = subAcntType,
    ivType = ivType,
    ivMinAmt = ivMinAmt,
    startDate = startDate,
    expiredDate = expiredDate,
    qmfdeEntryInd = qmfdeEntryInd,
    ivTargetYield = ivTargetYield,
    bundleInvInd = bundleInvInd
)
