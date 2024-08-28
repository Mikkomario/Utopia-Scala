package utopia.logos.database.access.many.text.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.text.DelimiterDbFactory
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

object ManyDelimitersAccess extends ViewFactory[ManyDelimitersAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyDelimitersAccess = _ManyDelimitersAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyDelimitersAccess(override val accessCondition: Option[Condition]) 
		extends ManyDelimitersAccess
}

/**
  * A common trait for access points which target multiple delimiters at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
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
	/**
	  * Unique ids of the accessible delimiters
	  */
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * All accessible delimiters as a delimiter-id map
	  * @param connection Implicit DB Connection
	  */
	def toMap(implicit connection: Connection) =
		pullColumnMap(model.text.column, index).map { case (text, id) => text.getString -> id.getInt }
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = DelimiterDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterDbFactory
	override protected def self = this
	
	override def apply(condition: Condition): ManyDelimitersAccess = ManyDelimitersAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param text text to target
	  * @return Copy of this access point that only includes delimiters with the specified text
	  */
	def matching(text: String) = filter(model.text.column <=> text)
	/**
	  * @param text Targeted text
	  * @return Copy of this access point that only includes delimiters where text is within the specified value set
	  */
	def matchingDelimiters(text: Iterable[String]) = filter(model.text.column.in(text))
}

