package io.github.cfstout.keycloak.dao

import org.jooq.DSLContext
import java.time.Clock

interface DaoFactory {
    fun userDao(txnContext: DSLContext): UserDao
}

class SqlDaoFactory(private val clock: Clock) : DaoFactory {
    override fun userDao(txnContext: DSLContext): UserDao = SqlUserDao(txnContext)
}
