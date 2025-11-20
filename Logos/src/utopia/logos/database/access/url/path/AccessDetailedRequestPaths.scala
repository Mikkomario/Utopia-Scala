package utopia.logos.database.access.url.path

import utopia.logos.database.access.url.domain.{AccessDomainValues, FilterDomains}
import utopia.logos.database.factory.url.DetailedRequestPathDbFactory
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.Condition

import scala.language.implicitConversions

object AccessDetailedRequestPaths extends AccessManyRoot[AccessDetailedRequestPaths[DetailedRequestPath]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(DetailedRequestPathDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessDetailedRequestPaths[_]): AccessRequestPathValues = access.values
}

/**
  * Used for accessing multiple detailed request paths from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessDetailedRequestPaths[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessDetailedRequestPaths[A], AccessDetailedRequestPath[A]] 
		with FilterRequestPaths[AccessDetailedRequestPaths[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible detailed request paths
	  */
	lazy val values = AccessRequestPathValues(wrapped)
	
	/**
	  * Access to domain -specific values
	  */
	lazy val domains = AccessDomainValues(wrapped)
	
	
	// COMPUTED	--------------------
	
	/**
	  * Access to domain -based filtering functions
	  */
	def whereDomains = FilterByDomain
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessDetailedRequestPaths(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = 
		AccessDetailedRequestPath(target)
	
	
	// NESTED	--------------------
	
	/**
	  * An interface for domain -based filtering
	  * @since 01.06.2025
	  */
	object FilterByDomain extends FilterDomains[AccessDetailedRequestPaths[A]]
	{
		// IMPLEMENTED	--------------------
		
		override def self: AccessDetailedRequestPaths[A] = AccessDetailedRequestPaths.this
		override def accessCondition = AccessDetailedRequestPaths.this.accessCondition
		override def table = AccessDetailedRequestPaths.this.table
		override def target = AccessDetailedRequestPaths.this.target
		
		override def apply(condition: Condition) = AccessDetailedRequestPaths.this(condition)
	}
}

