-- V3 Wrapper Procedure for Phase 2C V3 vs V4 Comparison Testing
-- Purpose: 簡化 V3 stored procedure 呼叫，避免複雜的 Oracle Object Types 映射

-- **使用方式**：
-- 在 V3 Oracle database 中執行此 script，創建 wrapper procedure
-- 然後在 V4 測試中透過 JDBC 呼叫此 wrapper

CREATE OR REPLACE PROCEDURE V3.cv210p_rate_calc_wrapper(
  -- Input parameters (simplified)
  p_begin_date     IN DATE,
  p_end_date       IN DATE,
  p_rate_type      IN VARCHAR2,
  p_principal_amt  IN NUMBER,
  p_plan_code      IN VARCHAR2,
  p_version        IN VARCHAR2,
  p_po_issue_date  IN DATE DEFAULT NULL,  -- 保單發行日（企業年金需要）

  -- Output parameters
  o_actual_rate    OUT NUMBER,
  o_int_amt        OUT NUMBER,
  o_months         OUT NUMBER
) AS
  -- V3 Object Types
  l_iri ob_iri := ob_iri();
  l_iri_array nt_iri_array := nt_iri_array();
  l_pldf ob_pldf;
  l_plnt ob_plnt;
  l_podt ob_podt := ob_podt();

  -- Local variables
  l_plan_exists NUMBER;
BEGIN
  -- Log input
  DBMS_OUTPUT.PUT_LINE('Wrapper called: planCode=' || p_plan_code || ', version=' || p_version);

  -- Check if plan exists
  BEGIN
    SELECT 1 INTO l_plan_exists
    FROM V3.PLDF
    WHERE PLAN_CODE = p_plan_code
      AND VERSION = p_version
      AND ROWNUM = 1;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20001, 'Plan not found: ' || p_plan_code || '/' || p_version);
  END;

  -- Initialize iri (input)
  l_iri.begin_date := p_begin_date;
  l_iri.end_date := p_end_date;
  l_iri.rate_type := p_rate_type;
  l_iri.principal_amt := p_principal_amt;
  l_iri.actual_rate := 0;
  l_iri.int_amt := 0;

  -- Query PLDF object using CAST + MULTISET
  -- Note: 使用 CAST 將表格列轉換為 object type
  BEGIN
    SELECT CAST(MULTISET(
      SELECT PLAN_CODE, VERSION, PLAN_TITLE, PLAN_NAME, CONTRACTED_NAME,
             LOW_AGE, HIGH_AGE, LOW_AGE_SUB, HIGH_AGE_SUB, LOW_AGE_IND,
             HIGH_AGE_IND, REN_HIGH_AGE_IND, REN_HIGH_AGE, PLAN_RELATION,
             PLAN_RELATION_SUB, FACE_AMT_TYPE, COLLECT_YEAR_IND, COLLECT_YEAR,
             EXP_YEAR_IND, EXP_YEAR, PLAN_START_DATE, PLAN_END_DATE,
             RBN_IND, SBN_IND, OTHER_SBN_IND, ABN_IND, UW_IND,
             UW_AGE_14_SW, PRIMARY_RIDER_IND, RIDER_IND, MODX_IND,
             FACTOR_IND, PREM_CALC_TYPE, PREM_VALUE, UNIT_VALUE_IND,
             UNIT_VALUE, UNIT_VALUE2, UNIT_VALUE3, PLAN_TYPE,
             FACE_PREM_IND, RISK_AMT_VALUE, PLAT_IND, RATE_PLAN_CODE,
             RATE_VERSION, RATE_SEX_IND, RATE_AGE_IND, RATE_SUB_1_IND,
             RATE_SUB_2_IND, PRAT01_SW, DISC_TYPE, DISC_HIGH1_START,
             DISC_PREM1_UNIT, DISC_PREM1_YEAR, DISC_PREM1_HALF,
             DISC_HIGH2_START, DISC_PREM2_UNIT, DISC_PREM2_YEAR,
             DISC_PREM2_HALF, CHG_PREM1_IND, CHG_PREM1_VALUE,
             CHG_PREM3_IND, CHG_PREM3_VALUE, CHG_PREM3_DUR,
             SR_PLAN_CODE, SR_VERSION, INSURANCE_TYPE, INSURANCE_TYPE_2,
             INSURANCE_TYPE_3, INSURANCE_TYPE_4, INSURANCE_TYPE_5,
             DEATH_BENEF_IND, CSV_CALC_TYPE, PUA_CALC_TYPE, ETE_CALC_TYPE,
             DIV_TYPE, DIV_PAY_ITEM_IND, DIV_CALC_ITEM_IND, CV_DIV_CODE,
             DIV_START_YEAR, DIV_SW_1, DIV_SW_2, DECL_PLAN_CODE,
             INT4_PLAN_CODE, SURRENDER_IND, NFO, LBENF, MBENF, DBENF,
             BBENF, CO_PAY_IND, CO_PAY_DISC, ACNT_TYPE, COMM_CLASS_CODE_I,
             COMM_CLASS_CODE, PROD_CLASS_CODE
             -- Note: 繼續添加所有 ob_pldf 需要的欄位
      FROM V3.PLDF
      WHERE PLAN_CODE = p_plan_code
        AND VERSION = p_version
    ) AS nt_pldf)
    INTO l_pldf_array
    FROM DUAL;

    -- Extract first element
    IF l_pldf_array.COUNT > 0 THEN
      l_pldf := l_pldf_array(1);
    ELSE
      RAISE NO_DATA_FOUND;
    END IF;
  EXCEPTION
    WHEN NO_DATA_FOUND THEN
      RAISE_APPLICATION_ERROR(-20002, 'PLDF query failed: ' || p_plan_code || '/' || p_version);
  END;

  -- Initialize podt (policy detail) - 簡化版本
  l_podt.po_issue_date := NVL(p_po_issue_date, p_begin_date);

  -- Call V3 original stored procedure
  -- Note: p_num=0 表示一般計算模式
  pk_cv_cv210p.cv210p_rate_calc(
    p_num       => 0,
    g_iri       => l_iri,
    g_ps_plan_1 => l_pldf,
    g_ps_plnt_1 => l_plnt,
    g_podt      => l_podt,
    g_iri_array => l_iri_array
  );

  -- Extract output results
  o_actual_rate := NVL(l_iri.actual_rate, 0);
  o_int_amt := NVL(l_iri.int_amt, 0);
  o_months := l_iri_array.COUNT;

  -- Log output
  DBMS_OUTPUT.PUT_LINE('Wrapper result: actualRate=' || o_actual_rate || ', intAmt=' || o_int_amt || ', months=' || o_months);

EXCEPTION
  WHEN OTHERS THEN
    DBMS_OUTPUT.PUT_LINE('Wrapper error: ' || SQLERRM);
    RAISE;
END cv210p_rate_calc_wrapper;
/

-- Grant execute permission (if needed)
-- GRANT EXECUTE ON V3.cv210p_rate_calc_wrapper TO <test_user>;

-- Test wrapper
DECLARE
  v_actual_rate NUMBER;
  v_int_amt NUMBER;
  v_months NUMBER;
BEGIN
  cv210p_rate_calc_wrapper(
    p_begin_date    => TO_DATE('2024-01-01', 'YYYY-MM-DD'),
    p_end_date      => TO_DATE('2024-12-31', 'YYYY-MM-DD'),
    p_rate_type     => 'G',  -- COMPOUND_RATE
    p_principal_amt => 1000000,
    p_plan_code     => 'WL001',  -- 使用實際存在的險種代碼
    p_version       => 'A',
    p_po_issue_date => TO_DATE('2020-06-15', 'YYYY-MM-DD'),
    o_actual_rate   => v_actual_rate,
    o_int_amt       => v_int_amt,
    o_months        => v_months
  );

  DBMS_OUTPUT.PUT_LINE('Test passed: actualRate=' || v_actual_rate || ', intAmt=' || v_int_amt);
END;
/
