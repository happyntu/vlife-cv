package com.vlife.cv.actuarial

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * UvalService 單元測試
 *
 * 覆蓋 UVAL/UVALBLB/UVALBLBS 三表的查詢與維護操作
 */
@DisplayName("UvalService 單元測試")
class UvalServiceTest {

    private lateinit var uvalMapper: UvalMapper
    private lateinit var uvalblbMapper: UvalblbMapper
    private lateinit var uvalblbsMapper: UvalblbsMapper
    private lateinit var service: UvalService

    @BeforeEach
    fun setup() {
        uvalMapper = mockk()
        uvalblbMapper = mockk()
        uvalblbsMapper = mockk()
        service = UvalService(uvalMapper, uvalblbMapper, uvalblbsMapper)
    }

    private fun createTestUval(
        planCode: String = "89TSO",
        version: String = "1",
        sex: String = "1",
        age: Int = 0,
        recordType: String = "CX",
        serialYear: Int = 97,
        deathRate: String = "90",
        uvRvfRate: String = "41.5",
        uvalValue: BigDecimal? = BigDecimal("528.826684")
    ) = UniversalValue(
        planCode = planCode,
        version = version,
        sex = sex,
        age = age,
        recordType = recordType,
        serialYear = serialYear,
        deathRate = deathRate,
        uvRvfRate = uvRvfRate,
        uvalValue = uvalValue
    )

    private fun createTestValues(size: Int = 5): List<Double?> =
        (1..size).map { it * 100.0 }

    private fun createStandardQuery(
        planCode: String = "89TSO",
        version: String = "1",
        sex: String = "1",
        age: Int = 30,
        recordType: String = "CX",
        planRelationSub: String? = null
    ) = ValueBlockQuery(
        planCode = planCode,
        version = version,
        sex = sex,
        age = age,
        uvSub1 = "0",
        uvSub2 = "0",
        recordType = recordType,
        planRelationSub = planRelationSub
    )

    private fun createSubstandardQuery(
        planCode: String = "89TSO",
        version: String = "1",
        sex: String = "1",
        age: Int = 30,
        recordType: String = "CX",
        rateSub1: String = "1",
        rateSub2: String = "35"
    ) = ValueBlockQuery(
        planCode = planCode,
        version = version,
        sex = sex,
        age = age,
        rateSub1 = rateSub1,
        rateSub2 = rateSub2,
        uvSub1 = "0",
        uvSub2 = "0",
        recordType = recordType,
        planRelationSub = "1"
    )

    // ===================================================================
    // UVAL 主表查詢
    // ===================================================================

    @Nested
    @DisplayName("getUval")
    inner class GetUval {

        @Test
        fun `should return universal value when found`() {
            val uval = createTestUval()
            every {
                uvalMapper.findByPrimaryKey("89TSO", "1", "1", 0, "CX", 97, "90", "41.5")
            } returns uval

            val result = service.getUval("89TSO", "1", "1", 0, "CX", 97, "90", "41.5")

            assertNotNull(result)
            assertEquals("89TSO", result.planCode)
            assertEquals(BigDecimal("528.826684"), result.uvalValue)
            verify(exactly = 1) {
                uvalMapper.findByPrimaryKey("89TSO", "1", "1", 0, "CX", 97, "90", "41.5")
            }
        }

        @Test
        fun `should return null when not found`() {
            every {
                uvalMapper.findByPrimaryKey("XXXXX", "1", "1", 0, "CX", 97, "90", "41.5")
            } returns null

            val result = service.getUval("XXXXX", "1", "1", 0, "CX", 97, "90", "41.5")

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("countByPlanCode")
    inner class CountByPlanCode {

        @Test
        fun `should return count for given plan code`() {
            every { uvalMapper.countByPlanCode("89TSO") } returns 1500

            val result = service.countByPlanCode("89TSO")

            assertEquals(1500, result)
        }

        @Test
        fun `should throw when plan code is blank`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                service.countByPlanCode("")
            }
        }
    }

    @Nested
    @DisplayName("existsByFullKey")
    inner class ExistsByFullKey {

        @Test
        fun `should return true when exists`() {
            every {
                uvalMapper.countByFullKey("89TSO", "1", "1", 0, "CX", 97, "90", "41.5")
            } returns 1

            val result = service.existsByFullKey("89TSO", "1", "1", 0, "CX", 97, "90", "41.5")

            assertTrue(result)
        }

        @Test
        fun `should return false when not exists`() {
            every {
                uvalMapper.countByFullKey("XXXXX", "1", "1", 0, "CX", 97, "90", "41.5")
            } returns 0

            val result = service.existsByFullKey("XXXXX", "1", "1", 0, "CX", 97, "90", "41.5")

            assertFalse(result)
        }
    }

    // ===================================================================
    // UVAL 主表異動
    // ===================================================================

    @Nested
    @DisplayName("createUval")
    inner class CreateUval {

        @Test
        fun `should insert uval record`() {
            val entity = createTestUval()
            every { uvalMapper.insert(entity) } returns 1

            val result = service.createUval(entity)

            assertEquals(1, result)
            verify(exactly = 1) { uvalMapper.insert(entity) }
        }
    }

    @Nested
    @DisplayName("updateUvalByPartialKey")
    inner class UpdateUvalByPartialKey {

        @Test
        fun `should update by 6-column key`() {
            val entity = createTestUval(uvalValue = BigDecimal("999.999"))
            every {
                uvalMapper.updateByPartialKey("89TSO", "1", "1", 0, "CX", 97, entity)
            } returns 1

            val result = service.updateUvalByPartialKey("89TSO", "1", "1", 0, "CX", 97, entity)

            assertEquals(1, result)
        }
    }

    @Nested
    @DisplayName("updateUvalByFullKey")
    inner class UpdateUvalByFullKey {

        @Test
        fun `should update by 8-column key`() {
            val entity = createTestUval(uvalValue = BigDecimal("999.999"))
            every {
                uvalMapper.updateByFullKey("89TSO", "1", "1", 0, "CX", 97, "90", "41.5", entity)
            } returns 1

            val result = service.updateUvalByFullKey(
                "89TSO", "1", "1", 0, "CX", 97, "90", "41.5", entity
            )

            assertEquals(1, result)
        }
    }

    @Nested
    @DisplayName("deleteUvalByPartialKey")
    inner class DeleteUvalByPartialKey {

        @Test
        fun `should delete by 6-column key`() {
            every {
                uvalMapper.deleteByPartialKey("89TSO", "1", "1", 0, "CX", 97)
            } returns 3

            val result = service.deleteUvalByPartialKey("89TSO", "1", "1", 0, "CX", 97)

            assertEquals(3, result)
        }
    }

    @Nested
    @DisplayName("deleteUvalByReducedKey")
    inner class DeleteUvalByReducedKey {

        @Test
        fun `should delete by 5-column key`() {
            every {
                uvalMapper.deleteByReducedKey("89TSO", "1", "1", 0, 97)
            } returns 5

            val result = service.deleteUvalByReducedKey("89TSO", "1", "1", 0, 97)

            assertEquals(5, result)
        }
    }

    @Nested
    @DisplayName("deleteUvalByFullKey")
    inner class DeleteUvalByFullKey {

        @Test
        fun `should delete by 8-column key`() {
            every {
                uvalMapper.deleteByFullKey("89TSO", "1", "1", 0, "CX", 97, "90", "41.5")
            } returns 1

            val result = service.deleteUvalByFullKey("89TSO", "1", "1", 0, "CX", 97, "90", "41.5")

            assertEquals(1, result)
        }
    }

    // ===================================================================
    // 價值區塊查詢（核心）
    // ===================================================================

    @Nested
    @DisplayName("getValueBlock - 標準體")
    inner class GetValueBlockStandard {

        @Test
        fun `should return values when standard block found`() {
            val values = createTestValues()
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = createStandardQuery()

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlock(query)

            assertNotNull(result)
            assertEquals(5, result.size)
            assertEquals(100.0, result[0])
        }

        @Test
        fun `should return null when standard block not found`() {
            val query = createStandardQuery(planCode = "XXXXX")

            every { uvalblbMapper.countByPrimaryKey("XXXXX", "1", "1", 30, "0", "0", "CX") } returns 0

            val result = service.getValueBlock(query)

            assertNull(result)
        }

        @Test
        fun `should return null when multiple standard blocks found`() {
            val query = createStandardQuery()

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 2

            val result = service.getValueBlock(query)

            assertNull(result)
        }

        @Test
        fun `should treat planRelationSub 0 as standard`() {
            val values = createTestValues(3)
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = createStandardQuery(planRelationSub = "0")

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlock(query)

            assertNotNull(result)
            assertEquals(3, result.size)
        }

        @Test
        fun `should treat planRelationSub 5 as standard`() {
            val values = createTestValues(3)
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = createStandardQuery(planRelationSub = "5")

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlock(query)

            assertNotNull(result)
        }
    }

    @Nested
    @DisplayName("getValueBlock - 多被保人")
    inner class GetValueBlockSubstandard {

        @Test
        fun `should return values when substandard block found`() {
            val values = createTestValues(3)
            val block = ValueBlockSub("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX", values)
            val query = createSubstandardQuery()

            every {
                uvalblbsMapper.countByPrimaryKey("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX")
            } returns 1
            every {
                uvalblbsMapper.findByPrimaryKey("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX")
            } returns block

            val result = service.getValueBlock(query)

            assertNotNull(result)
            assertEquals(3, result.size)
        }

        @Test
        fun `should pad rateSub1 and rateSub2 correctly`() {
            val values = createTestValues(2)
            val block = ValueBlockSub("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX", values)
            val query = createSubstandardQuery(rateSub1 = "1", rateSub2 = "35")

            every {
                uvalblbsMapper.countByPrimaryKey("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX")
            } returns 1
            every {
                uvalblbsMapper.findByPrimaryKey("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX")
            } returns block

            val result = service.getValueBlock(query)

            assertNotNull(result)
            verify(exactly = 1) {
                uvalblbsMapper.countByPrimaryKey("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX")
            }
        }

        @Test
        fun `should return null when substandard block not found`() {
            val query = createSubstandardQuery(planCode = "XXXXX")

            every {
                uvalblbsMapper.countByPrimaryKey("XXXXX", "1", "1", 30, "01", "035", "0", "0", "CX")
            } returns 0

            val result = service.getValueBlock(query)

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("getValueBlock - NVL 邏輯")
    inner class GetValueBlockNvl {

        @Test
        fun `should default uvSub1 and uvSub2 to 0 when null`() {
            val values = createTestValues(3)
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = ValueBlockQuery(
                planCode = "89TSO", version = "1", sex = "1", age = 30,
                uvSub1 = null, uvSub2 = null, recordType = "CX"
            )

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlock(query)

            assertNotNull(result)
            verify(exactly = 1) {
                uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX")
            }
        }
    }

    // ===================================================================
    // 巢狀表轉換
    // ===================================================================

    @Nested
    @DisplayName("getValueBlockAsTable")
    inner class GetValueBlockAsTable {

        @Test
        fun `should return all years as table entries`() {
            val values = listOf(100.0, 200.0, 300.0)
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = createStandardQuery()

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlockAsTable(query)

            assertEquals(3, result.size)
            assertEquals(1, result[0].policyYear)
            assertEquals(100.0, result[0].value)
            assertEquals(2, result[1].policyYear)
            assertEquals(200.0, result[1].value)
            assertEquals(3, result[2].policyYear)
            assertEquals(300.0, result[2].value)
        }

        @Test
        fun `should return single year when policyYear specified`() {
            val values = listOf(100.0, 200.0, 300.0)
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = createStandardQuery()

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlockAsTable(query, policyYear = 2)

            assertEquals(1, result.size)
            assertEquals(2, result[0].policyYear)
            assertEquals(200.0, result[0].value)
        }

        @Test
        fun `should convert null values to 0 point 0`() {
            val values = listOf(100.0, null, 300.0)
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = createStandardQuery()

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlockAsTable(query)

            assertEquals(3, result.size)
            assertEquals(0.0, result[1].value)
        }

        @Test
        fun `should return empty list when block not found`() {
            val query = createStandardQuery(planCode = "XXXXX")

            every { uvalblbMapper.countByPrimaryKey("XXXXX", "1", "1", 30, "0", "0", "CX") } returns 0

            val result = service.getValueBlockAsTable(query)

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getValueBlockCount")
    inner class GetValueBlockCount {

        @Test
        fun `should return count of non-null values`() {
            val values = listOf(100.0, null, 300.0, null, 500.0)
            val block = ValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)
            val query = createStandardQuery()

            every { uvalblbMapper.countByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns 1
            every { uvalblbMapper.findByPrimaryKey("89TSO", "1", "1", 30, "0", "0", "CX") } returns block

            val result = service.getValueBlockCount(query)

            assertEquals(3, result)
        }

        @Test
        fun `should return 0 when block not found`() {
            val query = createStandardQuery(planCode = "XXXXX")

            every { uvalblbMapper.countByPrimaryKey("XXXXX", "1", "1", 30, "0", "0", "CX") } returns 0

            val result = service.getValueBlockCount(query)

            assertEquals(0, result)
        }
    }

    // ===================================================================
    // UVALBLB 維護
    // ===================================================================

    @Nested
    @DisplayName("createValueBlock")
    inner class CreateValueBlock {

        @Test
        fun `should insert value block`() {
            val values = createTestValues()
            every {
                uvalblbMapper.insert("89TSO", "1", "1", 30, "0", "0", "CX", values)
            } returns 1

            val result = service.createValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)

            assertEquals(1, result)
        }
    }

    @Nested
    @DisplayName("updateValueBlock")
    inner class UpdateValueBlock {

        @Test
        fun `should update value block values`() {
            val values = createTestValues()
            every {
                uvalblbMapper.updateValues("89TSO", "1", "1", 30, "0", "0", "CX", values)
            } returns 1

            val result = service.updateValueBlock("89TSO", "1", "1", 30, "0", "0", "CX", values)

            assertEquals(1, result)
        }
    }

    @Nested
    @DisplayName("deleteValueBlock")
    inner class DeleteValueBlock {

        @Test
        fun `should delete value block`() {
            every {
                uvalblbMapper.delete("89TSO", "1", "1", 30, "0", "0", "CX")
            } returns 1

            val result = service.deleteValueBlock("89TSO", "1", "1", 30, "0", "0", "CX")

            assertEquals(1, result)
        }
    }

    // ===================================================================
    // UVALBLBS 維護
    // ===================================================================

    @Nested
    @DisplayName("createValueBlockSub")
    inner class CreateValueBlockSub {

        @Test
        fun `should insert value block sub`() {
            val values = createTestValues()
            every {
                uvalblbsMapper.insert("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX", values)
            } returns 1

            val result = service.createValueBlockSub(
                "89TSO", "1", "1", 30, "01", "035", "0", "0", "CX", values
            )

            assertEquals(1, result)
        }
    }

    @Nested
    @DisplayName("deleteValueBlockSub")
    inner class DeleteValueBlockSub {

        @Test
        fun `should delete value block sub`() {
            every {
                uvalblbsMapper.delete("89TSO", "1", "1", 30, "01", "035", "0", "0", "CX")
            } returns 1

            val result = service.deleteValueBlockSub(
                "89TSO", "1", "1", 30, "01", "035", "0", "0", "CX"
            )

            assertEquals(1, result)
        }
    }
}
