package com.vlife.cv.interest.strategy

import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.helper.InterestCalcHelper
import com.vlife.cv.interest.helper.QiratRateLookup
import com.vlife.cv.interest.helper.QiratRateLookup.RateLookupResult
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
 * LoanRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證貸款利率日加權計算邏輯
 * - 驗證累計後一次 ROUND（非逐月 ROUND）
 * - 驗證已知利率處理（actualRate != 0）
 * - 驗證 QIRAT 查詢（actualRate == 0）
 * - 驗證負利率檢查
 * - 驗證邊界案例（null 日期、beginDate > endDate）
 * - 驗證 int_rate_type 路由（'2' for 貸款利率）
 * - 驗證月底調整與天數計算
 */
@DisplayName("LoanRateStrategy 單元測試")
class LoanRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var strategy: LoanRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        strategy = LoanRateStrategy(qiratRateLookup, interestCalcHelper)
    }

    @Test
    fun `should support LOAN_RATE_MONTHLY and LOAN_RATE_MONTHLY_V2`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(2, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.LOAN_RATE_MONTHLY))
        assertTrue(supportedRateTypes.contains(RateType.LOAN_RATE_MONTHLY_V2))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
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
            rateType = RateType.LOAN_RATE_MONTHLY,
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
            rateType = RateType.LOAN_RATE_MONTHLY,
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
    fun `should use int_rate_type 2 for loan rate query`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

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
    fun `should use actualRate when provided`() {
        // Given - 已知利率
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200"),  // 已知利率
            rateSub = BigDecimal.ZERO,
            rateDisc = BigDecimal("100")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 使用已知利率，不查詢 QIRAT
        verify(exactly = 0) { qiratRateLookup.lookupRate(any(), any(), any()) }
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should apply rate_sub and rate_disc when actualRate is provided`() {
        // Given - 已知利率 + 減碼 + 折扣
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("300"),  // 原始利率
            rateSub = BigDecimal("50"),      // 減碼
            rateDisc = BigDecimal("90")      // 折扣 90%
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // Mock applyDiscounts
        every { qiratRateLookup.applyDiscounts(any(), any(), any()) } returns BigDecimal("200")

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 驗證呼叫 applyDiscounts
        verify { qiratRateLookup.applyDiscounts(BigDecimal("300"), BigDecimal("50"), BigDecimal("90")) }
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should query QIRAT when actualRate is zero`() {
        // Given - actualRate = 0
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("200")  // 減碼後
        )
        every { qiratRateLookup.lookupRate(any(), "2", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 查詢 QIRAT type='2'
        verify(exactly = 1) { qiratRateLookup.lookupRate(any(), "2", any()) }
        assertTrue(result.intAmt > BigDecimal.ZERO)
    }

    @Test
    fun `should handle negative rate correctly`() {
        // Given - 負利率
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("-50"),  // 負利率
            adjustedRate = BigDecimal("-50")
        )
        every { qiratRateLookup.lookupRate(any(), "2", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 負利率調整為 0
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should calculate correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
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
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
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

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(3, result.monthlyDetails.size)
    }

    @Test
    fun `should round to precision 0 for TWD`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 台幣精度 0（累計後一次 ROUND）
        assertEquals(0, result.intAmt.scale())
    }

    @Test
    fun `should round to precision 2 for foreign currency`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 1, 1)
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        // When
        val result = strategy.calculate(input, precision = 2)

        // Then - 外幣精度 2
        assertEquals(2, result.intAmt.scale())
    }

    @Test
    fun `should add 1 day for non-last month`() {
        // Given - 跨 2 個月
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 2, 28),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("250")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returnsMany listOf(31, 28)
        every { interestCalcHelper.toMonthStart(any()) } returnsMany listOf(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 2, 1)
        )
        every { interestCalcHelper.formatMonth(any()) } returnsMany listOf("2024/01", "2024/02")

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 第 1 月天數應 +1（非最後月）
        assertEquals(2, result.monthlyDetails.size)
        assertEquals(32, result.monthlyDetails[0].days)  // 31 + 1
        assertEquals(28, result.monthlyDetails[1].days)  // 最後月不加 1
    }
}
