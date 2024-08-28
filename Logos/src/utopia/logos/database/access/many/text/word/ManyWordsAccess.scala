package utopia.logos.database.access.many.text.word

import utopia.logos.database.factory.text.WordDbFactory
import utopia.logos.model.stored.text.Word
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyWordsAccess extends ViewFactory[ManyWordsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyWordsAccess = _ManyWordsAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyWordsAccess(override val accessCondition: Option[Condition])
		 extends ManyWordsAccess
}

/**
  * A common trait for access points which target multiple words at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait ManyWordsAccess extends ManyWordsAccessLike[Word, ManyWordsAccess] with ManyRowModelAccess[Word]
{
	// IMPLEMENTED	--------------------
	
	override def factory = WordDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyWordsAccess = ManyWordsAccess(condition)
}

