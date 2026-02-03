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

-- 授予 vlife 使用者存取 CV Schema 表格的權限
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CRAT TO vlife;
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CVCO TO vlife;
GRANT SELECT, INSERT, UPDATE, DELETE ON CV.CVPU TO vlife;
