package io.github.cfstout.keycloak.dao

import io.github.cfstout.keycloak.models.Email
import io.github.cfstout.keycloak.models.users.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

abstract class UserDaoTest {
    abstract fun withDao(block: (UserDao) -> Unit)

    @Test
    fun getUserReturnsNull() {
        withDao {
            assertNull(it.getUserById(1))
        }
    }

    @Test
    fun createAndGet() {
        withDao {
            val userName = "test"
            val email = Email.from("test@hersko.com") ?: throw IllegalStateException("Invalid email")
            val passwordHash = "password"
            val user = it.createUser(userName, email, passwordHash)
            assertEquals(userName, user.userName)
            assertEquals(email, user.email)
            assertEquals(passwordHash, user.passwordHash)
            // todo fix time comparisons
            assertEquals(user.updatedAt, user.createdAt)

            val fetchedUser = it.getUserById(user.id)
            assertUsersEqual(user, fetchedUser)

            val anotherFetchedUser = it.getUserById(fetchedUser!!.id + 1)
            assertNull(anotherFetchedUser)
        }
    }

    private fun assertUsersEqual(
        expected: User?,
        actual: User?,
    ) = run {
        assertEquals(expected?.id, actual?.id)
        assertEquals(expected?.userName, actual?.userName)
    }
}

class InMemoryUserDao : UserDao {
    private val db = mutableListOf<User>()
    private val recordNumber = AtomicLong(0)

    override fun createUser(
        userName: String,
        email: Email,
        passwordHash: String,
    ): User {
        val creationTime = Instant.now()
        val user =
            User(
                id = recordNumber.incrementAndGet(),
                email = email,
                passwordHash = passwordHash,
                userName = userName,
                createdAt = creationTime,
                updatedAt = creationTime,
            )
        db.add(user)
        return user
    }

    override fun getUserById(userId: Long): User? {
        return db.find { it.id == userId }
    }

    override fun getUserByEmail(email: Email): User? {
        return db.find { it.email == email }
    }
}

class InMemoryUserDaoTest : UserDaoTest() {
    override fun withDao(block: (UserDao) -> Unit) = block(InMemoryUserDao())
}

class SqlUserDaoTest : UserDaoTest() {
    private val dbTestHelpers = DbTestHelpers()

    override fun withDao(block: (UserDao) -> Unit) {
        dbTestHelpers.txnContext.transaction { t ->
            block(SqlUserDao(t.dsl()))
        }
    }

    @BeforeEach
    fun cleanDb() {
        dbTestHelpers.resetDb()
    }
}
