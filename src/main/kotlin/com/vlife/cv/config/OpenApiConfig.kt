package com.vlife.cv.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI 3.0 / Swagger UI 配置
 *
 * 提供 CV 模組 API 文件自動生成功能。
 *
 * 存取路徑：
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 * - OpenAPI YAML: /v3/api-docs.yaml
 */
@Configuration
class OpenApiConfig {

    @Value("\${spring.application.name:vlife-cv}")
    private lateinit var applicationName: String

    @Value("\${vlife.api.production-url:https://api.vlife.com.tw}")
    private lateinit var productionApiUrl: String

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(apiInfo())
            .servers(listOf(
                Server()
                    .url("/")
                    .description("本地開發環境"),
                Server()
                    .url(productionApiUrl)
                    .description("生產環境")
            ))
            .tags(listOf(
                Tag()
                    .name("Commission Rates")
                    .description("佣金率管理 API (CV.CRAT)"),
                Tag()
                    .name("Coverages")
                    .description("承保範圍管理 API (CV.CVCO)"),
                Tag()
                    .name("Product Units")
                    .description("紅利分配管理 API (CV.CVPU)"),
                Tag()
                    .name("Dividend Levels")
                    .description("紅利分配水準 API (CV.CVDI)"),
                Tag()
                    .name("Reserve Factors")
                    .description("準備金因子 API (CV.CVRF)")
            ))
    }

    private fun apiInfo(): Info {
        return Info()
            .title("VLIFE-V4 CV 模組 API")
            .description(
                """
                |## 產品與精算模組 (CV Module)
                |
                |提供以下核心功能：
                |
                |### 佣金管理
                |- 佣金率查詢 (CRAT)
                |- 業務線代號管理
                |- 佣金率型態管理
                |
                |### 承保管理
                |- 承保範圍查詢 (CVCO)
                |- 險種代碼管理
                |- 承保狀態管理
                |
                |### 紅利管理
                |- 紅利分配查詢 (CVPU)
                |- 紅利摘要統計
                |- 紅利分配水準 (CVDI)
                |
                |### 精算資料
                |- 準備金因子查詢 (CVRF)
                |
                |---
                |
                |**技術規格**
                |- 遵循 ADR-017 表格導向命名規範
                |- 使用 Caffeine 本地快取 (TTL: 1 小時)
                |- 支援 PageHelper 分頁
                """.trimMargin()
            )
            .version("4.0.0")
            .contact(
                Contact()
                    .name("VLIFE Development Team")
                    .email("dev@vlife.com.tw")
            )
            .license(
                License()
                    .name("Proprietary")
                    .url("https://vlife.com.tw/license")
            )
    }
}
