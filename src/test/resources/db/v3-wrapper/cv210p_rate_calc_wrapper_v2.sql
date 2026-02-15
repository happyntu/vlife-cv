-- V3 Wrapper Procedure for Phase 2C (Simplified Version)
-- Purpose: 簡化 V3 stored procedure 呼叫，避免複雜的 Oracle Object Types 映射
-- Strategy: 不完整調用 pk_cv_cv210p，僅用於測試連通性

CREATE OR REPLACE PROCEDURE V3.cv210p_wrapper_test(
  -- Input parameters
  p_begin_date     IN DATE,
  p_end_date       IN DATE,
  p_rate_type      IN VARCHAR2,
  p_principal_amt  IN NUMBER,
  p_plan_code      IN VARCHAR2,
  p_version        IN VARCHAR2,

  -- Output parameters
  o_actual_rate    OUT NUMBER,
  o_int_amt        OUT NUMBER,
  o_status         OUT VARCHAR2
) AS
  -- Check variables
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
      o_status := 'PLAN_NOT_FOUND';
      o_actual_rate := 0;
      o_int_amt := 0;
      RETURN;
  END;

  -- Placeholder: 實際應呼叫 pk_cv_cv210p.cv210p_rate_calc
  -- 當前版本僅返回測試數據
  o_status := 'TEST_MODE';
  o_actual_rate := 250;  -- 2.5% test rate
  o_int_amt := p_principal_amt * 250 / 10000;  -- Simple interest calculation

  -- Log output
  DBMS_OUTPUT.PUT_LINE('Wrapper result: status=' || o_status || ', actualRate=' || o_actual_rate || ', intAmt=' || o_int_amt);

EXCEPTION
  WHEN OTHERS THEN
    o_status := 'ERROR: ' || SQLERRM;
    o_actual_rate := 0;
    o_int_amt := 0;
    DBMS_OUTPUT.PUT_LINE('Wrapper error: ' || SQLERRM);
END cv210p_wrapper_test;
/

-- Test the wrapper
DECLARE
  v_actual_rate NUMBER;
  v_int_amt NUMBER;
  v_status VARCHAR2(200);
BEGIN
  DBMS_OUTPUT.PUT_LINE('Testing wrapper procedure...');

  cv210p_wrapper_test(
    p_begin_date    => TO_DATE('2024-01-01', 'YYYY-MM-DD'),
    p_end_date      => TO_DATE('2024-12-31', 'YYYY-MM-DD'),
    p_rate_type     => 'G',
    p_principal_amt => 1000000,
    p_plan_code     => '10A06',  -- From V3 PLDF first row
    p_version       => 'A',
    o_actual_rate   => v_actual_rate,
    o_int_amt       => v_int_amt,
    o_status        => v_status
  );

  DBMS_OUTPUT.PUT_LINE('Test result: status=' || v_status || ', actualRate=' || v_actual_rate || ', intAmt=' || v_int_amt);

  IF v_status = 'TEST_MODE' THEN
    DBMS_OUTPUT.PUT_LINE('SUCCESS: Wrapper procedure works!');
  ELSE
    DBMS_OUTPUT.PUT_LINE('FAILED: status=' || v_status);
  END IF;
END;
/
