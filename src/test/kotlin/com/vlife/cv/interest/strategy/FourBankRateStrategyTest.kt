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
 * FourBankRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證四行庫利率計算邏輯
 * - 驗證 f20 前置執行契約（input.actualRate 使用）
 * - 驗證 Fallback 機制（actualRate == 0 時）
 * - 驗證雙重利率使用（f20 for intAmt, f40 for actualRate）
 * - 驗證邊界案例（null 日期、beginDate > endDate）
 * - 驗證 int_rate_type 路由（'0' for 四行庫利率）
 * - 驗證月數上限（> 120 調整）
 */
@DisplayName("FourBankRateStrategy 單元測試")
class FourBankRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var strategy: FourBankRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        strategy = FourBankRateStrategy(qiratRateLookup, interestCalcHelper)
    }

    @Test
    fun `should support FOUR_BANK_RATE`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(1, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.FOUR_BANK_RATE))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
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
            rateType = RateType.FOUR_BANK_RATE,
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
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 12, 31),
            endDate = LocalDate.of(2024, 1, 1),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")  // f20 前置利率
        )

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should use int_rate_type 0 for four bank rate query`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")  // f20 前置利率
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then - 驗證使用 int_rate_type='0'
        verify { qiratRateLookup.lookupRate(any(), "0", any()) }
    }

    @Test
    fun `should use f20 rate for interest calculation and f40 rate for actualRate`() {
        // Given - f20 前置利率 = 200, f40 四行庫利率 = 180
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")  // f20 前置利率（用於 intAmt 計算）
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val f40RateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),  // f40 四行庫利率（用於 actualRate 計算）
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns f40RateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        // actualRate 應為 f40 四行庫利率日加權平均 = 180
        assertEquals(BigDecimal("180"), result.actualRate)
        // intAmt 應使用 f20 利率計算
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should use fallback mechanism when actualRate is zero`() {
        // Given - actualRate = 0（f20 未執行）
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO  // f20 未設定
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val f20FallbackResult = RateLookupResult(
            originalRate = BigDecimal("200"),
            adjustedRate = BigDecimal("200")
        )
        val f40RateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        // Fallback 查詢 type='2'（與 f20 相同）
        every { qiratRateLookup.lookupRate(any(), "2", any()) } returns f20FallbackResult
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns f40RateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 驗證使用 Fallback type='2'
        verify { qiratRateLookup.lookupRate(any(), "2", any()) }
        verify { qiratRateLookup.lookupRate(any(), "0", any()) }
        assertEquals(BigDecimal("180"), result.actualRate)  // f40 四行庫利率
    }

    @Test
    fun `should cap months at 120 when months exceeds 120`() {
        // Given - 設計超過 120 個月的輸入
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2014, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),  // 超過 10 年
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 131  // 131 個月
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2014, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2014/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 驗證月數上限為 120
        assertEquals(120, result.monthlyDetails.size)
    }

    @Test
    fun `should not cap months when months equals 120`() {
        // Given - 設計正好 120 個月的輸入
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2014, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 119  // 119 + 1 = 120
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2014, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2014/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 驗證月數 = 120（不應觸發上限調整）
        assertEquals(120, result.monthlyDetails.size)
    }

    @Test
    fun `should calculate correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("180"), result.actualRate)  // f40 四行庫利率
        assertTrue(result.intAmt > BigDecimal.ZERO)  // 使用 f20 利率計算
        assertEquals(1, result.monthlyDetails.size)
    }

    @Test
    fun `should calculate correctly with multiple months`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 366  // 2024 閏年
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 2
        every { interestCalcHelper.calculateDays(any(), any()) } returnsMany listOf(31, 29, 31)
        every { interestCalcHelper.toMonthStart(any()) } returnsMany listOf(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 3, 1)
        )
        every { interestCalcHelper.formatMonth(any()) } returnsMany listOf("2024/01", "2024/02", "2024/03")

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("180"), result.actualRate)  // 利率相同，日加權平均 = 180
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(3, result.monthlyDetails.size)
    }

    @Test
    fun `should round to precision 0 for TWD`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(0, result.intAmt.scale())  // 台幣精度 0
    }

    @Test
    fun `should round to precision 2 for foreign currency`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.FOUR_BANK_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("180"),
            adjustedRate = BigDecimal("180")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 2)

        // Then
        assertEquals(2, result.intAmt.scale())  // 外幣精度 2
    }
}
