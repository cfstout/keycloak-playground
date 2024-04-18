package io.github.cfstout.keycloak.endpoints

import io.github.cfstout.keycloak.dao.DaoFactory
import io.github.cfstout.keycloak.dao.DaoHelpers.txnWithDao
import io.github.cfstout.keycloak.endpoints.models.CreateUserRequest
import io.github.cfstout.keycloak.models.Email
import io.github.cfstout.keycloak.models.users.User
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.features.NotFoundException
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.jooq.DSLContext

class UserEndpoints(app: Application, jooq: DSLContext, private val daoFactory: DaoFactory) {
    init {
        app.routing {
            route("/user") {
                post("/create") {
                    val req: CreateUserRequest = call.receive()
                    val user =
                        jooq.txnWithDao(daoFactory::userDao) { dao ->
                            dao.createUser(
                                req.userName,
                                // safe to !! here as we check null in the init block
                                req.normalizedEmail!!,
                                req.passwordHash,
                            )
                        }
                    call.respond(user)
                }
                get("/id/{id}") {
                    val id =
                        call.parameters["id"]?.toLongOrNull()
                            ?: throw IllegalArgumentException("Invalid id")
                    val user =
                        jooq.txnWithDao(daoFactory::userDao) { dao ->
                            dao.getUserById(id)
                        }
                    call.respond(user ?: throw NotFoundException("User $id not found"))
                }
                get("/email/{email}") {
                    val email: Email =
                        Email.from(
                            call.parameters["email"]
                                ?: throw IllegalArgumentException("No email provided"),
                        ) ?: throw IllegalStateException("Email is invalid")
                    val user: User? =
                        jooq.txnWithDao(daoFactory::userDao) { dao ->
                            dao.getUserByEmail(email)
                        }
                    call.respond(user ?: throw NotFoundException("User $email not found"))
                }
            }
        }
    }
}
