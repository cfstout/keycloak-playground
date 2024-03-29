@file:UseSerializers(InstantSerializer::class)

package io.github.cfstout.keycloak.models.users

import io.github.cfstout.keycloak.models.Email
import io.github.cfstout.keycloak.serialization.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class User(
    val id: Long,
    val email: Email,
    val passwordHash: String,
    val userName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
