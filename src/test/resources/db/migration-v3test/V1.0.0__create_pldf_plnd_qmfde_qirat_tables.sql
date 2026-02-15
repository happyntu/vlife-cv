-- =============================================================================
-- V1.0.2: PLDF, PLND, QMFDE, QIRAT 表格建立
-- 來源: V3.CV (PLDF, PLND, QIRAT) + V3.QM (QMFDE)
-- 目標: Oracle 21c+ (V4.CV Schema)
-- 用途: AnnuityRateStrategy 整合測試（Phase 2C + Phase 4）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. 建立 CV Schema（如果不存在）
-- -----------------------------------------------------------------------------
-- 注意：TestContainers 中 'cv' user 已存在，此處建立 CV schema 並授權
DECLARE
    v_count NUMBER;
BEGIN
    -- 檢查 CV user 是否存在
    SELECT COUNT(*) INTO v_count FROM all_users WHERE username = 'CV';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER CV IDENTIFIED BY cv123';
        EXECUTE IMMEDIATE 'GRANT CONNECT, RESOURCE TO CV';
        EXECUTE IMMEDIATE 'GRANT UNLIMITED TABLESPACE TO CV';
    END IF;
END;
/

-- -----------------------------------------------------------------------------
-- PLDF - Plan Definition File (險種定義檔)
-- 來源: V3.CV.PLDF (139 欄位, 3,926 筆資料)
-- 用途: 險種基本規則、費率參數、業務規則
-- -----------------------------------------------------------------------------
CREATE TABLE CV.PLDF (
    -- ==================== 主鍵 ====================
    PLAN_CODE                VARCHAR2(20)   NOT NULL,  -- 險種代碼 (PK)
    VERSION                  VARCHAR2(4)    NOT NULL,  -- 版數 (PK)

    -- ==================== 基本資訊 ====================
    PLAN_TITLE               VARCHAR2(160),            -- 險種名稱（簡稱）
    PLAN_NAME                VARCHAR2(200),            -- 險種全名
    CONTRACTED_NAME          VARCHAR2(160),            -- 契約名稱

    -- ==================== 年齡限制 ====================
    LOW_AGE                  NUMBER(5)      NOT NULL,  -- 投保最小年齡
    HIGH_AGE                 NUMBER(5)      NOT NULL,  -- 投保最大年齡
    LOW_AGE_SUB              NUMBER(5),                -- 被保險人最小年齡
    HIGH_AGE_SUB             NUMBER(5),                -- 被保險人最大年齡
    LOW_AGE_IND              VARCHAR2(4),              -- 投保最小年齡指示 (1=足歲, 2=保險年齡)
    HIGH_AGE_IND             VARCHAR2(4),              -- 投保最大年齡指示
    REN_HIGH_AGE_IND         VARCHAR2(4),              -- 續保最高年齡指示
    REN_HIGH_AGE             NUMBER(5),                -- 續保最高年齡

    -- ==================== 繳費/保障年期 ====================
    COLLECT_YEAR_IND         VARCHAR2(4)    NOT NULL,  -- 繳費年期指示 (1=固定, 2=同保障, 3=至某歲, 4=可選)
    COLLECT_YEAR             NUMBER(5),                -- 繳費年期
    EXP_YEAR_IND             VARCHAR2(4)    NOT NULL,  -- 保障年期指示 (1=固定, 2=終身, 3=至某歲, 4=可選)
    EXP_YEAR                 NUMBER(5),                -- 保障年期

    -- ==================== 上市日期 ====================
    PLAN_START_DATE          DATE           NOT NULL,  -- 上市日期
    PLAN_END_DATE            DATE           NOT NULL,  -- 停賣日期

    -- ==================== 險種分類與關係 ====================
    PLAN_RELATION            VARCHAR2(4),              -- 險種關係
    PLAN_RELATION_SUB        VARCHAR2(4),              -- 被保險人險種關係
    PRIMARY_RIDER_IND        VARCHAR2(4),              -- 主附約指示 (P=主約, R=附約)
    RIDER_IND                VARCHAR2(4),              -- 附約指示
    INSURANCE_TYPE           VARCHAR2(4),              -- 保險型態1（核保類型）
    INSURANCE_TYPE_2         VARCHAR2(4),              -- 保險型態2（再保類型）
    INSURANCE_TYPE_3         VARCHAR2(4),              -- 保險型態3（準備金類型: A=壽險, B=健康, C=傷害, F=投資壽, G=利變年金, H=企業年金）
    INSURANCE_TYPE_4         VARCHAR2(4),              -- 保險型態4
    INSURANCE_TYPE_5         VARCHAR2(4),              -- 保險型態5
    PLAN_TYPE                VARCHAR2(4),              -- 險種型態 (1=傳統, 2=投資連結)

    -- ==================== 幣別與會計 ====================
    CURRENCY_1               VARCHAR2(12)   NOT NULL,  -- 幣別 (TWD=台幣, USD=美金)
    ACNT_TYPE                VARCHAR2(8),              -- 會計類別
    PLAN_ACCOUNT_IND         VARCHAR2(4)    NOT NULL,  -- 險種帳戶指示

    -- ==================== 保費相關 ====================
    FACE_AMT_TYPE            VARCHAR2(4),              -- 保額類型
    FACE_AMT_UNIT            NUMBER(10),               -- 保額單位
    TOP_FACE_AMT             NUMBER(10),               -- 最高保額
    FACE_PREM_IND            VARCHAR2(4),              -- 保額保費指示
    PREM_CALC_TYPE           VARCHAR2(4),              -- 保費計算類型
    PREM_VALUE               NUMBER(5,4),              -- 保費值
    PREM_LACK_IND            VARCHAR2(4)    NOT NULL,  -- 保費不足指示
    UNIT_VALUE_IND           VARCHAR2(4),              -- 單位值指示
    UNIT_VALUE               FLOAT(27),                -- 單位值
    UNIT_VALUE2              FLOAT(27),                -- 單位值2
    UNIT_VALUE3              FLOAT(27),                -- 單位值3
    RISK_AMT_VALUE           NUMBER(5),                -- 風險保額值
    PLAT_IND                 VARCHAR2(4),              -- 平準化指示

    -- ==================== 費率參數 ====================
    RATE_PLAN_CODE           VARCHAR2(20),             -- 費率險種代碼
    RATE_VERSION             VARCHAR2(4),              -- 費率險種版數
    RATE_SEX_IND             VARCHAR2(4),              -- 費率性別指示
    RATE_AGE_IND             VARCHAR2(4),              -- 費率年齡指示
    RATE_SUB_1_IND           VARCHAR2(4),              -- 費率子類別1指示
    RATE_SUB_2_IND           VARCHAR2(4),              -- 費率子類別2指示
    PRAT01_SW                VARCHAR2(4),              -- PRAT01 開關

    -- ==================== 折扣設定 ====================
    DISC_TYPE                VARCHAR2(4),              -- 折扣類型
    DISC_HIGH1_START         NUMBER(10),               -- 折扣級距1起點
    DISC_PREM1_UNIT          NUMBER(5),                -- 折扣1單位保費
    DISC_PREM1_YEAR          NUMBER(5),                -- 折扣1年繳保費
    DISC_PREM1_HALF          NUMBER(5),                -- 折扣1半年繳保費
    DISC_HIGH2_START         NUMBER(10),               -- 折扣級距2起點
    DISC_PREM2_UNIT          NUMBER(5),                -- 折扣2單位保費
    DISC_PREM2_YEAR          NUMBER(5),                -- 折扣2年繳保費
    DISC_PREM2_HALF          NUMBER(5),                -- 折扣2半年繳保費

    -- ==================== 保費變更 ====================
    CHG_PREM1_IND            VARCHAR2(4),              -- 保費變更1指示
    CHG_PREM1_VALUE          NUMBER(5),                -- 保費變更1值
    CHG_PREM3_IND            VARCHAR2(4),              -- 保費變更3指示
    CHG_PREM3_VALUE          NUMBER(5),                -- 保費變更3值
    CHG_PREM3_DUR            NUMBER(5),                -- 保費變更3期間

    -- ==================== 紅利設定 ====================
    RBN_IND                  VARCHAR2(4),              -- 還本紅利指示
    SBN_IND                  VARCHAR2(4),              -- 生存紅利指示
    OTHER_SBN_IND            VARCHAR2(20),             -- 其他生存紅利指示
    ABN_IND                  VARCHAR2(4),              -- 累積紅利指示
    DIV_TYPE                 VARCHAR2(8),              -- 紅利類型 (00=不分紅, 01=分紅)
    DIV_START_YEAR           NUMBER(5),                -- 紅利起算年度
    DIV_PAY_ITEM_IND         VARCHAR2(16),             -- 紅利給付項目指示
    DIV_CALC_ITEM_IND        VARCHAR2(16),             -- 紅利計算項目指示
    CV_DIV_CODE              VARCHAR2(20),             -- CV 紅利代碼
    DIV_SW_1                 VARCHAR2(28),             -- 紅利開關1
    DIV_SW_2                 VARCHAR2(36),             -- 紅利開關2
    DIV_SW_M                 VARCHAR2(4)    NOT NULL,  -- 紅利開關M

    -- ==================== 給付/保障 ====================
    DEATH_BENEF_IND          VARCHAR2(4),              -- 身故給付指示
    SURRENDER_IND            VARCHAR2(4),              -- 解約指示
    NFO                      VARCHAR2(4),              -- NFO 指示
    LBENF                    VARCHAR2(4),              -- 生存給付指示
    MBENF                    VARCHAR2(4),              -- 滿期給付指示
    DBENF                    VARCHAR2(4),              -- 身故給付指示
    BBENF                    VARCHAR2(4),              -- 保險金給付指示
    BENEF_IND                VARCHAR2(20),             -- 受益人指示
    CO_PAY_IND               VARCHAR2(4),              -- 共同給付指示
    CO_PAY_DISC              NUMBER(5),                -- 共同給付折扣
    DEDUCTIBLE_TYPE          VARCHAR2(4),              -- 自負額類型
    DEDUCTIBLE_AMT           NUMBER(10),               -- 自負額金額

    -- ==================== 計算類型 ====================
    CSV_CALC_TYPE            VARCHAR2(12)   NOT NULL,  -- 解約金計算類型
    PUA_CALC_TYPE            VARCHAR2(16)   NOT NULL,  -- 繳清計算類型 (Paid-Up Addition)
    ETE_CALC_TYPE            VARCHAR2(24)   NOT NULL,  -- 展期計算類型 (Extended Term)
    CSV_PRINT_IND            VARCHAR2(4),              -- 解約金列印指示
    CSV_PRINT_YEAR           NUMBER(5),                -- 解約金列印年數
    UV_PRINT_IND             VARCHAR2(4),              -- UV 列印指示
    WAIV_CSV_IND             VARCHAR2(4),              -- 豁免解約金指示
    WAIVER_PREM_IND          VARCHAR2(4),              -- 豁免保費指示
    SAVE_BENEF_IND           VARCHAR2(4),              -- 節省給付指示

    -- ==================== 貸款 ====================
    LOAN_AVAL_IND            VARCHAR2(4),              -- 可貸款指示 (Y=可, N=不可)
    LOAN_PLAN_CODE           VARCHAR2(20),             -- 貸款險種代碼
    LOAN_VERSION             VARCHAR2(4),              -- 貸款險種版數
    LOAN_AVAL_PERCENT        NUMBER(5),                -- 可貸款比例

    -- ==================== 佣金 ====================
    COMM_CLASS_CODE_I        VARCHAR2(4),              -- 佣金類別碼指示
    COMM_CLASS_CODE          VARCHAR2(20),             -- 佣金類別碼
    PROD_CLASS_CODE          VARCHAR2(12),             -- 產品類別碼

    -- ==================== 關聯險種 ====================
    SR_PLAN_CODE             VARCHAR2(20),             -- SR 險種代碼
    SR_VERSION               VARCHAR2(4),              -- SR 險種版數
    DECL_PLAN_CODE           VARCHAR2(20),             -- 宣告利率險種代碼
    INT4_PLAN_CODE           VARCHAR2(20),             -- 利率4險種代碼
    UW_PLAN_CODE             VARCHAR2(20)   NOT NULL,  -- 核保險種代碼
    UW_VERSION               VARCHAR2(4)    NOT NULL,  -- 核保險種版數
    UW_IND                   VARCHAR2(4),              -- 核保指示
    UW_AGE_14_SW             VARCHAR2(4),              -- 核保14歲開關
    CV_PLAN_CODE             VARCHAR2(20),             -- CV 險種代碼（精算）
    CV_VERSION               VARCHAR2(4),              -- CV 險種版數
    PC_PLAN_CODE             VARCHAR2(20),             -- 保全規則險種代碼
    PC_VERSION               VARCHAR2(4),              -- 保全規則險種版數
    YUR_PLAN_CODE            VARCHAR2(20),             -- YUR 險種代碼
    YUR_VERSION              VARCHAR2(4),              -- YUR 險種版數
    PLAN_CODE_SYS            VARCHAR2(20),             -- 系統險種代碼

    -- ==================== 再保 ====================
    TREATY_CODE              VARCHAR2(28),             -- 再保險契約代碼
    RISK_PREM_IND            VARCHAR2(4),              -- 風險保費指示
    RISK_PREM_CODE           VARCHAR2(20),             -- 風險保費代碼

    -- ==================== 其他規則 ====================
    RULE_CODE                VARCHAR2(60),             -- 規則代碼
    REPLACE_AGE_IND          VARCHAR2(4),              -- 替代年齡指示
    REPLACE_AGE_CODE         VARCHAR2(24),             -- 替代年齡代碼
    SAL_PAY_SW               VARCHAR2(4),              -- 薪轉開關
    PREPAY_SW                VARCHAR2(4),              -- 預繳開關
    ADD_AMT_SW               VARCHAR2(4),              -- 增額開關
    MODX_IND                 VARCHAR2(20),             -- MODX 指示
    FACTOR_IND               VARCHAR2(8),              -- 因子指示

    -- ==================== 年金相關 ====================
    ANNY_SW                  VARCHAR2(4)    NOT NULL,  -- 年金開關

    -- ==================== 監理代碼 ====================
    RISK_PREM_SAS            VARCHAR2(20),             -- 風險保費 SAS
    MAT_LIFE_BEN_SAS         VARCHAR2(8),              -- 滿期生存給付 SAS
    DEATH_BEN_SAS            VARCHAR2(8),              -- 身故給付 SAS
    PA_DEATH_BEN_SAS         VARCHAR2(8),              -- PA 身故給付 SAS

    -- ==================== 持續獎勵 ====================
    PERSIST_REWARD_IND       VARCHAR2(4)    NOT NULL,  -- 持續獎勵金指示
    PERSIST_PREM_VAL         NUMBER(5)      NOT NULL,  -- 持續保費值

    -- ==================== 主鍵約束 ====================
    CONSTRAINT PK_PLDF PRIMARY KEY (PLAN_CODE, VERSION)
);

COMMENT ON TABLE CV.PLDF IS '險種定義檔 (原 V3.CV.PLDF, 139 欄位)';
COMMENT ON COLUMN CV.PLDF.PLAN_CODE IS '險種代碼 (PK)';
COMMENT ON COLUMN CV.PLDF.VERSION IS '版數 (PK, 如 1, 2, A, B)';
COMMENT ON COLUMN CV.PLDF.INSURANCE_TYPE_3 IS '保險型態3（準備金類型: A=壽險, B=健康, C=傷害, F=投資壽, G=利變年金, H=企業年金）';
COMMENT ON COLUMN CV.PLDF.PLAN_START_DATE IS '上市日期';
COMMENT ON COLUMN CV.PLDF.PLAN_END_DATE IS '停賣日期';

-- 險種查詢索引
CREATE INDEX IDX_PLDF_PLAN_CODE ON CV.PLDF(PLAN_CODE);
CREATE INDEX IDX_PLDF_INSURANCE_TYPE_3 ON CV.PLDF(INSURANCE_TYPE_3);
CREATE INDEX IDX_PLDF_START_END_DATE ON CV.PLDF(PLAN_START_DATE, PLAN_END_DATE);

-- -----------------------------------------------------------------------------
-- PLND - Plan Investment Target Mapping (產品投資標的對應)
-- 來源: V3.CV.PLND (6 欄位, 924 筆資料)
-- 用途: 產品與投資標的關聯，投資型保單使用
-- -----------------------------------------------------------------------------
CREATE TABLE CV.PLND (
    PLAN_CODE                VARCHAR2(20)   NOT NULL,  -- 險種代碼 (PK)
    VERSION                  VARCHAR2(4)    NOT NULL,  -- 版數 (PK)
    IV_TARGET_CODE           VARCHAR2(40)   NOT NULL,  -- 投資標的代碼 (PK)
    IV_PERCENT               NUMBER(5,2)    NOT NULL,  -- 投資比例（百分比）
    IV_APPL_IND              VARCHAR2(4)    NOT NULL,  -- 投資適用指示
    IVHS_CODE_C              VARCHAR2(40),             -- IVHS 代碼C

    CONSTRAINT PK_PLND PRIMARY KEY (PLAN_CODE, VERSION, IV_TARGET_CODE)
);

COMMENT ON TABLE CV.PLND IS '產品投資標的對應 (原 V3.CV.PLND)';
COMMENT ON COLUMN CV.PLND.PLAN_CODE IS '險種代碼 (PK)';
COMMENT ON COLUMN CV.PLND.VERSION IS '版數 (PK)';
COMMENT ON COLUMN CV.PLND.IV_TARGET_CODE IS '投資標的代碼 (PK)';
COMMENT ON COLUMN CV.PLND.IV_PERCENT IS '投資比例（百分比）';

-- PLND 外鍵索引
CREATE INDEX IDX_PLND_PLAN_CODE ON CV.PLND(PLAN_CODE, VERSION);
CREATE INDEX IDX_PLND_IV_TARGET ON CV.PLND(IV_TARGET_CODE);

-- -----------------------------------------------------------------------------
-- QMFDE - Investment Target Definition (投資標的定義)
-- 來源: V3.QM.QMFDE (47 欄位, 190 筆資料)
-- 用途: 投資標的基本資料，投資型保單、年金使用
-- -----------------------------------------------------------------------------
CREATE TABLE CV.QMFDE (
    -- ==================== 主鍵 ====================
    IV_TARGET_CODE           VARCHAR2(40)   NOT NULL,  -- 投資標的代碼 (PK)

    -- ==================== 基本資訊 ====================
    IV_TARGET_TITLE          VARCHAR2(120)  NOT NULL,  -- 投資標的名稱
    IV_COMPANY_CODE          VARCHAR2(24),             -- 投資公司代碼
    IV_CURRENCY              VARCHAR2(12)   NOT NULL,  -- 幣別
    IV_TYPE                  VARCHAR2(4)    NOT NULL,  -- 投資標的類型

    -- ==================== 帳戶類型 ====================
    SUB_ACNT_TYPE            VARCHAR2(4)    NOT NULL,  -- 子帳戶類型
    SUB_ACNT_PLAN_CODE       VARCHAR2(20),             -- 子帳戶險種代碼
    SUB_ACNT_CALC_TYPE       VARCHAR2(4),              -- 子帳戶計算類型
    SUB_ACNT_TYPE_2          VARCHAR2(4)    NOT NULL,  -- 子帳戶類型2

    -- ==================== 利率設定 ====================
    INT_CALC_PROC            VARCHAR2(16)   NOT NULL,  -- 利率計算程序
    INT_APPLY_YR_IND         VARCHAR2(4),              -- 利率適用年度指示（P0-001: 'A'=依保單年度, 'B'=給付後調整）
    INT_APPLY_YR             NUMBER(3),                -- 利率適用年數（P0-001: 使用發行日利率的年數）

    -- ==================== 投資金額與單位 ====================
    IV_MIN_AMT               NUMBER(10),               -- 最低投資金額
    FUND_MIN_AMT             NUMBER(10),               -- 基金最低金額
    IV_UNIT                  NUMBER(18,8),             -- 投資單位
    IV_COST_PRICE            NUMBER(10,4),             -- 投資成本價
    IV_COST_EXRT             NUMBER(10,6),             -- 投資成本匯率
    IV_COST_VAL              NUMBER(12,6),             -- 投資成本值
    INVENTORY_QTY            NUMBER(10),               -- 庫存數量

    -- ==================== 日期設定 ====================
    UPDATE_DATE              DATE,                     -- 更新日期
    START_DATE               DATE,                     -- 開始日期
    EXPIRED_DATE             DATE,                     -- 到期日期
    QMFDE_STR_DATE           DATE,                     -- QMFDE 起始日期
    QMFDE_END_DATE           DATE,                     -- QMFDE 結束日期
    IV_SALES_END_DATE        DATE,                     -- 銷售結束日期
    INVEST_2ND_DATE          DATE,                     -- 第二次投資日期
    INVEST_3TH_DATE          DATE,                     -- 第三次投資日期

    -- ==================== 費率與收益 ====================
    PRINCIPLE_FREQ           NUMBER(5)      NOT NULL,  -- 本金頻率
    MATURE_VAL_RATE          NUMBER(5),                -- 到期值比率
    IV_TARGET_YIELD          NUMBER(7,2),              -- 投資標的收益率
    PROD_RATE1               NUMBER(5,2),              -- 產品費率1
    IV_PROFIT_RATE           NUMBER(5,2),              -- 投資利潤率
    LOWEST_IVEST_REWARD      NUMBER(5,2),              -- 最低投資報酬
    FY_FIXED_REWARD          NUMBER(5,2),              -- 固定年度報酬
    LOWEST_GUARANTEE_REWARD  NUMBER(5,2),              -- 最低保證報酬
    STANDARD_REWARD          NUMBER(5,2),              -- 標準報酬

    -- ==================== 百分比設定 ====================
    IV_PERCENT_TYPE          VARCHAR2(4),              -- 投資百分比類型
    IV_PERCENT_N             NUMBER(5),                -- 投資百分比N
    IV_PERCENT_STR           NUMBER(5,2),              -- 投資百分比起始
    IV_PERCENT_END           NUMBER(5,2),              -- 投資百分比結束

    -- ==================== 關聯代碼 ====================
    BUNDLE_INV_IND           VARCHAR2(4),              -- 綑綁投資指示
    NBDT_PLAN_CODE           VARCHAR2(40),             -- NBDT 險種代碼
    PC_DUR_PLAN              VARCHAR2(40),             -- PC 期間險種
    QMFDE_ENTRY_IND          VARCHAR2(4),              -- QMFDE 進入指示
    INT_PLAN_CODE            VARCHAR2(60),             -- 利率險種代碼
    IV_STANDARD_CODE         VARCHAR2(60)   NOT NULL,  -- 投資標準代碼
    QMFDE_AMT                NUMBER(10),               -- QMFDE 金額

    -- ==================== 主鍵約束 ====================
    CONSTRAINT PK_QMFDE PRIMARY KEY (IV_TARGET_CODE)
);

COMMENT ON TABLE CV.QMFDE IS '投資標的定義 (原 V3.QM.QMFDE, 47 欄位)';
COMMENT ON COLUMN CV.QMFDE.IV_TARGET_CODE IS '投資標的代碼 (PK)';
COMMENT ON COLUMN CV.QMFDE.IV_TARGET_TITLE IS '投資標的名稱';
COMMENT ON COLUMN CV.QMFDE.INT_APPLY_YR_IND IS '利率適用年度指示（A=依保單年度, B=給付後調整）';
COMMENT ON COLUMN CV.QMFDE.INT_APPLY_YR IS '利率適用年數（使用發行日利率的年數）';

-- QMFDE 查詢索引
CREATE INDEX IDX_QMFDE_SUB_ACNT_PLAN ON CV.QMFDE(SUB_ACNT_PLAN_CODE);
CREATE INDEX IDX_QMFDE_IV_TYPE ON CV.QMFDE(IV_TYPE);

-- -----------------------------------------------------------------------------
-- QIRAT - Interest Rate Table (利率表)
-- 來源: V3.CV.QIRAT (5 欄位, 1,313 筆資料)
-- 用途: 各種利率類型的利率查詢（企業年金利率、宣告利率等）
-- -----------------------------------------------------------------------------
CREATE TABLE CV.QIRAT (
    SUB_ACNT_PLAN_CODE       VARCHAR2(20)   NOT NULL,  -- 子帳戶險種代碼 (PK)
    INT_RATE_TYPE            VARCHAR2(4)    NOT NULL,  -- 利率類型 (PK, '5'=企業年金利率)
    INT_RATE_DATE_STR        DATE           NOT NULL,  -- 利率起始日 (PK)
    INT_RATE_DATE_END        DATE           NOT NULL,  -- 利率結束日
    INT_RATE                 NUMBER(6,2)    NOT NULL,  -- 利率（萬分率, 250 = 2.5%）

    CONSTRAINT PK_QIRAT PRIMARY KEY (SUB_ACNT_PLAN_CODE, INT_RATE_TYPE, INT_RATE_DATE_STR)
);

COMMENT ON TABLE CV.QIRAT IS '利率表 (原 V3.CV.QIRAT)';
COMMENT ON COLUMN CV.QIRAT.SUB_ACNT_PLAN_CODE IS '子帳戶險種代碼 (PK, 通常為險種代碼)';
COMMENT ON COLUMN CV.QIRAT.INT_RATE_TYPE IS '利率類型 (PK, 5=企業年金利率, 8=年金利率)';
COMMENT ON COLUMN CV.QIRAT.INT_RATE_DATE_STR IS '利率起始日 (PK)';
COMMENT ON COLUMN CV.QIRAT.INT_RATE_DATE_END IS '利率結束日';
COMMENT ON COLUMN CV.QIRAT.INT_RATE IS '利率（萬分率, 250 = 2.5%）';

-- QIRAT 查詢索引（高頻查詢優化）
CREATE INDEX IDX_QIRAT_PLAN_TYPE ON CV.QIRAT(SUB_ACNT_PLAN_CODE, INT_RATE_TYPE);
CREATE INDEX IDX_QIRAT_DATE_RANGE ON CV.QIRAT(INT_RATE_DATE_STR, INT_RATE_DATE_END);

-- =============================================================================
-- 結束
-- =============================================================================
