package utopia.logos.database.access.many.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.url.DomainFactory
import utopia.logos.database.model.url.DomainModel
import utopia.logos.model.stored.url.Domain

import java.time.Instant

object ManyDomainsAccess
{
	// NESTED	--------------------
	
	private class ManyDomainsSubView(condition: Condition) extends ManyDomainsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple domains at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
trait ManyDomainsAccess extends ManyRowModelAccess[Domain] with FilterableView[ManyDomainsAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * urls of the accessible domains
	  */
	def urls(implicit connection: Connection) = pullColumn(model.urlColumn).flatMap { _.string }
	
	/**
	  * creation times of the accessible domains
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn)
		.map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DomainModel
	
	/**
	 * @param connection Implicit DB connection
	 * @return A map containing all accessible domains as url-id pairs.
	 *         All urls are in lower case.
	 */
	def toMap(implicit connection: Connection) = pullColumnMap(model.urlColumn, index)
		.map { case (urlVal, idVal) => urlVal.getString.toLowerCase -> idVal.getInt }
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DomainFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyDomainsAccess = 
		new ManyDomainsAccess.ManyDomainsSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	 * @param domainUrls Targeted domain URLs
	 * @return Access to domains using those specific urls
	 */
	def matching(domainUrls: Iterable[String]) = filter(model.urlColumn.in(domainUrls))
	
	/**
	  * Updates the creation times of the targeted domains
	  * @param newCreated A new created to assign
	  * @return Whether any domain was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the urls of the targeted domains
	  * @param newUrl A new url to assign
	  * @return Whether any domain was affected
	  */
	def urls_=(newUrl: String)(implicit connection: Connection) = putColumn(model.urlColumn, newUrl)
}

