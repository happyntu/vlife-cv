-- =============================================================================
-- V1.0.1: PRAT 費率表
-- 來源: V3.CV.PRAT
-- 目標: Oracle 21c+
-- =============================================================================

-- -----------------------------------------------------------------------------
-- PRAT - Plan Rate (費率表)
-- -----------------------------------------------------------------------------
CREATE TABLE PRAT (
    PLAN_CODE         VARCHAR2(20)   NOT NULL,  -- 險種代碼 (PK, V3: 5 chars, V4: 20 chars for future use)
    VERSION           VARCHAR2(4)    NOT NULL,  -- 版本號 (PK, V3: 1 char, V4: 4 chars for future use)
    RATE_SEX          VARCHAR2(4)    NOT NULL,  -- 費率性別 (PK, 0=無性別區分, 1=男, 2=女)
    RATE_SUB_1        VARCHAR2(8)    NOT NULL,  -- 費率子鍵1 (PK, e.g., 吸菸狀況)
    RATE_SUB_2        VARCHAR2(12)   NOT NULL,  -- 費率子鍵2 (PK, e.g., 職業等級)
    RATE_AGE          NUMBER(5)      NOT NULL,  -- 費率年齡 (PK)
    ANNUAL_PREM       NUMBER(9,3)    NOT NULL,  -- 年繳保費（每萬元保額）
    ANNUAL_PREM2      NUMBER(9,3),              -- 第二年繳保費
    EMPLOYEE_DISC     NUMBER(7,4),              -- 員工折扣率（百分比）
    LOADING_RATE2     NUMBER(5,2),              -- 附加費率2

    CONSTRAINT PK_PRAT PRIMARY KEY (PLAN_CODE, VERSION, RATE_SEX, RATE_SUB_1, RATE_SUB_2, RATE_AGE)
);

COMMENT ON TABLE PRAT IS '費率表 (原 V3.CV.PRAT)';
COMMENT ON COLUMN PRAT.PLAN_CODE IS '險種代碼 (PK)';
COMMENT ON COLUMN PRAT.VERSION IS '版本號 (PK)';
COMMENT ON COLUMN PRAT.RATE_SEX IS '費率性別 (PK, 0=無性別區分, 1=男, 2=女)';
COMMENT ON COLUMN PRAT.RATE_SUB_1 IS '費率子鍵1 (PK, 如吸菸狀況)';
COMMENT ON COLUMN PRAT.RATE_SUB_2 IS '費率子鍵2 (PK, 如職業等級)';
COMMENT ON COLUMN PRAT.RATE_AGE IS '費率年齡 (PK)';
COMMENT ON COLUMN PRAT.ANNUAL_PREM IS '年繳保費（每萬元保額）';
COMMENT ON COLUMN PRAT.ANNUAL_PREM2 IS '第二年繳保費';
COMMENT ON COLUMN PRAT.EMPLOYEE_DISC IS '員工折扣率（百分比）';
COMMENT ON COLUMN PRAT.LOADING_RATE2 IS '附加費率2';

-- 費率查詢索引
CREATE INDEX IDX_PRAT_PLAN_CODE ON PRAT(PLAN_CODE);
CREATE INDEX IDX_PRAT_PLAN_VERSION ON PRAT(PLAN_CODE, VERSION);
