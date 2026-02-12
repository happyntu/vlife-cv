package com.vlife.cv.exclusion

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("XcldsService 單元測試")
class XcldsServiceTest {

    private lateinit var mapper: XcldsMapper
    private lateinit var service: XcldsService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = XcldsService(mapper)
    }

    private fun createTestXclds(
        policyNo: String = "L123456789",
        coverageNo: Int = 1,
        xcldsType: String = "2",
        xcldsDate: LocalDate = LocalDate.of(2025, 3, 15),
        referenceCode: String = "CL2025001234"
    ) = Xclds(policyNo, coverageNo, xcldsType, xcldsDate, referenceCode)

    @Nested
    @DisplayName("findByKey")
    inner class FindByKey {
        @Test
        fun `should return Xclds when found`() {
            val xclds = createTestXclds()
            every {
                mapper.findByPolicyNoAndCoverageNoAndXcldsType("L123456789", 1, "2")
            } returns xclds

            val result = service.findByKey("L123456789", 1, "2")

            assertNotNull(result)
            assertEquals("L123456789", result.policyNo)
            assertEquals(1, result.coverageNo)
            assertEquals("2", result.xcldsType)
        }

        @Test
        fun `should return null when not found`() {
            every {
                mapper.findByPolicyNoAndCoverageNoAndXcldsType("X999999999", 1, "2")
            } returns null

            assertNull(service.findByKey("X999999999", 1, "2"))
        }
    }

    @Nested
    @DisplayName("findDeathDate")
    inner class FindDeathDate {
        @Test
        fun `should return death date when found`() {
            val deathDate = LocalDate.of(2025, 3, 15)
            val xclds = createTestXclds(xcldsType = Xclds.TYPE_DEATH, xcldsDate = deathDate)
            every {
                mapper.findByPolicyNoAndCoverageNoAndXcldsType("L123456789", 1, Xclds.TYPE_DEATH)
            } returns xclds

            val result = service.findDeathDate("L123456789", 1)

            assertEquals(deathDate, result)
        }

        @Test
        fun `should return null when no death record`() {
            every {
                mapper.findByPolicyNoAndCoverageNoAndXcldsType("L123456789", 1, Xclds.TYPE_DEATH)
            } returns null

            assertNull(service.findDeathDate("L123456789", 1))
        }
    }

    @Nested
    @DisplayName("findByPolicyNo")
    inner class FindByPolicyNo {
        @Test
        fun `should return list of Xclds`() {
            val list = listOf(
                createTestXclds(xcldsType = "1"),
                createTestXclds(xcldsType = "2")
            )
            every { mapper.findByPolicyNo("L123456789") } returns list

            val result = service.findByPolicyNo("L123456789")

            assertEquals(2, result.size)
            assertEquals("1", result[0].xcldsType)
            assertEquals("2", result[1].xcldsType)
        }

        @Test
        fun `should return empty list when no records`() {
            every { mapper.findByPolicyNo("X999999999") } returns emptyList()

            val result = service.findByPolicyNo("X999999999")

            assertEquals(0, result.size)
        }
    }

    @Nested
    @DisplayName("findByPolicyNoAndCoverageNo")
    inner class FindByPolicyNoAndCoverageNo {
        @Test
        fun `should return list of Xclds for coverage`() {
            val list = listOf(
                createTestXclds(xcldsType = "1"),
                createTestXclds(xcldsType = "2"),
                createTestXclds(xcldsType = "3")
            )
            every { mapper.findByPolicyNoAndCoverageNo("L123456789", 1) } returns list

            val result = service.findByPolicyNoAndCoverageNo("L123456789", 1)

            assertEquals(3, result.size)
        }
    }

    @Nested
    @DisplayName("insert")
    inner class Insert {
        @Test
        fun `should insert successfully`() {
            val xclds = createTestXclds()
            every { mapper.insert(xclds) } returns 1

            assertEquals(1, service.insert(xclds))
            verify(exactly = 1) { mapper.insert(xclds) }
        }
    }

    @Nested
    @DisplayName("upsert")
    inner class Upsert {
        @Test
        fun `should delete then insert in order`() {
            val xclds = createTestXclds()
            every { mapper.deleteByPrimaryKey("L123456789", 1, "2") } returns 1
            every { mapper.insert(xclds) } returns 1

            val result = service.upsert(xclds)

            assertEquals(1, result)
            verifyOrder {
                mapper.deleteByPrimaryKey("L123456789", 1, "2")
                mapper.insert(xclds)
            }
        }

        @Test
        fun `should insert even when no existing record to delete`() {
            val xclds = createTestXclds()
            every { mapper.deleteByPrimaryKey("L123456789", 1, "2") } returns 0
            every { mapper.insert(xclds) } returns 1

            val result = service.upsert(xclds)

            assertEquals(1, result)
            verify(exactly = 1) { mapper.deleteByPrimaryKey("L123456789", 1, "2") }
            verify(exactly = 1) { mapper.insert(xclds) }
        }
    }

    @Nested
    @DisplayName("recordDeathDate")
    inner class RecordDeathDate {
        @Test
        fun `should call upsert with correct entity`() {
            val deathDate = LocalDate.of(2025, 6, 20)
            every { mapper.deleteByPrimaryKey("L123456789", 1, Xclds.TYPE_DEATH) } returns 0
            every { mapper.insert(match<Xclds> {
                it.policyNo == "L123456789" &&
                    it.coverageNo == 1 &&
                    it.xcldsType == Xclds.TYPE_DEATH &&
                    it.xcldsDate == deathDate &&
                    it.referenceCode == "CL2025009999"
            }) } returns 1

            val result = service.recordDeathDate("L123456789", 1, deathDate, "CL2025009999")

            assertEquals(1, result)
            verify(exactly = 1) {
                mapper.insert(match<Xclds> {
                    it.policyNo == "L123456789" &&
                        it.coverageNo == 1 &&
                        it.xcldsType == Xclds.TYPE_DEATH &&
                        it.xcldsDate == deathDate &&
                        it.referenceCode == "CL2025009999"
                })
            }
        }
    }
}
