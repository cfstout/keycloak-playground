package io.github.cfstout.keycloak.endpoints

import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test

internal class UserEndpointsTest {
    @Test
    internal fun getWithNoDataHasNullColor() {
        withTestApplication {
        }
    }
}
