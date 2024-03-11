package utopia.logos.database.access.single.url.request_path

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.url.RequestPathFactory
import utopia.logos.model.stored.url.RequestPath

object UniqueRequestPathAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueRequestPathAccess = new _UniqueRequestPathAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueRequestPathAccess(condition: Condition) extends UniqueRequestPathAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct request paths.
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait UniqueRequestPathAccess 
	extends UniqueRequestPathAccessLike[RequestPath] with SingleRowModelAccess[RequestPath] 
		with FilterableView[UniqueRequestPathAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = RequestPathFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueRequestPathAccess = 
		new UniqueRequestPathAccess._UniqueRequestPathAccess(mergeCondition(filterCondition))
}

