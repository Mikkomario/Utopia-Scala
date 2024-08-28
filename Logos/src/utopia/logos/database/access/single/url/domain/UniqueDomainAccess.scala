package utopia.logos.database.access.single.url.domain

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.url.DomainDbFactory
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.stored.url.Domain
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object UniqueDomainAccess extends ViewFactory[UniqueDomainAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueDomainAccess = _UniqueDomainAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueDomainAccess(override val accessCondition: Option[Condition]) 
		extends UniqueDomainAccess
}

/**
  * A common trait for access points that return individual and distinct domains.
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
trait UniqueDomainAccess 
	extends SingleRowModelAccess[Domain] with DistinctModelAccess[Domain, Option[Domain], Value] 
		with FilterableView[UniqueDomainAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Full http(s) address of this domain in string format. Includes protocol, 
	  * domain name and possible port number. 
	  * None if no domain (or value) was found.
	  */
	def url(implicit connection: Connection) = pullColumn(model.url.column).getString
	/**
	  * Time when this domain was added to the database. 
	  * None if no domain (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	/**
	  * Unique id of the accessible domain. None if no domain was accessible.
	  */
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = DomainDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DomainDbFactory
	override protected def self = this
	
	override def apply(condition: Condition): UniqueDomainAccess = UniqueDomainAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted domains
	  * @param newCreated A new created to assign
	  * @return Whether any domain was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	/**
	  * Updates the urls of the targeted domains
	  * @param newUrl A new url to assign
	  * @return Whether any domain was affected
	  */
	def url_=(newUrl: String)(implicit connection: Connection) = putColumn(model.url.column, newUrl)
}

