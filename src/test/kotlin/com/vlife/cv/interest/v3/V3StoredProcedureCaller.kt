package com.vlife.cv.interest.v3

import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.MonthlyRateDetail
import mu.KotlinLogging
import oracle.jdbc.OracleTypes
import java.math.BigDecimal
import java.sql.CallableStatement
import java.sql.Connection
import java.time.LocalDate
import javax.sql.DataSource

/**
 * V3 Stored Procedure Caller
 *
 * **用途**：呼叫 V3 Oracle PL/SQL stored procedure 取得原生計算結果，用於 V3 vs V4 比對測試。
 *
 * **主要功能**：
 * - 呼叫 pk_cv_cv210p.cv210p_rate_calc
 * - 處理 Oracle Object Types (ob_iri, nt_iri_array)
 * - 映射 V3 結果至 V4 資料結構
 *
 * **V3 Stored Procedure 簽章**：
 * ```sql
 * PROCEDURE cv210p_rate_calc(
 *   p_num       IN SMALLINT,
 *   g_iri       IN OUT NOCOPY ob_iri,
 *   g_ps_plan_1 IN ob_pldf,
 *   g_ps_plnt_1 IN ob_plnt,
 *   g_podt      IN ob_podt,
 *   g_iri_array IN OUT NOCOPY nt_iri_array
 * );
 * ```
 *
 * **Oracle Object Type 定義**：
 * - ob_iri: BEGIN_DATE, END_DATE, RATE_TYPE, ACTUAL_RATE, PRINCIPAL_AMT, INT_AMT, etc.
 * - rec_iri_array: str_date, end_date, month, days, i_rate, int_amt, principal_amt
 * - nt_iri_array: TABLE OF rec_iri_array
 *
 * **Phase 2C 使用方式**：
 * ```kotlin
 * val v3Result = caller.callCv210pRateCalc(input, planCode, version)
 * val v4Result = annuityRateStrategy.calculate(input, ...)
 * assertEquals(v3Result.intAmt, v4Result.intAmt, tolerance)
 * ```
 *
 * @see com.vlife.cv.interest.strategy.AnnuityRateStrategy
 * @see AnnuityRateStrategyV3ComparisonTest
 */
class V3StoredProcedureCaller(
    private val v3DataSource: DataSource
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 呼叫 V3 pk_cv_cv210p.cv210p_rate_calc stored procedure
     *
     * **實作策略**：
     * 1. Phase 2C-1：使用簡化版本，直接在 V3 schema 中創建臨時 wrapper procedure
     * 2. Phase 2C-2：完整實作 Oracle Object Types 映射（未來）
     *
     * **當前實作限制**：
     * - 需要 V3 database 有 wrapper procedure（見下方 SQL）
     * - 尚未處理完整的 ob_pldf, ob_plnt, ob_podt 參數
     * - 假設輸入參數已簡化
     *
     * **V3 Wrapper Procedure**（需在 V3 database 執行）：
     * ```sql
     * CREATE OR REPLACE PROCEDURE cv210p_rate_calc_wrapper(
     *   p_begin_date     IN DATE,
     *   p_end_date       IN DATE,
     *   p_rate_type      IN VARCHAR2,
     *   p_principal_amt  IN NUMBER,
     *   p_plan_code      IN VARCHAR2,
     *   p_version        IN VARCHAR2,
     *   o_actual_rate    OUT NUMBER,
     *   o_int_amt        OUT NUMBER
     * ) AS
     *   l_iri ob_iri := ob_iri();
     *   l_iri_array nt_iri_array := nt_iri_array();
     *   l_pldf ob_pldf;
     *   l_plnt ob_plnt;
     *   l_podt ob_podt := ob_podt();
     * BEGIN
     *   -- Initialize iri
     *   l_iri.begin_date := p_begin_date;
     *   l_iri.end_date := p_end_date;
     *   l_iri.rate_type := p_rate_type;
     *   l_iri.principal_amt := p_principal_amt;
     *
     *   -- Query PLDF
     *   SELECT VALUE(p) INTO l_pldf
     *   FROM TABLE(pk_lib_planproc.get_pldf(p_plan_code, p_version)) p
     *   WHERE ROWNUM = 1;
     *
     *   -- Call original procedure
     *   pk_cv_cv210p.cv210p_rate_calc(0, l_iri, l_pldf, l_plnt, l_podt, l_iri_array);
     *
     *   -- Return results
     *   o_actual_rate := l_iri.actual_rate;
     *   o_int_amt := l_iri.int_amt;
     * END;
     * /
     * ```
     *
     * @param input V4 InterestRateInput
     * @param planCode 險種代碼
     * @param version 險種版本
     * @return V3 計算結果（映射至 V4 資料結構）
     */
    fun callCv210pRateCalc(
        input: InterestRateInput,
        planCode: String,
        version: String
    ): InterestRateCalculationResult {
        return v3DataSource.connection.use { connection ->
            callCv210pRateCalcInternal(connection, input, planCode, version)
        }
    }

    private fun callCv210pRateCalcInternal(
        connection: Connection,
        input: InterestRateInput,
        planCode: String,
        version: String
    ): InterestRateCalculationResult {
        logger.info { "Calling V3 stored procedure: cv210p_wrapper_test(planCode=$planCode, version=$version)" }

        // CallableStatement: {call cv210p_wrapper_test(?, ?, ?, ?, ?, ?, ?, ?, ?)}
        // Parameters: 6 IN + 3 OUT (actual_rate, int_amt, status)
        val sql = "{call V3.cv210p_wrapper_test(?, ?, ?, ?, ?, ?, ?, ?, ?)}"

        connection.prepareCall(sql).use { stmt ->
            // IN parameters
            stmt.setDate(1, java.sql.Date.valueOf(input.beginDate!!))
            stmt.setDate(2, java.sql.Date.valueOf(input.endDate!!))
            stmt.setString(3, input.rateType?.code ?: "G")
            stmt.setBigDecimal(4, input.principalAmt)
            stmt.setString(5, planCode)
            stmt.setString(6, version)

            // OUT parameters
            stmt.registerOutParameter(7, OracleTypes.NUMBER)  // o_actual_rate
            stmt.registerOutParameter(8, OracleTypes.NUMBER)  // o_int_amt
            stmt.registerOutParameter(9, OracleTypes.VARCHAR)  // o_status

            // Execute
            stmt.execute()

            // Extract results
            val actualRate = stmt.getBigDecimal(7) ?: BigDecimal.ZERO
            val intAmt = stmt.getBigDecimal(8) ?: BigDecimal.ZERO
            val status = stmt.getString(9) ?: "UNKNOWN"

            logger.info { "V3 result: status=$status, actualRate=$actualRate, intAmt=$intAmt" }

            // 映射至 V4 資料結構
            return InterestRateCalculationResult(
                actualRate = actualRate,
                intAmt = intAmt,
                monthlyDetails = emptyList()  // 簡化版本不返回月度明細
            )
        }
    }

    /**
     * 完整版本（未來實作）：處理 Oracle Object Types
     *
     * **挑戰**：
     * - Oracle Object Types 需要使用 oracle.sql.STRUCT
     * - 需要 TypeDescriptor 來描述 Object Type 結構
     * - 需要處理 nested types (nt_iri_array)
     *
     * **參考資料**：
     * - Oracle JDBC Developer's Guide: Working with Oracle Object Types
     * - oracle.sql.STRUCT, oracle.sql.ARRAY
     */
    @Suppress("unused")
    private fun callCv210pRateCalcFull(
        connection: Connection,
        input: InterestRateInput,
        planCode: String,
        version: String
    ): InterestRateCalculationResult {
        // TODO: 實作完整版本（Phase 2C-2）
        throw NotImplementedError("Full Oracle Object Types mapping not implemented yet")
    }
}
