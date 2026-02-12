package com.vlife.cv.plnd

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PlndService 單元測試
 *
 * 使用 MockK mock PlndMapper，驗證 Service 層邏輯。
 */
@DisplayName("PlndService 單元測試")
class PlndServiceTest {

    private lateinit var mapper: PlndMapper
    private lateinit var service: PlndService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = PlndService(mapper)
    }

    private fun createTestPlnd(
        planCode: String = "IVL01",
        version: String = "1",
        ivTargetCode: String = "FND01",
        ivApplInd: String = "1",
        ivPercent: BigDecimal? = BigDecimal("50.00"),
        ivhsCodeC: String? = null
    ) = Plnd(
        planCode = planCode,
        version = version,
        ivTargetCode = ivTargetCode,
        ivApplInd = ivApplInd,
        ivPercent = ivPercent,
        ivhsCodeC = ivhsCodeC
    )

    @Nested
    @DisplayName("exists")
    inner class Exists {

        @Test
        fun `should return true when PLND exists`() {
            every { mapper.exists("IVL01", "1") } returns true

            val result = service.exists("IVL01", "1")

            assertTrue(result)
            verify(exactly = 1) { mapper.exists("IVL01", "1") }
        }

        @Test
        fun `should return false when PLND not exists`() {
            every { mapper.exists("XXXXX", "1") } returns false

            val result = service.exists("XXXXX", "1")

            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("findByPlanCodeAndVersion")
    inner class FindByPlanCodeAndVersion {

        @Test
        fun `should return PlndDto list`() {
            val entities = listOf(
                createTestPlnd(ivTargetCode = "FND01"),
                createTestPlnd(ivTargetCode = "FND02", ivPercent = BigDecimal("30.00"))
            )
            every { mapper.findByPlanCodeAndVersion("IVL01", "1") } returns entities

            val result = service.findByPlanCodeAndVersion("IVL01", "1")

            assertEquals(2, result.size)
            assertEquals("FND01", result[0].ivTargetCode)
            assertEquals("FND02", result[1].ivTargetCode)
            verify(exactly = 1) { mapper.findByPlanCodeAndVersion("IVL01", "1") }
        }

        @Test
        fun `should return empty list when no data`() {
            every { mapper.findByPlanCodeAndVersion("XXXXX", "1") } returns emptyList()

            val result = service.findByPlanCodeAndVersion("XXXXX", "1")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("findByPlanCodeAndVersionAndTargetCode")
    inner class FindByPlanCodeAndVersionAndTargetCode {

        @Test
        fun `should return filtered PlndDto list`() {
            val entities = listOf(
                createTestPlnd(ivTargetCode = "FND01", ivApplInd = "1"),
                createTestPlnd(ivTargetCode = "FND01", ivApplInd = "2")
            )
            every { mapper.findByPlanCodeAndVersionAndTargetCode("IVL01", "1", "FND01") } returns entities

            val result = service.findByPlanCodeAndVersionAndTargetCode("IVL01", "1", "FND01")

            assertEquals(2, result.size)
        }
    }

    @Nested
    @DisplayName("findByAllConditions")
    inner class FindByAllConditions {

        @Test
        fun `should return PlndDto when found`() {
            val entity = createTestPlnd()
            every { mapper.findByAllConditions("IVL01", "1", "FND01", "1") } returns entity

            val result = service.findByAllConditions("IVL01", "1", "FND01", "1")

            assertNotNull(result)
            assertEquals("IVL01", result.planCode)
            assertEquals("FND01", result.ivTargetCode)
        }

        @Test
        fun `should return null when not found`() {
            every { mapper.findByAllConditions("IVL01", "1", "XXX", "1") } returns null

            val result = service.findByAllConditions("IVL01", "1", "XXX", "1")

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("sumRatioByPlanCodeAndVersion")
    inner class SumRatioByPlanCodeAndVersion {

        @Test
        fun `should return sum of ratios`() {
            every { mapper.sumRatioByPlanCodeAndVersion("IVL01", "1") } returns BigDecimal("80.00")

            val result = service.sumRatioByPlanCodeAndVersion("IVL01", "1")

            assertEquals(BigDecimal("80.00"), result)
        }

        @Test
        fun `should return null when no data`() {
            every { mapper.sumRatioByPlanCodeAndVersion("XXXXX", "1") } returns null

            val result = service.sumRatioByPlanCodeAndVersion("XXXXX", "1")

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("findEffectiveDatesByTargetCode")
    inner class FindEffectiveDatesByTargetCode {

        @Test
        fun `should return date range`() {
            val dateRange = PlndDateRangeDto(
                minStartDate = LocalDate.of(2020, 1, 1),
                maxEndDate = LocalDate.of(2050, 12, 31)
            )
            every { mapper.findEffectiveDatesByTargetCode("FND01") } returns dateRange

            val result = service.findEffectiveDatesByTargetCode("FND01")

            assertNotNull(result)
            assertEquals(LocalDate.of(2020, 1, 1), result.minStartDate)
            assertEquals(LocalDate.of(2050, 12, 31), result.maxEndDate)
        }

        @Test
        fun `should return null when target code not found`() {
            every { mapper.findEffectiveDatesByTargetCode("XXX") } returns null

            val result = service.findEffectiveDatesByTargetCode("XXX")

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {

        @Test
        fun `should insert and return affected rows`() {
            val entity = createTestPlnd()
            every { mapper.insert(entity) } returns 1

            val result = service.create(entity, "ADMIN")

            assertEquals(1, result)
            verify(exactly = 1) { mapper.insert(entity) }
        }
    }

    @Nested
    @DisplayName("updateByPlanCodeAndVersion")
    inner class UpdateByPlanCodeAndVersion {

        @Test
        fun `should update and return affected rows`() {
            val entity = createTestPlnd()
            every { mapper.updateByPlanCodeAndVersion("IVL01", "1", entity) } returns 1

            val result = service.updateByPlanCodeAndVersion("IVL01", "1", entity, "ADMIN")

            assertEquals(1, result)
            verify(exactly = 1) { mapper.updateByPlanCodeAndVersion("IVL01", "1", entity) }
        }
    }

    @Nested
    @DisplayName("updateByAllConditions")
    inner class UpdateByAllConditions {

        @Test
        fun `should update and return affected rows`() {
            val entity = createTestPlnd()
            every { mapper.updateByAllConditions(entity) } returns 1

            val result = service.updateByAllConditions(entity, "ADMIN")

            assertEquals(1, result)
            verify(exactly = 1) { mapper.updateByAllConditions(entity) }
        }
    }

    @Nested
    @DisplayName("deleteByPlanCodeAndVersion")
    inner class DeleteByPlanCodeAndVersion {

        @Test
        fun `should delete and return affected rows`() {
            every { mapper.deleteByPlanCodeAndVersion("IVL01", "1") } returns 3

            val result = service.deleteByPlanCodeAndVersion("IVL01", "1", "ADMIN")

            assertEquals(3, result)
            verify(exactly = 1) { mapper.deleteByPlanCodeAndVersion("IVL01", "1") }
        }
    }

    @Nested
    @DisplayName("deleteByAllConditions")
    inner class DeleteByAllConditions {

        @Test
        fun `should delete single record`() {
            every { mapper.deleteByAllConditions("IVL01", "1", "FND01", "1") } returns 1

            val result = service.deleteByAllConditions("IVL01", "1", "FND01", "1", "ADMIN")

            assertEquals(1, result)
            verify(exactly = 1) { mapper.deleteByAllConditions("IVL01", "1", "FND01", "1") }
        }
    }
}
