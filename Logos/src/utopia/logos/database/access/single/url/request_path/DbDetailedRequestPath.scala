package utopia.logos.database.access.single.url.request_path

import utopia.logos.database.factory.url.DetailedRequestPathDbFactory
import utopia.logos.database.storable.url.{DomainModel, RequestPathModel}
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual detailed request paths
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbDetailedRequestPath 
	extends SingleRowModelAccess[DetailedRequestPath] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * A database model (factory) used for interacting with linked request paths
	  */
	protected def model = RequestPathModel
	
	/**
	  * A database model (factory) used for interacting with the linked domain
	  */
	protected def domainModel = DomainModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DetailedRequestPathDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted detailed request path
	  * @return An access point to that detailed request path
	  */
	def apply(id: Int) = DbSingleDetailedRequestPath(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique detailed request paths.
	  * @return An access point to the detailed request path that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = 
		UniqueDetailedRequestPathAccess(mergeCondition(condition))
}

