-- =============================================================================
-- TestContainers Oracle 初始化腳本
-- 建立 CV Schema 並授權給 vlife 使用者
-- =============================================================================

-- 建立 CV 使用者 (Schema)
CREATE USER CV IDENTIFIED BY cv123
    DEFAULT TABLESPACE USERS
    QUOTA UNLIMITED ON USERS;

GRANT CONNECT TO CV;
GRANT RESOURCE TO CV;

-- 在 CV Schema 下建立表格
-- -----------------------------------------------------------------------------
-- 1. 佣金率表 (CRAT)
-- -----------------------------------------------------------------------------
CREATE TABLE CV.CRAT (
    CRAT_SERIAL       NUMBER(10)     NOT NULL,
    COMM_CLASS_CODE   VARCHAR2(10)   NOT NULL,
    COMM_LINE_CODE    VARCHAR2(10)   NOT NULL,
    CRAT_TYPE         VARCHAR2(2),
    PROJ_NO           VARCHAR2(10),
    STR_DATE          DATE           NOT NULL,
    END_DATE          DATE           NOT NULL,
    CRAT_KEY1         VARCHAR2(10),
    CRAT_KEY2         VARCHAR2(10),
    COMM_START_YEAR   NUMBER(5),
    COMM_END_YEAR     NUMBER(5),
    COMM_START_AGE    NUMBER(5),
    COMM_END_AGE      NUMBER(5),
    COMM_START_MODX   NUMBER(5),
    COMM_END_MODX     NUMBER(5),
    COMM_RATE         NUMBER(10,6)   NOT NULL,
    COMM_RATE_ORG     NUMBER(10,6),
    PREM_LIMIT_STR    NUMBER(15,2),
    PREM_LIMIT_END    NUMBER(15,2),
    CONSTRAINT PK_CRAT PRIMARY KEY (CRAT_SERIAL)
);

CREATE INDEX CV.IDX_CRAT_CLASS_CODE ON CV.CRAT(COMM_CLASS_CODE);
CREATE INDEX CV.IDX_CRAT_LINE_CODE ON CV.CRAT(COMM_LINE_CODE);

-- -----------------------------------------------------------------------------
-- 2. 承保範圍表 (CVCO)
-- -----------------------------------------------------------------------------
CREATE TABLE CV.CVCO (
    POLICY_NO         VARCHAR2(10)   NOT NULL,
    COVERAGE_NO       NUMBER(5)      NOT NULL,
    PLAN_CODE         VARCHAR2(5)    NOT NULL,
    VERSION           VARCHAR2(3)    NOT NULL,
    RATE_SEX          VARCHAR2(1)    NOT NULL,
    RATE_AGE          NUMBER(5)      NOT NULL,
    RATE_SUB_1        VARCHAR2(10)   NOT NULL,
    RATE_SUB_2        VARCHAR2(10)   NOT NULL,
    CO_ISSUE_DATE     DATE           NOT NULL,
    CO_STATUS_CODE    VARCHAR2(1)    NOT NULL,
    INSURANCE_TYPE_3  VARCHAR2(1)    NOT NULL,
    PROCESS_DATE      DATE           NOT NULL,
    PROCESS_TYPE      VARCHAR2(2)    NOT NULL,
    POLICY_TYPE       VARCHAR2(1),
    CO_STATUS_CODE2   VARCHAR2(1),
    CONSTRAINT PK_CVCO PRIMARY KEY (POLICY_NO, COVERAGE_NO)
);

CREATE INDEX CV.IDX_CVCO_PLAN_CODE ON CV.CVCO(PLAN_CODE);
CREATE INDEX CV.IDX_CVCO_STATUS ON CV.CVCO(CO_STATUS_CODE);

-- -----------------------------------------------------------------------------
-- 3. 產品單位表 (CVPU)
-- -----------------------------------------------------------------------------
CREATE TABLE CV.CVPU (
    POLICY_NO          VARCHAR2(10)   NOT NULL,
    COVERAGE_NO        NUMBER(5)      NOT NULL,
    PS06_TYPE          VARCHAR2(1)    NOT NULL,
    CVPU_TYPE          VARCHAR2(1)    NOT NULL,
    LAST_ANNIV_DUR     NUMBER(5)      NOT NULL,
    CVPU_STATUS_CODE   VARCHAR2(1),
    DIV_DECLARE        NUMBER(15,2)   DEFAULT 0,
    DIV_PUA_AMT        NUMBER(15,2)   DEFAULT 0,
    FINANCIAL_DATE     DATE,
    PCPO_NO            VARCHAR2(10),
    PROGRAM_ID         VARCHAR2(10),
    PROCESS_DATE       DATE,
    POLICY_TYPE        VARCHAR2(1),
    CVPU_APPROVED_DATE DATE,
    PROGRAM_ID_CVPU    VARCHAR2(10),
    CONSTRAINT PK_CVPU PRIMARY KEY (POLICY_NO, COVERAGE_NO, PS06_TYPE, CVPU_TYPE, LAST_ANNIV_DUR)
);

CREATE INDEX CV.IDX_CVPU_COVERAGE ON CV.CVPU(POLICY_NO, COVERAGE_NO);

-- -----------------------------------------------------------------------------
-- 4. 紅利分配水準表 (CVDI)
-- -----------------------------------------------------------------------------
CREATE TABLE CV.CVDI (
    PLAN_CODE         VARCHAR2(5)    NOT NULL,
    VERSION           VARCHAR2(1)    NOT NULL,
    PAID_STATUS       VARCHAR2(2)    NOT NULL,
    RATE_SEX          VARCHAR2(1)    NOT NULL,
    AGE_LIMIT_STR     NUMBER(5)      NOT NULL,
    AGE_LIMIT_END     NUMBER(5)      NOT NULL,
    FACE_AMT_STR      NUMBER(10)     NOT NULL,
    FACE_AMT_END      NUMBER(10)     NOT NULL,
    MODE_PREM_S       NUMBER(12,2)   NOT NULL,
    MODE_PREM_E       NUMBER(12,2)   NOT NULL,
    POLICY_YEAR       NUMBER(5)      NOT NULL,
    DECL_DATE         DATE           NOT NULL,
    RATE_RATIO        NUMBER(7,6),
    DEATH_RATIO       NUMBER(7,6),
    LOADING_RATIO     NUMBER(7,6),
    REWARD_RATIO      NUMBER(7,6),
    DEATH_1_RATIO     NUMBER(7,6),
    DEATH_2_RATIO     NUMBER(7,6),
    RATIO_FEE         NUMBER(8,6),
    FIX_FEE           NUMBER(10,4),
    DET_BIR_RATE      NUMBER(7,6),
    CONFIRM_FLAG      VARCHAR2(1),
    CONFIRM_OPER      VARCHAR2(8),
    CONFIRM_DATE      DATE,
    AVERAGE_DISCOUNT  NUMBER(7,6),
    CONSTRAINT PK_CVDI PRIMARY KEY (PLAN_CODE, VERSION, PAID_STATUS, RATE_SEX,
        AGE_LIMIT_STR, AGE_LIMIT_END, FACE_AMT_STR, FACE_AMT_END,
        MODE_PREM_S, MODE_PREM_E, POLICY_YEAR, DECL_DATE)
);

CREATE INDEX CV.IDX_CVDI_PLAN ON CV.CVDI(PLAN_CODE, VERSION);

-- -----------------------------------------------------------------------------
-- 5. 準備金因子表 (CVRF)
-- -----------------------------------------------------------------------------
CREATE TABLE CV.CVRF (
    PLAN_CODE         VARCHAR2(5)    NOT NULL,
    VERSION           VARCHAR2(1)    NOT NULL,
    DUR_TYPE          NUMBER(5)      NOT NULL,
    DUR_YEAR          NUMBER(5),
    COLL_YEAR         NUMBER(5),
    PAY_MODE          NUMBER(5),
    INSURED_TYPE      NUMBER(5),
    PO_RVF_DEATH      NUMBER(5),
    PO_RVF_RATE       NUMBER(5,2),
    RVF_DEATH         NUMBER(5),
    RVF_RATE          NUMBER(5,2),
    PO_RVF_TSO        VARCHAR2(5),
    RVF_TSO           VARCHAR2(5),
    ET_RVF_RATE       NUMBER(5,2),
    ET_RVF_TSO_IND    VARCHAR2(3),
    RBN_TYPE          VARCHAR2(1),
    SBN_TYPE          VARCHAR2(1),
    I2_6_OR_NOT       VARCHAR2(1),
    I2_6_PRAT         NUMBER(5,2),
    ACCI_OR_NOT       VARCHAR2(1),
    ACCI_PRAT         NUMBER(5,2),
    RET_PREM_IND      VARCHAR2(1),
    RETURN_INT_TYPE   VARCHAR2(1),
    RETURN_INT        NUMBER(5,2),
    RETURN_COST_F     VARCHAR2(1)    NOT NULL,
    MODIFY_RV_IND     VARCHAR2(1)    NOT NULL,
    RECORD_TYPE_10    VARCHAR2(10)   NOT NULL,
    MIX_RVF_IND       VARCHAR2(3)    NOT NULL,
    RET_PREM_IND2     VARCHAR2(1),
    ET_PO_RVF_RATE    NUMBER(5,2),
    ET_PO_RVF_TSO_IND VARCHAR2(3),
    ET_ACCI_IND       VARCHAR2(1),
    ET_ACCI_RATE      NUMBER(10,9),
    CONSTRAINT PK_CVRF PRIMARY KEY (PLAN_CODE, VERSION, DUR_TYPE)
);

CREATE INDEX CV.IDX_CVRF_PLAN ON CV.CVRF(PLAN_CODE, VERSION);

-- 授予 vlife 使用者存取 CV Schema 表格的權限
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CRAT TO vlife;
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CVCO TO vlife;
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CVPU TO vlife;
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CVDI TO vlife;
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CVRF TO vlife;
