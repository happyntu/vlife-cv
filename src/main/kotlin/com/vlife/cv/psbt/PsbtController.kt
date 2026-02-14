package com.vlife.cv.psbt

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * PSBT 險種給付參數子表 REST API
 *
 * 供 PS (給付) 等模組跨模組查詢精算參數。
 * Design Spec: pk-lib-psbtproc.md
 */
@RestController
@RequestMapping("/api/v1/product-sub-benefits")
class PsbtController(
    private val psbtService: PsbtService
) {

    /**
     * 範圍匹配查詢：依險種複合鍵 + KEY 範圍匹配。
     * 供 PS (給付模組) 跨模組呼叫。
     *
     * 設計備註：因 PSBT 有 5 層複合鍵 + 2 個範圍參數，
     * 採用全 Query Parameter 方案避免 URI 過深。
     */
    @GetMapping("/lookup")
    fun findByKeysAndRange(
        @RequestParam planCode: String,
        @RequestParam version: String,
        @RequestParam rateSex: String,
        @RequestParam psbtType: String,
        @RequestParam psbtCode: String,
        @RequestParam key1: Long,
        @RequestParam key2: Long
    ): ResponseEntity<Psbt> {
        val result = psbtService.findByKeysAndRange(planCode, version, rateSex, psbtType, psbtCode, key1, key2)
        return if (result != null) ResponseEntity.ok(result) else ResponseEntity.notFound().build()
    }

    /**
     * 批次查詢（供快取預載）：依險種複合鍵查詢所有記錄。
     */
    @GetMapping
    fun findAllByKeys(
        @RequestParam planCode: String,
        @RequestParam version: String,
        @RequestParam rateSex: String,
        @RequestParam psbtType: String,
        @RequestParam psbtCode: String
    ): List<Psbt> {
        return psbtService.findAllByKeys(planCode, version, rateSex, psbtType, psbtCode)
    }
}
