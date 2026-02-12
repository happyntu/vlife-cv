package com.vlife.cv.cvet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CvetService 單元測試
 *
 * 使用 MockK mock CvetMapper，驗證展期定期 Service 層邏輯。
 */
@DisplayName("CvetService 單元測試")
class CvetServiceTest {

    private lateinit var mapper: CvetMapper
    private lateinit var service: CvetService

    @BeforeEach
    fun setup() {
        mapper = mockk()
        service = CvetService(mapper)
    }

    private fun createTestCvet(
        policyNo: String = "A123456789",
        coverageNo: Int = ExtendedTermPolicy.MAIN_COVERAGE,
        etFaceAmt: BigDecimal? = BigDecimal("1000000.00"),
        etEffectDate: LocalDate? = LocalDate.of(2020, 1, 1),
        etExpiredDate: LocalDate? = LocalDate.of(2040, 12, 31)
    ) = ExtendedTermPolicy(
        policyNo = policyNo,
        coverageNo = coverageNo,
        etTso = null,
        etFaceAmt = etFaceAmt,
        etEffectDate = etEffectDate,
        etExpiredDate = etExpiredDate,
        etPuaDate = null,
        etPuaValue = null,
        etLoanAmt = BigDecimal.ZERO,
        etAplAmt = BigDecimal.ZERO,
        etDisabBenef = null,
        policyType = "L"
    )

    @Nested
    @DisplayName("findByPolicyAndCoverage")
    inner class FindByPolicyAndCoverage {

        @Test
        fun `should return ExtendedTermPolicy when exists`() {
            val cvet = createTestCvet()
            every { mapper.findByPolicyAndCoverage("A123456789", 1) } returns cvet

            val result = service.findByPolicyAndCoverage("A123456789", 1)

            assertNotNull(result)
            assertEquals("A123456789", result.policyNo)
            assertEquals(1, result.coverageNo)
            assertEquals(BigDecimal("1000000.00"), result.etFaceAmt)
        }

        @Test
        fun `should return null when not exists`() {
            every { mapper.findByPolicyAndCoverage("ZZZZZZZZZZ", 1) } returns null

            val result = service.findByPolicyAndCoverage("ZZZZZZZZZZ", 1)

            assertNull(result)
        }
    }

    @Nested
    @DisplayName("isExtendedTerm")
    inner class IsExtendedTerm {

        @Test
        fun `should return true when extended term exists`() {
            every { mapper.findByPolicyAndCoverage("A123456789", 1) } returns createTestCvet()

            val result = service.isExtendedTerm("A123456789", 1)

            assertTrue(result)
        }

        @Test
        fun `should return false when not extended term`() {
            every { mapper.findByPolicyAndCoverage("B987654321", 1) } returns null

            val result = service.isExtendedTerm("B987654321", 1)

            assertFalse(result)
        }
    }

    @Nested
    @DisplayName("countByPolicyNo")
    inner class CountByPolicyNo {

        @Test
        fun `should return count`() {
            every { mapper.countByPolicyNo("A123456789") } returns 2

            val result = service.countByPolicyNo("A123456789")

            assertEquals(2, result)
        }

        @Test
        fun `should return zero when no records`() {
            every { mapper.countByPolicyNo("ZZZZZZZZZZ") } returns 0

            val result = service.countByPolicyNo("ZZZZZZZZZZ")

            assertEquals(0, result)
        }
    }

    @Nested
    @DisplayName("insert")
    inner class Insert {

        @Test
        fun `should insert successfully`() {
            val cvet = createTestCvet()
            every { mapper.insert(cvet) } returns 1

            service.insert(cvet)

            verify(exactly = 1) { mapper.insert(cvet) }
        }

        @Test
        fun `should throw when insert fails`() {
            val cvet = createTestCvet()
            every { mapper.insert(cvet) } returns 0

            val ex = assertThrows<IllegalStateException> {
                service.insert(cvet)
            }

            assertTrue(ex.message!!.contains("新增展期定期記錄失敗"))
            assertTrue(ex.message!!.contains("A123456789"))
        }
    }

    @Nested
    @DisplayName("deleteByPolicyAndCoverage")
    inner class DeleteByPolicyAndCoverage {

        @Test
        fun `should delete successfully`() {
            every { mapper.deleteByPolicyAndCoverage("A123456789", 1) } returns 1

            service.deleteByPolicyAndCoverage("A123456789", 1)

            verify(exactly = 1) { mapper.deleteByPolicyAndCoverage("A123456789", 1) }
        }

        @Test
        fun `should throw when delete fails`() {
            every { mapper.deleteByPolicyAndCoverage("ZZZZZZZZZZ", 1) } returns 0

            val ex = assertThrows<IllegalStateException> {
                service.deleteByPolicyAndCoverage("ZZZZZZZZZZ", 1)
            }

            assertTrue(ex.message!!.contains("刪除展期定期記錄失敗"))
            assertTrue(ex.message!!.contains("ZZZZZZZZZZ"))
        }
    }
}
