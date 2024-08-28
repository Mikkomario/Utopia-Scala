package utopia.logos.database.access.single.url.link

import utopia.logos.database.factory.url.LinkDbFactory
import utopia.logos.model.stored.url.Link
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

object UniqueLinkAccess extends ViewFactory[UniqueLinkAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueLinkAccess = _UniqueLinkAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueLinkAccess(override val accessCondition: Option[Condition])
		 extends UniqueLinkAccess
}

/**
  * A common trait for access points that return individual and distinct links.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait UniqueLinkAccess 
	extends UniqueLinkAccessLike[Link, UniqueLinkAccess] with SingleRowModelAccess[Link]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LinkDbFactory
	override protected def self = this
	
	override def apply(condition: Condition): UniqueLinkAccess = UniqueLinkAccess(condition)
}

