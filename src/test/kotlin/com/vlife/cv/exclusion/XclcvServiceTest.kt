package com.vlife.cv.exclusion

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

/**
 * XclcvService 單元測試
 *
 * 測試策略：
 * - 使用 MockK 模擬 Mapper
 * - 目標覆蓋率：90%+
 * - 測試範圍：計數、優先權查詢、指標查詢、狀態查詢、正規匹配、最新記錄、新增、刪除
 */
@ExtendWith(MockKExtension::class)
class XclcvServiceTest {

    @MockK
    private lateinit var mapper: XclcvMapper

    private lateinit var service: XclcvService

    @BeforeEach
    fun setUp() {
        service = XclcvService(mapper)
    }

    private fun createTestXclcv(
        id: Long = 482350L,
        policyNo: String = "A123456789",
        claimNo: String = "X00057864301",
        coverageNo: Int = 1,
        benefCode: String = "7C01",
        eventDateS: LocalDate = LocalDate.of(2010, 1, 1),
        processDate: LocalDate = LocalDate.of(2011, 1, 28),
        clbfRvfInd: String = "2",
        codtStatusCode: String = "W",
        codtStatusCode2: String? = "W"
    ) = Xclcv(id, policyNo, claimNo, coverageNo, benefCode, eventDateS, processDate, clbfRvfInd, codtStatusCode, codtStatusCode2)

    // === countByPolicyAndCoverage ===

    @Nested
    inner class CountByPolicyAndCoverage {

        @Test
        fun `有記錄時回傳正確數量`() {
            // Given
            every { mapper.countByPolicyAndCoverage("A123456789", 1) } returns 3

            // When
            val result = service.countByPolicyAndCoverage("A123456789", 1)

            // Then
            assertEquals(3, result)
            verify { mapper.countByPolicyAndCoverage("A123456789", 1) }
        }

        @Test
        fun `無記錄時回傳 0`() {
            // Given
            every { mapper.countByPolicyAndCoverage("INVALID", 99) } returns 0

            // When
            val result = service.countByPolicyAndCoverage("INVALID", 99)

            // Then
            assertEquals(0, result)
            verify { mapper.countByPolicyAndCoverage("INVALID", 99) }
        }
    }

    // === findByPriority ===

    @Nested
    inner class FindByPriority {

        @Test
        fun `查詢到記錄時回傳 Xclcv`() {
            // Given
            val entity = createTestXclcv(clbfRvfInd = "2")
            val eventDate = LocalDate.of(2011, 6, 30)
            every { mapper.findByPolicyAndCoverageWithPriority("A123456789", 1, eventDate) } returns entity

            // When
            val result = service.findByPriority("A123456789", 1, eventDate)

            // Then
            assertNotNull(result)
            assertEquals(482350L, result?.id)
            assertEquals("2", result?.clbfRvfInd)
            verify { mapper.findByPolicyAndCoverageWithPriority("A123456789", 1, eventDate) }
        }

        @Test
        fun `查無記錄時回傳 null`() {
            // Given
            val eventDate = LocalDate.of(2005, 1, 1)
            every { mapper.findByPolicyAndCoverageWithPriority("A123456789", 1, eventDate) } returns null

            // When
            val result = service.findByPriority("A123456789", 1, eventDate)

            // Then
            assertNull(result)
            verify { mapper.findByPolicyAndCoverageWithPriority("A123456789", 1, eventDate) }
        }
    }

    // === findByInd ===

    @Nested
    inner class FindByInd {

        @Test
        fun `依指標查詢到記錄`() {
            // Given
            val entity = createTestXclcv(clbfRvfInd = "Z")
            every { mapper.findByPolicyCoverageAndInd("A123456789", 1, "Z") } returns entity

            // When
            val result = service.findByInd("A123456789", 1, "Z")

            // Then
            assertNotNull(result)
            assertEquals("Z", result?.clbfRvfInd)
            verify { mapper.findByPolicyCoverageAndInd("A123456789", 1, "Z") }
        }

        @Test
        fun `查無記錄時回傳 null`() {
            // Given
            every { mapper.findByPolicyCoverageAndInd("A123456789", 1, "X") } returns null

            // When
            val result = service.findByInd("A123456789", 1, "X")

            // Then
            assertNull(result)
            verify { mapper.findByPolicyCoverageAndInd("A123456789", 1, "X") }
        }
    }

    // === findByStatus ===

    @Nested
    inner class FindByStatus {

        @Test
        fun `codtStatusCode2 為 null 時查詢`() {
            // Given
            val entity = createTestXclcv(codtStatusCode = "W", codtStatusCode2 = null)
            every { mapper.findByPolicyCoverageAndStatus("A123456789", 1, "W", null) } returns entity

            // When
            val result = service.findByStatus("A123456789", 1, "W", null)

            // Then
            assertNotNull(result)
            assertEquals("W", result?.codtStatusCode)
            assertNull(result?.codtStatusCode2)
            verify { mapper.findByPolicyCoverageAndStatus("A123456789", 1, "W", null) }
        }

        @Test
        fun `codtStatusCode2 有值時查詢`() {
            // Given
            val entity = createTestXclcv(codtStatusCode = "W", codtStatusCode2 = "W")
            every { mapper.findByPolicyCoverageAndStatus("A123456789", 1, "W", "W") } returns entity

            // When
            val result = service.findByStatus("A123456789", 1, "W", "W")

            // Then
            assertNotNull(result)
            assertEquals("W", result?.codtStatusCode)
            assertEquals("W", result?.codtStatusCode2)
            verify { mapper.findByPolicyCoverageAndStatus("A123456789", 1, "W", "W") }
        }

        @Test
        fun `查無記錄時回傳 null`() {
            // Given
            every { mapper.findByPolicyCoverageAndStatus("A123456789", 1, "X", null) } returns null

            // When
            val result = service.findByStatus("A123456789", 1, "X", null)

            // Then
            assertNull(result)
            verify { mapper.findByPolicyCoverageAndStatus("A123456789", 1, "X", null) }
        }
    }

    // === findByIndPattern ===

    @Nested
    inner class FindByIndPattern {

        @Test
        fun `依正規表示式匹配查詢到記錄`() {
            // Given
            val entity = createTestXclcv(clbfRvfInd = "2")
            every { mapper.findByPolicyCoverageAndIndPattern("A123456789", 1, "^[23Z]$") } returns entity

            // When
            val result = service.findByIndPattern("A123456789", 1, "^[23Z]$")

            // Then
            assertNotNull(result)
            assertEquals("2", result?.clbfRvfInd)
            verify { mapper.findByPolicyCoverageAndIndPattern("A123456789", 1, "^[23Z]$") }
        }

        @Test
        fun `查無匹配記錄時回傳 null`() {
            // Given
            every { mapper.findByPolicyCoverageAndIndPattern("A123456789", 1, "^[XY]$") } returns null

            // When
            val result = service.findByIndPattern("A123456789", 1, "^[XY]$")

            // Then
            assertNull(result)
            verify { mapper.findByPolicyCoverageAndIndPattern("A123456789", 1, "^[XY]$") }
        }
    }

    // === findLatest ===

    @Nested
    inner class FindLatest {

        @Test
        fun `查詢到最新記錄`() {
            // Given
            val entity = createTestXclcv(
                eventDateS = LocalDate.of(2011, 6, 30),
                processDate = LocalDate.of(2011, 7, 15)
            )
            every { mapper.findLatestByPolicyAndCoverage("A123456789", 1) } returns entity

            // When
            val result = service.findLatest("A123456789", 1)

            // Then
            assertNotNull(result)
            assertEquals(LocalDate.of(2011, 6, 30), result?.eventDateS)
            assertEquals(LocalDate.of(2011, 7, 15), result?.processDate)
            verify { mapper.findLatestByPolicyAndCoverage("A123456789", 1) }
        }

        @Test
        fun `查無記錄時回傳 null`() {
            // Given
            every { mapper.findLatestByPolicyAndCoverage("INVALID", 99) } returns null

            // When
            val result = service.findLatest("INVALID", 99)

            // Then
            assertNull(result)
            verify { mapper.findLatestByPolicyAndCoverage("INVALID", 99) }
        }
    }

    // === insert ===

    @Nested
    inner class Insert {

        @Test
        fun `新增記錄成功`() {
            // Given
            val entity = createTestXclcv()
            every { mapper.insert(entity) } returns 1

            // When
            val result = service.insert(entity)

            // Then
            assertEquals(1, result)
            verify { mapper.insert(entity) }
        }
    }

    // === deleteByClaimNo ===

    @Nested
    inner class DeleteByClaimNo {

        @Test
        fun `刪除記錄成功`() {
            // Given
            every { mapper.deleteByClaimNo("X00057864301") } returns 2

            // When
            val result = service.deleteByClaimNo("X00057864301")

            // Then
            assertEquals(2, result)
            verify { mapper.deleteByClaimNo("X00057864301") }
        }

        @Test
        fun `刪除無匹配記錄時回傳 0`() {
            // Given
            every { mapper.deleteByClaimNo("INVALID") } returns 0

            // When
            val result = service.deleteByClaimNo("INVALID")

            // Then
            assertEquals(0, result)
            verify { mapper.deleteByClaimNo("INVALID") }
        }
    }
}
