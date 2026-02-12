package com.vlife.cv.quote

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("QmfdeService 單元測試")
class QmfdeServiceTest {

    private lateinit var mapper: QmfdeMapper
    private lateinit var service: QmfdeService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = QmfdeService(mapper)
    }

    private fun createTestQmfde(
        ivTargetCode: String = "FND001",
        ivTargetTitle: String = "Test Fund",
        ivStandardCode: String = "TW1234567890"
    ) = Qmfde(
        ivTargetCode = ivTargetCode,
        ivTargetTitle = ivTargetTitle,
        ivCompanyCode = "COMP01",
        ivCurrency = "NTD",
        ivStandardCode = ivStandardCode,
        subAcntType = "1",
        subAcntPlanCode = null,
        subAcntCalcType = null,
        subAcntType2 = "1",
        intCalcProc = "P001",
        ivType = "5",
        ivMinAmt = 10000L,
        fundMinAmt = 5000L,
        ivUnit = null,
        ivCostPrice = null,
        ivCostExrt = null,
        ivCostVal = null,
        updateDate = null,
        inventoryQty = null,
        principleFreq = 12,
        bundleInvInd = null,
        nbdtPlanCode = null,
        pcDurPlan = null,
        startDate = LocalDate.of(2020, 1, 1),
        expiredDate = LocalDate.of(2050, 12, 31),
        qmfdeStrDate = null,
        qmfdeEndDate = null,
        qmfdeEntryInd = "Y",
        matureValRate = null,
        ivTargetYield = null,
        prodRate1 = null,
        ivProfitRate = null,
        ivPercentType = null,
        ivPercentN = null,
        ivPercentStr = null,
        ivPercentEnd = null,
        intApplyYrInd = null,
        intApplyYr = null,
        ivSalesEndDate = null,
        qmfdeAmt = null,
        invest2ndDate = null,
        invest3thDate = null,
        intPlanCode = null,
        lowestIvestReward = null,
        fyFixedReward = null,
        lowestGuaranteeReward = null,
        standardReward = null
    )

    @Nested
    @DisplayName("getByTargetCode")
    inner class GetByTargetCode {
        @Test
        fun `should return DTO when found`() {
            every { mapper.findByTargetCode("FND001") } returns createTestQmfde()
            val result = service.getByTargetCode("FND001")
            assertNotNull(result)
            assertEquals("FND001", result.ivTargetCode)
            assertEquals("Test Fund", result.ivTargetTitle)
        }

        @Test
        fun `should return null when not found`() {
            every { mapper.findByTargetCode("XXXXX") } returns null
            assertNull(service.getByTargetCode("XXXXX"))
        }
    }

    @Nested
    @DisplayName("getByStandardCode")
    inner class GetByStandardCode {
        @Test
        fun `should return DTO when found`() {
            every { mapper.findByStandardCode("TW1234567890") } returns createTestQmfde()
            val result = service.getByStandardCode("TW1234567890")
            assertNotNull(result)
            assertEquals("FND001", result.ivTargetCode)
        }

        @Test
        fun `should return null when not found`() {
            every { mapper.findByStandardCode("XXXXX") } returns null
            assertNull(service.getByStandardCode("XXXXX"))
        }
    }

    @Nested
    @DisplayName("countByPlanCodeAndVersionAndDate")
    inner class CountByPlanCodeAndVersionAndDate {
        @Test
        fun `should return count`() {
            every { mapper.countByPlanCodeAndVersionAndDate("PLAN1", "1", LocalDate.of(2023, 6, 1)) } returns 3
            assertEquals(3, service.countByPlanCodeAndVersionAndDate("PLAN1", "1", LocalDate.of(2023, 6, 1)))
        }
    }

    @Nested
    @DisplayName("countByEntryInd")
    inner class CountByEntryInd {
        @Test
        fun `should return count`() {
            every { mapper.countByEntryInd("Y") } returns 5
            assertEquals(5, service.countByEntryInd("Y"))
        }
    }

    @Nested
    @DisplayName("exists")
    inner class Exists {
        @Test
        fun `should return true when exists`() {
            every { mapper.exists("FND001") } returns true
            assertTrue(service.exists("FND001"))
        }

        @Test
        fun `should return false when not exists`() {
            every { mapper.exists("XXXXX") } returns false
            assertFalse(service.exists("XXXXX"))
        }
    }

    @Nested
    @DisplayName("existsByStandardCode")
    inner class ExistsByStandardCode {
        @Test
        fun `should return true when exists`() {
            every { mapper.existsByStandardCode("TW1234567890") } returns true
            assertTrue(service.existsByStandardCode("TW1234567890"))
        }

        @Test
        fun `should return false when not exists`() {
            every { mapper.existsByStandardCode("XXXXX") } returns false
            assertFalse(service.existsByStandardCode("XXXXX"))
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {
        @Test
        fun `should insert and return affected rows`() {
            val entity = createTestQmfde()
            every { mapper.insert(entity) } returns 1
            assertEquals(1, service.create(entity, "ADMIN"))
            verify(exactly = 1) { mapper.insert(entity) }
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {
        @Test
        fun `should update and return affected rows`() {
            val entity = createTestQmfde()
            every { mapper.update(entity) } returns 1
            assertEquals(1, service.update(entity, "ADMIN"))
            verify(exactly = 1) { mapper.update(entity) }
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {
        @Test
        fun `should delete and return affected rows`() {
            every { mapper.delete("FND001") } returns 1
            assertEquals(1, service.delete("FND001", "ADMIN"))
            verify(exactly = 1) { mapper.delete("FND001") }
        }
    }
}
