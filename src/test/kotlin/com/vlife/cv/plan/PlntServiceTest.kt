package com.vlife.cv.plan

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * PlntService 單元測試
 *
 * 測試策略：
 * - 使用 MockK mock PlntMapper
 * - 驗證快取行為（Cache Hit/Miss）
 * - 驗證業務邏輯正確性
 */
class PlntServiceTest {

    private lateinit var plntMapper: PlntMapper
    private lateinit var plntService: PlntService

    @BeforeEach
    fun setUp() {
        plntMapper = mockk()
        plntService = PlntService(plntMapper)
    }

    @Test
    fun `findByPlanCodeAndVersion - should return PlanNote when exists`() {
        // Given
        val planCode = "L2050"
        val version = "3"
        val expectedPlanNote = createPlanNote(planCode, version)

        every { plntMapper.findByPlanCodeAndVersion(planCode, version) } returns expectedPlanNote

        // When
        val result = plntService.findByPlanCodeAndVersion(planCode, version)

        // Then
        assertNotNull(result)
        assertEquals(planCode, result?.planCode)
        assertEquals(version, result?.version)
        assertEquals("C", result?.insuranceType6)
        verify(exactly = 1) { plntMapper.findByPlanCodeAndVersion(planCode, version) }
    }

    @Test
    fun `findByPlanCodeAndVersion - should return null when not exists`() {
        // Given
        val planCode = "XXXXX"
        val version = "9"

        every { plntMapper.findByPlanCodeAndVersion(planCode, version) } returns null

        // When
        val result = plntService.findByPlanCodeAndVersion(planCode, version)

        // Then
        assertNull(result)
        verify(exactly = 1) { plntMapper.findByPlanCodeAndVersion(planCode, version) }
    }

    @Test
    fun `findByPlanCodeAndVersion - should use cache on second query`() {
        // Given
        val planCode = "L2050"
        val version = "3"
        val expectedPlanNote = createPlanNote(planCode, version)

        every { plntMapper.findByPlanCodeAndVersion(planCode, version) } returns expectedPlanNote

        // When
        val result1 = plntService.findByPlanCodeAndVersion(planCode, version)
        val result2 = plntService.findByPlanCodeAndVersion(planCode, version)

        // Then
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(result1, result2)
        // Mapper 應該只被呼叫一次（第二次從 cache 取得）
        verify(exactly = 1) { plntMapper.findByPlanCodeAndVersion(planCode, version) }
    }

    @Test
    fun `findByPlanCode - should return all versions`() {
        // Given
        val planCode = "QF521"
        val expectedList = listOf(
            createPlanNote(planCode, "1"),
            createPlanNote(planCode, "2")
        )

        every { plntMapper.findByPlanCode(planCode) } returns expectedList

        // When
        val result = plntService.findByPlanCode(planCode)

        // Then
        assertEquals(2, result.size)
        assertEquals(planCode, result[0].planCode)
        assertEquals(planCode, result[1].planCode)
        verify(exactly = 1) { plntMapper.findByPlanCode(planCode) }
    }

    @Test
    fun `findAll - should return all plan notes`() {
        // Given
        val expectedList = listOf(
            createPlanNote("L2050", "3"),
            createPlanNote("QF521", "1"),
            createPlanNote("VBZ20", "1")
        )

        every { plntMapper.findAll() } returns expectedList

        // When
        val result = plntService.findAll()

        // Then
        assertEquals(3, result.size)
        verify(exactly = 1) { plntMapper.findAll() }
    }

    @Test
    fun `evictCache - should remove specific cache entry`() {
        // Given
        val planCode = "L2050"
        val version = "3"
        val planNote = createPlanNote(planCode, version)

        every { plntMapper.findByPlanCodeAndVersion(planCode, version) } returns planNote

        // When: 第一次查詢（建立快取）
        plntService.findByPlanCodeAndVersion(planCode, version)

        // 清除快取
        plntService.evictCache(planCode, version)

        // 再次查詢（應該重新查詢資料庫）
        plntService.findByPlanCodeAndVersion(planCode, version)

        // Then: Mapper 應該被呼叫兩次（快取被清除）
        verify(exactly = 2) { plntMapper.findByPlanCodeAndVersion(planCode, version) }
    }

    @Test
    fun `evictAllCache - should remove all cache entries`() {
        // Given
        val planNote1 = createPlanNote("L2050", "3")
        val planNote2 = createPlanNote("QF521", "1")

        every { plntMapper.findByPlanCodeAndVersion("L2050", "3") } returns planNote1
        every { plntMapper.findByPlanCodeAndVersion("QF521", "1") } returns planNote2

        // When: 建立兩筆快取
        plntService.findByPlanCodeAndVersion("L2050", "3")
        plntService.findByPlanCodeAndVersion("QF521", "1")

        // 清除所有快取
        plntService.evictAllCache()

        // 再次查詢
        plntService.findByPlanCodeAndVersion("L2050", "3")
        plntService.findByPlanCodeAndVersion("QF521", "1")

        // Then: 每個查詢應該被呼叫兩次（快取被清除）
        verify(exactly = 2) { plntMapper.findByPlanCodeAndVersion("L2050", "3") }
        verify(exactly = 2) { plntMapper.findByPlanCodeAndVersion("QF521", "1") }
    }

    @Test
    fun `getCacheStats - should return cache statistics`() {
        // Given
        val planNote = createPlanNote("L2050", "3")
        every { plntMapper.findByPlanCodeAndVersion("L2050", "3") } returns planNote

        // When: 多次查詢以產生統計資料
        plntService.findByPlanCodeAndVersion("L2050", "3") // Cache miss
        plntService.findByPlanCodeAndVersion("L2050", "3") // Cache hit
        plntService.findByPlanCodeAndVersion("L2050", "3") // Cache hit

        val stats = plntService.getCacheStats()

        // Then
        assertNotNull(stats)
        assertTrue(stats.contains("hitRate="))
        assertTrue(stats.contains("hitCount="))
        assertTrue(stats.contains("missCount="))
    }

    @Test
    fun `cache key uniqueness - different versions should not share cache`() {
        // Given
        val planCode = "L2050"
        val planNote1 = createPlanNote(planCode, "1", "A")
        val planNote2 = createPlanNote(planCode, "2", "B")

        every { plntMapper.findByPlanCodeAndVersion(planCode, "1") } returns planNote1
        every { plntMapper.findByPlanCodeAndVersion(planCode, "2") } returns planNote2

        // When
        val result1 = plntService.findByPlanCodeAndVersion(planCode, "1")
        val result2 = plntService.findByPlanCodeAndVersion(planCode, "2")

        // Then: 不同版本應該取得不同資料
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals("A", result1?.insuranceType6)
        assertEquals("B", result2?.insuranceType6)
        assertNotEquals(result1, result2)
    }

    /**
     * 建立測試用 PlanNote
     */
    private fun createPlanNote(
        planCode: String,
        version: String,
        insuranceType6: String = "C"
    ): PlanNote {
        return PlanNote(
            planCode = planCode,
            version = version,
            insuranceType6 = insuranceType6,
            freeLookInd = "0000",
            freeLookDurInd = "0",
            effPeriod = 0,
            freeLookRateCode = null,
            premToFundInd = "0",
            premToFundDur = 0,
            deathBenefOpt = "00000",
            deathPlanCode = null,
            deathPlanAgeInd = "0",
            ivFundsetInd = "0",
            fundInd = "000",
            fundBalanceInd = "000",
            premPlanCode = null,
            premPreInd = "4",
            extraModxSw = "N",
            extraInvSw = "N",
            loanAcntInd = "1",
            loanPlanCode = null,
            expenPlanCode = null,
            allocPlanCode = null,
            coiCalcInd = "0",
            coiPlanCode = null,
            coiPlanAgeInd = "0",
            coiExpnInd = "0",
            withdrawTypeInd = "00",
            withdrawIvAlloc = "0000",
            withdrawDbInd = "0",
            ivChgPerInd = "N",
            ivChgCntInd = "0",
            felCommPremInd = "0",
            felCommPremCal = "0",
            tpCalcInd = "0",
            tpCalcInd2 = "0",
            targetPremCode = null,
            tpAgeInd = "0",
            billInformInd = "0",
            billInformFreq = 0,
            cvCalcFreq = 0,
            returnSeqInd = "000",
            returnSeqInd2 = "Z",
            matureDurInd = "0",
            matureDur = 0,
            matureDurInd2 = "0",
            matureValInd = "0",
            matureDivInd = "0",
            matureOptInd = "00000",
            matureDefInd = "1",
            annyInd = "5",
            annyAdjFactor = "0",
            payOption = "01000",
            annyPlanCode = null,
            annyAgeMax = 50,
            annyStartInd = "5",
            annyDeferedDur = 0,
            annyPrePayInd = "00",
            annyPrePayYear = 0,
            annyGarInd = "1",
            annyGarPeriod = 10,
            annyAgeMin = 50,
            annyStrInd = "1",
            annyStrVal = 50,
            matureReValInd = "0",
            matureNonInd = "0",
            nbdtPlanCode = null,
            pcDurPlan = null,
            businessNo = null,
            riderPremInd = "2",
            ivCompanyCode2 = null,
            rvfInd = "Y",
            corridorSw = "0"
        )
    }
}
