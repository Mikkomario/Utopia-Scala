package utopia.logos.database.access.single.url.link

import utopia.logos.database.factory.url.LinkDbFactory
import utopia.logos.database.storable.url.LinkModel
import utopia.logos.model.stored.url.Link
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual links
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DbLink extends SingleRowModelAccess[Link] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LinkDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted link
	  * @return An access point to that link
	  */
	def apply(id: Int) = DbSingleLink(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique links.
	  * @return An access point to the link that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueLinkAccess(mergeCondition(condition))
}

