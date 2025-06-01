package utopia.logos.database.access.url.path

import utopia.logos.database.access.url.domain.{AccessDomainValue, FilterDomains}
import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vault.sql.Condition

import scala.language.implicitConversions

object AccessDetailedRequestPath extends AccessOneRoot[AccessDetailedRequestPath[DetailedRequestPath]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessDetailedRequestPaths.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessDetailedRequestPath[_]): AccessRequestPathValue = access.values
}

/**
  * Used for accessing individual detailed request paths from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessDetailedRequestPath[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessDetailedRequestPath[A]] 
		with FilterRequestPaths[AccessDetailedRequestPath[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible detailed request path
	  */
	lazy val values = AccessRequestPathValue(wrapped)
	
	/**
	  * Access to domain -specific values
	  */
	lazy val domain = AccessDomainValue(wrapped)
	
	
	// COMPUTED	--------------------
	
	/**
	  * Access to domain -based filtering functions
	  */
	def whereDomain = FilterByDomain
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessDetailedRequestPath(newTarget)
	
	
	// NESTED	--------------------
	
	/**
	  * An interface for domain -based filtering
	  * @since 01.06.2025
	  */
	object FilterByDomain extends FilterDomains[AccessDetailedRequestPath[A]]
	{
		// IMPLEMENTED	--------------------
		
		override protected def self: AccessDetailedRequestPath[A] = AccessDetailedRequestPath.this
		override def accessCondition = AccessDetailedRequestPath.this.accessCondition
		override def table = AccessDetailedRequestPath.this.table
		override def target = AccessDetailedRequestPath.this.target
		
		override def apply(condition: Condition) = AccessDetailedRequestPath.this(condition)
	}
}

