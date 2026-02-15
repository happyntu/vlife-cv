-- V3 QIRAT Test Data Insertion for Phase 2C Integration Testing
-- Purpose: 提供 AnnuityRateStrategy V3 vs V4 比對測試所需的費率資料
-- Target Plan: 1J099 (insurance_type_3='G', 利變年金)

-- Delete existing test data (if any)
DELETE FROM V3.QIRAT
WHERE SUB_ACNT_PLAN_CODE = '1J099'
  AND INT_RATE_TYPE = '5';

-- Insert test rate data for plan 1J099
-- INT_RATE_TYPE='5': 企業年金利率（AnnuityRateStrategy 使用）
-- Rate period: 2020-01-01 ~ 2030-12-31 (涵蓋測試期間)
-- INT_RATE=250: 2.5% 利率（萬分率 250 = 2.5%）
INSERT INTO V3.QIRAT (
    SUB_ACNT_PLAN_CODE,
    INT_RATE_TYPE,
    INT_RATE_DATE_STR,
    INT_RATE_DATE_END,
    INT_RATE
) VALUES (
    '1J099',                            -- Plan code
    '5',                                -- Interest rate type (企業年金)
    TO_DATE('2020-01-01', 'YYYY-MM-DD'), -- Start date
    TO_DATE('2030-12-31', 'YYYY-MM-DD'), -- End date
    250                                 -- Rate (2.5%, 萬分率)
);

COMMIT;

-- Verify insertion
SELECT SUB_ACNT_PLAN_CODE, INT_RATE_TYPE, INT_RATE_DATE_STR, INT_RATE_DATE_END, INT_RATE
FROM V3.QIRAT
WHERE SUB_ACNT_PLAN_CODE = '1J099'
  AND INT_RATE_TYPE = '5';
