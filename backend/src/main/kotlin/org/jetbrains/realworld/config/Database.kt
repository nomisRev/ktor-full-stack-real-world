package org.jetbrains.realworld.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import io.ktor.server.config.getAs
import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import kotlin.apply

@Serializable
data class DatabaseConfig(
    val driverClassName: String,
    val host: String,
    val port: Int,
    val name: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int,
    val cachePrepStmts: Boolean,
    val prepStmtCacheSize: Int,
    val prepStmtCacheSqlLimit: Int,
    val migrations: Migrations,
) {
    @Serializable
    data class Migrations(
        val locations: String,
        val baselineOnMigrate: Boolean,
    )
}

fun Application.setupDatabase(config: DatabaseConfig): Database {
    val dataSource = dataSource(config)
    migrate(dataSource, config.migrations)
    val database = Database.connect(dataSource)

    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
        dataSource.close()
    }

    return database
}

fun dataSource(config: DatabaseConfig): HikariDataSource =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.name}"
        username = config.username
        password = config.password
        driverClassName = config.driverClassName
        maximumPoolSize = config.maxPoolSize
        addDataSourceProperty("cachePrepStmts", config.cachePrepStmts.toString())
        addDataSourceProperty("prepStmtCacheSize", config.prepStmtCacheSize.toString())
        addDataSourceProperty("prepStmtCacheSqlLimit", config.prepStmtCacheSqlLimit.toString())
    })

@IgnorableReturnValue
fun migrate(dataSource: HikariDataSource, config: DatabaseConfig.Migrations): MigrateResult =
    Flyway.configure()
        .dataSource(dataSource)
        .locations(config.locations)
        .baselineOnMigrate(config.baselineOnMigrate)
        .load()
        .migrate()
