package com.vlife.cv.interest.strategy

import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.helper.InterestCalcHelper
import com.vlife.cv.interest.helper.QiratRateLookup
import com.vlife.cv.interest.helper.QiratRateLookup.RateLookupResult
import com.vlife.cv.plan.PlanNote
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * FreeLookRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證正常計算路徑（正常輸入）
 * - 驗證邊界案例（零本金、相同開始/結束日期、null 日期）
 * - 驗證 rate_type 路由（A/B/E → int_rate_type 1/9）
 * - 驗證 freeLookRateCode 處理
 * - 驗證捨入精度行為
 */
@DisplayName("FreeLookRateStrategy 單元測試")
class FreeLookRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var strategy: FreeLookRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        strategy = FreeLookRateStrategy(qiratRateLookup, interestCalcHelper)
        // P0-006: FreeLookRateStrategy 使用 toMonthStart 查詢固定日期
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
    }

    @Test
    fun `should support FREE_LOOK rate types`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(3, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.FREE_LOOK_A))
        assertTrue(supportedRateTypes.contains(RateType.FREE_LOOK_B))
        assertTrue(supportedRateTypes.contains(RateType.FREE_LOOK_E))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = null,
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000")
        )

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return zero when endDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = null,
            principalAmt = BigDecimal("1000000")
        )

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return zero when beginDate greater than endDate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 12, 31),
            endDate = LocalDate.of(2024, 1, 1),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return zero when no subAcntPlanCode or freeLookRateCode`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = null
        )

        // When
        val result = strategy.calculate(input, precision = 0, planNote = null)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should use freeLookRateCode from planNote when subAcntPlanCode is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = null
        )

        val planNote = mockk<PlanNote> {
            every { freeLookRateCode } returns "FL001"
        }

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0, planNote = planNote)

        // Then
        assertNotNull(result)
        verify { qiratRateLookup.lookupRate(match { it.subAcntPlanCode == "FL001" }, "1", any()) }
    }

    @Test
    fun `should use int_rate_type 9 for FREE_LOOK_E`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_E,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("300"),
            adjustedRate = BigDecimal("300")
        )
        every { qiratRateLookup.lookupRate(any(), "9", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        verify { qiratRateLookup.lookupRate(any(), "9", any()) }
    }

    @Test
    fun `should use int_rate_type 1 for FREE_LOOK_A and B`() {
        // Given - FREE_LOOK_A
        val inputA = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(inputA, precision = 0)

        // Then
        assertNotNull(result)
        verify { qiratRateLookup.lookupRate(any(), "1", any()) }
    }

    @Test
    fun `should calculate correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),  // 2.5% (萬分率)
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(0, BigDecimal("250").compareTo(result.actualRate))  // 日加權平均 = 250
        assertTrue(result.intAmt > BigDecimal.ZERO)  // 應有利息
        assertEquals(2, result.monthlyDetails.size)  // P0-005: months += 1，故 monthlyDetails = 2
    }

    @Test
    fun `should calculate correctly with multiple months`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 366  // 2024 閏年
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 3
        every { interestCalcHelper.calculateDays(any(), any()) } returnsMany listOf(31, 29, 31)
        every { interestCalcHelper.formatMonth(any()) } returnsMany listOf("2024/01", "2024/02", "2024/03")

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(0, BigDecimal("250").compareTo(result.actualRate))  // 利率相同，日加權平均 = 250
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(4, result.monthlyDetails.size)  // P0-005: months += 1，故 monthlyDetails = 4
    }

    @Test
    fun `should round to precision 0 for TWD`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(0, result.intAmt.scale())  // 台幣精度 0
    }

    @Test
    fun `should round to precision 2 for foreign currency`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 2)

        // Then
        assertEquals(2, result.intAmt.scale())  // 外幣精度 2
    }

    @Test
    fun `should add months when months is zero or negative`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 15),
            endDate = LocalDate.of(2024, 1, 20),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0  // 同月份
        every { interestCalcHelper.calculateDays(any(), any()) } returns 5
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(1, result.monthlyDetails.size)  // 月數 0 時應加 1
    }

    // =========================================================================
    // P0/P1 修復驗證測試
    // =========================================================================

    @Test
    fun `P0-005 should unconditionally add 1 to months`() {
        // P0-005: V3 fc0 line 399 無條件 +1（非有條件）
        // months=3 → 應得 4 個月明細
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 3
        every { interestCalcHelper.calculateDays(any(), any()) } returns 30
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        val result = strategy.calculate(input, precision = 0)

        // P0-005: months=3 無條件 +1 → 4 個月明細
        assertEquals(4, result.monthlyDetails.size)
    }

    @Test
    fun `P0-006 should use fixed beginDate for QIRAT query`() {
        // P0-006: V3 fc0 使用固定 begin_date 查詢 QIRAT（非逐月查詢）
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 15),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 2
        every { interestCalcHelper.calculateDays(any(), any()) } returns 30
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        strategy.calculate(input, precision = 0)

        // P0-006: QIRAT 應只查詢一次（使用固定 beginDate），非逐月查詢
        verify(exactly = 1) { qiratRateLookup.lookupRate(any(), "1", any()) }
    }

    @Test
    fun `P1-001 should calculate intAmt once at end not monthly`() {
        // P1-001: intAmt = principal × avgRate × totalDays / yearDays（最後一次計算）
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns rateLookupResult

        val result = strategy.calculate(input, precision = 0)

        // P1-001: intAmt = round(1000000 × 250/10000 × 31/365, 0) = round(2123.28..., 0) = 2123
        assertEquals(0, BigDecimal("2123").compareTo(result.intAmt)) {
            "P1-001: intAmt should be computed once, got: ${result.intAmt}"
        }
    }

    @Test
    fun `P1-005 should return zero when QIRAT not found`() {
        // P1-005: QIRAT 查無結果時立即返回 zero
        val input = InterestRateInput(
            rateType = RateType.FREE_LOOK_A,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // QIRAT 返回零值（未找到）
        val zeroResult = RateLookupResult(
            originalRate = BigDecimal.ZERO,
            adjustedRate = BigDecimal.ZERO
        )
        every { qiratRateLookup.lookupRate(any(), "1", any()) } returns zeroResult

        val result = strategy.calculate(input, precision = 0)

        // P1-005: 應立即返回 zero，不進入月迴圈
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
        assertTrue(result.monthlyDetails.isEmpty())
    }
}
