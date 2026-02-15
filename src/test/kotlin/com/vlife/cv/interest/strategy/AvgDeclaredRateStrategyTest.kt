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
 * AvgDeclaredRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock QiratRateLookup 和 InterestCalcHelper
 * - 驗證宣告利率 12 月平均計算邏輯
 * - 驗證從 end_date 往前推 12 個月
 * - 驗證使用 originalRate（非 adjustedRate）
 * - 驗證 intAmt = 0、monthlyDetails = empty
 * - 驗證邊界案例（null 日期）
 * - 驗證 int_rate_type 路由（'5' for 宣告利率）
 */
@DisplayName("AvgDeclaredRateStrategy 單元測試")
class AvgDeclaredRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var strategy: AvgDeclaredRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        strategy = AvgDeclaredRateStrategy(qiratRateLookup, interestCalcHelper)
    }

    @Test
    fun `should support AVG_DECLARED_RATE`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(1, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.AVG_DECLARED_RATE))
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
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
            rateType = RateType.AVG_DECLARED_RATE,
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
    fun `should use int_rate_type 5 for declared rate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then - 驗證使用 int_rate_type='5'
        verify(exactly = 12) { qiratRateLookup.lookupRate(any(), "5", any()) }
    }

    @Test
    fun `should use originalRate not adjustedRate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        // originalRate = 300, adjustedRate = 250（rate_disc 後）
        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("300"),  // 應使用這個
            adjustedRate = BigDecimal("250")   // 不應使用這個
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 應使用 originalRate
        // 平均 = 300 × 12 / 12 = 300
        assertEquals(BigDecimal("300.0000000000"), result.actualRate)
    }

    @Test
    fun `should calculate 12 month average correctly with same rate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 12 個月相同利率，平均 = 250
        assertEquals(BigDecimal("250.0000000000"), result.actualRate)
    }

    @Test
    fun `should calculate 12 month average correctly with different rates`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        // 前 6 個月 200，後 6 個月 300
        val rates = listOf(
            BigDecimal("300"), BigDecimal("300"), BigDecimal("300"),
            BigDecimal("300"), BigDecimal("300"), BigDecimal("300"),
            BigDecimal("200"), BigDecimal("200"), BigDecimal("200"),
            BigDecimal("200"), BigDecimal("200"), BigDecimal("200")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returnsMany rates.map {
            RateLookupResult(originalRate = it, adjustedRate = it)
        }

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 平均 = (300×6 + 200×6) / 12 = 3000 / 12 = 250
        assertEquals(BigDecimal("250.0000000000"), result.actualRate)
    }

    @Test
    fun `should return zero intAmt`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - f50 僅計算利率，不計算利息
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return empty monthlyDetails`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 不回傳月份明細
        assertTrue(result.monthlyDetails.isEmpty())
    }

    @Test
    fun `should query 12 months backward from endDate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies - 調整至月初後為 2024/12/01
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then - 驗證查詢了 12 次
        verify(exactly = 12) { qiratRateLookup.lookupRate(any(), "5", any()) }
    }

    @Test
    fun `should handle zero rates correctly`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        // 所有月份利率 = 0
        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal.ZERO,
            adjustedRate = BigDecimal.ZERO
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 平均 = 0 / 12 = 0
        assertEquals(BigDecimal("0.0000000000"), result.actualRate)
    }

    @Test
    fun `should handle mixed zero and non-zero rates`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(any()) } returns LocalDate.of(2024, 12, 1)

        // 前 6 個月 300，後 6 個月 0
        val rates = listOf(
            BigDecimal("300"), BigDecimal("300"), BigDecimal("300"),
            BigDecimal("300"), BigDecimal("300"), BigDecimal("300"),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returnsMany rates.map {
            RateLookupResult(originalRate = it, adjustedRate = it)
        }

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then - 平均 = (300×6 + 0×6) / 12 = 1800 / 12 = 150
        assertEquals(BigDecimal("150.0000000000"), result.actualRate)
    }

    @Test
    fun `should adjust endDate to month start before querying`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.AVG_DECLARED_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 25),  // 月中某日
            principalAmt = BigDecimal("1000000"),
            subAcntPlanCode = "TEST"
        )

        // Mock dependencies
        every { interestCalcHelper.toMonthStart(LocalDate.of(2024, 12, 25)) } returns LocalDate.of(2024, 12, 1)

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0)

        // Then - 驗證呼叫了 toMonthStart
        verify(exactly = 1) { interestCalcHelper.toMonthStart(LocalDate.of(2024, 12, 25)) }
    }
}
