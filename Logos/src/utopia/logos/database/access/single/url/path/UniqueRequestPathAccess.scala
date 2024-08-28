package utopia.logos.database.access.single.url.path

import utopia.logos.database.factory.url.RequestPathDbFactory
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object UniqueRequestPathAccess extends ViewFactory[UniqueRequestPathAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override
		 def apply(condition: Condition): UniqueRequestPathAccess = _UniqueRequestPathAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueRequestPathAccess(override val accessCondition: Option[Condition]) 
		extends UniqueRequestPathAccess
}

/**
  * A common trait for access points that return individual and distinct request paths.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueRequestPathAccess 
	extends UniqueRequestPathAccessLike[RequestPath, UniqueRequestPathAccess] 
		with SingleRowModelAccess[RequestPath]
{
	// IMPLEMENTED	--------------------
	
	override def factory = RequestPathDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueRequestPathAccess = UniqueRequestPathAccess(condition)
}

