package utopia.logos.database.access.many.word

import utopia.logos.database.factory.word.WordDbFactory
import utopia.logos.model.stored.word.Word
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

object ManyWordsAccess
{
	// NESTED	--------------------
	
	private class ManyWordsSubView(condition: Condition) extends ManyWordsAccess
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
	
	override def filter(filterCondition: Condition): ManyWordsAccess = 
		new ManyWordsAccess.ManyWordsSubView(mergeCondition(filterCondition))
}

