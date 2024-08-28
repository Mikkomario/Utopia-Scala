package utopia.logos.database.access.single.text.word

import utopia.logos.database.factory.text.WordDbFactory
import utopia.logos.model.stored.text.Word
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object UniqueWordAccess extends ViewFactory[UniqueWordAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueWordAccess = _UniqueWordAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueWordAccess(override val accessCondition: Option[Condition])
		 extends UniqueWordAccess
}

/**
  * A common trait for access points that return individual and distinct words.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueWordAccess extends UniqueWordAccessLike[Word, UniqueWordAccess] with SingleRowModelAccess[Word]
{
	// IMPLEMENTED	--------------------
	
	override def factory = WordDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueWordAccess = UniqueWordAccess(condition)
}

