package com.vlife.cv.commission

import com.vlife.common.response.ApiResponse
import com.vlife.common.security.AdminOnly
import com.vlife.cv.common.PageRequest
import com.vlife.cv.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 佣金率 REST API (CV.CRAT)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供佣金率的查詢服務，供業管 (AG) 及其他業務模組呼叫。
 *
 * 業務別名：CommissionRateController
 *
 * API 端點：
 * - GET /api/v1/commission-rates                    - 搜尋佣金率
 * - GET /api/v1/commission-rates/{serial}           - 依序號查詢
 * - GET /api/v1/commission-rates/class/{classCode}  - 依類別碼查詢
 * - GET /api/v1/commission-rates/effective          - 查詢有效佣金率
 * - GET /api/v1/commission-rates/line-codes         - 列出所有業務線代號
 * - GET /api/v1/commission-rates/crat-types         - 列出所有佣金率型態
 * - POST /api/v1/commission-rates                   - 新增佣金率 (CV004M)
 * - PUT /api/v1/commission-rates/{serial}           - 修改佣金率 (CV004M)
 * - DELETE /api/v1/commission-rates/{serial}        - 刪除佣金率 (CV004M)
 * - POST /api/v1/commission-rates/refresh           - 刷新快取
 */
@RestController
@RequestMapping("/api/v1/commission-rates")
@Validated
@Tag(name = "Commission Rates", description = "佣金率管理 API (CV.CRAT)")
class CratController(
    private val service: CratService
) {

    companion object {
        private const val MAX_CLASS_CODE_LENGTH = 5
        private const val MAX_LINE_CODE_LENGTH = 2
        private const val MAX_CRAT_TYPE_LENGTH = 1
    }

    /**
     * 搜尋佣金率 (分頁)
     */
    @GetMapping
    @Operation(
        summary = "搜尋佣金率",
        description = "依條件搜尋佣金率，支援分頁。可依類別碼、業務線代號、型態、生效日期篩選。"
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜尋成功")
    )
    fun search(
        @Parameter(description = "佣金率類別碼 (最長 5 碼)")
        @RequestParam(required = false) @Size(max = MAX_CLASS_CODE_LENGTH) commClassCode: String?,
        @Parameter(description = "業務線代號 (最長 2 碼)")
        @RequestParam(required = false) @Size(max = MAX_LINE_CODE_LENGTH) commLineCode: String?,
        @Parameter(description = "佣金率型態 (1 碼)")
        @RequestParam(required = false) @Size(max = MAX_CRAT_TYPE_LENGTH) cratType: String?,
        @Parameter(description = "生效日期 (格式 yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) effectiveDate: LocalDate?,
        @Parameter(description = "頁碼 (從 1 開始)")
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @Parameter(description = "每頁筆數 (1-100)")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<CratResponse>>> {
        val query = CratQuery(
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
     */
    @GetMapping("/{serial}")
    @Operation(
        summary = "依序號查詢佣金率",
        description = "依主鍵序號查詢單筆佣金率資料。"
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "佣金率不存在")
    )
    fun getBySerial(
        @Parameter(description = "佣金率序號 (主鍵)")
        @PathVariable serial: Long
    ): ResponseEntity<ApiResponse<CratResponse>> {
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
     */
    @GetMapping("/class/{classCode}")
    @Operation(
        summary = "依類別碼查詢佣金率",
        description = "查詢指定類別碼下的所有佣金率。"
    )
    fun getByClassCode(
        @Parameter(description = "佣金率類別碼 (1-5 碼)")
        @PathVariable @Size(min = 1, max = MAX_CLASS_CODE_LENGTH) classCode: String
    ): ResponseEntity<ApiResponse<List<CratResponse>>> {
        val rates = service.findByClassCode(classCode)
        val response = rates.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 查詢有效佣金率
     */
    @GetMapping("/effective")
    @Operation(
        summary = "查詢有效佣金率",
        description = "依類別碼、業務線代號及生效日期查詢有效的佣金率。可選擇性依年齡篩選。"
    )
    fun getEffectiveRates(
        @Parameter(description = "佣金率類別碼 (1-5 碼)", required = true)
        @RequestParam @Size(min = 1, max = MAX_CLASS_CODE_LENGTH) commClassCode: String,
        @Parameter(description = "業務線代號 (1-2 碼)", required = true)
        @RequestParam @Size(min = 1, max = MAX_LINE_CODE_LENGTH) commLineCode: String,
        @Parameter(description = "生效日期 (格式 yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) effectiveDate: LocalDate,
        @Parameter(description = "年齡 (0-150)")
        @RequestParam(required = false) @Min(0) @Max(150) age: Int?
    ): ResponseEntity<ApiResponse<List<CratResponse>>> {
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
    @Operation(
        summary = "列出所有業務線代號",
        description = "取得資料庫中所有不重複的業務線代號清單。"
    )
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
    @Operation(
        summary = "列出所有佣金率型態",
        description = "取得資料庫中所有不重複的佣金率型態清單。"
    )
    fun getAllCratTypes(): ResponseEntity<ApiResponse<List<CratTypeResponse>>> {
        val types = service.findAllCratTypes()
        val response = types.map { code ->
            val enumValue = CratType.fromCode(code)
            CratTypeResponse(
                code = code,
                description = enumValue?.description
            )
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依業務線代號查詢所有類別碼
     */
    @GetMapping("/line-codes/{lineCode}/class-codes")
    @Operation(
        summary = "依業務線查詢類別碼",
        description = "取得指定業務線代號下所有不重複的類別碼清單。"
    )
    fun getClassCodesByLineCode(
        @Parameter(description = "業務線代號 (1-2 碼)")
        @PathVariable @Size(min = 1, max = MAX_LINE_CODE_LENGTH) lineCode: String
    ): ResponseEntity<ApiResponse<List<String>>> {
        val classCodes = service.findClassCodesByLineCode(lineCode)
        return ResponseEntity.ok(ApiResponse.success(classCodes))
    }

    // ==================== CUD 操作 (CV004M) ====================

    /**
     * 新增佣金率 (CV004M - Insert)
     */
    @PostMapping
    @Operation(
        summary = "新增佣金率",
        description = """新增佣金率資料。對應 V3 CV004M 作業的新增功能。

新增前會檢查 key 值是否重疊（類別碼、業務線、型態、日期範圍、年期範圍）。
若有重疊會回傳 409 Conflict。"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "新增成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求格式錯誤"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Key 值重疊")
    )
    fun create(
        @RequestBody @Validated request: CratCreateRequest
    ): ResponseEntity<ApiResponse<CratResponse>> {
        return try {
            val crat = service.create(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(crat.toResponse()))
        } catch (e: CratOverlapException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                    ApiResponse.error(
                        httpStatus = 409,
                        errorCode = "COMMISSION_RATE_OVERLAP",
                        message = e.message ?: "Commission rate key overlap"
                    )
                )
        }
    }

    /**
     * 修改佣金率 (CV004M - Update)
     */
    @PutMapping("/{serial}")
    @Operation(
        summary = "修改佣金率",
        description = """修改佣金率資料。對應 V3 CV004M 作業的修改功能。

修改日期範圍或年期範圍時會檢查 key 值是否重疊。
注意：commClassCode (佣金率類別碼) 不可修改。"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求格式錯誤"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "佣金率不存在"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Key 值重疊")
    )
    fun update(
        @Parameter(description = "佣金率序號 (主鍵)")
        @PathVariable serial: Long,
        @RequestBody @Validated request: CratUpdateRequest
    ): ResponseEntity<ApiResponse<CratResponse>> {
        return try {
            val crat = service.update(serial, request)
            ResponseEntity.ok(ApiResponse.success(crat.toResponse()))
        } catch (e: CratNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "COMMISSION_RATE_NOT_FOUND",
                        message = e.message ?: "Commission rate not found"
                    )
                )
        } catch (e: CratOverlapException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                    ApiResponse.error(
                        httpStatus = 409,
                        errorCode = "COMMISSION_RATE_OVERLAP",
                        message = e.message ?: "Commission rate key overlap"
                    )
                )
        }
    }

    /**
     * 刪除佣金率 (CV004M - Delete)
     */
    @DeleteMapping("/{serial}")
    @Operation(
        summary = "刪除佣金率",
        description = "刪除佣金率資料。對應 V3 CV004M 作業的刪除功能。"
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "刪除成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "佣金率不存在")
    )
    fun delete(
        @Parameter(description = "佣金率序號 (主鍵)")
        @PathVariable serial: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        return try {
            service.delete(serial)
            ResponseEntity.noContent().build()
        } catch (e: CratNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "COMMISSION_RATE_NOT_FOUND",
                        message = e.message ?: "Commission rate not found"
                    )
                )
        }
    }

    /**
     * 刷新佣金率快取
     *
     * **存取控制**：此端點為管理員專用，需在 API Gateway (Kong) 配置 ACL 限制。
     */
    @PostMapping("/refresh")
    @AdminOnly(description = "快取刷新需管理員權限")
    @Operation(
        summary = "刷新快取 (管理員)",
        description = """清除並刷新佣金率快取。

**存取控制**：此端點僅限管理員使用。
- 生產環境透過 Kong API Gateway ACL 控制存取
- 開發環境可直接呼叫（測試用途）"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "快取刷新成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "無權限存取")
    )
    fun refreshCache(): ResponseEntity<ApiResponse<String>> {
        service.refreshCache()
        return ResponseEntity.ok(ApiResponse.success(data = "Cache refreshed"))
    }

    private fun Crat.toResponse() = CratResponse(
        serial = serial,
        commClassCode = commClassCode,
        commLineCode = commLineCode,
        commLineCodeDesc = CommissionLineCode.fromCode(commLineCode)?.description,
        cratType = cratType,
        cratTypeDesc = CratType.fromCode(cratType)?.description,
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
 * 佣金率 API 回應格式 (CV.CRAT)
 */
data class CratResponse(
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
