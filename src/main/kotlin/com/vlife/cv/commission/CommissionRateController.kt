package com.vlife.cv.commission

import com.vlife.common.response.ApiResponse
import com.vlife.cv.common.PageRequest
import com.vlife.cv.common.PageResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 佣金率 REST API
 *
 * 提供佣金率的查詢服務，供業管 (AG) 及其他業務模組呼叫。
 *
 * API 端點：
 * - GET /api/v1/commission-rates                    - 搜尋佣金率
 * - GET /api/v1/commission-rates/{serial}           - 依序號查詢
 * - GET /api/v1/commission-rates/class/{classCode}  - 依類別碼查詢
 * - GET /api/v1/commission-rates/effective          - 查詢有效佣金率
 * - GET /api/v1/commission-rates/line-codes         - 列出所有業務線代號
 * - GET /api/v1/commission-rates/crat-types         - 列出所有佣金率型態
 * - POST /api/v1/commission-rates/refresh           - 刷新快取
 */
@RestController
@RequestMapping("/api/v1/commission-rates")
@Validated
class CommissionRateController(
    private val service: CommissionRateService
) {

    companion object {
        private const val MAX_CLASS_CODE_LENGTH = 5
        private const val MAX_LINE_CODE_LENGTH = 2
        private const val MAX_CRAT_TYPE_LENGTH = 1
    }

    /**
     * 搜尋佣金率 (分頁)
     *
     * @param commClassCode 佣金率類別碼 (可選，最長 5 碼)
     * @param commLineCode 業務線代號 (可選，最長 2 碼)
     * @param cratType 佣金率型態 (可選，1 碼)
     * @param effectiveDate 生效日期 (可選，格式 yyyy-MM-dd)
     * @param pageNum 頁碼 (從 1 開始，預設 1)
     * @param pageSize 每頁筆數 (1-100，預設 20)
     */
    @GetMapping
    fun search(
        @RequestParam(required = false) @Size(max = MAX_CLASS_CODE_LENGTH) commClassCode: String?,
        @RequestParam(required = false) @Size(max = MAX_LINE_CODE_LENGTH) commLineCode: String?,
        @RequestParam(required = false) @Size(max = MAX_CRAT_TYPE_LENGTH) cratType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) effectiveDate: LocalDate?,
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CommissionRateResponse>>> {
        val query = CommissionRateQuery(
            commClassCode = commClassCode,
            commLineCode = commLineCode,
            cratType = cratType,
            effectiveDate = effectiveDate
        )
        val pageInfo = service.search(query, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依序號查詢佣金率
     *
     * @param serial 序號 (主鍵)
     */
    @GetMapping("/{serial}")
    fun getBySerial(
        @PathVariable serial: Long
    ): ResponseEntity<ApiResponse<CommissionRateResponse?>> {
        val rate = service.findBySerial(serial)
        return if (rate != null) {
            ResponseEntity.ok(ApiResponse.success(rate.toResponse()))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "COMMISSION_RATE_NOT_FOUND",
                        message = "Commission rate not found: $serial"
                    )
                )
        }
    }

    /**
     * 依佣金類別碼查詢
     *
     * @param classCode 佣金率類別碼 (1-5 碼)
     */
    @GetMapping("/class/{classCode}")
    fun getByClassCode(
        @PathVariable @Size(min = 1, max = MAX_CLASS_CODE_LENGTH) classCode: String
    ): ResponseEntity<ApiResponse<List<CommissionRateResponse>>> {
        val rates = service.findByClassCode(classCode)
        val response = rates.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 查詢有效佣金率
     *
     * @param commClassCode 佣金率類別碼 (1-5 碼)
     * @param commLineCode 業務線代號 (1-2 碼)
     * @param effectiveDate 生效日期 (格式 yyyy-MM-dd)
     * @param age 年齡 (可選)
     */
    @GetMapping("/effective")
    fun getEffectiveRates(
        @RequestParam @Size(min = 1, max = MAX_CLASS_CODE_LENGTH) commClassCode: String,
        @RequestParam @Size(min = 1, max = MAX_LINE_CODE_LENGTH) commLineCode: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) effectiveDate: LocalDate,
        @RequestParam(required = false) @Min(0) @Max(150) age: Int?
    ): ResponseEntity<ApiResponse<List<CommissionRateResponse>>> {
        val rates = if (age != null) {
            val rate = service.findEffectiveRateForAge(commClassCode, commLineCode, effectiveDate, age)
            if (rate != null) listOf(rate) else emptyList()
        } else {
            service.findEffectiveRates(commClassCode, commLineCode, effectiveDate)
        }
        val response = rates.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 列出所有業務線代號
     */
    @GetMapping("/line-codes")
    fun getAllLineCodes(): ResponseEntity<ApiResponse<List<LineCodeResponse>>> {
        val lineCodes = service.findAllLineCodes()
        val response = lineCodes.map { code ->
            val enumValue = CommissionLineCode.fromCode(code)
            LineCodeResponse(
                code = code,
                description = enumValue?.description
            )
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 列出所有佣金率型態
     */
    @GetMapping("/crat-types")
    fun getAllCratTypes(): ResponseEntity<ApiResponse<List<CratTypeResponse>>> {
        val types = service.findAllCratTypes()
        val response = types.map { code ->
            val enumValue = CommissionRateType.fromCode(code)
            CratTypeResponse(
                code = code,
                description = enumValue?.description
            )
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依業務線代號查詢所有類別碼
     *
     * @param lineCode 業務線代號 (1-2 碼)
     */
    @GetMapping("/line-codes/{lineCode}/class-codes")
    fun getClassCodesByLineCode(
        @PathVariable @Size(min = 1, max = MAX_LINE_CODE_LENGTH) lineCode: String
    ): ResponseEntity<ApiResponse<List<String>>> {
        val classCodes = service.findClassCodesByLineCode(lineCode)
        return ResponseEntity.ok(ApiResponse.success(classCodes))
    }

    /**
     * 刷新佣金率快取
     *
     * 此為內部管理 API，生產環境應配置 Spring Security 限制存取。
     */
    @PostMapping("/refresh")
    fun refreshCache(): ResponseEntity<ApiResponse<String>> {
        service.refreshCache()
        return ResponseEntity.ok(ApiResponse.success("Cache refreshed"))
    }

    private fun CommissionRate.toResponse() = CommissionRateResponse(
        serial = serial,
        commClassCode = commClassCode,
        commLineCode = commLineCode,
        commLineCodeDesc = CommissionLineCode.fromCode(commLineCode)?.description,
        cratType = cratType,
        cratTypeDesc = CommissionRateType.fromCode(cratType)?.description,
        projectNo = projectNo,
        startDate = startDate,
        endDate = endDate,
        cratKey1 = cratKey1,
        cratKey2 = cratKey2,
        commStartYear = commStartYear,
        commEndYear = commEndYear,
        commStartAge = commStartAge,
        commEndAge = commEndAge,
        commStartModx = commStartModx,
        commEndModx = commEndModx,
        commRate = commRate,
        commRateOrg = commRateOrg,
        premLimitStart = premLimitStart,
        premLimitEnd = premLimitEnd
    )
}

/**
 * 佣金率 API 回應格式
 */
data class CommissionRateResponse(
    val serial: Long,
    val commClassCode: String,
    val commLineCode: String,
    val commLineCodeDesc: String?,
    val cratType: String,
    val cratTypeDesc: String?,
    val projectNo: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val cratKey1: String,
    val cratKey2: String,
    val commStartYear: Int?,
    val commEndYear: Int?,
    val commStartAge: Int?,
    val commEndAge: Int?,
    val commStartModx: Int?,
    val commEndModx: Int?,
    val commRate: BigDecimal?,
    val commRateOrg: BigDecimal?,
    val premLimitStart: BigDecimal?,
    val premLimitEnd: BigDecimal?
)

/**
 * 業務線代號回應格式
 */
data class LineCodeResponse(
    val code: String,
    val description: String?
)

/**
 * 佣金率型態回應格式
 */
data class CratTypeResponse(
    val code: String,
    val description: String?
)
