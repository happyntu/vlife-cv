package com.vlife.cv.psbt

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * PSBT 險種給付參數子表 Mapper
 *
 * 遵循 ADR-016: SQL 必須寫在 XML Mapper
 * 遵循 ADR-017: Mapper 命名 = PsbtMapper (表格導向)
 *
 * V3 equivalent: PK_LIB_PSBTPROC
 */
@Mapper
interface PsbtMapper {

    /**
     * 依險種+版本+性別+類型+代碼 + KEY 範圍匹配查詢
     *
     * V3: f99_get_psbt
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param rateSex 性別費率
     * @param psbtType 參數類型
     * @param psbtCode 參數代碼
     * @param key1 範圍鍵1（用於匹配 psbtKey1 <= key1 <= psbtKey2）
     * @param key2 範圍鍵2（用於匹配 psbtKey3 <= key2 <= psbtKey4）
     * @return PSBT 記錄，若不存在則返回 null
     */
    fun findByKeysAndRange(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("rateSex") rateSex: String,
        @Param("psbtType") psbtType: String,
        @Param("psbtCode") psbtCode: String,
        @Param("key1") key1: Long,
        @Param("key2") key2: Long
    ): Psbt?

    /**
     * 查詢指定組合的所有記錄（供快取預載使用）
     *
     * @param planCode 險種代碼
     * @param version 版本號
     * @param rateSex 性別費率
     * @param psbtType 參數類型
     * @param psbtCode 參數代碼
     * @return PSBT 記錄列表，按 KEY1, KEY3 排序
     */
    fun findAllByKeys(
        @Param("planCode") planCode: String,
        @Param("version") version: String,
        @Param("rateSex") rateSex: String,
        @Param("psbtType") psbtType: String,
        @Param("psbtCode") psbtCode: String
    ): List<Psbt>
}
