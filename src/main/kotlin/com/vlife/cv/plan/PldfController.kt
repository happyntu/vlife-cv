package com.vlife.cv.plan

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
import java.time.LocalDate

/**
 * 險種描述 REST API (CV.PLDF)
 *
 * 遵循 ADR-017 規範，採用表格導向命名。
 * 提供險種描述的 CRUD 服務，對應 V3 CV001M 作業。
 *
 * 業務別名：PlanDefinitionController
 *
 * API 端點：
 * - GET /api/v1/plan-definitions                           - 搜尋險種
 * - GET /api/v1/plan-definitions/{planCode}/{version}      - 依主鍵查詢
 * - GET /api/v1/plan-definitions/plan/{planCode}           - 依險種代碼查詢所有版本
 * - GET /api/v1/plan-definitions/effective                 - 查詢有效險種
 * - GET /api/v1/plan-definitions/plan-codes                - 列出所有險種代碼
 * - GET /api/v1/plan-definitions/insurance-types           - 列出所有保險型態3
 * - POST /api/v1/plan-definitions                          - 新增險種 (CV001M)
 * - PUT /api/v1/plan-definitions/{planCode}/{version}      - 修改險種 (CV001M)
 * - DELETE /api/v1/plan-definitions/{planCode}/{version}   - 刪除險種 (CV001M)
 * - POST /api/v1/plan-definitions/refresh                  - 刷新快取
 */
@RestController
@RequestMapping("/api/v1/plan-definitions")
@Validated
@Tag(name = "Plan Definitions", description = "險種描述管理 API (CV.PLDF) - CV001M")
class PldfController(
    private val service: PldfService
) {

    companion object {
        private const val MAX_PLAN_CODE_LENGTH = 5
        private const val MAX_VERSION_LENGTH = 1
    }

    /**
     * 搜尋險種描述 (分頁)
     */
    @GetMapping
    @Operation(
        summary = "搜尋險種描述",
        description = "依條件搜尋險種描述，支援分頁。可依險種代碼、版數、主附約指示、保險型態3、生效日期篩選。"
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜尋成功")
    )
    fun search(
        @Parameter(description = "險種代碼 (最長 5 碼，支援模糊查詢)")
        @RequestParam(required = false) @Size(max = MAX_PLAN_CODE_LENGTH) planCode: String?,
        @Parameter(description = "版數 (1 碼)")
        @RequestParam(required = false) @Size(max = MAX_VERSION_LENGTH) version: String?,
        @Parameter(description = "主附約指示 (1=主約, 2=附約)")
        @RequestParam(required = false) @Size(max = 1) primaryRiderInd: String?,
        @Parameter(description = "保險型態3 (A=壽險, B=健康險, C=傷害險, F=投資型壽險...)")
        @RequestParam(required = false) @Size(max = 1) insuranceType3: String?,
        @Parameter(description = "險種型態 (1=個險傳統, 2=個險投資型, 3=團險)")
        @RequestParam(required = false) @Size(max = 1) planType: String?,
        @Parameter(description = "生效日期 (格式 yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) effectiveDate: LocalDate?,
        @Parameter(description = "頁碼 (從 1 開始)")
        @RequestParam(defaultValue = "1") @Min(1) pageNum: Int,
        @Parameter(description = "每頁筆數 (1-100)")
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) pageSize: Int
    ): ResponseEntity<ApiResponse<PageResponse<PldfResponse>>> {
        val query = PldfQuery(
            planCode = planCode,
            version = version,
            primaryRiderInd = primaryRiderInd,
            insuranceType3 = insuranceType3,
            planType = planType,
            effectiveDate = effectiveDate
        )
        val pageInfo = service.search(query, PageRequest(pageNum, pageSize))
        val response = PageResponse.from(pageInfo) { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 依主鍵查詢險種描述
     */
    @GetMapping("/{planCode}/{version}")
    @Operation(
        summary = "依主鍵查詢險種描述",
        description = "依險種代碼和版數查詢單筆險種描述資料。"
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查詢成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "險種不存在")
    )
    fun getByPlanCodeAndVersion(
        @Parameter(description = "險種代碼 (1-5 碼)")
        @PathVariable @Size(min = 1, max = MAX_PLAN_CODE_LENGTH) planCode: String,
        @Parameter(description = "版數 (1 碼)")
        @PathVariable @Size(min = 1, max = MAX_VERSION_LENGTH) version: String
    ): ResponseEntity<ApiResponse<PldfResponse>> {
        val pldf = service.findByPlanCodeAndVersion(planCode, version)
        return if (pldf != null) {
            ResponseEntity.ok(ApiResponse.success(pldf.toResponse()))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "PLAN_DEFINITION_NOT_FOUND",
                        message = "Plan definition not found: planCode=$planCode, version=$version"
                    )
                )
        }
    }

    /**
     * 依險種代碼查詢所有版本
     */
    @GetMapping("/plan/{planCode}")
    @Operation(
        summary = "依險種代碼查詢所有版本",
        description = "查詢指定險種代碼下的所有版本。"
    )
    fun getByPlanCode(
        @Parameter(description = "險種代碼 (1-5 碼)")
        @PathVariable @Size(min = 1, max = MAX_PLAN_CODE_LENGTH) planCode: String
    ): ResponseEntity<ApiResponse<List<PldfResponse>>> {
        val plans = service.findByPlanCode(planCode)
        val response = plans.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 查詢有效險種
     */
    @GetMapping("/effective")
    @Operation(
        summary = "查詢有效險種",
        description = "查詢指定日期上市中的險種清單。"
    )
    fun getEffective(
        @Parameter(description = "生效日期 (格式 yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) effectiveDate: LocalDate
    ): ResponseEntity<ApiResponse<List<PldfResponse>>> {
        val plans = service.findEffective(effectiveDate)
        val response = plans.map { it.toResponse() }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 列出所有險種代碼
     */
    @GetMapping("/plan-codes")
    @Operation(
        summary = "列出所有險種代碼",
        description = "取得資料庫中所有不重複的險種代碼清單。"
    )
    fun getAllPlanCodes(): ResponseEntity<ApiResponse<List<String>>> {
        val planCodes = service.findAllPlanCodes()
        return ResponseEntity.ok(ApiResponse.success(planCodes))
    }

    /**
     * 列出所有保險型態3
     */
    @GetMapping("/insurance-types")
    @Operation(
        summary = "列出所有保險型態3",
        description = "取得資料庫中所有不重複的保險型態3清單。"
    )
    fun getAllInsuranceType3(): ResponseEntity<ApiResponse<List<InsuranceType3Response>>> {
        val types = service.findAllInsuranceType3()
        val response = types.map { code ->
            val enumValue = InsuranceType3.fromCode(code)
            InsuranceType3Response(
                code = code,
                description = enumValue?.description
            )
        }
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    // ==================== CUD 操作 (CV001M) ====================

    /**
     * 新增險種描述 (CV001M - Insert)
     */
    @PostMapping
    @Operation(
        summary = "新增險種描述",
        description = """新增險種描述資料。對應 V3 CV001M 作業的新增功能。

新增前會檢查主鍵（險種代碼+版數）是否已存在。
若已存在會回傳 409 Conflict。"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "新增成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求格式錯誤"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "險種已存在")
    )
    fun create(
        @RequestBody @Validated request: PldfCreateRequest
    ): ResponseEntity<ApiResponse<PldfResponse>> {
        return try {
            val pldf = service.create(request)
            ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(pldf.toResponse()))
        } catch (e: PldfAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                    ApiResponse.error(
                        httpStatus = 409,
                        errorCode = "PLAN_DEFINITION_ALREADY_EXISTS",
                        message = e.message ?: "Plan definition already exists"
                    )
                )
        }
    }

    /**
     * 修改險種描述 (CV001M - Update)
     */
    @PutMapping("/{planCode}/{version}")
    @Operation(
        summary = "修改險種描述",
        description = """修改險種描述資料。對應 V3 CV001M 作業的修改功能。

注意：主鍵（險種代碼+版數）不可修改。"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "修改成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求格式錯誤"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "險種不存在")
    )
    fun update(
        @Parameter(description = "險種代碼 (1-5 碼)")
        @PathVariable @Size(min = 1, max = MAX_PLAN_CODE_LENGTH) planCode: String,
        @Parameter(description = "版數 (1 碼)")
        @PathVariable @Size(min = 1, max = MAX_VERSION_LENGTH) version: String,
        @RequestBody @Validated request: PldfUpdateRequest
    ): ResponseEntity<ApiResponse<PldfResponse>> {
        return try {
            val pldf = service.update(planCode, version, request)
            ResponseEntity.ok(ApiResponse.success(pldf.toResponse()))
        } catch (e: PldfNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "PLAN_DEFINITION_NOT_FOUND",
                        message = e.message ?: "Plan definition not found"
                    )
                )
        }
    }

    /**
     * 刪除險種描述 (CV001M - Delete)
     */
    @DeleteMapping("/{planCode}/{version}")
    @Operation(
        summary = "刪除險種描述",
        description = """刪除險種描述資料。對應 V3 CV001M 作業的刪除功能。

注意：V3 中刪除時會同時刪除相關的 PLNT、QPLYR、CRTB、PLAT 資料，
V4 版本目前僅刪除 PLDF 主表，後續擴充時會加入關聯刪除。"""
    )
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "刪除成功"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "險種不存在")
    )
    fun delete(
        @Parameter(description = "險種代碼 (1-5 碼)")
        @PathVariable @Size(min = 1, max = MAX_PLAN_CODE_LENGTH) planCode: String,
        @Parameter(description = "版數 (1 碼)")
        @PathVariable @Size(min = 1, max = MAX_VERSION_LENGTH) version: String
    ): ResponseEntity<ApiResponse<Unit>> {
        return try {
            service.delete(planCode, version)
            ResponseEntity.noContent().build()
        } catch (e: PldfNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                    ApiResponse.error(
                        httpStatus = 404,
                        errorCode = "PLAN_DEFINITION_NOT_FOUND",
                        message = e.message ?: "Plan definition not found"
                    )
                )
        }
    }

    /**
     * 刷新險種描述快取
     *
     * **存取控制**：此端點為管理員專用，需在 API Gateway (Kong) 配置 ACL 限制。
     */
    @PostMapping("/refresh")
    @AdminOnly(description = "快取刷新需管理員權限")
    @Operation(
        summary = "刷新快取 (管理員)",
        description = """清除並刷新險種描述快取。

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

    private fun Pldf.toResponse() = PldfResponse(
        planCode = planCode,
        version = version,
        contractedName = contractedName,
        planTitle = planTitle,
        planTitleEn = planTitleEn,
        planStartDate = planStartDate,
        planEndDate = planEndDate,
        currency = currency,
        primaryRiderInd = primaryRiderInd,
        primaryRiderIndDesc = PrimaryRiderInd.fromCode(primaryRiderInd)?.description,
        insuranceType1 = insuranceType1,
        insuranceType2 = insuranceType2,
        insuranceType3 = insuranceType3,
        insuranceType3Desc = InsuranceType3.fromCode(insuranceType3)?.description,
        lowAge = lowAge,
        highAge = highAge,
        lowAgeInd = lowAgeInd,
        highAgeInd = highAgeInd,
        payYearInd = payYearInd,
        payYear = payYear,
        covYearInd = covYearInd,
        covYear = covYear,
        planType = planType,
        planTypeDesc = PlanType.fromCode(planType)?.description,
        planClass = planClass,
        divType = divType,
        loanAvalInd = loanAvalInd,
        csvCalcType = csvCalcType,
        rpuCalcType = rpuCalcType,
        etpCalcType = etpCalcType,
        commClassCode = commClassCode,
        commClassCodeI = commClassCodeI,
        uwPlanCode = uwPlanCode,
        uwVersion = uwVersion,
        pcPlanCode = pcPlanCode,
        pcVersion = pcVersion
    )
}

/**
 * 險種描述 API 回應格式 (CV.PLDF)
 */
data class PldfResponse(
    val planCode: String,
    val version: String,
    val contractedName: String?,
    val planTitle: String?,
    val planTitleEn: String?,
    val planStartDate: LocalDate,
    val planEndDate: LocalDate,
    val currency: String?,
    val primaryRiderInd: String?,
    val primaryRiderIndDesc: String?,
    val insuranceType1: String?,
    val insuranceType2: String?,
    val insuranceType3: String?,
    val insuranceType3Desc: String?,
    val lowAge: Int?,
    val highAge: Int?,
    val lowAgeInd: String?,
    val highAgeInd: String?,
    val payYearInd: String?,
    val payYear: Int?,
    val covYearInd: String?,
    val covYear: Int?,
    val planType: String?,
    val planTypeDesc: String?,
    val planClass: String?,
    val divType: String?,
    val loanAvalInd: String?,
    val csvCalcType: String?,
    val rpuCalcType: String?,
    val etpCalcType: String?,
    val commClassCode: String?,
    val commClassCodeI: String?,
    val uwPlanCode: String?,
    val uwVersion: String?,
    val pcPlanCode: String?,
    val pcVersion: String?
)

/**
 * 保險型態3 回應格式
 */
data class InsuranceType3Response(
    val code: String,
    val description: String?
)

/**
 * 主附約指示
 */
enum class PrimaryRiderInd(val code: String, val description: String) {
    PRIMARY("1", "主約"),
    RIDER("2", "附約");

    companion object {
        fun fromCode(code: String?): PrimaryRiderInd? =
            entries.find { it.code == code }
    }
}

/**
 * 保險型態3 (準備金)
 */
enum class InsuranceType3(val code: String, val description: String) {
    LIFE("A", "壽險"),
    HEALTH("B", "健康險"),
    ACCIDENT("C", "傷害險"),
    ANNUITY("D", "年金險"),
    ENDOWMENT("E", "儲蓄險"),
    INVESTMENT_LIFE("F", "投資型壽險"),
    INVESTMENT_ANNUITY("G", "投資型年金"),
    VARIABLE("H", "變額壽險");

    companion object {
        fun fromCode(code: String?): InsuranceType3? =
            entries.find { it.code == code }
    }
}

/**
 * 險種型態
 */
enum class PlanType(val code: String, val description: String) {
    INDIVIDUAL_TRADITIONAL("1", "個險傳統"),
    INDIVIDUAL_INVESTMENT("2", "個險投資型"),
    GROUP("3", "團險"),
    ACCIDENT("9", "意外險");

    companion object {
        fun fromCode(code: String?): PlanType? =
            entries.find { it.code == code }
    }
}
