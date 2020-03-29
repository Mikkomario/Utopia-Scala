package utopia.vault.test

import utopia.flow.generic.DataType
import utopia.vault.database.Connection
import utopia.vault.sql.Delete
import utopia.flow.datastructure.immutable.Model
import utopia.vault.sql.Insert

import utopia.vault.sql.JoinType._
import utopia.vault.sql.Select
import utopia.vault.sql.SelectAll
import utopia.vault.sql.Where
import utopia.flow.generic.ValueConversions._

/**
 * This test tests the use of joined sql targets and the use of references too. It is expected 
 * that SimpleStatementTest and ColumnConditionTest run successfully before attempting this test.
 * @author Mikko Hilpinen
 * @since 3.6.2017
 */
object JoinTest extends App
{
    DataType.setup()
    
    val person = TestTables.person
    val strength = TestTables.strength
    
    // Uses a single connection throughout the tests
    implicit val connection: Connection = new Connection()
    try
    {
        connection(Delete(person))
        
        // Inserts test persons
        val arttu = Model(Vector("name" -> "Arttu", "age" -> 16))
        val bertta = Model(Vector("name" -> "Bertta", "age" -> 8))
        val camilla = Model(Vector("name" -> "Camilla", "age" -> 31))
        
        Insert(person, arttu)
        val berttaId = Insert(person, bertta).generatedKeys.head
        val camillaId = Insert(person, camilla).generatedKeys.head
        
        // Adds test powers
        val berttaPower = Model(Vector("ownerId" -> berttaId, "name" -> "imagination", "powerLevel" -> 9999))
        val camillaPower1 = Model(Vector("ownerId" -> camillaId, "name" -> "is teacher"))
        val camillaPower2 = Model(Vector("ownerId" -> camillaId, "name" -> "discipline", "powerLevel" -> 172))
        val camillaPower3 = Model(Vector("ownerId" -> camillaId, "name" -> "imagination", 
                "powerLevel" -> 250))
        
        Insert(strength, berttaPower, camillaPower1, camillaPower2, camillaPower3)
        
        // Counts the number of rows on each join type
        def countRows(joinType: JoinType) = connection(Select.nothing(person.join(strength, joinType))).rows.size
        
        assert(countRows(Left) == 5)
        assert(countRows(Right) == 4)
        assert(countRows(Inner) == 4)
        
        // Tries retrieving row data with a conditional select
        val result1 = connection(SelectAll(person join strength) + Where(strength("name") <=> "discipline"))
        
        assert(result1.rows.size == 1)
        assert(result1.rows.head(person)("rowId") == camillaId)
        assert(result1.rows.head(strength)("name") == "discipline".toValue)
        
        def powersForPerson(personName: String) = connection(
                Select(person join strength, strength.columns) + Where(person("name") <=> personName))
        
        assert(powersForPerson("Arttu").isEmpty)
        assert(powersForPerson("Camilla").rows.size == 3)
        assert(powersForPerson("Bertta").rows.head("name") == "imagination".toValue)
        
        println("Success!")
    }
    finally
    {
        connection.close()
    }
}