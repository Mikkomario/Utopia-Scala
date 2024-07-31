package utopia.logos.database.access.single.url.link

import utopia.logos.database.factory.url.LinkDbFactory
import utopia.logos.model.stored.url.Link
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

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
  * @since 20.03.2024, v0.2
  */
trait UniqueLinkAccess 
	extends UniqueLinkAccessLike[Link] with SingleRowModelAccess[Link] with FilterableView[UniqueLinkAccess]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LinkDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueLinkAccess = UniqueLinkAccess(condition)
}

