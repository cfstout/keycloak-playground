package io.github.cfstout.keycloak

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.zaxxer.hikari.HikariDataSource
import freemarker.cache.ClassTemplateLoader
import io.github.cfstout.keycloak.config.fromDirectory
import io.github.cfstout.keycloak.dao.SqlDaoFactory
import io.github.cfstout.keycloak.endpoints.Static
import io.github.cfstout.keycloak.endpoints.UiEndpoints
import io.github.cfstout.keycloak.endpoints.UserEndpoints
import io.github.cfstout.keycloak.hikari.buildHikariConfig
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.NotFoundException
import io.ktor.features.StatusPages
import io.ktor.freemarker.FreeMarker
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.HttpMethodRouteSelector
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object Server {
    private val logger = LoggerFactory.getLogger(Server::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        val clock = Clock.systemUTC()
        val start = clock.instant()
        val warmupPool = Executors.newCachedThreadPool(DaemonThreadFactory)
        val configDir =
            Path.of(
                args.getOrNull(0)
                    ?: throw IllegalArgumentException("First argument must be config dir"),
            )

        val config = EnvironmentVariables() overriding ConfigurationProperties.fromDirectory(configDir)
        val jooqFuture =
            warmupPool.submit(
                Callable {
                    DSL.using(
                        HikariDataSource(buildHikariConfig(config, "DB_")),
                        SQLDialect.POSTGRES,
                    )
                },
            )
        logger.info("Starting up server")
        val server =
            embeddedServer(Netty, port = HttpServerConfig(config).port) {
                install(CallLogging) {
                    level = Level.INFO
                }
                // todo error handling in it's own class
                install(StatusPages) {
                    exception<Throwable> {
                        logger.error("Unhandled exception", it)
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                    exception<IllegalArgumentException> {
                        logger.error("Bad request", it)
                        call.respond(HttpStatusCode.BadRequest, it.message ?: "Bad request")
                    }
                    exception<NotFoundException> {
                        logger.warn("Not found", it)
                        call.respond(HttpStatusCode.NotFound, it.message ?: "Not found")
                    }
                }
                install(ContentNegotiation) {
                    json()
                }
                install(FreeMarker) {
                    templateLoader = ClassTemplateLoader(this::class.java.classLoader, "/templates")
                }

                Static(this)
                val sqlDaoFactory = SqlDaoFactory(clock)
                UserEndpoints(this, jooqFuture.get(), sqlDaoFactory)
                UiEndpoints(this, clock)

                val root = feature(Routing)
                val allRoutes = allRoutes(root)
                val allRoutesWithMethod = allRoutes.filter { it.selector is HttpMethodRouteSelector }
                allRoutesWithMethod.forEach {
                    logger.info("route: $it")
                }
                logger.info("Startup time: ${Duration.between(start, Instant.now()).toMillis()}ms")
            }
        warmupPool.shutdown()
        server.start(wait = true)
    }

    private fun allRoutes(root: Route): List<Route> {
        return listOf(root) + root.children.flatMap { allRoutes(it) }.sortedBy { it.toString() }
    }
}

class HttpServerConfig(config: Configuration) {
    val port: Int = config[Key("HTTP_LISTEN_PORT", intType)]
}

object DaemonThreadFactory : ThreadFactory {
    private val delegate = Executors.defaultThreadFactory()

    override fun newThread(r: Runnable): Thread =
        delegate.newThread(r).apply {
            isDaemon = true
        }
}
