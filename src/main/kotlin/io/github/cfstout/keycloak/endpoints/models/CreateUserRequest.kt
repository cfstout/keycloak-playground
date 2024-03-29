package io.github.cfstout.keycloak.endpoints.models

import io.github.cfstout.keycloak.models.Email
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val userName: String,
    private val email: String,
    val passwordHash: String,
) {
    val normalizedEmail = Email.from(email)

    init {
        require(userName.isNotBlank()) { "userName must not be blank" }
        require(normalizedEmail != null) { "email is invalid" }
    }
}
