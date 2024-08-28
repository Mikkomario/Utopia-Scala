package utopia.logos.database.access.single.url.path

import utopia.logos.database.factory.url.DetailedRequestPathDbFactory
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object UniqueDetailedRequestPathAccess extends ViewFactory[UniqueDetailedRequestPathAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): UniqueDetailedRequestPathAccess = 
		_UniqueDetailedRequestPathAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _UniqueDetailedRequestPathAccess(override val accessCondition: Option[Condition]) 
		extends UniqueDetailedRequestPathAccess
}

/**
  * A common trait for access points that return distinct detailed request paths
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueDetailedRequestPathAccess 
	extends UniqueRequestPathAccessLike[DetailedRequestPath, UniqueDetailedRequestPathAccess] 
		with SingleRowModelAccess[DetailedRequestPath]
{
	// COMPUTED	--------------------
	
	/**
	  * Full http(s) address of this domain in string format. Includes protocol, 
	  * domain name and possible port number. 
	  * None if no domain (or value) was found.
	  */
	def domainUrl(implicit connection: Connection) = pullColumn(domainModel.url.column).getString
	
	/**
	  * Time when this domain was added to the database. 
	  * None if no domain (or value) was found.
	  */
	def domainCreated(implicit connection: Connection) = pullColumn(domainModel.created.column).instant
	
	/**
	  * A database model (factory) used for interacting with the linked domain
	  */
	protected def domainModel = DomainDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DetailedRequestPathDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): UniqueDetailedRequestPathAccess = 
		UniqueDetailedRequestPathAccess(condition)
}

