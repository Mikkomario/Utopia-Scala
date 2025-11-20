package utopia.logos.database.access.url.domain

import utopia.logos.database.factory.url.DomainDbFactory
import utopia.logos.model.stored.url.Domain
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessDomains extends AccessManyRoot[AccessDomains[Domain]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(DomainDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessDomains[_]): AccessDomainValues = access.values
}

/**
  * Used for accessing multiple domains from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessDomains[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessDomains[A], AccessDomain[A]] with FilterDomains[AccessDomains[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible domains
	  */
	lazy val values = AccessDomainValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessDomains(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessDomain(target)
}

