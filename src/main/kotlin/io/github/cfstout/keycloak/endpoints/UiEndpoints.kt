package io.github.cfstout.keycloak.endpoints

import io.ktor.application.Application
import io.ktor.routing.route
import io.ktor.routing.routing
import java.time.Clock

class UiEndpoints(
    app: Application,
    clock: Clock,
) {
    init {
        app.routing {
            route("/ui") {
            }
        }
    }
}
