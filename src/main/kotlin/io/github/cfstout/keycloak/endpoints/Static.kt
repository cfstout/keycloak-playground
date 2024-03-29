package io.github.cfstout.keycloak.endpoints

import io.ktor.application.Application
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing

class Static(app: Application) {
    init {
        app.routing {
            static("/static") {
                resources("static")
            }
        }
    }
}
