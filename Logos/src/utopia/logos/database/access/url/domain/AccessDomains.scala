package utopia.logos.database.access.url.domain

import utopia.logos.database.reader.url.DomainDbReader
import utopia.logos.model.stored.url.Domain
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessDomains extends AccessManyRoot[AccessDomainRows[Domain]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessDomainRows(AccessManyRows(DomainDbReader))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessDomains[_, _]): AccessDomainValues = access.values
}

/**
  * Used for accessing multiple domains from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessDomains[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessDomain[A]] with FilterDomains[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible domains
	  */
	lazy val values = AccessDomainValues(wrapped)
}

/**
  * Provides access to row-specific domain -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessDomainRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessDomains[A, AccessDomainRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessDomainRows[A], AccessDomain[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessDomainRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessDomain(target)
}

/**
  * Used for accessing domain items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedDomains[A](wrapped: TargetingMany[A]) 
	extends AccessDomains[A, AccessCombinedDomains[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedDomains[A], AccessDomain[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedDomains(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessDomain(target)
}

