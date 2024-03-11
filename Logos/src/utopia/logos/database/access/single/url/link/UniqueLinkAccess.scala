package utopia.logos.database.access.single.url.link

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.url.LinkFactory
import utopia.logos.model.stored.url.Link

object UniqueLinkAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueLinkAccess = new _UniqueLinkAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueLinkAccess(condition: Condition) extends UniqueLinkAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct links.
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait UniqueLinkAccess 
	extends UniqueLinkAccessLike[Link] with SingleRowModelAccess[Link] with FilterableView[UniqueLinkAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LinkFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueLinkAccess = 
		new UniqueLinkAccess._UniqueLinkAccess(mergeCondition(filterCondition))
}

