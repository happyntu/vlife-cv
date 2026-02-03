package com.vlife.cv.coverage

import com.vlife.common.response.ApiResponse
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 承保範圍與紅利分配 REST API
 *
 * 提供 CVCO (承保範圍) 和 CVPU (紅利分配) 的查詢服務。
 *
 * API 端點：
 * - GET /api/v1/coverages/policy/{policyNo}              - 依保單查詢承保範圍
 * - GET /api/v1/coverages/{policyNo}/{coverageNo}        - 查詢單筆承保範圍
 * - GET /api/v1/coverages/plan/{planCode}                - 依險種查詢承保範圍
 * - GET /api/v1/coverages/status/{statusCode}            - 依狀態查詢承保範圍
 * - GET /api/v1/coverages/{policyNo}/{coverageNo}/dividends - 查詢紅利分配
 * - GET /api/v1/coverages/{policyNo}/{coverageNo}/dividend-summary - 紅利摘要
 */
@RestController
@RequestMapping("/api/v1/coverages")
@Validated
class CoverageController(
    private val coverageService: CoverageValueChangeService,
    private val productUnitService: ProductUnitService
) {

    companion object {
        private const val MAX_POLICY_NO_LENGTH = 10
        private const val MAX_PLAN_CODE_LENGTH = 5
        private const val MAX_STATUS_CODE_LENGTH = 1
    }

    /**
     * 依保單號碼查詢所有承保範圍
     *
     * @param policyNo 保單號碼 (1-10 碼)
     */
    @GetMapping("/policy/{policyNo}")
    fun getCoveragesByPolicy(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String
    ): ResponseEntity<ApiResponse<List<CoverageResponse>>> {
        val coverages = coverageService.findByPolicyNo(policyNo)
        val response = coverages.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依主鍵查詢單筆承保範圍
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     */
    @GetMapping("/{policyNo}/{coverageNo}")
    fun getCoverageById(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<CoverageResponse?>> {
        val coverage = coverageService.findById(policyNo, coverageNo)
        return if (coverage != null) {
            ResponseEntity.ok(ApiResponse.success(coverage.toResponse()))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "COVERAGE_NOT_FOUND",
                        message = "Coverage not found: $policyNo:$coverageNo"
                    )
                )
        }
    }

    /**
     * 依險種代碼查詢承保範圍
     *
     * @param planCode 險種代碼 (1-5 碼)
     */
    @GetMapping("/plan/{planCode}")
    fun getCoveragesByPlanCode(
        @PathVariable @Size(min = 1, max = MAX_PLAN_CODE_LENGTH) planCode: String
    ): ResponseEntity<ApiResponse<List<CoverageResponse>>> {
        val coverages = coverageService.findByPlanCode(planCode)
        val response = coverages.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依承保狀態碼查詢承保範圍
     *
     * @param statusCode 承保狀態碼 (1 碼)
     */
    @GetMapping("/status/{statusCode}")
    fun getCoveragesByStatus(
        @PathVariable @Size(min = 1, max = MAX_STATUS_CODE_LENGTH) statusCode: String
    ): ResponseEntity<ApiResponse<List<CoverageResponse>>> {
        val coverages = coverageService.findByStatusCode(statusCode)
        val response = coverages.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 查詢所有不重複的險種代碼
     */
    @GetMapping("/plan-codes")
    fun getAllPlanCodes(): ResponseEntity<ApiResponse<List<String>>> {
        val planCodes = coverageService.findAllPlanCodes()
        return ResponseEntity.ok(ApiResponse.success(planCodes))
    }

    /**
     * 查詢指定承保範圍的紅利分配記錄
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     */
    @GetMapping("/{policyNo}/{coverageNo}/dividends")
    fun getDividends(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<List<ProductUnitResponse>>> {
        val productUnits = productUnitService.findByCoverage(policyNo, coverageNo)
        val response = productUnits.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 查詢指定承保範圍的紅利摘要
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     */
    @GetMapping("/{policyNo}/{coverageNo}/dividend-summary")
    fun getDividendSummary(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<DividendSummaryResponse>> {
        val summary = productUnitService.getDividendSummary(policyNo, coverageNo)
        return ResponseEntity.ok(ApiResponse.success(summary.toResponse()))
    }

    /**
     * 依保單號碼查詢所有紅利分配記錄
     *
     * @param policyNo 保單號碼 (1-10 碼)
     */
    @GetMapping("/policy/{policyNo}/dividends")
    fun getDividendsByPolicy(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String
    ): ResponseEntity<ApiResponse<List<ProductUnitResponse>>> {
        val productUnits = productUnitService.findByPolicyNo(policyNo)
        val response = productUnits.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    private fun CoverageValueChange.toResponse() = CoverageResponse(
        policyNo = policyNo,
        coverageNo = coverageNo,
        planCode = planCode,
        version = version,
        rateSex = rateSex,
        rateAge = rateAge,
        rateSub1 = rateSub1,
        rateSub2 = rateSub2,
        issueDate = issueDate,
        statusCode = statusCode,
        statusDesc = CoverageStatusCode.fromCode(statusCode)?.description,
        insuranceType3 = insuranceType3,
        insuranceType3Desc = InsuranceType3.fromCode(insuranceType3)?.description,
        processDate = processDate,
        processType = processType,
        policyType = policyType,
        statusCode2 = statusCode2,
        isActive = isActive()
    )

    private fun ProductUnit.toResponse() = ProductUnitResponse(
        policyNo = policyNo,
        coverageNo = coverageNo,
        ps06Type = ps06Type,
        cvpuType = cvpuType,
        lastAnnivDur = lastAnnivDur,
        statusCode = statusCode,
        statusDesc = statusCode?.let { ProductUnitStatusCode.fromCode(it)?.description },
        divDeclare = divDeclare,
        divPuaAmt = divPuaAmt,
        totalDividend = getTotalDividend(),
        financialDate = financialDate,
        pcpoNo = pcpoNo,
        programId = programId,
        processDate = processDate,
        policyType = policyType,
        approvedDate = approvedDate,
        programIdCvpu = programIdCvpu
    )

    private fun DividendSummary.toResponse() = DividendSummaryResponse(
        policyNo = policyNo,
        coverageNo = coverageNo,
        totalDivDeclare = totalDivDeclare,
        totalDivPuaAmt = totalDivPuaAmt,
        totalDividend = totalDividend,
        recordCount = recordCount
    )
}

/**
 * 承保範圍 API 回應格式
 */
data class CoverageResponse(
    val policyNo: String,
    val coverageNo: Int,
    val planCode: String,
    val version: String,
    val rateSex: String,
    val rateAge: Int,
    val rateSub1: String,
    val rateSub2: String,
    val issueDate: LocalDate,
    val statusCode: String,
    val statusDesc: String?,
    val insuranceType3: String,
    val insuranceType3Desc: String?,
    val processDate: LocalDate,
    val processType: String,
    val policyType: String?,
    val statusCode2: String?,
    val isActive: Boolean
)

/**
 * 紅利分配 API 回應格式
 */
data class ProductUnitResponse(
    val policyNo: String,
    val coverageNo: Int,
    val ps06Type: String,
    val cvpuType: String,
    val lastAnnivDur: Int,
    val statusCode: String?,
    val statusDesc: String?,
    val divDeclare: BigDecimal,
    val divPuaAmt: BigDecimal,
    val totalDividend: BigDecimal,
    val financialDate: LocalDate?,
    val pcpoNo: String?,
    val programId: String?,
    val processDate: LocalDate?,
    val policyType: String?,
    val approvedDate: LocalDate?,
    val programIdCvpu: String?
)

/**
 * 紅利摘要 API 回應格式
 */
data class DividendSummaryResponse(
    val policyNo: String,
    val coverageNo: Int,
    val totalDivDeclare: BigDecimal,
    val totalDivPuaAmt: BigDecimal,
    val totalDividend: BigDecimal,
    val recordCount: Int
)
