plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

description = "VLIFE-V4 產品與精算模組"

// 暫時停用 bootJar (尚無 main class)
tasks.named("bootJar") { enabled = false }
tasks.named("jar") { enabled = true }

// 測試配置：傳遞環境變數到測試 JVM (Phase 2C V3 連線測試)
tasks.test {
    // 從 .env 檔案讀取 Oracle 連線資訊
    val dotenvFile = rootProject.file(".env")
    if (dotenvFile.exists()) {
        dotenvFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val (key, value) = line.split("=", limit = 2)
                val trimmedKey = key.trim()
                val trimmedValue = value.trim()

                // Only pass Oracle-related environment variables
                if (trimmedKey.startsWith("ORACLE_")) {
                    systemProperty(trimmedKey, trimmedValue)
                    println("Test: Setting $trimmedKey = $trimmedValue")
                }
            }
    }

    // Also try from system environment (fallback)
    val envVars = listOf("ORACLE_HOST", "ORACLE_PORT", "ORACLE_SERVICE", "ORACLE_USERNAME", "ORACLE_PASSWORD")
    envVars.forEach { key ->
        val value = System.getenv(key)
        if (value != null && System.getProperty(key) == null) {
            systemProperty(key, value)
        }
    }
}

dependencies {
    implementation(project(":modules:vlife-common"))
    implementation(project(":modules:vlife-domain-api"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // PageHelper 分頁插件 (ADR-015)
    implementation("com.github.pagehelper:pagehelper-spring-boot-starter:2.1.0")

    // OpenAPI / Swagger UI (P2-8)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
}
