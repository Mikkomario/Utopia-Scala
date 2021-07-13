package utopia.vault.test

import utopia.flow.generic.DataType
import utopia.vault.database.Connection
import utopia.vault.sql.Delete
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable.Model
import utopia.vault.sql.Insert
import utopia.vault.sql.Condition
import utopia.vault.sql.SelectAll
import utopia.vault.sql.Where
import utopia.flow.generic.ValueConversions._

import utopia.vault.sql.SqlExtensions._

/**
 * This test makes sure basic column-generated conditions are working properly. RawStatement-, 
 * Insert- and SimpleStatement tests should succeed before attempting this one
 * @author Mikko Hilpinen
 * @since 23.5.2017
 */
object ColumnConditionTest extends App
{
    DataType.setup()
    
    val table = TestTables.person
    
    Connection.doTransaction
    {
        implicit connection => 
        
            // Empties the table, like usually
            Delete(table).execute()
            
            // Inserts various rows
            val test1 = Model(Vector("name" -> "test 1", "age" -> 31, "isAdmin" -> true))
            val test2 = Model(Vector("name" -> "test 2", "age" -> 32))
            val test3 = Model(Vector("name" -> "test 3", "age" -> 3))
            val test4 = Model(Vector("name" -> "test 4"))
            
            Insert(table, test1, test2, test3, test4)
            
            def countRows(condition: Condition) = connection(SelectAll(table) + Where(condition)).rows.size
            
            val isAdminColumn = table("isAdmin")
            assert(countRows(isAdminColumn <=> true) == 1)
            
            val ageColumn = table("age")
            assert(countRows(ageColumn > 31) == 1)
            assert(countRows(ageColumn >= 31) == 2)
            assert(countRows(ageColumn < 31) == 1)
            assert(countRows(ageColumn <= 31) == 2)
            assert(countRows(ageColumn <=> 31) == 1)
            assert(countRows(ageColumn <> 31) == 2)
            
            assert(countRows(ageColumn.isNull) == 1)
            assert(countRows(ageColumn <=> Value.empty) == 1)
            assert(countRows(ageColumn.isNotNull) == 3)
            assert(countRows(ageColumn <> Value.empty) == 3)
            
            assert(countRows(isAdminColumn <=> true || (ageColumn < 5)) == 2)
            assert(countRows(ageColumn > 5 && (ageColumn < 32)) == 1)
            assert(countRows(!ageColumn.isNull) == 3)
            
            assert(countRows(ageColumn.in(Vector(31, 32))) == 2)
            assert(countRows(ageColumn.isBetween(1, 31)) == 2)
            
            println("Success!")
    }
}