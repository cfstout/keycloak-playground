package io.github.cfstout.keycloak.dao

import io.github.cfstout.keycloak.models.Email
import io.github.cfstout.keycloak.models.users.User
import io.github.cfstout.keycloak_playground.tables.Users.USERS
import io.github.cfstout.keycloak_playground.tables.records.UsersRecord
import org.jooq.DSLContext
import java.time.Instant

interface UserDao {
    fun createUser(
        userName: String,
        email: Email,
        passwordHash: String,
    ): User

    fun getUserById(userId: Long): User?

    fun getUserByEmail(email: Email): User?
}

class SqlUserDao(
    private val txnContext: DSLContext,
) : UserDao {
    override fun createUser(
        userName: String,
        email: Email,
        passwordHash: String,
    ): User {
        return txnContext.insertInto(USERS)
            .set(USERS.USERNAME, userName)
            .set(USERS.EMAIL, email.value)
            .set(USERS.PASSWORD_HASH, passwordHash)
            .returning()
            .fetchOne()
            .toUser()
    }

    override fun getUserById(userId: Long): User? {
        return txnContext.selectFrom(USERS)
            .where(USERS.ID.eq(userId))
            .fetchOne()
            ?.toUser()
    }

    override fun getUserByEmail(email: Email): User? {
        return txnContext.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email.value))
            .fetchOne()
            ?.toUser()
    }

    companion object {
        private fun UsersRecord.toUser(): User {
            return User(
                this.id,
                Email.from(this.email) ?: throw IllegalStateException("Invalid email!!"),
                this.passwordHash,
                this.username,
                Instant.ofEpochMilli(this.createdAt),
                Instant.ofEpochMilli(this.updatedAt),
            )
        }
    }
}
