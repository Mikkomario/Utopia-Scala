package utopia.logos.database.access.many.word

import utopia.logos.database.factory.word.WordDbFactory
import utopia.logos.model.stored.word.Word
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

@deprecated("Replaced with a new version", "v0.3")
object ManyWordsAccess extends ViewFactory[ManyWordsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyWordsAccess = new _ManyWordsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyWordsAccess(condition: Condition) extends ManyWordsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple words at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
trait ManyWordsAccess extends ManyWordsAccessLike[Word, ManyWordsAccess] with ManyRowModelAccess[Word]
{
	// COMPUTED	--------------------
	
	/**
	  * All accessible word ids mapped to their string values
	  * @param connection Implicit DB Connection
	  */
	def toMap(implicit connection: Connection) = 
		pullColumnMap(model.text.column, index).map { case (text, id) => text.getString -> id.getInt }
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = WordDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyWordsAccess = ManyWordsAccess(condition)
}

