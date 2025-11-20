package utopia.logos.database.access.url.link

import utopia.logos.database.factory.url.LinkDbFactory
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessLinks extends AccessManyRoot[AccessLinks[StoredLink]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(LinkDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessLinks[_]): AccessLinkValues = access.values
}

/**
  * Used for accessing multiple links from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessLinks[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessLinks[A], AccessLink[A]] with FilterLinks[AccessLinks[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible links
	  */
	lazy val values = AccessLinkValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessLinks(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessLink(target)
}

