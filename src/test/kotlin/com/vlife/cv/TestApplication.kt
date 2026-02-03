package com.vlife.cv

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * 整合測試用 Spring Boot 應用程式
 *
 * 僅用於測試，提供 Spring ApplicationContext
 * 掃描 vlife-cv 和 vlife-common 模組的元件
 */
@SpringBootApplication(
    scanBasePackages = ["com.vlife.cv", "com.vlife.common"],
    excludeName = [
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    ]
)
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}
