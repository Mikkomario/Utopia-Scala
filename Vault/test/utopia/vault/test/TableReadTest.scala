package utopia.vault.test

import utopia.flow.generic.DataType
import utopia.vault.database.Connection
import utopia.vault.database.DatabaseTableReader

/**
 * This test makes sure table reading is working as intended - and that the database structure 
 * matches that of the test tables. A database connection is required for this one. Raw statement 
 * test should succeed before attempting this one.
 * @author Mikko Hilpinen
 * @since 9.6.2017
 */
object TableReadTest extends App
{
    DataType.setup()
    
    Connection.doTransaction(implicit connection => 
    {    
        connection.dbName = "vault_test"
        val testResults = connection.executeQuery("DESCRIBE person ")
        testResults.foreach { _.foreach { case (key, value) => println(s"$key: $value") } }
        
        assert(DatabaseTableReader("vault_test", "person") == TestTables.person)
        assert(DatabaseTableReader("vault_test", "strength") == TestTables.strength)
        
        println("Success!")
    } )
}