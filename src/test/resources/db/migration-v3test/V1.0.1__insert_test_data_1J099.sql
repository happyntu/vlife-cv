-- =============================================================================
-- V1.0.3: 測試資料插入 (Plan 1J099)
-- 目的: 支援 AnnuityRateStrategy Phase 2C/Phase 4 整合測試
-- 測試案例: PC-004 V3 vs V4 比對測試（insurance_type_3='G'）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- PLDF: 1J099 (利變年金保險 G 型)
-- -----------------------------------------------------------------------------
INSERT INTO CV.PLDF (
    -- 主鍵
    PLAN_CODE, VERSION,
    -- 基本資訊
    PLAN_TITLE, PLAN_NAME, CONTRACTED_NAME,
    -- 年齡限制
    LOW_AGE, HIGH_AGE, LOW_AGE_SUB, HIGH_AGE_SUB,
    LOW_AGE_IND, HIGH_AGE_IND, REN_HIGH_AGE_IND, REN_HIGH_AGE,
    -- 繳費/保障年期
    COLLECT_YEAR_IND, COLLECT_YEAR, EXP_YEAR_IND, EXP_YEAR,
    -- 上市日期
    PLAN_START_DATE, PLAN_END_DATE,
    -- 險種分類
    PLAN_RELATION, PLAN_RELATION_SUB, PRIMARY_RIDER_IND, RIDER_IND,
    INSURANCE_TYPE, INSURANCE_TYPE_2, INSURANCE_TYPE_3, INSURANCE_TYPE_4, INSURANCE_TYPE_5,
    PLAN_TYPE,
    -- 幣別與會計
    CURRENCY_1, ACNT_TYPE, PLAN_ACCOUNT_IND,
    -- 保費相關
    FACE_AMT_TYPE, FACE_AMT_UNIT, TOP_FACE_AMT, FACE_PREM_IND,
    PREM_CALC_TYPE, PREM_VALUE, PREM_LACK_IND,
    UNIT_VALUE_IND, UNIT_VALUE, UNIT_VALUE2, UNIT_VALUE3,
    RISK_AMT_VALUE, PLAT_IND,
    -- 費率參數
    RATE_PLAN_CODE, RATE_VERSION, RATE_SEX_IND, RATE_AGE_IND,
    RATE_SUB_1_IND, RATE_SUB_2_IND, PRAT01_SW,
    -- 折扣設定
    DISC_TYPE, DISC_HIGH1_START, DISC_PREM1_UNIT, DISC_PREM1_YEAR, DISC_PREM1_HALF,
    DISC_HIGH2_START, DISC_PREM2_UNIT, DISC_PREM2_YEAR, DISC_PREM2_HALF,
    -- 保費變更
    CHG_PREM1_IND, CHG_PREM1_VALUE, CHG_PREM3_IND, CHG_PREM3_VALUE, CHG_PREM3_DUR,
    -- 紅利設定
    RBN_IND, SBN_IND, OTHER_SBN_IND, ABN_IND,
    DIV_TYPE, DIV_START_YEAR, DIV_PAY_ITEM_IND, DIV_CALC_ITEM_IND,
    CV_DIV_CODE, DIV_SW_1, DIV_SW_2, DIV_SW_M,
    -- 給付/保障
    DEATH_BENEF_IND, SURRENDER_IND, NFO, LBENF, MBENF, DBENF, BBENF, BENEF_IND,
    CO_PAY_IND, CO_PAY_DISC, DEDUCTIBLE_TYPE, DEDUCTIBLE_AMT,
    -- 計算類型
    CSV_CALC_TYPE, PUA_CALC_TYPE, ETE_CALC_TYPE,
    CSV_PRINT_IND, CSV_PRINT_YEAR, UV_PRINT_IND,
    WAIV_CSV_IND, WAIVER_PREM_IND, SAVE_BENEF_IND,
    -- 貸款
    LOAN_AVAL_IND, LOAN_PLAN_CODE, LOAN_VERSION, LOAN_AVAL_PERCENT,
    -- 佣金
    COMM_CLASS_CODE_I, COMM_CLASS_CODE, PROD_CLASS_CODE,
    -- 關聯險種
    SR_PLAN_CODE, SR_VERSION, DECL_PLAN_CODE, INT4_PLAN_CODE,
    UW_PLAN_CODE, UW_VERSION, UW_IND, UW_AGE_14_SW,
    CV_PLAN_CODE, CV_VERSION, PC_PLAN_CODE, PC_VERSION,
    YUR_PLAN_CODE, YUR_VERSION, PLAN_CODE_SYS,
    -- 再保
    TREATY_CODE, RISK_PREM_IND, RISK_PREM_CODE,
    -- 其他規則
    RULE_CODE, REPLACE_AGE_IND, REPLACE_AGE_CODE,
    SAL_PAY_SW, PREPAY_SW, ADD_AMT_SW, MODX_IND, FACTOR_IND,
    -- 年金相關
    ANNY_SW,
    -- 監理代碼
    RISK_PREM_SAS, MAT_LIFE_BEN_SAS, DEATH_BEN_SAS, PA_DEATH_BEN_SAS,
    -- 持續獎勵
    PERSIST_REWARD_IND, PERSIST_PREM_VAL
) VALUES (
    -- 主鍵
    '1J099', '1',
    -- 基本資訊
    '煒來人壽新集利利率變動型年金保險【乙型】', '新集利【乙型】', NULL,
    -- 年齡限制
    0, 70, NULL, NULL,
    '1', '1', '0', NULL,
    -- 繳費/保障年期
    '1', NULL, '4', 999,
    -- 上市日期
    TO_DATE('2007-06-13', 'YYYY-MM-DD'), TO_DATE('2007-09-02', 'YYYY-MM-DD'),
    -- 險種分類
    NULL, NULL, '1', '0',
    'O', '0', 'G', 'V', 'N',
    NULL,
    -- 幣別與會計
    'NTD', '41', '0',
    -- 保費相關
    NULL, 1, NULL, NULL,
    NULL, 1, 'N',
    NULL, 10000, 1, 9,
    0, '2',
    -- 費率參數
    NULL, NULL, '3', '0',
    '0', '0', 'N',
    -- 折扣設定
    '0', NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL,
    -- 保費變更
    NULL, NULL, NULL, NULL, NULL,
    -- 紅利設定
    '0', '0', '00000', '0',
    '20', NULL, '0000', '0000',
    '201', NULL, '999', '0',
    -- 給付/保障
    '000', '1', '0', 'N', 'N', 'Y', 'N', NULL,
    'N', 100, NULL, NULL,
    -- 計算類型
    '000', '0000', '000000',
    '1', NULL, 'N',
    '2', '1', '0',
    -- 貸款
    'Y', 'LE001', '1', NULL,
    -- 佣金
    '2', '91J01', NULL,
    -- 關聯險種
    NULL, NULL, NULL, '4I0',
    '1J099', '1', 'Y', 'N',
    '1J0', '1', '1J01', '1',
    NULL, NULL, '1J0',
    -- 再保
    NULL, 'N', NULL,
    -- 其他規則
    NULL, 'N', NULL,
    'N', 'N', 'N', '10000', '04',
    -- 年金相關
    '1',
    -- 監理代碼
    NULL, '4', NULL, NULL,
    -- 持續獎勵
    'N', 0
);

-- -----------------------------------------------------------------------------
-- PLND: 1J099 → 2J0ND1 (投資標的關聯)
-- -----------------------------------------------------------------------------
INSERT INTO CV.PLND (
    PLAN_CODE, VERSION, IV_TARGET_CODE, IV_PERCENT, IV_APPL_IND, IVHS_CODE_C
) VALUES (
    '1J099', '1', '2J0ND1', 100, '1', 'SKL'
);

-- -----------------------------------------------------------------------------
-- QMFDE: 2J0ND1 (投資標的定義 - 新集利年金)
-- -----------------------------------------------------------------------------
INSERT INTO CV.QMFDE (
    -- 主鍵
    IV_TARGET_CODE,
    -- 基本資訊
    IV_TARGET_TITLE, IV_COMPANY_CODE, IV_CURRENCY, IV_TYPE,
    -- 帳戶類型
    SUB_ACNT_TYPE, SUB_ACNT_PLAN_CODE, SUB_ACNT_CALC_TYPE, SUB_ACNT_TYPE_2,
    -- 利率設定（P0-001 關鍵欄位）
    INT_CALC_PROC, INT_APPLY_YR_IND, INT_APPLY_YR,
    -- 投資金額與單位
    IV_MIN_AMT, FUND_MIN_AMT, IV_UNIT, IV_COST_PRICE, IV_COST_EXRT, IV_COST_VAL, INVENTORY_QTY,
    -- 日期設定
    UPDATE_DATE, START_DATE, EXPIRED_DATE,
    QMFDE_STR_DATE, QMFDE_END_DATE, IV_SALES_END_DATE,
    INVEST_2ND_DATE, INVEST_3TH_DATE,
    -- 費率與收益
    PRINCIPLE_FREQ, MATURE_VAL_RATE, IV_TARGET_YIELD, PROD_RATE1, IV_PROFIT_RATE,
    LOWEST_IVEST_REWARD, FY_FIXED_REWARD, LOWEST_GUARANTEE_REWARD, STANDARD_REWARD,
    -- 百分比設定
    IV_PERCENT_TYPE, IV_PERCENT_N, IV_PERCENT_STR, IV_PERCENT_END,
    -- 關聯代碼
    BUNDLE_INV_IND, NBDT_PLAN_CODE, PC_DUR_PLAN, QMFDE_ENTRY_IND,
    INT_PLAN_CODE, IV_STANDARD_CODE, QMFDE_AMT
) VALUES (
    -- 主鍵
    '2J0ND1',
    -- 基本資訊
    '新集利年金2J1J', NULL, 'NTD', '4',
    -- 帳戶類型
    '1', NULL, NULL, '1',
    -- 利率設定（P0-001: intApplyYrInd='A', intApplyYr=5 → 前 5 年使用發行日利率）
    '0000', 'A', 5,
    -- 投資金額與單位
    NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    -- 日期設定
    NULL, NULL, NULL,
    NULL, NULL, NULL,
    NULL, NULL,
    -- 費率與收益
    12, NULL, NULL, NULL, NULL,
    NULL, NULL, NULL, NULL,
    -- 百分比設定
    NULL, NULL, NULL, NULL,
    -- 關聯代碼
    NULL, NULL, NULL, NULL,
    NULL, '2J0ND1', NULL
);

-- -----------------------------------------------------------------------------
-- QIRAT: 1J099 企業年金利率 (2020-2030, 2.5%)
-- -----------------------------------------------------------------------------
INSERT INTO CV.QIRAT (
    SUB_ACNT_PLAN_CODE, INT_RATE_TYPE, INT_RATE_DATE_STR, INT_RATE_DATE_END, INT_RATE
) VALUES (
    '1J099',                            -- Plan code
    '5',                                -- Interest rate type (企業年金)
    TO_DATE('2020-01-01', 'YYYY-MM-DD'), -- Start date
    TO_DATE('2030-12-31', 'YYYY-MM-DD'), -- End date
    250                                 -- Rate (2.5%, 萬分率)
);

COMMIT;

-- =============================================================================
-- 驗證測試資料
-- =============================================================================

-- 驗證 PLDF
SELECT PLAN_CODE, VERSION, INSURANCE_TYPE_3, PLAN_START_DATE, PLAN_END_DATE
FROM CV.PLDF
WHERE PLAN_CODE = '1J099' AND VERSION = '1';

-- 驗證 PLND
SELECT PLAN_CODE, VERSION, IV_TARGET_CODE, IV_PERCENT
FROM CV.PLND
WHERE PLAN_CODE = '1J099' AND VERSION = '1';

-- 驗證 QMFDE
SELECT IV_TARGET_CODE, IV_TARGET_TITLE, INT_APPLY_YR_IND, INT_APPLY_YR
FROM CV.QMFDE
WHERE IV_TARGET_CODE = '2J0ND1';

-- 驗證 QIRAT
SELECT SUB_ACNT_PLAN_CODE, INT_RATE_TYPE, INT_RATE_DATE_STR, INT_RATE_DATE_END, INT_RATE
FROM CV.QIRAT
WHERE SUB_ACNT_PLAN_CODE = '1J099' AND INT_RATE_TYPE = '5';

-- =============================================================================
-- 結束
-- =============================================================================
