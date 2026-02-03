package com.vlife.cv.coverage

import com.vlife.common.response.ApiResponse
import com.vlife.cv.common.PageRequest
import com.vlife.cv.common.PageResponse
import jakarta.validation.constraints.Max
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
 * 承保範圍與紅利分配 REST API (CV.CVCO, CV.CVPU)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供 CVCO (承保範圍) 和 CVPU (紅利分配) 的查詢服務。
 *
 * 業務別名：CoverageController
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
class CvcoController(
    private val cvcoService: CvcoService,
    private val cvpuService: CvpuService
) {

    companion object {
        private const val MAX_POLICY_NO_LENGTH = 10
        private const val MAX_PLAN_CODE_LENGTH = 5
        private const val MAX_STATUS_CODE_LENGTH = 1
    }

    /**
     * 依保單號碼查詢所有承保範圍 (分頁)
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param pageNum 頁碼 (從 1 開始，預設 1)
     * @param pageSize 每頁筆數 (1-100，預設 20)
     */
    @GetMapping("/policy/{policyNo}")
    fun getCoveragesByPolicy(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CvcoResponse>>> {
        val pageInfo = cvcoService.findByPolicyNo(policyNo, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
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
    ): ResponseEntity<ApiResponse<CvcoResponse?>> {
        val coverage = cvcoService.findById(policyNo, coverageNo)
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
     * 依險種代碼查詢承保範圍 (分頁)
     *
     * @param planCode 險種代碼 (1-5 碼)
     * @param pageNum 頁碼 (從 1 開始，預設 1)
     * @param pageSize 每頁筆數 (1-100，預設 20)
     */
    @GetMapping("/plan/{planCode}")
    fun getCoveragesByPlanCode(
        @PathVariable @Size(min = 1, max = MAX_PLAN_CODE_LENGTH) planCode: String,
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CvcoResponse>>> {
        val pageInfo = cvcoService.findByPlanCode(planCode, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依承保狀態碼查詢承保範圍 (分頁)
     *
     * @param statusCode 承保狀態碼 (1 碼)
     * @param pageNum 頁碼 (從 1 開始，預設 1)
     * @param pageSize 每頁筆數 (1-100，預設 20)
     */
    @GetMapping("/status/{statusCode}")
    fun getCoveragesByStatus(
        @PathVariable @Size(min = 1, max = MAX_STATUS_CODE_LENGTH) statusCode: String,
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CvcoResponse>>> {
        val pageInfo = cvcoService.findByStatusCode(statusCode, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 查詢所有不重複的險種代碼
     */
    @GetMapping("/plan-codes")
    fun getAllPlanCodes(): ResponseEntity<ApiResponse<List<String>>> {
        val planCodes = cvcoService.findAllPlanCodes()
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
    ): ResponseEntity<ApiResponse<List<CvpuResponse>>> {
        val productUnits = cvpuService.findByCoverage(policyNo, coverageNo)
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
        val summary = cvpuService.getDividendSummary(policyNo, coverageNo)
        return ResponseEntity.ok(ApiResponse.success(summary.toResponse()))
    }

    /**
     * 依保單號碼查詢所有紅利分配記錄 (分頁)
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param pageNum 頁碼 (從 1 開始，預設 1)
     * @param pageSize 每頁筆數 (1-100，預設 20)
     */
    @GetMapping("/policy/{policyNo}/dividends")
    fun getDividendsByPolicy(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CvpuResponse>>> {
        val pageInfo = cvpuService.findByPolicyNo(policyNo, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    private fun Cvco.toResponse() = CvcoResponse(
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

    private fun Cvpu.toResponse() = CvpuResponse(
        policyNo = policyNo,
        coverageNo = coverageNo,
        ps06Type = ps06Type,
        cvpuType = cvpuType,
        lastAnnivDur = lastAnnivDur,
        statusCode = statusCode,
        statusDesc = statusCode?.let { CvpuStatusCode.fromCode(it)?.description },
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
 * 承保範圍 API 回應格式 (CV.CVCO)
 */
data class CvcoResponse(
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
 * 紅利分配 API 回應格式 (CV.CVPU)
 */
data class CvpuResponse(
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
