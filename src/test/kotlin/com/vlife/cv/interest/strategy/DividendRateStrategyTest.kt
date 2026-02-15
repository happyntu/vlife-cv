package com.vlife.cv.interest.strategy

import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.helper.InterestCalcHelper
import com.vlife.cv.interest.helper.QiratRateLookup
import com.vlife.cv.interest.helper.RateLookupResult
import com.vlife.cv.plan.Pldf
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
 * DividendRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證分紅利率月加權平均計算邏輯
 * - 驗證雙重路由（insurance_type_3='G' → int_rate_type='5'，否則='0'）
 * - 驗證已知利率處理（actualRate != 0）
 * - 驗證 QIRAT 查詢（actualRate == 0）
 * - 驗證邊界案例（null 日期、beginDate > endDate）
 * - 驗證 intAmt = 0、monthlyDetails = empty
 * - 驗證 months <= 0 時調整
 */
@DisplayName("DividendRateStrategy 單元測試")
class DividendRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var strategy: DividendRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        strategy = DividendRateStrategy(qiratRateLookup, interestCalcHelper)
    }

    @Test
    fun `should support DIVIDEND_RATE`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(1, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.DIVIDEND_RATE))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
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
            rateType = RateType.DIVIDEND_RATE,
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
            rateType = RateType.DIVIDEND_RATE,
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
    fun `should use int_rate_type 5 when insurance_type_3 is G`() {
        // Given - 利變年金
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "G"
        }

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0, plan = plan)

        // Then - 驗證使用 int_rate_type='5'
        verify { qiratRateLookup.lookupRate(any(), "5", any()) }
    }

    @Test
    fun `should use int_rate_type 0 when insurance_type_3 is not G`() {
        // Given - 非利變年金
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
        }

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0, plan = plan)

        // Then - 驗證使用 int_rate_type='0'
        verify { qiratRateLookup.lookupRate(any(), "0", any()) }
    }

    @Test
    fun `should use int_rate_type 0 when insurance_type_3 is null`() {
        // Given - 無 insurance_type_3
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns null
        }

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0, plan = plan)

        // Then - 驗證使用 int_rate_type='0'
        verify { qiratRateLookup.lookupRate(any(), "0", any()) }
    }

    @Test
    fun `should use actualRate when provided`() {
        // Given - 已知利率
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal("200")  // 已知利率
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 使用已知利率，不查詢 QIRAT
        assertEquals(BigDecimal("200.0000000000"), result.actualRate)
        verify(exactly = 0) { qiratRateLookup.lookupRate(any(), any(), any()) }
    }

    @Test
    fun `should query QIRAT when actualRate is zero`() {
        // Given - actualRate = 0
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000"),
            actualRate = BigDecimal.ZERO
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("200")  // 減碼後
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 查詢 QIRAT
        verify(exactly = 1) { qiratRateLookup.lookupRate(any(), "0", any()) }
        assertEquals(BigDecimal("200.0000000000"), result.actualRate)  // adjustedRate
    }

    @Test
    fun `should calculate correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 單月平均 = 250 / 1 = 250
        assertNotNull(result)
        assertEquals(BigDecimal("250.0000000000"), result.actualRate)
    }

    @Test
    fun `should calculate correctly with multiple months same rate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 3

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 3 個月相同利率，平均 = 750 / 3 = 250
        assertNotNull(result)
        assertEquals(BigDecimal("250.0000000000"), result.actualRate)
    }

    @Test
    fun `should calculate correctly with multiple months different rates`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 3, 31),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 3

        // 第 1 月 200，第 2 月 250，第 3 月 300
        val rates = listOf(BigDecimal("200"), BigDecimal("250"), BigDecimal("300"))
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returnsMany rates.map {
            RateLookupResult(originalRate = it, adjustedRate = it)
        }

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 平均 = (200 + 250 + 300) / 3 = 750 / 3 = 250
        assertNotNull(result)
        assertEquals(BigDecimal("250.0000000000"), result.actualRate)
    }

    @Test
    fun `should add months when months is zero or negative`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 15),
            endDate = LocalDate.of(2024, 1, 20),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0  // 同月份

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 月數 0 時應加 1
        assertNotNull(result)
        verify(exactly = 1) { qiratRateLookup.lookupRate(any(), "0", any()) }
    }

    @Test
    fun `should return zero intAmt`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 分紅利率不計算利息
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return empty monthlyDetails`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 月加權不回傳明細
        assertTrue(result.monthlyDetails.isEmpty())
    }

    @Test
    fun `should handle zero rates correctly`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DIVIDEND_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        // Mock dependencies
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal.ZERO,
            adjustedRate = BigDecimal.ZERO
        )
        every { qiratRateLookup.lookupRate(any(), "0", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 平均 = 0 / 1 = 0
        assertEquals(BigDecimal("0.0000000000"), result.actualRate)
    }
}
