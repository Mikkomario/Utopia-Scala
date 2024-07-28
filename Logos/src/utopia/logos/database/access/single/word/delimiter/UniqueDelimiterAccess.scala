package utopia.logos.database.access.single.word.delimiter

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.word.DelimiterDbFactory
import utopia.logos.database.storable.word.DelimiterModel
import utopia.logos.model.stored.word.Delimiter
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

object UniqueDelimiterAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueDelimiterAccess = new _UniqueDelimiterAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueDelimiterAccess(condition: Condition) extends UniqueDelimiterAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct delimiters.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait UniqueDelimiterAccess 
	extends SingleRowModelAccess[Delimiter] with FilterableView[UniqueDelimiterAccess] 
		with DistinctModelAccess[Delimiter, Option[Delimiter], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * The characters that form this delimiter. None if no delimiter (or value) was found.
	  */
	def text(implicit connection: Connection) = pullColumn(model.text.column).getString
	
	/**
	  * Time when this delimiter was added to the database. None if no delimiter (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DelimiterModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterDbFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueDelimiterAccess = 
		new UniqueDelimiterAccess._UniqueDelimiterAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted delimiters
	  * @param newCreated A new created to assign
	  * @return Whether any delimiter was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * Updates the text of the targeted delimiters
	  * @param newText A new text to assign
	  * @return Whether any delimiter was affected
	  */
	def text_=(newText: String)(implicit connection: Connection) = putColumn(model.text.column, newText)
}

