@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import java.net.URI

plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    id(libs.plugins.kotlin.serialization.get().pluginId)
    id(libs.plugins.android.multiplatform.library.get().pluginId)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_4_0`,
                reason = "Work around Kotlin/Native cache generation failure for navigation-common 2.9.0-alpha15.",
                issueUrl = URI("https://kotl.in/disable-native-cache"),
            )
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidLibrary {
        namespace = "org.jetbrains.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.security.crypto)
            implementation(ktorLibs.client.android)
        }
        commonMain.dependencies {
            implementation(projects.conduitApi)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.navigation.compose)
            implementation(ktorLibs.client.core)
            implementation(ktorLibs.client.resources)
            implementation(ktorLibs.client.contentNegotiation)
            implementation(ktorLibs.serialization.kotlinx.json)
        }
        jvmMain.dependencies {
            implementation(ktorLibs.client.cio)
        }
        iosMain.dependencies {
            implementation(ktorLibs.client.darwin)
        }
        jsMain.dependencies {
            implementation(ktorLibs.client.js)
        }
        wasmJsMain.dependencies {
            implementation(ktorLibs.client.js)
        }
    }
}

dependencies {
    androidRuntimeClasspath(compose.uiTooling)
}
