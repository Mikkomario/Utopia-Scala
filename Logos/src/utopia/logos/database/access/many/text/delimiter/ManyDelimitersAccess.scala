package utopia.logos.database.access.many.text.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.text.DelimiterFactory
import utopia.logos.database.model.text.DelimiterModel
import utopia.logos.model.stored.text.Delimiter

import java.time.Instant

object ManyDelimitersAccess
{
	// NESTED	--------------------
	
	private class ManyDelimitersSubView(condition: Condition) extends ManyDelimitersAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple delimiters at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait ManyDelimitersAccess 
	extends ManyRowModelAccess[Delimiter] with FilterableView[ManyDelimitersAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	 * @param connection Implicit DB Connection
	 * @return All accessible delimiters as a delimiter-id map
	 */
	def toMap(implicit connection: Connection) = pullColumnMap(model.textColumn, index)
		.map { case (text, id) => text.getString -> id.getInt }
	
	/**
	  * text of the accessible delimiters
	  */
	def text(implicit connection: Connection) = pullColumn(model.textColumn).flatMap { _.string }
	
	/**
	  * creation times of the accessible delimiters
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn)
		.map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DelimiterModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyDelimitersAccess = 
		new ManyDelimitersAccess.ManyDelimitersSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	 * @param delimiters Targeted delimiters
	 * @return Access to those delimiters in the DB
	 */
	def matching(delimiters: Iterable[String]) = filter(model.textColumn.in(delimiters))
	
	/**
	  * Updates the creation times of the targeted delimiters
	  * @param newCreated A new created to assign
	  * @return Whether any delimiter was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the text of the targeted delimiters
	  * @param newText A new text to assign
	  * @return Whether any delimiter was affected
	  */
	def text_=(newText: String)(implicit connection: Connection) = putColumn(model.textColumn, newText)
}

