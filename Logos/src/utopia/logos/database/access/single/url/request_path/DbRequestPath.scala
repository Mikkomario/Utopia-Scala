package utopia.logos.database.access.single.url.request_path

import utopia.logos.database.factory.url.RequestPathDbFactory
import utopia.logos.database.storable.url.RequestPathModel
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual request paths
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object DbRequestPath extends SingleRowModelAccess[RequestPath] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = RequestPathModel
	
	
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
	protected def filterDistinct(condition: Condition) = UniqueRequestPathAccess(mergeCondition(condition))
}

