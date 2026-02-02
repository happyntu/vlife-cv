plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

description = "VLIFE-V4 產品與精算模組"

dependencies {
    implementation(project(":modules:vlife-common"))
    implementation(project(":modules:vlife-domain-api"))
}
