package com.vlife.cv.annuityreservefactor

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("QcvrfService 單元測試")
class QcvrfServiceTest {

    private lateinit var mapper: QcvrfMapper
    private lateinit var service: QcvrfService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = QcvrfService(mapper)
    }

    private fun createTestEntity(
        annyPlanCode: String = "ANP01",
        strDate: LocalDate = LocalDate.of(2000, 1, 1),
        endDate: LocalDate = LocalDate.of(2010, 12, 31)
    ) = AnnuityReserveFactor(
        annyPlanCode = annyPlanCode,
        strDate = strDate,
        endDate = endDate,
        durType = 1,
        durYear = 0,
        poRvfDeath = 100,
        poRvfTso = "84TSO",
        rvfDeath = 100,
        rvfTso = "84TSO",
        intPlanCode = "IRP01"
    )

    @Nested
    @DisplayName("getByAnnyPlanCode")
    inner class GetByAnnyPlanCode {
        @Test
        fun `should return DTO list`() {
            val entities = listOf(createTestEntity(), createTestEntity(strDate = LocalDate.of(2011, 1, 1), endDate = LocalDate.of(2020, 12, 31)))
            every { mapper.findByAnnyPlanCode("ANP01") } returns entities
            val result = service.getByAnnyPlanCode("ANP01")
            assertEquals(2, result.size)
            assertEquals("ANP01", result[0].annyPlanCode)
            verify(exactly = 1) { mapper.findByAnnyPlanCode("ANP01") }
        }

        @Test
        fun `should return empty list when no data`() {
            every { mapper.findByAnnyPlanCode("XXXXX") } returns emptyList()
            val result = service.getByAnnyPlanCode("XXXXX")
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getByPk")
    inner class GetByPk {
        @Test
        fun `should return DTO when found`() {
            val entity = createTestEntity()
            every { mapper.findByPk("ANP01", LocalDate.of(2000, 1, 1), LocalDate.of(2010, 12, 31)) } returns entity
            val result = service.getByPk("ANP01", LocalDate.of(2000, 1, 1), LocalDate.of(2010, 12, 31))
            assertNotNull(result)
            assertEquals("ANP01", result.annyPlanCode)
            assertEquals("IRP01", result.intPlanCode)
        }

        @Test
        fun `should return null when not found`() {
            every { mapper.findByPk("XXXXX", any(), any()) } returns null
            val result = service.getByPk("XXXXX", LocalDate.of(2000, 1, 1), LocalDate.of(2010, 12, 31))
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should insert and return affected rows`() {
            val entity = createTestEntity()
            every { mapper.insert(entity) } returns 1
            val result = service.create(entity, "SYSTEM")
            assertEquals(1, result)
            verify(exactly = 1) { mapper.insert(entity) }
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {
        @Test
        fun `should update and return affected rows`() {
            val entity = createTestEntity()
            val str = LocalDate.of(2000, 1, 1)
            val end = LocalDate.of(2010, 12, 31)
            every { mapper.updateByPk("ANP01", str, end, entity) } returns 1
            val result = service.update("ANP01", str, end, entity, "SYSTEM")
            assertEquals(1, result)
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        fun `should delete and return affected rows`() {
            val str = LocalDate.of(2000, 1, 1)
            val end = LocalDate.of(2010, 12, 31)
            every { mapper.deleteByPk("ANP01", str, end) } returns 1
            val result = service.delete("ANP01", str, end, "SYSTEM")
            assertEquals(1, result)
            verify(exactly = 1) { mapper.deleteByPk("ANP01", str, end) }
        }
    }
}
