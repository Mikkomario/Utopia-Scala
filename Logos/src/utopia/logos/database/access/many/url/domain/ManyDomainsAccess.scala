package utopia.logos.database.access.many.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.factory.url.DomainDbFactory
import utopia.logos.database.storable.url.DomainModel
import utopia.logos.model.stored.url.Domain
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyDomainsAccess extends ViewFactory[ManyDomainsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyDomainsAccess = new _ManyDomainsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyDomainsAccess(condition: Condition) extends ManyDomainsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple domains at a time
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait ManyDomainsAccess extends ManyRowModelAccess[Domain] with FilterableView[ManyDomainsAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * urls of the accessible domains
	  */
	def urls(implicit connection: Connection) = pullColumn(model.url.column).flatMap { _.string }
	
	/**
	  * creation times of the accessible domains
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created.column).map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * A map containing all accessible domains as url-id pairs.
	  * All urls are in lower case.
	  * @param connection Implicit DB connection
	  */
	def toMap(implicit connection: Connection) = {
		pullColumnMap(model.url.column, index).map 
		{
			 case (urlVal, 
					idVal) => urlVal.getString.toLowerCase -> idVal.getInt 
		}
	}
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DomainModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DomainDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyDomainsAccess = ManyDomainsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted domains
	  * @param newCreated A new created to assign
	  * @return Whether any domain was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * @param domainUrls Targeted domain URLs
	  * @return Access to domains using those specific urls
	  */
	def matching(domainUrls: Iterable[String]) = filter(model.url.column.in(domainUrls))
	
	/**
	  * Updates the urls of the targeted domains
	  * @param newUrl A new url to assign
	  * @return Whether any domain was affected
	  */
	def urls_=(newUrl: String)(implicit connection: Connection) = putColumn(model.url.column, newUrl)
}

