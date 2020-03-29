package utopia.vault.test

import utopia.flow.generic.DataType
import utopia.vault.database.Connection
import utopia.vault.sql.Insert
import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._

/**
 * This is a test for the use of the insert statement. RawStatementTest should succeed before 
 * this can give valid results.
 * @author Mikko Hilpinen
 * @since 22.5.2017
 */
object InsertTest extends App
{
    DataType.setup()
    
    val table = TestTables.person
    
    // Uses a single connection throughout the test
    val connection = new Connection(Some(table.databaseName))
    try
    {
        // Removes any existing data
        connection.execute(s"DELETE FROM ${table.name}")
        
        // Inserts some simple cases
        val arttu = Model(Vector("name" -> "Arttu", "age" -> 18))
        val belinda = Model(Vector("name" -> "Belinda", "age" -> 22))
        
        assert(Insert(table, Vector(arttu, belinda))(connection).generatedKeys.size == 2)
        
        // Inserts cases where all values are not provided
        val cecilia = Model(Vector("name" -> "Cecilia", "age" -> 25))
        val daavid = Model(Vector("name" -> "Daavid"))
        
        assert(Insert(table, Vector(cecilia, daavid))(connection).generatedKeys.size == 2)
        
        // Inserts a row that would include a row id
        val elias = Model(Vector("name" -> "Elias", "rowId" -> 22))
        assert(Insert(table, elias)(connection).generatedKeys.size == 1)
        
        println("Success!")
    }
    finally
    {
        connection.close()
    }
}