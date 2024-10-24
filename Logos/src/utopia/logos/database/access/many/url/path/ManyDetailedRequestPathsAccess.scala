package utopia.logos.database.access.many.url.path

import utopia.logos.database.factory.url.DetailedRequestPathDbFactory
import utopia.logos.database.storable.url.DomainDbModel
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyDetailedRequestPathsAccess extends ViewFactory[ManyDetailedRequestPathsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyDetailedRequestPathsAccess = 
		_ManyDetailedRequestPathsAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _ManyDetailedRequestPathsAccess(override val accessCondition: Option[Condition]) 
		extends ManyDetailedRequestPathsAccess
}

/**
  * A common trait for access points that return multiple detailed request paths at a time
  * @author Mikko Hilpinen
  * @since 27.08.2024
  */
trait ManyDetailedRequestPathsAccess 
	extends ManyRequestPathsAccessLike[DetailedRequestPath, ManyDetailedRequestPathsAccess] 
		with ManyRowModelAccess[DetailedRequestPath]
{
	// COMPUTED	--------------------
	
	/**
	  * urls of the accessible domains
	  */
	def domainUrls(implicit connection: Connection) = pullColumn(domainModel.url.column).flatMap { _.string }
	
	/**
	  * creation times of the accessible domains
	  */
	def domainCreationTimes(implicit connection: Connection) = 
		pullColumn(domainModel.created.column).map { v => v.getInstant }
	
	/**
	  * Model (factory) used for interacting the domains associated with this detailed request path
	  */
	protected def domainModel = DomainDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DetailedRequestPathDbFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyDetailedRequestPathsAccess = 
		ManyDetailedRequestPathsAccess(condition)
}

