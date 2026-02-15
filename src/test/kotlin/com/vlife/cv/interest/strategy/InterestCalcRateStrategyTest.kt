package com.vlife.cv.interest.strategy

import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.helper.InterestCalcHelper
import com.vlife.cv.interest.helper.QiratRateLookup
import com.vlife.cv.interest.helper.RateLookupResult
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
 * InterestCalcRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證計息利率日加權計算邏輯
 * - 驗證利息金額計算
 * - 驗證已知利率處理（actualRate != 0）
 * - 驗證 QIRAT 查詢（actualRate == 0）
 * - 驗證邊界案例（null 日期、beginDate >= endDate）
 * - 驗證 int_rate_type 路由（'0' for 計息利率）
 * - 驗證 monthlyDetails 包含資料
 */
@DisplayName("InterestCalcRateStrategy 單元測試")
class InterestCalcRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var strategy: InterestCalcRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        strategy = InterestCalcRateStrategy(qiratRateLookup, interestCalcHelper)
    }

    @Test
    fun `should support INTEREST_CALC_RATE`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(1, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.INTEREST_CALC_RATE))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
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
            rateType = RateType.INTEREST_CALC_RATE,
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
    fun `should return zero when beginDate equals endDate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 1),
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
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 12, 31),
            endDate = LocalDate.of(2024, 1, 1),
            principalAmt = BigDecimal("1000000")
        )

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should use int_rate_type 0 for interest calc rate query`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then - 驗證使用 int_rate_type='0'
        verify { qiratRateLookup.lookupRate(any(), "0", any()) }
    }

    @Test
    fun `should use actualRate when provided`() {
        // Given - 已知利率
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")  // 已知利率
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 使用已知利率，不查詢 QIRAT
        verify(exactly = 0) { qiratRateLookup.lookupRate(any(), any(), any()) }
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should query QIRAT when actualRate is zero`() {
        // Given - actualRate = 0
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("200")  // 減碼後
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 查詢 QIRAT type='0'
        verify(exactly = 1) { qiratRateLookup.lookupRate(any(), "0", any()) }
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should calculate correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(1, result.monthlyDetails.size)
    }

    @Test
    fun `should calculate correctly with multiple months`() {
        // Given - 跨 3 個月
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 366  // 2024 閏年
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 2
        every { interestCalcHelper.calculateDays(any(), any()) } returnsMany listOf(31, 29, 31)
        every { interestCalcHelper.formatMonth(any()) } returnsMany listOf("2024/01", "2024/02", "2024/03")

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(3, result.monthlyDetails.size)
    }

    @Test
    fun `should calculate day-weighted average rate correctly`() {
        // Given - 不同月份不同利率
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 2, 29),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 366
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returnsMany listOf(31, 29)
        every { interestCalcHelper.formatMonth(any()) } returnsMany listOf("2024/01", "2024/02")

        // 第 1 月 200，第 2 月 300
        val rates = listOf(BigDecimal("200"), BigDecimal("300"))
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returnsMany rates.map {
            RateLookupResult(originalRate = it, adjustedRate = it)
        }

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 日加權平均 = (200*31 + 300*29) / (31+29) = 14900 / 60 = 248.33...
        assertNotNull(result)
        assertTrue(result.actualRate > BigDecimal("248"))
        assertTrue(result.actualRate < BigDecimal("249"))
    }

    @Test
    fun `should calculate intAmt using day-weighted average rate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - intAmt = 1000000 × (250/10000) × (31/365)
        assertNotNull(result)
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should round to precision 0 for TWD`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 台幣精度 0
        assertEquals(0, result.intAmt.scale())
    }

    @Test
    fun `should round to precision 2 for foreign currency`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 2)

        // Then - 外幣精度 2
        assertEquals(2, result.intAmt.scale())
    }

    @Test
    fun `should return zero intAmt when principal is zero`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal.ZERO,  // 本金 = 0
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return monthlyDetails with data`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.INTEREST_CALC_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 2, 29),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 366
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returnsMany listOf(31, 29)
        every { interestCalcHelper.formatMonth(any()) } returnsMany listOf("2024/01", "2024/02")

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - monthlyDetails 包含資料
        assertEquals(2, result.monthlyDetails.size)
        assertEquals(31, result.monthlyDetails[0].days)
        assertEquals(29, result.monthlyDetails[1].days)
        // f12 不記錄逐月利息（intAmt = 0）
        assertEquals(BigDecimal.ZERO, result.monthlyDetails[0].intAmt)
        assertEquals(BigDecimal.ZERO, result.monthlyDetails[1].intAmt)
    }
}
