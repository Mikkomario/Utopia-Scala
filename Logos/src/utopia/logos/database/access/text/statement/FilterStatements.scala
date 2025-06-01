package utopia.logos.database.access.text.statement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.StatementDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on statement properties
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait FilterStatements[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines statement database properties
	  */
	def statementModel = StatementDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param delimiterId delimiter id to target
	  * @return Copy of this access point that only includes statements with the specified delimiter id
	  */
	def endingWith(delimiterId: Int) = filter(statementModel.delimiterId.column <=> delimiterId)
	
	/**
	  * @param delimiterIds Targeted delimiter ids
	  * @return Copy of this access point that only includes statements where delimiter id is within the 
	  * specified value set
	  */
	def endingWithDelimiters(delimiterIds: IterableOnce[Int]) = 
		filter(statementModel.delimiterId.column.in(IntSet.from(delimiterIds)))
}

