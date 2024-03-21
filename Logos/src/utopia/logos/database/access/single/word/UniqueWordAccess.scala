package utopia.logos.database.access.single.word

import utopia.logos.database.factory.word.WordDbFactory
import utopia.logos.model.stored.word.Word
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object UniqueWordAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueWordAccess = new _UniqueWordAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueWordAccess(condition: Condition) extends UniqueWordAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct words.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
trait UniqueWordAccess 
	extends UniqueWordAccessLike[Word] with SingleRowModelAccess[Word] with FilterableView[UniqueWordAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = WordDbFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueWordAccess = 
		new UniqueWordAccess._UniqueWordAccess(mergeCondition(filterCondition))
}

