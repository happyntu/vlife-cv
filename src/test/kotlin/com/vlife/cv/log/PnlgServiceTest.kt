package com.vlife.cv.log

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

/**
 * PnlgService 單元測試
 *
 * 測試策略:
 * - 使用 MockK 模擬 PnlgMapper
 * - 驗證業務邏輯與參數驗證
 * - 覆蓋率目標: 80%+
 */
class PnlgServiceTest {

    private val pnlgMapper: PnlgMapper = mockk()
    private val pnlgService = PnlgService(pnlgMapper)

    @Test
    fun `findByPnlgSerial - 成功查詢`() {
        // Given
        val pnlgSerial = 629198L
        val expected = createSamplePolicyLog(pnlgSerial)
        every { pnlgMapper.findByPnlgSerial(pnlgSerial) } returns expected

        // When
        val result = pnlgService.findByPnlgSerial(pnlgSerial)

        // Then
        assertNotNull(result)
        assertEquals(pnlgSerial, result?.pnlgSerial)
        assertEquals("AQ218501", result?.operator)
        assertEquals("U", result?.actionType)
        verify(exactly = 1) { pnlgMapper.findByPnlgSerial(pnlgSerial) }
    }

    @Test
    fun `findByPnlgSerial - 記錄不存在`() {
        // Given
        val pnlgSerial = 999999L
        every { pnlgMapper.findByPnlgSerial(pnlgSerial) } returns null

        // When
        val result = pnlgService.findByPnlgSerial(pnlgSerial)

        // Then
        assertNull(result)
        verify(exactly = 1) { pnlgMapper.findByPnlgSerial(pnlgSerial) }
    }

    @Test
    fun `findHistoryByPlanCodeAndVersion - 查詢變更歷史`() {
        // Given
        val planCode = "7H015"
        val version = "1"
        val history = listOf(
            createSamplePolicyLog(629198L, planCode, version, "U"),
            createSamplePolicyLog(500000L, planCode, version, "I")
        )
        every { pnlgMapper.findByPlanCodeAndVersion(planCode, version) } returns history

        // When
        val result = pnlgService.findHistoryByPlanCodeAndVersion(planCode, version)

        // Then
        assertEquals(2, result.size)
        assertEquals("U", result[0].actionType) // 最新的在前
        assertEquals("I", result[1].actionType)
        verify(exactly = 1) { pnlgMapper.findByPlanCodeAndVersion(planCode, version) }
    }

    @Test
    fun `findHistoryByPlanCodeAndVersion - 無歷史記錄`() {
        // Given
        val planCode = "XXXXX"
        val version = "1"
        every { pnlgMapper.findByPlanCodeAndVersion(planCode, version) } returns emptyList()

        // When
        val result = pnlgService.findHistoryByPlanCodeAndVersion(planCode, version)

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 1) { pnlgMapper.findByPlanCodeAndVersion(planCode, version) }
    }

    @Test
    fun `findByOperator - 查詢操作人員記錄`() {
        // Given
        val operator = "AQ218501"
        val records = listOf(
            createSamplePolicyLog(629198L, operator = operator),
            createSamplePolicyLog(600000L, operator = operator)
        )
        every { pnlgMapper.findByOperator(operator) } returns records

        // When
        val result = pnlgService.findByOperator(operator)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.operator == operator })
        verify(exactly = 1) { pnlgMapper.findByOperator(operator) }
    }

    @Test
    fun `findByActionType - 查詢 INSERT 操作`() {
        // Given
        val actionType = "I"
        val records = listOf(createSamplePolicyLog(500000L, actionType = actionType))
        every { pnlgMapper.findByActionType(actionType) } returns records

        // When
        val result = pnlgService.findByActionType(actionType)

        // Then
        assertEquals(1, result.size)
        assertEquals("I", result[0].actionType)
        verify(exactly = 1) { pnlgMapper.findByActionType(actionType) }
    }

    @Test
    fun `findByActionType - 查詢 UPDATE 操作`() {
        // Given
        val actionType = "U"
        val records = listOf(createSamplePolicyLog(629198L, actionType = actionType))
        every { pnlgMapper.findByActionType(actionType) } returns records

        // When
        val result = pnlgService.findByActionType(actionType)

        // Then
        assertEquals(1, result.size)
        assertEquals("U", result[0].actionType)
        verify(exactly = 1) { pnlgMapper.findByActionType(actionType) }
    }

    @Test
    fun `findByActionType - 查詢 DELETE 操作`() {
        // Given
        val actionType = "D"
        val records = listOf(createSamplePolicyLog(700000L, actionType = actionType))
        every { pnlgMapper.findByActionType(actionType) } returns records

        // When
        val result = pnlgService.findByActionType(actionType)

        // Then
        assertEquals(1, result.size)
        assertEquals("D", result[0].actionType)
        verify(exactly = 1) { pnlgMapper.findByActionType(actionType) }
    }

    @Test
    fun `findByActionType - 無效操作類型應拋出異常`() {
        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            pnlgService.findByActionType("X")
        }
        assertTrue(exception.message!!.contains("Invalid actionType"))
    }

    @Test
    fun `findAll - 查詢全部記錄`() {
        // Given
        val allRecords = listOf(
            createSamplePolicyLog(629198L),
            createSamplePolicyLog(495198L),
            createSamplePolicyLog(603198L)
        )
        every { pnlgMapper.findAll() } returns allRecords

        // When
        val result = pnlgService.findAll()

        // Then
        assertEquals(3, result.size)
        verify(exactly = 1) { pnlgMapper.findAll() }
    }

    @Test
    fun `findAll - 空資料庫`() {
        // Given
        every { pnlgMapper.findAll() } returns emptyList()

        // When
        val result = pnlgService.findAll()

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 1) { pnlgMapper.findAll() }
    }

    // === 測試輔助方法 ===

    private fun createSamplePolicyLog(
        pnlgSerial: Long,
        planCode: String = "7H015",
        version: String = "1",
        actionType: String = "U",
        operator: String = "AQ218501"
    ): PolicyLog {
        return PolicyLog(
            pnlgSerial = pnlgSerial,
            operator = operator,
            processDate = LocalDate.of(2010, 4, 27),
            actionType = actionType,
            planCode = planCode,
            version = version,
            planTitle = "煒來人壽外幣保單",
            planName = "VL Foreign currency Endowment Ins.",
            contractedName = "外幣",
            lowAge = 0,
            highAge = 80,
            lowAgeSub = null,
            highAgeSub = null,
            lowAgeInd = "1",
            highAgeInd = "1",
            renHighAgeInd = "0",
            renHighAge = 0,
            planRelation = "1",
            planRelationSub = "0",
            faceAmtType = "1",
            collectYearInd = "1",
            collectYear = 15,
            expYearInd = "4",
            expYear = 999,
            planStartDate = LocalDate.of(2008, 7, 1),
            planEndDate = LocalDate.of(2010, 12, 31),
            rbnInd = "1",
            sbnInd = "0",
            otherSbnInd = "00000",
            abnInd = "0",
            uwInd = "Y",
            uwAge14Sw = "N",
            primaryRiderInd = "1",
            riderInd = "0",
            modxInd = "01111",
            factorInd = "04",
            premCalcType = "G",
            premValue = BigDecimal("1.0000"),
            unitValueInd = "1",
            unitValue = 10000.0,
            unitValue2 = 1.0,
            unitValue3 = 1.0,
            planType = "1",
            facePremInd = "100",
            riskAmtValue = 0,
            platInd = null,
            ratePlanCode = "ATA15",
            rateVersion = "1",
            rateSexInd = "1",
            rateAgeInd = "1",
            rateSub1Ind = "0",
            rateSub2Ind = "0",
            prat01Sw = "N",
            discType = "0",
            discHigh1Start = 0L,
            discPrem1Unit = 0,
            discPrem1Year = 0,
            discPrem1Half = 0,
            discHigh2Start = 0L,
            discPrem2Unit = 0,
            discPrem2Year = 0,
            discPrem2Half = 0,
            chgPrem1Ind = "0",
            chgPrem1Value = 0,
            chgPrem3Ind = "0",
            chgPrem3Value = 0,
            chgPrem3Dur = 0,
            srPlanCode = "ATA15",
            srVersion = "1",
            insuranceType = "H",
            insuranceType2 = "1",
            insuranceType3 = "R",
            insuranceType4 = "E",
            insuranceType5 = "N",
            deathBenefInd = "7",
            csvCalcType = "300",
            puaCalcType = "0000",
            eteCalcType = "000000",
            divType = "40",
            divPayItemInd = "0000",
            divCalcItemInd = "0000",
            cvDivCode = null,
            divStartYear = 999,
            divSw1 = "0000000",
            divSw2 = "000000000",
            declPlanCode = null,
            int4PlanCode = "3",
            surrenderInd = "0",
            nfo = "N",
            lbenf = "Y",
            mbenf = "Y",
            dbenf = "N",
            bbenf = "N",
            coPayInd = "N",
            coPayDisc = 100,
            acntType = "11",
            commClassCodeI = "2",
            commClassCode = "1ATA1",
            prodClassCode = null,
            benefInd = null,
            deductibleType = "0",
            deductibleAmt = 0L,
            treatyCode = "PLF7H",
            planCodeSys = "ATA",
            faceAmtUnit = 1L,
            uwPlanCode = "ATA06",
            uwVersion = "1",
            cvPlanCode = "ATA15",
            cvVersion = "1",
            csvPrintInd = "1",
            csvPrintYear = 5,
            uvPrintInd = "Y",
            topFaceAmt = 4000000L,
            yurPlanCode = null,
            yurVersion = null,
            ruleCode = "AT01",
            waivCsvInd = "2",
            waiverPremInd = "1",
            saveBenefInd = "0",
            pcPlanCode = "ATA",
            pcVersion = "1",
            loanAvalInd = "Y",
            loanPlanCode = "LV001",
            loanVersion = "1",
            loanAvalPercent = 0,
            riskPremInd = "1",
            riskPremCode = null,
            replaceAgeInd = "N",
            replaceAgeCode = null,
            salPaySw = "N",
            prepaySw = "N",
            addAmtSw = "N",
            annySw = "0",
            divSwM = "0",
            currency1 = "USD",
            planAccountInd = "0",
            premLackInd = "N",
            riskPremSas = "7HA1A",
            matLifeBenSas = "1",
            deathBenSas = "1",
            paDeathBenSas = "1",
            persistRewardInd = "N",
            persistPremVal = 0
        )
    }
}
