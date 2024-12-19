package utopia.vault.test.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.vault.model.immutable.Column

/**
  * Tests condition-conversion from an IntSet
  * @author Mikko Hilpinen
  * @since 16.12.2024, v1.20.1
  */
object IntSetConditionTest extends App
{
	private val set1 = IntSet(1, 2, 3, 4, 5)
	private val set2 = (set1 ++ Vector(8, 9, 10, 11)).toIntSet
	private val set3 = IntSet(1, 2, 4, 5, 6, 7)
	private val set4 = IntSet(1, 3, 5)
	
	private val testColumn = Column("test", "test", "test_table", IntType)
	
	println(testColumn.in(set1).segment.description)
	println(testColumn.in(set2).segment.description)
	println(testColumn.in(set3).segment.description)
	println(testColumn.in(set4).segment.description)
}
