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
 * 產品單位 (紅利分配) REST API (CV.CVPU)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供 CVPU 表格的直接存取端點。
 *
 * 業務別名：ProductUnitController
 *
 * API 端點：
 * - GET /api/v1/cvpu/policy/{policyNo}                    - 依保單查詢紅利分配（分頁）
 * - GET /api/v1/cvpu/coverage/{policyNo}/{coverageNo}     - 依承保範圍查詢紅利分配
 * - GET /api/v1/cvpu/{policyNo}/{coverageNo}/{ps06Type}/{cvpuType}/{lastAnnivDur} - 主鍵查詢
 * - GET /api/v1/cvpu/latest/{policyNo}/{coverageNo}       - 查詢最新紅利分配
 * - GET /api/v1/cvpu/summary/{policyNo}/{coverageNo}      - 紅利摘要
 *
 * @see CvcoController 承保範圍 Controller（提供子資源端點 /coverages/.../dividends）
 */
@RestController
@RequestMapping("/api/v1/cvpu")
@Validated
class CvpuController(
    private val service: CvpuService
) {

    companion object {
        private const val MAX_POLICY_NO_LENGTH = 10
    }

    /**
     * 依保單號碼查詢所有紅利分配記錄（分頁）
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param pageNum 頁碼（從 1 開始，預設 1）
     * @param pageSize 每頁筆數（1-100，預設 20）
     */
    @GetMapping("/policy/{policyNo}")
    fun findByPolicyNo(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CvpuDto>>> {
        val pageInfo = service.findByPolicyNo(policyNo, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toDto() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依承保範圍查詢紅利分配記錄
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     */
    @GetMapping("/coverage/{policyNo}/{coverageNo}")
    fun findByCoverage(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<List<CvpuDto>>> {
        val productUnits = service.findByCoverage(policyNo, coverageNo)
        val response = productUnits.map { it.toDto() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依主鍵查詢單筆紅利分配記錄
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     * @param ps06Type PS06 類型 (1 碼)
     * @param cvpuType CVPU 類型 (1 碼)
     * @param lastAnnivDur 週年期間
     */
    @GetMapping("/{policyNo}/{coverageNo}/{ps06Type}/{cvpuType}/{lastAnnivDur}")
    fun findById(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int,
        @PathVariable @Size(min = 1, max = 1) ps06Type: String,
        @PathVariable @Size(min = 1, max = 1) cvpuType: String,
        @PathVariable @Min(0) lastAnnivDur: Int
    ): ResponseEntity<ApiResponse<CvpuDto?>> {
        val id = CvpuId(policyNo, coverageNo, ps06Type, cvpuType, lastAnnivDur)
        val productUnit = service.findById(id)
        return if (productUnit != null) {
            ResponseEntity.ok(ApiResponse.success(productUnit.toDto()))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "CVPU_NOT_FOUND",
                        message = "Product unit not found: $id"
                    )
                )
        }
    }

    /**
     * 查詢最新週年期間的紅利分配記錄
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     */
    @GetMapping("/latest/{policyNo}/{coverageNo}")
    fun findLatestByCoverage(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<CvpuDto?>> {
        val productUnit = service.findLatestByCoverage(policyNo, coverageNo)
        return if (productUnit != null) {
            ResponseEntity.ok(ApiResponse.success(productUnit.toDto()))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "CVPU_NOT_FOUND",
                        message = "No product unit found for coverage: $policyNo:$coverageNo"
                    )
                )
        }
    }

    /**
     * 取得紅利摘要
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     */
    @GetMapping("/summary/{policyNo}/{coverageNo}")
    fun getDividendSummary(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<DividendSummaryDto>> {
        val summary = service.getDividendSummary(policyNo, coverageNo)
        return ResponseEntity.ok(ApiResponse.success(summary.toDto()))
    }

    /**
     * 檢查承保範圍是否有紅利分配記錄
     *
     * @param policyNo 保單號碼 (1-10 碼)
     * @param coverageNo 承保範圍編號
     */
    @GetMapping("/exists/{policyNo}/{coverageNo}")
    fun existsByCoverage(
        @PathVariable @Size(min = 1, max = MAX_POLICY_NO_LENGTH) @Pattern(regexp = "^[A-Z0-9]+$") policyNo: String,
        @PathVariable @Min(0) coverageNo: Int
    ): ResponseEntity<ApiResponse<Boolean>> {
        val exists = service.existsByCoverage(policyNo, coverageNo)
        return ResponseEntity.ok(ApiResponse.success(exists))
    }

    private fun Cvpu.toDto() = CvpuDto(
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
        programIdCvpu = programIdCvpu,
        isActive = isActive()
    )

    private fun DividendSummary.toDto() = DividendSummaryDto(
        policyNo = policyNo,
        coverageNo = coverageNo,
        totalDivDeclare = totalDivDeclare,
        totalDivPuaAmt = totalDivPuaAmt,
        totalDividend = totalDividend,
        recordCount = recordCount
    )
}

/**
 * 紅利分配 DTO (CV.CVPU)
 *
 * 遵循 ADR-017 規範，使用 {TableName}Dto 命名。
 */
data class CvpuDto(
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
    val programIdCvpu: String?,
    val isActive: Boolean
)

/**
 * 紅利摘要 DTO
 */
data class DividendSummaryDto(
    val policyNo: String,
    val coverageNo: Int,
    val totalDivDeclare: BigDecimal,
    val totalDivPuaAmt: BigDecimal,
    val totalDividend: BigDecimal,
    val recordCount: Int
)
