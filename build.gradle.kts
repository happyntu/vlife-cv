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
}
