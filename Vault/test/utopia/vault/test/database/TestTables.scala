package utopia.vault.test.database

import utopia.vault.database.Tables
import utopia.vault.model.immutable.Table

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
	
	def dateTimeTest = apply(dbName, "datetime_test")
	
	def apply(tableName: String): Table = apply(dbName, tableName)
}
