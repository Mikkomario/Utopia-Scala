package utopia.logos.database.access.single.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.url.DomainDbFactory
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.partial.url.DomainData
import utopia.logos.model.stored.url.Domain
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual domains
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbDomain extends SingleRowModelAccess[Domain] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	private def model = DomainDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DomainDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted domain
	  * @return An access point to that domain
	  */
	def apply(id: Int) = DbSingleDomain(id)
	
	/**
	 * @param domainUrl A domain's url. e.g. https://www.example.com
	 * @return Access to a domain entry that exactly matches the specified url
	 */
	def matching(domainUrl: String) = new DbMatchingDomain(domainUrl)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique domains.
	  * @return An access point to the domain that satisfies the specified condition
	  */
	private def distinct(condition: Condition) = UniqueDomainAccess(condition)
	
	
	// NESTED   ---------------------
	
	class DbMatchingDomain(url: String) extends UniqueDomainAccess
	{
		// IMPLEMENTED  -------------------------
		
		override lazy val accessCondition: Option[Condition] = Some(DbDomain.model.url <=> url)
		
		
		// OTHER    -----------------------------
		
		/**
		 * Inserts this domain to the DB, if not already found
		 * @param connection Implicit DB connection
		 * @return If this domain already existed in the DB, returns its ID (right).
		 *         Otherwise returns the inserted domain (left).
		 */
		def idOrInsert()(implicit connection: Connection) = id.toRight {
			this.model.insert(DomainData(url))
		}
	}
}

