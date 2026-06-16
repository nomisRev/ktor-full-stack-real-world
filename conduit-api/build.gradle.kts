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
    wasmJs {
        browser()
        d8()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(ktorLibs.resources)
            implementation(libs.kotlinx.datetime)
        }
    }
}
