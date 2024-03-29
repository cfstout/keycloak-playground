package io.github.cfstout.keycloak.dao

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cfstout.keycloak_playground.Tables
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderNameCase
import org.jooq.conf.RenderQuotedNames
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import java.io.Closeable
import java.util.UUID

class DbTestHelpers : Closeable {
    private val config =
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1" // In-memory database
            driverClassName = "org.h2.Driver"
            isAutoCommit = false
            username = "sa" // Default username for H2
            password = "" // Default password for H2 is empty
            maximumPoolSize = 2
        }

    private val dataSource: HikariDataSource = HikariDataSource(config)
    val configuration =
        DefaultConfiguration().apply {
            set(dataSource)
            set(SQLDialect.H2)
            settings()
                .withRenderSchema(false)
                .withRenderNameCase(RenderNameCase.LOWER)
                .withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED)
        }
    val txnContext: DSLContext = DSL.using(configuration)

    fun randomUsername(): String = "user${UUID.randomUUID()}"

    fun createUser(): Long {
        val userName = randomUsername()
        return txnContext.transactionResult { t ->
            t.dsl().insertInto(Tables.USERS)
                .set(Tables.USERS.USERNAME, userName)
                .set(Tables.USERS.EMAIL, "$userName@keycloak-playground.com")
                .set(Tables.USERS.PASSWORD_HASH, "test")
                .returning()
                .fetchOne()
                .id
        }
    }

    fun resetDb() {
        val flyway =
            Flyway.configure()
                .cleanDisabled(false)
                .dataSource(dataSource)
                .load()
        flyway.clean()
        flyway.migrate()
    }

    override fun close() {
        dataSource.close()
        txnContext.close()
    }
}
