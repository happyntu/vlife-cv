package com.vlife.cv.log

import java.math.BigDecimal
import java.time.LocalDate

/**
 * PNLG - 保單日誌表 (Policy Log)
 *
 * 記錄保單定義 (PLDF) 的變更歷史，用於稽核追蹤。
 * 每次對 PLDF 進行 INSERT/UPDATE/DELETE 操作時，觸發器會自動記錄至此表。
 *
 * Schema: CV.PNLG
 * Total Records: 351
 */
data class PolicyLog(
    // === 主鍵 ===
    val pnlgSerial: Long,                       // 日誌序號 (PK)

    // === 稽核欄位 ===
    val operator: String,                        // 操作人員
    val processDate: LocalDate,                  // 處理日期
    val actionType: String,                      // 操作類型 (I=新增, U=修改, D=刪除)

    // === 險種基本資料 ===
    val planCode: String?,                       // 險種代碼
    val version: String?,                        // 版本號
    val planTitle: String?,                      // 險種標題
    val planName: String?,                       // 險種名稱
    val contractedName: String?,                 // 契約名稱

    // === 年齡限制 ===
    val lowAge: Int?,                            // 最低投保年齡
    val highAge: Int?,                           // 最高投保年齡
    val lowAgeSub: Int?,                         // 被保人最低年齡
    val highAgeSub: Int?,                        // 被保人最高年齡
    val lowAgeInd: String?,                      // 最低年齡計算方式
    val highAgeInd: String?,                     // 最高年齡計算方式
    val renHighAgeInd: String?,                  // 續保最高年齡計算方式
    val renHighAge: Int?,                        // 續保最高年齡

    // === 關係與保額設定 ===
    val planRelation: String?,                   // 要保人與被保人關係
    val planRelationSub: String?,                // 被保人關係
    val faceAmtType: String?,                    // 保額類型
    val collectYearInd: String?,                 // 繳費年期計算方式
    val collectYear: Int?,                       // 繳費年期
    val expYearInd: String?,                     // 保障年期計算方式
    val expYear: Int?,                           // 保障年期

    // === 生效日期 ===
    val planStartDate: LocalDate?,               // 險種生效日
    val planEndDate: LocalDate?,                 // 險種終止日

    // === 給付指標 ===
    val rbnInd: String?,                         // 退休給付指標
    val sbnInd: String?,                         // 生存給付指標
    val otherSbnInd: String?,                    // 其他生存給付
    val abnInd: String?,                         // 年金給付指標

    // === 核保設定 ===
    val uwInd: String?,                          // 核保指標
    val uwAge14Sw: String?,                      // 14歲以下核保開關
    val primaryRiderInd: String?,                // 主約附約指標
    val riderInd: String?,                       // 附約指標
    val modxInd: String?,                        // 繳別指標
    val factorInd: String?,                      // 因子指標

    // === 保費計算 ===
    val premCalcType: String?,                   // 保費計算方式
    val premValue: BigDecimal?,                  // 保費值
    val unitValueInd: String?,                   // 單位值指標
    val unitValue: Double?,                      // 單位值1
    val unitValue2: Double?,                     // 單位值2
    val unitValue3: Double?,                     // 單位值3
    val planType: String?,                       // 險種類型
    val facePremInd: String?,                    // 保額保費指標
    val riskAmtValue: Int?,                      // 危險保額值
    val platInd: String?,                        // 平台指標

    // === 費率設定 ===
    val ratePlanCode: String?,                   // 費率險種代碼
    val rateVersion: String?,                    // 費率版本
    val rateSexInd: String?,                     // 費率性別指標
    val rateAgeInd: String?,                     // 費率年齡指標
    val rateSub1Ind: String?,                    // 費率子鍵1指標
    val rateSub2Ind: String?,                    // 費率子鍵2指標
    val prat01Sw: String?,                       // PRAT01開關

    // === 折扣設定 ===
    val discType: String?,                       // 折扣類型
    val discHigh1Start: Long?,                   // 第一級折扣起始保額
    val discPrem1Unit: Int?,                     // 第一級折扣單位
    val discPrem1Year: Int?,                     // 第一級折扣年繳
    val discPrem1Half: Int?,                     // 第一級折扣半年繳
    val discHigh2Start: Long?,                   // 第二級折扣起始保額
    val discPrem2Unit: Int?,                     // 第二級折扣單位
    val discPrem2Year: Int?,                     // 第二級折扣年繳
    val discPrem2Half: Int?,                     // 第二級折扣半年繳

    // === 保費變更 ===
    val chgPrem1Ind: String?,                    // 保費變更1指標
    val chgPrem1Value: Int?,                     // 保費變更1值
    val chgPrem3Ind: String?,                    // 保費變更3指標
    val chgPrem3Value: Int?,                     // 保費變更3值
    val chgPrem3Dur: Int?,                       // 保費變更3期間

    // === 關聯險種 ===
    val srPlanCode: String?,                     // SR險種代碼
    val srVersion: String?,                      // SR版本
    val insuranceType: String?,                  // 保險類型1
    val insuranceType2: String?,                 // 保險類型2
    val insuranceType3: String?,                 // 保險類型3
    val insuranceType4: String?,                 // 保險類型4
    val insuranceType5: String?,                 // 保險類型5

    // === 給付計算 ===
    val deathBenefInd: String?,                  // 身故給付指標
    val csvCalcType: String?,                    // 解約金計算方式
    val puaCalcType: String?,                    // 增額計算方式
    val eteCalcType: String?,                    // 展期計算方式

    // === 紅利設定 ===
    val divType: String?,                        // 紅利類型
    val divPayItemInd: String?,                  // 紅利給付項目
    val divCalcItemInd: String?,                 // 紅利計算項目
    val cvDivCode: String?,                      // CV紅利代碼
    val divStartYear: Int?,                      // 紅利開始年度
    val divSw1: String?,                         // 紅利開關1
    val divSw2: String?,                         // 紅利開關2

    // === 宣告/利率險種 ===
    val declPlanCode: String?,                   // 宣告險種代碼
    val int4PlanCode: String?,                   // 利率險種代碼

    // === 給付項目開關 ===
    val surrenderInd: String?,                   // 解約指標
    val nfo: String?,                            // NFO
    val lbenf: String?,                          // L給付
    val mbenf: String?,                          // M給付
    val dbenf: String?,                          // D給付
    val bbenf: String?,                          // B給付

    // === 共同給付設定 ===
    val coPayInd: String?,                       // 共同給付指標
    val coPayDisc: Int?,                         // 共同給付折扣

    // === 會計/佣金/商品分類 ===
    val acntType: String?,                       // 會計類型
    val commClassCodeI: String?,                 // 佣金類別代碼(I)
    val commClassCode: String?,                  // 佣金類別代碼
    val prodClassCode: String?,                  // 商品類別代碼
    val benefInd: String?,                       // 給付指標

    // === 自負額設定 ===
    val deductibleType: String?,                 // 自負額類型
    val deductibleAmt: Long?,                    // 自負額金額

    // === 再保/系統代碼 ===
    val treatyCode: String?,                     // 再保條約代碼
    val planCodeSys: String?,                    // 系統險種代碼
    val faceAmtUnit: Long?,                      // 保額單位

    // === 核保/保單價值/解約金 ===
    val uwPlanCode: String?,                     // 核保險種代碼
    val uwVersion: String?,                      // 核保版本
    val cvPlanCode: String?,                     // CV險種代碼
    val cvVersion: String?,                      // CV版本
    val csvPrintInd: String?,                    // 解約金列印指標
    val csvPrintYear: Int?,                      // 解約金列印年度
    val uvPrintInd: String?,                     // UV列印指標
    val topFaceAmt: Long?,                       // 最高保額

    // === 累計規則 ===
    val yurPlanCode: String?,                    // YUR險種代碼
    val yurVersion: String?,                     // YUR版本
    val ruleCode: String?,                       // 規則代碼

    // === 豁免/給付指標 ===
    val waivCsvInd: String?,                     // 豁免解約金指標
    val waiverPremInd: String?,                  // 豁免保費指標
    val saveBenefInd: String?,                   // 儲蓄給付指標

    // === 保全/借款設定 ===
    val pcPlanCode: String?,                     // 保全險種代碼
    val pcVersion: String?,                      // 保全版本
    val loanAvalInd: String?,                    // 借款可用指標
    val loanPlanCode: String?,                   // 借款險種代碼
    val loanVersion: String?,                    // 借款版本
    val loanAvalPercent: Int?,                   // 借款可用百分比

    // === 危險保費 ===
    val riskPremInd: String?,                    // 危險保費指標
    val riskPremCode: String?,                   // 危險保費代碼
    val replaceAgeInd: String?,                  // 替代年齡指標
    val replaceAgeCode: String?,                 // 替代年齡代碼

    // === 給付方式開關 ===
    val salPaySw: String?,                       // 薪資給付開關
    val prepaySw: String?,                       // 預付開關
    val addAmtSw: String?,                       // 增額開關
    val annySw: String?,                         // 年金開關
    val divSwM: String?,                         // 紅利開關M

    // === 幣別/帳戶設定 ===
    val currency1: String?,                      // 幣別
    val planAccountInd: String?,                 // 險種帳戶指標
    val premLackInd: String?,                    // 保費不足指標

    // === SAS統計代碼 ===
    val riskPremSas: String?,                    // 危險保費SAS
    val matLifeBenSas: String?,                  // 滿期生存給付SAS
    val deathBenSas: String?,                    // 身故給付SAS
    val paDeathBenSas: String?,                  // 意外身故給付SAS

    // === 續保獎勵 ===
    val persistRewardInd: String?,               // 續保獎勵指標
    val persistPremVal: Int?                     // 續保保費值
)
