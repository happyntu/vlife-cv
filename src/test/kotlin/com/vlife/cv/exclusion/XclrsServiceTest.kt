package com.vlife.cv.exclusion

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

@DisplayName("XclrsService 單元測試")
class XclrsServiceTest {

    private lateinit var mapper: XclrsMapper
    private lateinit var service: XclrsService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = XclrsService(mapper)
    }

    private fun createTestXclrs(
        xclrsSerial: Long = 1014523L,
        claimReceNo: String = "A1146002009110101",
        eventId: String = "T123131149",
        claimType: String = "2",
        claimTypeSub: String? = null,
        xclrsCode: String? = "0103",
        eventDateS: LocalDate = LocalDate.of(2009, 10, 15),
        processDate: LocalDate = LocalDate.of(2009, 11, 5)
    ) = Xclrs(xclrsSerial, claimReceNo, eventId, claimType, claimTypeSub, xclrsCode, eventDateS, processDate)

    @Nested
    @DisplayName("getByXclrsSerial")
    inner class GetByXclrsSerial {
        @Test
        fun `should return Xclrs when found`() {
            val entity = createTestXclrs()
            every { mapper.findByXclrsSerial(1014523L) } returns entity

            val result = service.getByXclrsSerial(1014523L)

            assertNotNull(result)
            assertEquals(1014523L, result.xclrsSerial)
            assertEquals("A1146002009110101", result.claimReceNo)
            verify(exactly = 1) { mapper.findByXclrsSerial(1014523L) }
        }

        @Test
        fun `should return null when not found`() {
            every { mapper.findByXclrsSerial(999999L) } returns null

            assertNull(service.getByXclrsSerial(999999L))
            verify(exactly = 1) { mapper.findByXclrsSerial(999999L) }
        }
    }

    @Nested
    @DisplayName("getByClaimReceNoAndClaimTypeAndXclrsCode")
    inner class GetByClaimReceNoAndClaimTypeAndXclrsCode {
        @Test
        fun `should return Xclrs when found`() {
            val entity = createTestXclrs()
            every {
                mapper.findByClaimReceNoAndClaimTypeAndXclrsCode("A1146002009110101", "2", "0103")
            } returns entity

            val result = service.getByClaimReceNoAndClaimTypeAndXclrsCode("A1146002009110101", "2", "0103")

            assertNotNull(result)
            assertEquals("A1146002009110101", result.claimReceNo)
            assertEquals("2", result.claimType)
            assertEquals("0103", result.xclrsCode)
        }

        @Test
        fun `should return null when not found`() {
            every {
                mapper.findByClaimReceNoAndClaimTypeAndXclrsCode("INVALID", "9", "XXXX")
            } returns null

            assertNull(service.getByClaimReceNoAndClaimTypeAndXclrsCode("INVALID", "9", "XXXX"))
        }
    }

    @Nested
    @DisplayName("getFirstByClaimReceNoAndClaimType")
    inner class GetFirstByClaimReceNoAndClaimType {
        @Test
        fun `should return first Xclrs when found`() {
            val entity = createTestXclrs()
            every {
                mapper.findFirstByClaimReceNoAndClaimType("A1146002009110101", "2")
            } returns entity

            val result = service.getFirstByClaimReceNoAndClaimType("A1146002009110101", "2")

            assertNotNull(result)
            assertEquals("A1146002009110101", result.claimReceNo)
            assertEquals("2", result.claimType)
        }

        @Test
        fun `should return null when not found`() {
            every {
                mapper.findFirstByClaimReceNoAndClaimType("INVALID", "9")
            } returns null

            assertNull(service.getFirstByClaimReceNoAndClaimType("INVALID", "9"))
        }
    }

    @Nested
    @DisplayName("getByClaimReceNo")
    inner class GetByClaimReceNo {
        @Test
        fun `should return list when records exist`() {
            val records = listOf(
                createTestXclrs(xclrsSerial = 1L, xclrsCode = "0101"),
                createTestXclrs(xclrsSerial = 2L, xclrsCode = "0103")
            )
            every { mapper.findByClaimReceNo("A1146002009110101") } returns records

            val result = service.getByClaimReceNo("A1146002009110101")

            assertEquals(2, result.size)
            assertEquals(1L, result[0].xclrsSerial)
            assertEquals(2L, result[1].xclrsSerial)
        }

        @Test
        fun `should return empty list when no records`() {
            every { mapper.findByClaimReceNo("INVALID") } returns emptyList()

            val result = service.getByClaimReceNo("INVALID")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getByEventId")
    inner class GetByEventId {
        @Test
        fun `should return list when records exist`() {
            val records = listOf(
                createTestXclrs(xclrsSerial = 10L),
                createTestXclrs(xclrsSerial = 20L, claimReceNo = "B2250001001010101")
            )
            every { mapper.findByEventId("T123131149") } returns records

            val result = service.getByEventId("T123131149")

            assertEquals(2, result.size)
            assertEquals("T123131149", result[0].eventId)
        }
    }

    @Nested
    @DisplayName("getEventDateS")
    inner class GetEventDateS {
        @Test
        fun `should always return null (V3 no-op)`() {
            val result = service.getEventDateS("T123131149", "2")

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should call insert and return entity`() {
            val entity = createTestXclrs()
            every { mapper.insert(entity) } returns 1

            val result = service.create(entity)

            assertEquals(entity, result)
            verify(exactly = 1) { mapper.insert(entity) }
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {
        @Test
        fun `should return affected rows`() {
            val entity = createTestXclrs()
            every { mapper.update(entity) } returns 1

            val result = service.update(entity)

            assertEquals(1, result)
            verify(exactly = 1) { mapper.update(entity) }
        }
    }

    @Nested
    @DisplayName("deleteByXclrsSerial")
    inner class DeleteByXclrsSerial {
        @Test
        fun `should return affected rows`() {
            every { mapper.deleteByXclrsSerial(1014523L) } returns 1

            val result = service.deleteByXclrsSerial(1014523L)

            assertEquals(1, result)
            verify(exactly = 1) { mapper.deleteByXclrsSerial(1014523L) }
        }
    }

    @Nested
    @DisplayName("deleteByClaimReceNo")
    inner class DeleteByClaimReceNo {
        @Test
        fun `should return affected rows`() {
            every { mapper.deleteByClaimReceNo("A1146002009110101") } returns 3

            val result = service.deleteByClaimReceNo("A1146002009110101")

            assertEquals(3, result)
            verify(exactly = 1) { mapper.deleteByClaimReceNo("A1146002009110101") }
        }
    }
}
