package utopia.vault.test

import utopia.flow.generic.IntType
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.DataType
import utopia.vault.database.Connection

import utopia.flow.generic.VectorType

import scala.collection.immutable.HashSet
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
 * This test runs some raw statements using the sql client and checks the results
 * @since 19.5.2017
 */
object RawStatementTest extends App
{
    // Sets up the data types
    DataType.setup()
    
    // Creates the test table first
    val table = TestTables.person
    
    // Uses a single connection throughout the test
    val connection = new Connection(Some(table.databaseName))
    try
    {
        // Makes sure the table is empty
        connection.execute(s"DELETE FROM ${table.name}")
        
        def insert(name: String, age: Int, isAdmin: Boolean = false) = 
        {
            assert(connection(s"INSERT INTO ${table.name} (name, age, is_admin) VALUES (?, ?, ?)",
                    Vector(name, age, isAdmin), HashSet(), returnGeneratedKeys = true).generatedKeys.nonEmpty)
        }
        
        // Inserts a couple of elements into the table. Makes sure new indices are generated
        insert("Arttu", 15, isAdmin = true)
        insert("Belinda", 22)
        insert("Cecilia", 23)
        
        // Reads person data from the database
        val results = connection(s"SELECT * FROM ${table.name}", Vector(), HashSet(table)).rowModels
        results.foreach { row => println(row.toJson) }
        
        assert(results.size == 3)
        
        // Tries to insert null values
        connection(s"INSERT INTO ${table.name} (name, age) VALUES (?, ?)", Vector("Test", Value.emptyWithType(IntType)))
        
        // Also tries inserting a time value
        val creationTime = Now.toValue
        val latestIndex = connection(s"INSERT INTO ${table.name} (name, created) VALUES (?, ?)", 
                Vector("Test2", creationTime), HashSet(), returnGeneratedKeys = true).generatedKeys.head
        
        // Checks that the time value was preserved
        val lastResult = connection(s"SELECT created FROM ${table.name} WHERE row_id = ?", 
                Vector(latestIndex), HashSet(table), returnGeneratedKeys = false).rows.head.toModel
        
        println(lastResult.toJson)
        println(s"Previously ${creationTime.longOr()} (${creationTime.dataType}), now ${lastResult("created").longOr()} (${lastResult("created").dataType})")
        assert(lastResult("created").getLong / 1000 == creationTime.getLong / 1000)
        
        // Tests a bit more tricky version where data types may not be correct
        connection(s"INSERT INTO ${table.name} (name) VALUES (?)", Vector(32))
        connection(s"INSERT INTO ${table.name} (name, created) VALUES (?, ?)", Vector("Null Test", Value.emptyWithType(VectorType)))
        
        println("Success!")
    }
    finally
    {
        connection.close()
    }
}