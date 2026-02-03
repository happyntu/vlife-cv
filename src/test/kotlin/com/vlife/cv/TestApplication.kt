package com.vlife.cv

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

/**
 * 整合測試用 Spring Boot 應用程式
 *
 * 僅用於測試，提供 Spring ApplicationContext
 * 掃描 vlife-cv 模組和 vlife-common 必要的套件
 *
 * 限制掃描範圍避免載入依賴 Mapper 的元件：
 * - com.vlife.cv: CV 模組本身
 * - com.vlife.common.response: ApiResponse
 * - com.vlife.common.exception: VlifeException
 * - com.vlife.common.web: GlobalExceptionHandler
 *
 * 不掃描：
 * - com.vlife.common.code: CvtbController/CvtbService 等需要 Mapper
 * - com.vlife.common.config: CacheConfig bean 名稱衝突
 * - com.vlife.common.event: OutboxEventPublisher 需要 Mapper
 */
@SpringBootApplication(
    excludeName = [
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    ]
)
@ComponentScan(
    basePackages = [
        "com.vlife.cv",
        "com.vlife.common.response",
        "com.vlife.common.exception",
        "com.vlife.common.web",
        "com.vlife.common.validation",
        "com.vlife.common.logging",
        "com.vlife.common.util"
    ]
)
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}
