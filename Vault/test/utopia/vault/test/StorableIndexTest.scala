package utopia.vault.test

import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.DataType
import utopia.vault.database.Connection
import utopia.vault.sql.{Delete, Exists}

/**
  * Tests storables in tables without auto-increment
  * @author Mikko Hilpinen
  * @since 12.7.2019, v1.2.2+
  */
object StorableIndexTest extends App
{
	DataType.setup()
	
	Connection.doTransaction
	{
		implicit connection =>
		
			// Empties target table
			Delete(TestTables.indexTest)
			
			// Tries inserting a new storable
			val newItem = IndexStorable(5, "Test")
			assert(newItem.insert().int.contains(5))
			
			// Makes sure the row was actually inserted
			assert(Exists.index(TestTables.indexTest, 5))
			
			// Tries to update same row
			val itemV2 = IndexStorable(5, "Test2")
			assert(itemV2.push().int.contains(5))
			
			// Checks that update came through
			assert(IndexStorable.get(5).contains(itemV2))
	}
	
	println("Success!")
}
