
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
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

@Suppress("OPT_IN_USAGE")
powerAssert {
    functions = listOf("kotlin.test.assertEquals")
}

dependencies {
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)

    // Database
    implementation(libs.postgresql)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.time)
    implementation(libs.hikaricp)
    implementation(libs.flyway)

    // Metrics and Monitoring
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)

    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.bouncycastle)

    // Server and Logging
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)

    // Testing
    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.bundles.testing)
}
