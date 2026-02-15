package com.vlife.cv.controller

import com.vlife.cv.dto.*
import com.vlife.cv.engine.contract.Cv210pInterestRateCalculator
import com.vlife.cv.interest.InterestRateInput
import com.vlife.cv.interest.RateType
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * 利率計算 REST API Controller（L3）
 *
 * V3 對應：
 * - Service: vlife.cv.impl.PlanServiceImpl (Java Service 層)
 * - PL/SQL: pk_cv_cv210p.pck（精算利率計算引擎）
 *
 * **API 設計**：
 * - POST /api/v1/cv/interest-rate/calculate：單筆計算
 * - POST /api/v1/cv/interest-rate/calculate/batch：批量計算
 * - GET /api/v1/cv/interest-rate/supported-rate-types：查詢支援的 rate_type
 *
 * **架構分層**（ADR-027）：
 * ```
 * L3 REST API (InterestRateController)
 *   ↓ 注入 Cv210pInterestRateCalculator interface (L2 Engine Contract)
 * Cv210pInterestRateCalculatorImpl (@Primary)
 *   ↓ 注入 InterestRateService
 * InterestRateService
 *   ↓ 注入 RateStrategyDispatcher
 * 各種 InterestRateStrategy 實作
 * ```
 *
 * **錯誤處理**：
 * - rate_type 無效 → 400 Bad Request
 * - 輸入驗證失敗 → 400 Bad Request
 * - 系統錯誤 → 500 Internal Server Error
 *
 * @see Cv210pInterestRateCalculator
 * @see com.vlife.cv.engine.impl.Cv210pInterestRateCalculatorImpl
 */
@RestController
@RequestMapping("/api/v1/cv/interest-rate")
class InterestRateController(
    private val cv210pInterestRateCalculator: Cv210pInterestRateCalculator
) {

    /**
     * 計算利率與利息（單筆）
     *
     * V3 對應：cv210p_rate_value_calculation (pk_cv_cv210p.pck lines 1944-2091)
     *
     * **請求範例**：
     * ```json
     * {
     *   "rateType": "2",
     *   "beginDate": "2024-01-01",
     *   "endDate": "2024-12-31",
     *   "principalAmt": 1000000,
     *   "actualRate": 0,
     *   "rateSub": 0,
     *   "rateDisc": 100,
     *   "subAcntPlanCode": "ABC123",
     *   "companyCode": "1",
     *   "companyCode2": "1",
     *   "precision": 0
     * }
     * ```
     *
     * **回應範例**：
     * ```json
     * {
     *   "actualRate": 350.5,
     *   "intAmt": 35050,
     *   "monthlyDetails": [...]
     * }
     * ```
     *
     * @param request 利率計算請求
     * @return 利率計算結果
     */
    @PostMapping("/calculate")
    fun calculateRate(
        @RequestBody request: InterestRateRequestDto
    ): ResponseEntity<InterestRateResponseDto> {
        logger.debug { "POST /api/v1/cv/interest-rate/calculate: rateType=${request.rateType}" }

        // 轉換 DTO → Domain Object
        val rateType = RateType.fromCode(request.rateType)
            ?: return ResponseEntity.badRequest().build()

        val input = InterestRateInput(
            rateType = rateType,
            beginDate = request.beginDate,
            endDate = request.endDate,
            principalAmt = request.principalAmt,
            actualRate = request.actualRate,
            rateSub = request.rateSub,
            rateDisc = request.rateDisc,
            subAcntPlanCode = request.subAcntPlanCode,
            ivTargetCode = request.ivTargetCode
        )

        // 執行計算
        val result = try {
            cv210pInterestRateCalculator.calculateRate(input, request.precision)
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Invalid input: ${e.message}" }
            return ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error(e) { "Failed to calculate rate: ${e.message}" }
            return ResponseEntity.internalServerError().build()
        }

        // 轉換 Domain Object → DTO
        val response = InterestRateResponseDto.from(result)
        return ResponseEntity.ok(response)
    }

    /**
     * 批量計算利率與利息
     *
     * V3 對應：多次呼叫 cv210p_rate_value_calculation（無批次 API）
     *
     * **請求範例**：
     * ```json
     * {
     *   "inputs": [
     *     { "rateType": "2", ... },
     *     { "rateType": "3", ... }
     *   ],
     *   "precision": 0
     * }
     * ```
     *
     * **回應範例**：
     * ```json
     * {
     *   "results": [
     *     { "actualRate": 350.5, "intAmt": 35050, ... },
     *     { "actualRate": 400.0, "intAmt": 40000, ... }
     *   ]
     * }
     * ```
     *
     * @param request 批量利率計算請求
     * @return 批量利率計算結果
     */
    @PostMapping("/calculate/batch")
    fun calculateRateBatch(
        @RequestBody request: InterestRateBatchRequestDto
    ): ResponseEntity<InterestRateBatchResponseDto> {
        logger.debug {
            "POST /api/v1/cv/interest-rate/calculate/batch: ${request.inputs.size} inputs"
        }

        // 轉換 DTO → Domain Objects
        val inputs = request.inputs.mapNotNull { dto ->
            val rateType = RateType.fromCode(dto.rateType) ?: return@mapNotNull null
            InterestRateInput(
                rateType = rateType,
                beginDate = dto.beginDate,
                endDate = dto.endDate,
                principalAmt = dto.principalAmt,
                actualRate = dto.actualRate,
                rateSub = dto.rateSub,
                rateDisc = dto.rateDisc,
                subAcntPlanCode = dto.subAcntPlanCode,
                ivTargetCode = dto.ivTargetCode
            )
        }

        if (inputs.size != request.inputs.size) {
            logger.warn { "Some inputs have invalid rateType, rejected ${request.inputs.size - inputs.size} inputs" }
            return ResponseEntity.badRequest().build()
        }

        // 執行批量計算
        val results = try {
            cv210pInterestRateCalculator.calculateRateBatch(inputs, request.precision)
        } catch (e: Exception) {
            logger.error(e) { "Failed to calculate batch rates: ${e.message}" }
            return ResponseEntity.internalServerError().build()
        }

        // 轉換 Domain Objects → DTO
        val response = InterestRateBatchResponseDto.from(results)
        return ResponseEntity.ok(response)
    }

    /**
     * 查詢支援的利率類型
     *
     * V3 對應：cv210p_rate_calc 中的 IF/CASE 邏輯（lines 1877-1943）
     *
     * **回應範例**：
     * ```json
     * {
     *   "rateTypes": [
     *     { "code": "0", "description": "一般宣告利率" },
     *     { "code": "1", "description": "四行庫利率" },
     *     { "code": "2", "description": "貸款利率（日加權月計）" },
     *     ...
     *   ]
     * }
     * ```
     *
     * @return 支援的利率類型清單
     */
    @GetMapping("/supported-rate-types")
    fun getSupportedRateTypes(): ResponseEntity<SupportedRateTypesResponseDto> {
        logger.debug { "GET /api/v1/cv/interest-rate/supported-rate-types" }

        val supportedRateTypes = cv210pInterestRateCalculator.getSupportedRateTypes()
        val response = SupportedRateTypesResponseDto(
            rateTypes = supportedRateTypes.map { RateTypeDto(it.code, it.description) }
        )

        return ResponseEntity.ok(response)
    }

    /**
     * 檢查是否支援指定的 rate_type
     *
     * **回應範例**：
     * ```json
     * { "supported": true }
     * ```
     *
     * @param rateType 利率類型代碼
     * @return 是否支援
     */
    @GetMapping("/supports/{rateType}")
    fun supportsRateType(
        @PathVariable rateType: String
    ): ResponseEntity<Map<String, Boolean>> {
        logger.debug { "GET /api/v1/cv/interest-rate/supports/$rateType" }

        val rateTypeEnum = RateType.fromCode(rateType)
            ?: return ResponseEntity.ok(mapOf("supported" to false))

        val supported = cv210pInterestRateCalculator.supportsRateType(rateTypeEnum)
        return ResponseEntity.ok(mapOf("supported" to supported))
    }
}
