package utopia.vault.test

import utopia.vault.database.Tables

/**
 * This is a collection of tables used in the tests
 */
object TestTables extends Tables(TestConnectionPool)(TestThreadPool.executionContext)
{
    // ATTRIBUTES   -----------------
    
    private val dbName = "vault_test"
    
    
    // COMPUTED ---------------------
    
    def person = apply(dbName, "person")
    
    def strength = apply(dbName, "strength")
    
    def indexTest = apply(dbName, "index_test")
}