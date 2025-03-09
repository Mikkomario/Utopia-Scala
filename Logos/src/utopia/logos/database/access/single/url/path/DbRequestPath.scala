package utopia.logos.database.access.single.url.path

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.single.url.domain.DbDomain
import utopia.logos.database.factory.url.RequestPathDbFactory
import utopia.logos.database.storable.url.RequestPathDbModel
import utopia.logos.model.cached.Link
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.database.Connection
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
	 * @param domainId Id of the targeted domain
	 * @param path Targeted path as a string
	 * @return Access to that path in the DB
	 */
	def matching(domainId: Int, path: String) = new DbDistinctRequestPath(domainId, path)
	
	/**
	 * Makes sure the specified link's request path & domain exist in the DB
	 * @param link Link whose request path is targeted
	 * @param connection Implicit DB connection
	 * @return If this path already existed in the DB, returns its ID (right).
	 *         Otherwise returns the newly inserted request path (left).
	 */
	def storeFrom(link: Link)(implicit connection: Connection) = {
		DbDomain.matching(link.domain).idOrInsert() match {
			// Case: Domain already existed in the DB => Looks whether this request path also exists
			case Right(domainId) => matching(domainId, link.path).idOrInsert()
			
			// Case: Domain didn't exist in the DB => Inserts a new path without checking for duplicates
			case Left(domain) => Left(model.insert(RequestPathData(domain.id, link.path)))
		}
	}
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique request paths.
	  * @return An access point to the request path that satisfies the specified condition
	  */
	private def distinct(condition: Condition) = UniqueRequestPathAccess(condition)
	
	
	// NESTED   --------------------------
	
	class DbDistinctRequestPath(domainId: Int, path: String) extends UniqueRequestPathAccess
	{
		// ATTRIBUTES   ------------------------
		
		override lazy val accessCondition: Option[Condition] =
			Some(this.model.domainId <=> domainId && this.model.path <=> path)
			
		
		// OTHER    ----------------------------
		
		/**
		 * Looks for this request path from the DB & inserts a new entry if not found
		 * @param connection Implicit DB connection
		 * @return If this path already existed in the DB, returns its ID (right).
		 *         Otherwise returns the inserted request path.
		 */
		def idOrInsert()(implicit connection: Connection) = id.toRight {
			DbRequestPath.model.insert(RequestPathData(domainId, path))
		}
	}
}

