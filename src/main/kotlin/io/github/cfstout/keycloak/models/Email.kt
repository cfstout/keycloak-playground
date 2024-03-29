
package io.github.cfstout.keycloak.models

import kotlinx.serialization.Serializable
import org.apache.commons.validator.routines.EmailValidator

fun isValidEmail(email: String): Boolean {
    return EmailValidator.getInstance().isValid(email)
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class Email private constructor(val value: String) {
    companion object {
        fun from(rawEmail: String): Email? {
            val trimmedEmail = rawEmail.trim().lowercase()

            // Use Apache Commons EmailValidator for a comprehensive email validation check
            if (!isValidEmail(trimmedEmail)) {
                return null // Invalid email format
            }

            // Split the email into local and domain parts
            val parts = trimmedEmail.split("@")
            if (parts.size != 2) return null // Email must contain exactly one '@'

            // Remove all periods and anything after a '+' in the local part
            val normalizedLocalPart =
                parts[0]
                    .substringBefore('+') // Remove subaddressing
                    .replace(".", "") // Remove all periods

            // Reconstruct the email with the normalized local part and original domain part
            val normalizedEmail = "$normalizedLocalPart@${parts[1]}"

            return Email(normalizedEmail)
        }
    }

    override fun toString(): String {
        // Take the first three characters of the email
        val visiblePart = value.take(3)

        // Find the index of '@' to keep the domain part visible
        val atIndex = value.indexOf('@')

        // Ensure there's an '@' symbol and the visible part doesn't overshoot it
        return if (atIndex != -1 && atIndex > 3) {
            // Combine the visible part, stars, and the domain part
            "$visiblePart***${value.substring(atIndex)}"
        } else {
            // If the email is too short or doesn't have an '@', just return the obfuscated part
            "$visiblePart***"
        }
    }
}
