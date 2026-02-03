package com.vlife.cv.commission

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import java.time.LocalDate

/**
 * 佣金率 MyBatis Mapper
 *
 * 遵循 ADR-009 規範，SQL 必須明確指定 CV Schema。
 */
@Mapper
interface CommissionRateMapper {

    /**
     * 依序號查詢佣金率
     *
     * @param serial 序號 (主鍵)
     * @return 佣金率資料，不存在時回傳 null
     */
    @Select("""
        SELECT
            CRAT_SERIAL as serial,
            COMM_CLASS_CODE as commClassCode,
            COMM_LINE_CODE as commLineCode,
            CRAT_TYPE as cratType,
            PROJ_NO as projectNo,
            STR_DATE as startDate,
            END_DATE as endDate,
            CRAT_KEY1 as cratKey1,
            CRAT_KEY2 as cratKey2,
            COMM_START_YEAR as commStartYear,
            COMM_END_YEAR as commEndYear,
            COMM_START_AGE as commStartAge,
            COMM_END_AGE as commEndAge,
            COMM_START_MODX as commStartModx,
            COMM_END_MODX as commEndModx,
            COMM_RATE as commRate,
            COMM_RATE_ORG as commRateOrg,
            PREM_LIMIT_STR as premLimitStart,
            PREM_LIMIT_END as premLimitEnd
        FROM CV.CRAT
        WHERE CRAT_SERIAL = #{serial}
    """)
    fun findBySerial(@Param("serial") serial: Long): CommissionRate?

    /**
     * 依佣金類別碼查詢佣金率清單
     *
     * @param commClassCode 佣金率類別碼
     * @return 佣金率清單
     */
    @Select("""
        SELECT
            CRAT_SERIAL as serial,
            COMM_CLASS_CODE as commClassCode,
            COMM_LINE_CODE as commLineCode,
            CRAT_TYPE as cratType,
            PROJ_NO as projectNo,
            STR_DATE as startDate,
            END_DATE as endDate,
            CRAT_KEY1 as cratKey1,
            CRAT_KEY2 as cratKey2,
            COMM_START_YEAR as commStartYear,
            COMM_END_YEAR as commEndYear,
            COMM_START_AGE as commStartAge,
            COMM_END_AGE as commEndAge,
            COMM_START_MODX as commStartModx,
            COMM_END_MODX as commEndModx,
            COMM_RATE as commRate,
            COMM_RATE_ORG as commRateOrg,
            PREM_LIMIT_STR as premLimitStart,
            PREM_LIMIT_END as premLimitEnd
        FROM CV.CRAT
        WHERE COMM_CLASS_CODE = #{commClassCode}
        ORDER BY STR_DATE DESC, CRAT_SERIAL
    """)
    fun findByClassCode(@Param("commClassCode") commClassCode: String): List<CommissionRate>

    /**
     * 查詢指定日期有效的佣金率
     *
     * @param commClassCode 佣金率類別碼
     * @param commLineCode 業務線代號
     * @param effectiveDate 生效日期
     * @return 有效的佣金率清單
     */
    @Select("""
        SELECT
            CRAT_SERIAL as serial,
            COMM_CLASS_CODE as commClassCode,
            COMM_LINE_CODE as commLineCode,
            CRAT_TYPE as cratType,
            PROJ_NO as projectNo,
            STR_DATE as startDate,
            END_DATE as endDate,
            CRAT_KEY1 as cratKey1,
            CRAT_KEY2 as cratKey2,
            COMM_START_YEAR as commStartYear,
            COMM_END_YEAR as commEndYear,
            COMM_START_AGE as commStartAge,
            COMM_END_AGE as commEndAge,
            COMM_START_MODX as commStartModx,
            COMM_END_MODX as commEndModx,
            COMM_RATE as commRate,
            COMM_RATE_ORG as commRateOrg,
            PREM_LIMIT_STR as premLimitStart,
            PREM_LIMIT_END as premLimitEnd
        FROM CV.CRAT
        WHERE COMM_CLASS_CODE = #{commClassCode}
          AND COMM_LINE_CODE = #{commLineCode}
          AND STR_DATE <= #{effectiveDate}
          AND END_DATE >= #{effectiveDate}
        ORDER BY CRAT_TYPE, CRAT_SERIAL
    """)
    fun findEffectiveRates(
        @Param("commClassCode") commClassCode: String,
        @Param("commLineCode") commLineCode: String,
        @Param("effectiveDate") effectiveDate: LocalDate
    ): List<CommissionRate>

    /**
     * 依業務線代號查詢所有佣金率類別碼
     *
     * @param commLineCode 業務線代號
     * @return 佣金率類別碼清單
     */
    @Select("""
        SELECT DISTINCT COMM_CLASS_CODE
        FROM CV.CRAT
        WHERE COMM_LINE_CODE = #{commLineCode}
        ORDER BY COMM_CLASS_CODE
    """)
    fun findClassCodesByLineCode(@Param("commLineCode") commLineCode: String): List<String>

    /**
     * 查詢所有不重複的業務線代號
     *
     * @return 業務線代號清單
     */
    @Select("""
        SELECT DISTINCT COMM_LINE_CODE
        FROM CV.CRAT
        ORDER BY COMM_LINE_CODE
    """)
    fun findAllLineCodes(): List<String>

    /**
     * 查詢所有不重複的佣金率型態
     *
     * @return 佣金率型態清單
     */
    @Select("""
        SELECT DISTINCT CRAT_TYPE
        FROM CV.CRAT
        ORDER BY CRAT_TYPE
    """)
    fun findAllCratTypes(): List<String>

    /**
     * 依多條件查詢佣金率
     *
     * 注意：此方法應搭配 PageHelper.startPage() 使用以支援分頁。
     *
     * @param commClassCode 佣金率類別碼 (可選)
     * @param commLineCode 業務線代號 (可選)
     * @param cratType 佣金率型態 (可選)
     * @param effectiveDate 生效日期 (可選)
     * @return 符合條件的佣金率清單
     */
    @Select("""
        <script>
        SELECT
            CRAT_SERIAL as serial,
            COMM_CLASS_CODE as commClassCode,
            COMM_LINE_CODE as commLineCode,
            CRAT_TYPE as cratType,
            PROJ_NO as projectNo,
            STR_DATE as startDate,
            END_DATE as endDate,
            CRAT_KEY1 as cratKey1,
            CRAT_KEY2 as cratKey2,
            COMM_START_YEAR as commStartYear,
            COMM_END_YEAR as commEndYear,
            COMM_START_AGE as commStartAge,
            COMM_END_AGE as commEndAge,
            COMM_START_MODX as commStartModx,
            COMM_END_MODX as commEndModx,
            COMM_RATE as commRate,
            COMM_RATE_ORG as commRateOrg,
            PREM_LIMIT_STR as premLimitStart,
            PREM_LIMIT_END as premLimitEnd
        FROM CV.CRAT
        <where>
            <if test="commClassCode != null">
                AND COMM_CLASS_CODE = #{commClassCode}
            </if>
            <if test="commLineCode != null">
                AND COMM_LINE_CODE = #{commLineCode}
            </if>
            <if test="cratType != null">
                AND CRAT_TYPE = #{cratType}
            </if>
            <if test="effectiveDate != null">
                AND STR_DATE &lt;= #{effectiveDate}
                AND END_DATE &gt;= #{effectiveDate}
            </if>
        </where>
        ORDER BY COMM_CLASS_CODE, COMM_LINE_CODE, STR_DATE DESC
        </script>
    """)
    fun search(
        @Param("commClassCode") commClassCode: String?,
        @Param("commLineCode") commLineCode: String?,
        @Param("cratType") cratType: String?,
        @Param("effectiveDate") effectiveDate: LocalDate?
    ): List<CommissionRate>

    /**
     * 計算指定佣金類別碼的資料筆數
     *
     * @param commClassCode 佣金率類別碼
     * @return 資料筆數
     */
    @Select("""
        SELECT COUNT(*)
        FROM CV.CRAT
        WHERE COMM_CLASS_CODE = #{commClassCode}
    """)
    fun countByClassCode(@Param("commClassCode") commClassCode: String): Int
}
