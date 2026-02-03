package com.vlife.cv.coverage

import com.vlife.common.response.ApiResponse
import com.vlife.cv.common.PageRequest
import com.vlife.cv.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Coverages", description = "承保範圍管理 API (CV.CVCO)")
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
     */
    @GetMapping("/policy/{policyNo}")
    @Operation(
        summary = "依保單查詢承保範圍",
        description = "依保單號碼查詢所有承保範圍，支援分頁。"
    )
    fun getCoveragesByPolicy(
        @Parameter(description = "保單號碼 (1-10 碼，僅英數字)")
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @Parameter(description = "頁碼 (從 1 開始)")
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @Parameter(description = "每頁筆數 (1-100)")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CvcoResponse>>> {
        val pageInfo = cvcoService.findByPolicyNo(policyNo, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依主鍵查詢單筆承保範圍
     */
    @GetMapping("/{policyNo}/{coverageNo}")
    @Operation(
        summary = "查詢單筆承保範圍",
        description = "依保單號碼與承保範圍編號查詢單筆承保範圍。"
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "承保範圍不存在")
    )
    fun getCoverageById(
        @Parameter(description = "保單號碼 (1-10 碼)")
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @Parameter(description = "承保範圍編號")
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
     */
    @GetMapping("/plan/{planCode}")
    @Operation(
        summary = "依險種查詢承保範圍",
        description = "依險種代碼查詢所有承保範圍，支援分頁。"
    )
    fun getCoveragesByPlanCode(
        @Parameter(description = "險種代碼 (1-5 碼)")
        @PathVariable @Size(min = 1, max = MAX_PLAN_CODE_LENGTH) planCode: String,
        @Parameter(description = "頁碼 (從 1 開始)")
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @Parameter(description = "每頁筆數 (1-100)")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CvcoResponse>>> {
        val pageInfo = cvcoService.findByPlanCode(planCode, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依承保狀態碼查詢承保範圍 (分頁)
     */
    @GetMapping("/status/{statusCode}")
    @Operation(
        summary = "依狀態查詢承保範圍",
        description = "依承保狀態碼查詢所有承保範圍，支援分頁。"
    )
    fun getCoveragesByStatus(
        @Parameter(description = "承保狀態碼 (1 碼)")
        @PathVariable @Size(min = 1, max = MAX_STATUS_CODE_LENGTH) statusCode: String,
        @Parameter(description = "頁碼 (從 1 開始)")
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @Parameter(description = "每頁筆數 (1-100)")
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
    @Operation(
        summary = "列出所有險種代碼",
        description = "取得資料庫中所有不重複的險種代碼清單。"
    )
    fun getAllPlanCodes(): ResponseEntity<ApiResponse<List<String>>> {
        val planCodes = cvcoService.findAllPlanCodes()
        return ResponseEntity.ok(ApiResponse.success(planCodes))
    }

    /**
     * 查詢指定承保範圍的紅利分配記錄
     */
    @GetMapping("/{policyNo}/{coverageNo}/dividends")
    @Tag(name = "Product Units", description = "紅利分配管理 API (CV.CVPU)")
    @Operation(
        summary = "查詢紅利分配記錄",
        description = "查詢指定承保範圍的所有紅利分配記錄。"
    )
    fun getDividends(
        @Parameter(description = "保單號碼 (1-10 碼)")
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @Parameter(description = "承保範圍編號")
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<List<CvpuResponse>>> {
        val productUnits = cvpuService.findByCoverage(policyNo, coverageNo)
        val response = productUnits.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 查詢指定承保範圍的紅利摘要
     */
    @GetMapping("/{policyNo}/{coverageNo}/dividend-summary")
    @Tag(name = "Product Units", description = "紅利分配管理 API (CV.CVPU)")
    @Operation(
        summary = "查詢紅利摘要",
        description = "查詢指定承保範圍的紅利摘要統計，包含總紅利宣告金額、總增額繳清保額等。"
    )
    fun getDividendSummary(
        @Parameter(description = "保單號碼 (1-10 碼)")
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @Parameter(description = "承保範圍編號")
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<DividendSummaryResponse>> {
        val summary = cvpuService.getDividendSummary(policyNo, coverageNo)
        return ResponseEntity.ok(ApiResponse.success(summary.toResponse()))
    }

    /**
     * 依保單號碼查詢所有紅利分配記錄 (分頁)
     */
    @GetMapping("/policy/{policyNo}/dividends")
    @Tag(name = "Product Units", description = "紅利分配管理 API (CV.CVPU)")
    @Operation(
        summary = "依保單查詢紅利分配",
        description = "依保單號碼查詢所有紅利分配記錄，支援分頁。"
    )
    fun getDividendsByPolicy(
        @Parameter(description = "保單號碼 (1-10 碼)")
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @Parameter(description = "頁碼 (從 1 開始)")
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @Parameter(description = "每頁筆數 (1-100)")
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
