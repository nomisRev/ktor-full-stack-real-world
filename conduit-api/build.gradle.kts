import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js { browser() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        d8()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.server.resources)
            implementation(libs.kotlinx.datetime)
        }
    }
}
