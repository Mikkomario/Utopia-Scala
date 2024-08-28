package utopia.logos.database.access.single.url.path

import utopia.logos.database.factory.url.RequestPathDbFactory
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual request paths
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbRequestPath extends SingleRowModelAccess[RequestPath] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	private def model = RequestPathDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = RequestPathDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted request path
	  * @return An access point to that request path
	  */
	def apply(id: Int) = DbSingleRequestPath(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique request paths.
	  * @return An access point to the request path that satisfies the specified condition
	  */
	private def distinct(condition: Condition) = UniqueRequestPathAccess(condition)
}

