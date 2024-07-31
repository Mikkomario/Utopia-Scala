package utopia.logos.database.access.many.word.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.word.DelimiterDbFactory
import utopia.logos.database.storable.word.DelimiterModel
import utopia.logos.model.stored.word.Delimiter
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyDelimitersAccess extends ViewFactory[ManyDelimitersAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyDelimitersAccess = new _ManyDelimitersAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyDelimitersAccess(condition: Condition) extends ManyDelimitersAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple delimiters at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait ManyDelimitersAccess 
	extends ManyRowModelAccess[Delimiter] with FilterableView[ManyDelimitersAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * text of the accessible delimiters
	  */
	def text(implicit connection: Connection) = pullColumn(model.text.column).flatMap { _.string }
	
	/**
	  * creation times of the accessible delimiters
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created.column).map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * All accessible delimiters as a delimiter-id map
	  * @param connection Implicit DB Connection
	  */
	def toMap(implicit connection: Connection) = 
		pullColumnMap(model.text.column, index).map { case (text, id) => text.getString -> id.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DelimiterModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyDelimitersAccess = ManyDelimitersAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted delimiters
	  * @param newCreated A new created to assign
	  * @return Whether any delimiter was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * @param delimiters Targeted delimiters
	  * @return Access to those delimiters in the DB
	  */
	def matching(delimiters: Iterable[String]) = filter(model.text.column.in(delimiters))
	
	/**
	  * Updates the text of the targeted delimiters
	  * @param newText A new text to assign
	  * @return Whether any delimiter was affected
	  */
	def text_=(newText: String)(implicit connection: Connection) = putColumn(model.text.column, newText)
}

