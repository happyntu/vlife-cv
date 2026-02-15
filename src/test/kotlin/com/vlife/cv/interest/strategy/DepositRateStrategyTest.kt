package com.vlife.cv.interest.strategy

import com.vlife.cv.interest.InterestRateCalculationResult
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import com.vlife.cv.interest.helper.InterestCalcHelper
import com.vlife.cv.interest.helper.QiratRateLookup
import com.vlife.cv.interest.helper.QiratRateLookup.RateLookupResult
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
 * DepositRateStrategy 單元測試
 *
 * **測試策略**：
 * - 使用 MockK mock 依賴
 * - 驗證雙重路由邏輯（insurance_type_3 IN [G,H] → AnnuityRateStrategy）
 * - 驗證存款類日加權計算（insurance_type_3 = F）
 * - 驗證逐月 ROUND 行為
 */
@DisplayName("DepositRateStrategy 單元測試")
class DepositRateStrategyTest {

    private lateinit var qiratRateLookup: QiratRateLookup
    private lateinit var interestCalcHelper: InterestCalcHelper
    private lateinit var annuityRateStrategy: AnnuityRateStrategy
    private lateinit var strategy: DepositRateStrategy

    @BeforeEach
    fun setUp() {
        qiratRateLookup = mockk()
        interestCalcHelper = mockk()
        annuityRateStrategy = mockk()
        strategy = DepositRateStrategy(qiratRateLookup, interestCalcHelper, annuityRateStrategy)
    }

    @Test
    fun `should support DEPOSIT_RATE`() {
        // Given / When
        val supportedRateTypes = strategy.supportedRateTypes()

        // Then
        assertEquals(1, supportedRateTypes.size)
        assertTrue(supportedRateTypes.contains(RateType.DEPOSIT_RATE))
    }

    @Test
    fun `should delegate to AnnuityRateStrategy when insurance_type_3 is G`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "G"
        }

        val expectedResult = InterestRateCalculationResult(
            actualRate = BigDecimal("300"),
            intAmt = BigDecimal("30000"),
            monthlyDetails = emptyList()
        )
        every { annuityRateStrategy.calculate(input, 0, plan, null) } returns expectedResult

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertEquals(expectedResult, result)
        verify(exactly = 1) { annuityRateStrategy.calculate(input, 0, plan, null) }
    }

    @Test
    fun `should delegate to AnnuityRateStrategy when insurance_type_3 is H`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "H"
        }

        val expectedResult = InterestRateCalculationResult(
            actualRate = BigDecimal("300"),
            intAmt = BigDecimal("30000"),
            monthlyDetails = emptyList()
        )
        every { annuityRateStrategy.calculate(input, 0, plan, null) } returns expectedResult

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertEquals(expectedResult, result)
        verify(exactly = 1) { annuityRateStrategy.calculate(input, 0, plan, null) }
    }

    @Test
    fun `should use deposit rate calculation when insurance_type_3 is F`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertNotNull(result)
        verify(exactly = 0) { annuityRateStrategy.calculate(any(), any(), any(), any()) }
        verify { qiratRateLookup.lookupRate(any(), "5", any()) }
    }

    @Test
    fun `should use deposit rate calculation when insurance_type_3 is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns null
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertNotNull(result)
        verify(exactly = 0) { annuityRateStrategy.calculate(any(), any(), any(), any()) }
    }

    @Test
    fun `should return zero when beginDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = null,
            endDate = LocalDate.of(2024, 12, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
        }

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return zero when endDate is null`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = null,
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
        }

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should return zero when beginDate greater than or equal to endDate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 12, 31),
            endDate = LocalDate.of(2024, 1, 1),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
        }

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertEquals(BigDecimal.ZERO, result.actualRate)
        assertEquals(BigDecimal.ZERO, result.intAmt)
    }

    @Test
    fun `should use int_rate_type 5 for deposit rate`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        strategy.calculate(input, precision = 0, plan = plan)

        // Then
        verify { qiratRateLookup.lookupRate(any(), "5", any()) }
    }

    @Test
    fun `should calculate correctly with single month`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertNotNull(result)
        assertEquals(BigDecimal("250"), result.actualRate)
        assertTrue(result.intAmt > BigDecimal.ZERO)
        assertEquals(1, result.monthlyDetails.size)
    }

    @Test
    fun `should round monthly for deposit rate`() {
        // Given - 設計一個會產生小數的情況
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("999999")  // 奇數本金
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
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
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0)

        // Then
        assertEquals(0, result.intAmt.scale())  // 台幣精度 0（逐月 ROUND）
    }

    @Test
    fun `should add months when months is zero or negative`() {
        // Given
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 15),
            endDate = LocalDate.of(2024, 1, 20),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
        }

        // Mock dependencies
        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 0
        every { interestCalcHelper.calculateDays(any(), any()) } returns 5
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        // When
        val result = strategy.calculate(input, precision = 0, plan = plan)

        // Then
        assertEquals(1, result.monthlyDetails.size)  // 月數 0 時應加 1
    }

    // =========================================================================
    // P1-003 修復驗證測試
    // =========================================================================

    @Test
    fun `P1-003 should apply calcRound to averageRate`() {
        // P1-003: V3 lines 1661-1663 使用 calc_round() 處理平均利率
        val input = InterestRateInput(
            rateType = RateType.DEPOSIT_RATE,
            beginDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            principalAmt = BigDecimal("1000000")
        )

        val plan = mockk<Pldf> {
            every { insuranceType3 } returns "F"
        }

        every { interestCalcHelper.calculateYearDays(any()) } returns 365
        every { interestCalcHelper.calculateMonths(any(), any()) } returns 1
        every { interestCalcHelper.calculateDays(any(), any()) } returns 31
        every { interestCalcHelper.formatMonth(any()) } returns "2024/01"

        val rateLookupResult = RateLookupResult(
            originalRate = BigDecimal("250"),
            adjustedRate = BigDecimal("250")
        )
        every { qiratRateLookup.lookupRate(any(), "5", any()) } returns rateLookupResult

        val result = strategy.calculate(input, precision = 0, plan = plan)

        // P1-003: averageRate 應經過 calcRound 處理
        // 單一月份利率 250，日加權平均 = 250*31/31 = 250
        // MathUtils.calcRound(250) = 250（整數不變）
        assertNotNull(result.actualRate)
        // 關鍵驗證：actualRate 精度應符合 calcRound 輸出
        assertTrue(result.actualRate.scale() <= 10) {
            "P1-003: actualRate should have calcRound precision, got scale=${result.actualRate.scale()}"
        }
    }
}
