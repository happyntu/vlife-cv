package com.vlife.cv.dividend

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CvdpService 單元測試
 *
 * 使用 MockK mock CvdpMapper，驗證紅利分配預估 Service 層邏輯。
 */
@DisplayName("CvdpService 單元測試")
class CvdpServiceTest {

    private lateinit var mapper: CvdpMapper
    private lateinit var service: CvdpService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = CvdpService(mapper)
    }

    private fun createTestCvdp(
        serialYear3: String = "097",
        policyNo: String = "1234567890",
        coverageNo: Int = 1
    ) = Cvdp(
        serialYear3 = serialYear3,
        policyNo = policyNo,
        coverageNo = coverageNo,
        planCode = "AABB1",
        version = "1",
        deathRatio = BigDecimal("0.001234"),
        rateRatio = BigDecimal("0.005678"),
        loadingRatio = BigDecimal("0.002345"),
        deathDivValue = BigDecimal("100.00"),
        intDivValue = BigDecimal("200.00"),
        expenDivValue = BigDecimal("50.00"),
        divAmt = BigDecimal("350.00"),
        cvdpCode = "1",
        divDate = LocalDate.of(2008, 12, 31),
        processDate = LocalDate.of(2008, 12, 31)
    )

    @Nested
    @DisplayName("getByPrimaryKey")
    inner class GetByPrimaryKey {

        @Test
        fun `should return Cvdp when exists`() {
            val entity = createTestCvdp()
            every { mapper.findByPrimaryKey("097", "1234567890", 1) } returns entity

            val result = service.getByPrimaryKey("097", "1234567890", 1)

            assertNotNull(result)
            assertEquals("097", result.serialYear3)
            assertEquals("1234567890", result.policyNo)
            assertEquals(1, result.coverageNo)
            assertEquals(BigDecimal("350.00"), result.divAmt)
        }

        @Test
        fun `should return null when not exists`() {
            every { mapper.findByPrimaryKey("999", "INVALID", 1) } returns null

            val result = service.getByPrimaryKey("999", "INVALID", 1)

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("getBySerialYear3AndPolicyNo")
    inner class GetBySerialYear3AndPolicyNo {

        @Test
        fun `should return list of Cvdp`() {
            val entities = listOf(
                createTestCvdp(coverageNo = 1),
                createTestCvdp(coverageNo = 2)
            )
            every { mapper.findBySerialYear3AndPolicyNo("097", "1234567890") } returns entities

            val result = service.getBySerialYear3AndPolicyNo("097", "1234567890")

            assertEquals(2, result.size)
            assertEquals(1, result[0].coverageNo)
            assertEquals(2, result[1].coverageNo)
        }

        @Test
        fun `should return empty list when no data`() {
            every { mapper.findBySerialYear3AndPolicyNo("999", "INVALID") } returns emptyList()

            val result = service.getBySerialYear3AndPolicyNo("999", "INVALID")

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("create")
    inner class Create {

        @Test
        fun `should insert and return affected rows`() {
            val entity = createTestCvdp()
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
            val entity = createTestCvdp()
            every { mapper.update(entity) } returns 1

            val result = service.update(entity, "SYSTEM")

            assertEquals(1, result)
            verify(exactly = 1) { mapper.update(entity) }
        }

        @Test
        fun `should return zero when no record to update`() {
            val entity = createTestCvdp(serialYear3 = "999")
            every { mapper.update(entity) } returns 0

            val result = service.update(entity, "SYSTEM")

            assertEquals(0, result)
        }
    }

    @Nested
    @DisplayName("delete")
    inner class Delete {

        @Test
        fun `should delete and return affected rows`() {
            every { mapper.delete("097", "1234567890", 1) } returns 1

            val result = service.delete("097", "1234567890", 1, "SYSTEM")

            assertEquals(1, result)
            verify(exactly = 1) { mapper.delete("097", "1234567890", 1) }
        }

        @Test
        fun `should return zero when no record to delete`() {
            every { mapper.delete("999", "INVALID", 1) } returns 0

            val result = service.delete("999", "INVALID", 1, "SYSTEM")

            assertEquals(0, result)
        }
    }
}
