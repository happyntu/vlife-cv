package com.vlife.cv.xcvrg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("XcvrgService 單元測試")
class XcvrgServiceTest {

    private lateinit var mapper: XcvrgMapper
    private lateinit var service: XcvrgService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = XcvrgService(mapper)
    }

    private fun createTestXcvrg(
        replaceAgeCode: String = "RA01",
        rateSex: String = "1",
        rateAge: Int = 30,
        rateSubAge: Int = 0,
        rateAgeChgInd: Int = 1
    ) = Xcvrg(
        replaceAgeCode = replaceAgeCode,
        rateSex = rateSex,
        rateAge = rateAge,
        rateSubAge = rateSubAge,
        rateAgeChgInd = rateAgeChgInd
    )

    @Nested
    @DisplayName("findByPrimaryKey")
    inner class FindByPrimaryKey {
        @Test
        fun `should return Xcvrg when found`() {
            val xcvrg = createTestXcvrg()
            every { mapper.findByPrimaryKey("RA01", "1", 30, 0) } returns xcvrg
            val result = service.findByPrimaryKey("RA01", "1", 30, 0)
            assertNotNull(result)
            assertEquals("RA01", result.replaceAgeCode)
            assertEquals(1, result.rateAgeChgInd)
        }

        @Test
        fun `should return null when not found`() {
            every { mapper.findByPrimaryKey("XXXX", "1", 99, 0) } returns null
            assertNull(service.findByPrimaryKey("XXXX", "1", 99, 0))
        }
    }

    @Nested
    @DisplayName("existsByPrimaryKey")
    inner class ExistsByPrimaryKey {
        @Test
        fun `should return true when exists`() {
            every { mapper.countByPrimaryKey("RA01", "1", 30, 0) } returns 1
            assertTrue(service.existsByPrimaryKey("RA01", "1", 30, 0))
        }

        @Test
        fun `should return false when not exists`() {
            every { mapper.countByPrimaryKey("XXXX", "1", 99, 0) } returns 0
            assertFalse(service.existsByPrimaryKey("XXXX", "1", 99, 0))
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should insert successfully`() {
            val xcvrg = createTestXcvrg()
            every { mapper.insert(xcvrg) } returns 1
            assertEquals(1, service.create(xcvrg))
            verify(exactly = 1) { mapper.insert(xcvrg) }
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        fun `should delete successfully`() {
            every { mapper.deleteByPrimaryKey("RA01", "1", 30, 0) } returns 1
            assertEquals(1, service.delete("RA01", "1", 30, 0))
            verify(exactly = 1) { mapper.deleteByPrimaryKey("RA01", "1", 30, 0) }
        }

        @Test
        fun `should return zero when not found`() {
            every { mapper.deleteByPrimaryKey("XXXX", "1", 99, 0) } returns 0
            assertEquals(0, service.delete("XXXX", "1", 99, 0))
        }
    }
}
