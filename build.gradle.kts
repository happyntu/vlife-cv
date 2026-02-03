plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

description = "VLIFE-V4 產品與精算模組"

// 暫時停用 bootJar (尚無 main class)
tasks.named("bootJar") { enabled = false }
tasks.named("jar") { enabled = true }

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
