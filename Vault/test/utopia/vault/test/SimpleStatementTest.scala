package utopia.vault.test

import utopia.flow.generic.DataType
import utopia.vault.database.Connection
import utopia.vault.sql.SelectAll
import utopia.vault.sql.Delete
import utopia.flow.datastructure.immutable.Model
import utopia.vault.sql.Insert
import utopia.vault.sql.Select
import utopia.vault.sql.Limit
import utopia.vault.sql.Update
import utopia.vault.sql.OrderBy
import utopia.flow.generic.ValueConversions._

/**
 * sqlt tests basic uses cases for very simple statements delete and select all.
 * @author Mikko Hilpinen
 * @since 22.5.2017
 */
object SimpleStatementTest extends App
{
    DataType.setup()
    val table = TestTables.person
    
    // Uses a single connection
    implicit val connection: Connection = new Connection()
    try
    {
        def countRows = connection(SelectAll(table)).rows.size
        
        // Empties the database and makes sure it is empty
        connection(Delete(table))
        assert(countRows == 0)
        
        val testModel = Model(Vector("name" -> "SimpleStatementTest"))
        Insert(table, Vector(testModel, testModel, testModel))
        
        assert(countRows == 3)
        assert(connection(Select.nothing(table)).rows.size == 3)
        assert(connection(Select.nothing(table) + Limit(1)).rows.size == 1)
        
        val result = connection(SelectAll(table))
        assert(result.rows.head.toModel("name") == "SimpleStatementTest".toValue)
        assert(connection(Select(table, table.columns)) == result)
        assert(connection(Select(table, "name")).rows.head.toModel("name") == "SimpleStatementTest".toValue)
        
        connection(Update(table, "age", 22))
        assert(connection(Select(table, "age")).rows.head.toModel("age") == 22.toValue)
        
        Insert(table, Model(Vector("name" -> "Last", "age" -> 2, "isAdmin" -> true)))
        assert(connection(SelectAll(table) + OrderBy.ascending(table("age")) + Limit(1)).rows.head.toModel("name") == "Last".toValue)
        
        val result2 = connection(Select(table, "isAdmin")).rowModels
        assert(result2.nonEmpty)
        //result2.foreach(println)
        assert(result2.exists { _("isAdmin").booleanOr() })
        
        connection(Delete(table))
        assert(countRows == 0)
        
        println("Success!")
    }
    finally
    {
        connection.close()
    }
}