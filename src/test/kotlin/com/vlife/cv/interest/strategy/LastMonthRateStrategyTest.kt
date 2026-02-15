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
 * LastMonthRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證最後月利率計算邏輯
 * - 驗證已知利率處理（actualRate != 0）
 * - 驗證 QIRAT 查詢（actualRate == 0）
 * - 驗證邊界案例（null 日期、beginDate > endDate）
 * - 驗證 int_rate_type 路由（'2' for 貸款利率）
 * - 驗證精度測試
 */
@DisplayName("LastMonthRateStrategy 單元測試")
class LastMonthRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var strategy: LastMonthRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        strategy = LastMonthRateStrategy(qiratRateLookup, interestCalcHelper)
    }

    @Test
    fun `should support LOAN_RATE_LAST_MONTH`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(1, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.LOAN_RATE_LAST_MONTH))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
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
            rateType = RateType.LOAN_RATE_LAST_MONTH,
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
            rateType = RateType.LOAN_RATE_LAST_MONTH,
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
    fun `should use actualRate when provided`() {
        // Given - 已知利率 = 200
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")  // 已知利率
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 使用已知利率，不查詢 QIRAT
        assertEquals(BigDecimal("200"), result.actualRate)
        assertTrue(result.intAmt > BigDecimal.ZERO)
        verify(exactly = 0) { qiratRateLookup.lookupRate(any(), any(), any()) }
    }

    @Test
    fun `should query QIRAT when actualRate is zero`() {
        // Given - actualRate = 0
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("200")  // 減碼後
        )
        every { qiratRateLookup.lookupRate(any(), "2", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 查詢 QIRAT type='2'
        verify(exactly = 1) { qiratRateLookup.lookupRate(any(), "2", any()) }
        assertEquals(BigDecimal("200"), result.actualRate)  // adjustedRate
    }

    @Test
    fun `should use int_rate_type 2 for loan rate query`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "2", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then - 驗證使用 int_rate_type='2'
        verify { qiratRateLookup.lookupRate(any(), "2", any()) }
    }

    @Test
    fun `should calculate correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("250"), result.actualRate)
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should calculate correctly with multiple months`() {
        // Given - 跨 3 個月
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 366  // 2024 閏年
        every { interestCalcHelper.calculateDays(any(), any()) } returns 91  // 總天數
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 3, 1)

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("250"), result.actualRate)
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should return zero intAmt when principal is zero`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal.ZERO,  // 本金 = 0
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal("250"), result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)  // 本金 = 0 時利息 = 0
    }

    @Test
    fun `should return zero intAmt when days is zero`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 1),  // 同一天
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 0  // 天數 = 0
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal("250"), result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)  // 天數 = 0 時利息 = 0
    }

    @Test
    fun `should round to precision 0 for TWD`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(0, result.intAmt.scale())  // 台幣精度 0
    }

    @Test
    fun `should round to precision 2 for foreign currency`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // When
        val result = strategy.calculate(input, precision = 2)

        // Then
        assertEquals(2, result.intAmt.scale())  // 外幣精度 2
    }

    @Test
    fun `should return empty monthlyDetails`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 最後月利率不回傳月份明細
        assertTrue(result.monthlyDetails.isEmpty())
    }

    @Test
    fun `should adjust endDate to month start for QIRAT query`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_LAST_MONTH,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 25),  // 月中某日
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateDays(any(), any()) } returns 25
        every { interestCalcHelper.toMonthStart(LocalDate.of(2024, 1, 25)) } returns LocalDate.of(2024, 1, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "2", LocalDate.of(2024, 1, 1)) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then - 驗證呼叫了 toMonthStart
        verify(exactly = 1) { interestCalcHelper.toMonthStart(LocalDate.of(2024, 1, 25)) }
        verify(exactly = 1) { qiratRateLookup.lookupRate(any(), "2", LocalDate.of(2024, 1, 1)) }
    }
}
