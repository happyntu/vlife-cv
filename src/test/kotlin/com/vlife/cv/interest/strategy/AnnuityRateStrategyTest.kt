package com.vlife.cv.interest.strategy

import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.helper.InterestCalcHelper
import com.vlife.cv.interest.helper.QiratRateLookup
import com.vlife.cv.interest.helper.QiratRateLookup.RateLookupResult
import com.vlife.cv.plnd.PlndService
import com.vlife.cv.quote.QmfdeService
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
 * AnnuityRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證複利計算路徑（COMPOUND_RATE, POWER 公式）
 * - 驗證線性計算路徑（ANNUITY_RATE_D, 單利）
 * - 驗證邊界案例（null 日期、beginDate >= endDate）
 * - 驗證 int_rate_type 路由（'5' for COMPOUND_RATE, '8' for ANNUITY_RATE_D）
 * - 驗證捨入精度行為
 * - 驗證 months <= 0 時調整行為
 */
@DisplayName("AnnuityRateStrategy 單元測試")
class AnnuityRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var plndService: PlndService
    private lateinit var qmfdeService: QmfdeService
    private lateinit var strategy: AnnuityRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        plndService = mockk()
        qmfdeService = mockk()
        strategy = AnnuityRateStrategy(qiratRateLookup, interestCalcHelper, plndService, qmfdeService)
    }

    @Test
    fun `should support ANNUITY_RATE_D and COMPOUND_RATE`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(2, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.ANNUITY_RATE_D))
        assertTrue(supportedRateTypes.contains(RateType.COMPOUND_RATE))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
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
            rateType = RateType.COMPOUND_RATE,
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
            rateType = RateType.COMPOUND_RATE,
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
            rateType = RateType.COMPOUND_RATE,
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
    fun `should use int_rate_type 5 for COMPOUND_RATE`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then
        verify { qiratRateLookup.lookupRate(any(), "5", any()) }
    }

    @Test
    fun `should use int_rate_type 8 for ANNUITY_RATE_D`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.ANNUITY_RATE_D,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "8", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then
        verify { qiratRateLookup.lookupRate(any(), "8", any()) }
    }

    @Test
    fun `should calculate compound interest correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        // P0-004: 複利 actualRate = (Π - 1) × 10000
        // rateFactor = (1 + 250/10000)^(31/365) = 1.025^0.0849... ≈ 1.002094
        // actualRate = (1.002094 - 1) × 10000 ≈ 20.94
        assertTrue(result.actualRate >= BigDecimal("20") && result.actualRate <= BigDecimal("21"))
        assertTrue(result.intAmt > BigDecimal.ZERO)  // 應有複利利息
        assertEquals(1, result.monthlyDetails.size)
    }

    @Test
    fun `should calculate compound interest correctly with multiple months`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        // P0-004: 複利 actualRate = (Π - 1) × 10000
        // P1-004: 每月重算 yearDays（處理閏年），導致計算結果與原先預期略有不同
        // 3個月累乘（2024閏年，366天）：
        // Month 1: 31天, Month 2: 29天, Month 3: 31天
        // actualRate ≈ 61.58（實測值）
        // 使用寬容範圍檢查（考慮BigDecimal精度與P1-004改進）
        assertTrue(
            result.actualRate.compareTo(BigDecimal("60")) >= 0 &&
            result.actualRate.compareTo(BigDecimal("62")) <= 0,
            "actualRate should be ~61.58 (with P1-004 yearDays recalculation), got: ${result.actualRate}"
        )
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(3, result.monthlyDetails.size)
    }

    @Test
    fun `should calculate linear interest correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.ANNUITY_RATE_D,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "8", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(0, BigDecimal("250").compareTo(result.actualRate))  // 日加權平均 = 250
        assertTrue(result.intAmt > BigDecimal.ZERO)  // 應有線性利息
        assertEquals(1, result.monthlyDetails.size)
    }

    @Test
    fun `should calculate linear interest correctly with multiple months`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.ANNUITY_RATE_D,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "8", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertNotNull(result)
        assertEquals(0, BigDecimal("250").compareTo(result.actualRate))  // 利率相同，日加權平均 = 250
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(3, result.monthlyDetails.size)
    }

    @Test
    fun `should round to precision 0 for TWD (compound)`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(0, result.intAmt.scale())  // 台幣精度 0
    }

    @Test
    fun `should round to precision 2 for foreign currency (compound)`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 2)

        // Then
        assertEquals(2, result.intAmt.scale())  // 外幣精度 2
    }

    @Test
    fun `should round to precision 0 for TWD (linear)`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.ANNUITY_RATE_D,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "8", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(0, result.intAmt.scale())  // 台幣精度 0（逐月 ROUND）
    }

    @Test
    fun `should round to precision 2 for foreign currency (linear)`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.ANNUITY_RATE_D,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
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
        every { qiratRateLookup.lookupRate(any(), "8", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 2)

        // Then
        assertEquals(2, result.intAmt.scale())  // 外幣精度 2
    }

    @Test
    fun `should add months when months is zero or negative (compound)`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 15),
            endDate = LocalDate.of(2024, 1, 20),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0  // 同月份
        every { interestCalcHelper.calculateDays(any(), any()) } returns 5
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(1, result.monthlyDetails.size)  // 月數 0 時應加 1
    }

    @Test
    fun `should add months when months is zero or negative (linear)`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.ANNUITY_RATE_D,
            beginDate = LocalDate.of(2024, 1, 15),
            endDate = LocalDate.of(2024, 1, 20),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0  // 同月份
        every { interestCalcHelper.calculateDays(any(), any()) } returns 5
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "8", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(1, result.monthlyDetails.size)  // 月數 0 時應加 1
    }

    @Test
    fun `should return zero for unsupported rate type`() {
        // Given - 使用不支援的 RateType
        val input = InterestRateInput(
            rateType = RateType.LOAN_RATE_MONTHLY,  // 不支援的類型
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    // =========================================================================
    // P0-007 修復驗證：BigDecimal 精度
    // =========================================================================

    @Test
    fun `P0-007 compound interest should use BigDecimal precision`() {
        // P0-007: 複利計算使用 BigDecimal 精確次方（非 Math.pow double）
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("100000000")  // 1 億（大金額測試精度）
        )

        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        val result = strategy.calculate(input, precision = 0)

        // P0-007: 大金額複利不應有浮點累積誤差
        // rate_factor = (1 + 250/10000)^(31/365) = 1.025^0.08493...
        // intAmt = round(100000000 × (rateFactor - 1), 0)
        assertTrue(result.intAmt > BigDecimal.ZERO)
        // 確認 intAmt 規模合理：2.5% × 31/365 × 100M ≈ 212,329
        assertTrue(result.intAmt > BigDecimal("200000"))
        assertTrue(result.intAmt < BigDecimal("230000"))
    }

    @Test
    fun `P0-007 compound multi-month should accumulate BigDecimal factors`() {
        // P0-007: 多月複利因子累乘使用 BigDecimal（非 Double 累乘）
        val input = InterestRateInput(
            rateType = RateType.COMPOUND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000")
        )

        every { interestCalcHelper.calculateYearDays(any()) } returns 366  // 2024 閏年
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 3
        every { interestCalcHelper.calculateDays(any(), any()) } returnsMany listOf(31, 29, 31)
        every { interestCalcHelper.formatMonth(any()) } returnsMany listOf("2024/01", "2024/02", "2024/03")

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        val result = strategy.calculate(input, precision = 0)

        // 3 個月複利：total days = 91, yearDays = 366
        // 複利因子 ≈ (1.025)^(31/366) × (1.025)^(29/366) × (1.025)^(31/366)
        // ≈ (1.025)^(91/366) ≈ 1.00619...
        // intAmt ≈ 1000000 × 0.00619... ≈ 6190
        assertTrue(result.intAmt > BigDecimal("6000"))
        assertTrue(result.intAmt < BigDecimal("6500"))
        assertEquals(3, result.monthlyDetails.size)
    }
}
