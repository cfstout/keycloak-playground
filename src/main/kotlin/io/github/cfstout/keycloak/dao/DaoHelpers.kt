package io.github.cfstout.keycloak.dao

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext

object DaoHelpers {
    suspend fun <T, D> DSLContext.txnWithDao(
        daoMaker: (DSLContext) -> D,
        block: (D) -> T,
    ): T {
        return withContext(Dispatchers.IO) {
            transactionResult { c ->
                val dao = daoMaker(c.dsl())
                block(dao)
            }
        }
    }
}
