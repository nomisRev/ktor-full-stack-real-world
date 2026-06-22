
plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(ktorLibs.plugins.ktor.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.assert)
}

group = "org.jetbrains"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-sensitive-resolution",
            "-Xreturn-value-checker=full",
            "-Xcollection-literals",
            "-Xname-based-destructuring=complete",
        )
        allWarningsAsErrors = true
        extraWarnings = true
    }
}

@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf("kotlin.test.assertEquals")
}

dependencies {
    implementation(projects.conduitApi)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)

    // Database
    implementation(libs.postgresql)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.time)
    implementation(libs.hikaricp)
    implementation(libs.flyway)

    // Metrics and Monitoring
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.autoHeadResponse)
    implementation(ktorLibs.server.resources)
    implementation(ktorLibs.server.requestValidation)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.callLogging)

    implementation(ktorLibs.server.auth.jwt)
    implementation(libs.bouncycastle)

    // Server and Logging
    implementation(libs.logback.classic)
    implementation(ktorLibs.server.config.yaml)

    // Testing
    testImplementation(ktorLibs.client.cio)
    testImplementation(ktorLibs.client.resources)
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.bundles.testing)
}
