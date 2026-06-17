package org.jetbrains.realworld

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.realworld.config.dataSource
import org.jetbrains.realworld.config.migrate
import kotlin.test.BeforeTest

abstract class DatabaseSpec {
    lateinit var database: Database

    @BeforeTest
    fun setup() {
        val config = PostgresContainer.getDatabaseConfig()
        val dataSource = dataSource(config)
        migrate(dataSource, config.migrations)
        database = Database.connect(dataSource)
    }
}
