package utopia.logos.database.access.single.text.delimiter

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.text.DelimiterDbFactory
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

object UniqueDelimiterAccess extends ViewFactory[UniqueDelimiterAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueDelimiterAccess = _UniqueDelimiterAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueDelimiterAccess(override val accessCondition: Option[Condition]) 
		extends UniqueDelimiterAccess
}

/**
  * A common trait for access points that return individual and distinct delimiters.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueDelimiterAccess 
	extends SingleRowModelAccess[Delimiter] with DistinctModelAccess[Delimiter, Option[Delimiter], Value] 
		with FilterableView[UniqueDelimiterAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * The characters that form this delimiter. 
	  * None if no delimiter (or value) was found.
	  */
	def text(implicit connection: Connection) = pullColumn(model.text.column).getString
	
	/**
	  * Time when this delimiter was added to the database. 
	  * None if no delimiter (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	
	/**
	  * Unique id of the accessible delimiter. None if no delimiter was accessible.
	  */
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = DelimiterDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueDelimiterAccess = UniqueDelimiterAccess(condition)
}

