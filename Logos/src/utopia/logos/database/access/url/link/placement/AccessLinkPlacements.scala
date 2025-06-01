package utopia.logos.database.access.url.link.placement

import utopia.logos.database.factory.url.LinkPlacementDbFactory
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessLinkPlacements extends AccessManyRoot[AccessLinkPlacements[LinkPlacement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(LinkPlacementDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessLinkPlacements[_]): AccessLinkPlacementValues = access.values
}

/**
  * Used for accessing multiple link placements from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessLinkPlacements[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessLinkPlacements[A], AccessLinkPlacement[A]] 
		with FilterLinkPlacements[AccessLinkPlacements[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible link placements
	  */
	lazy val values = AccessLinkPlacementValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessLinkPlacements(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessLinkPlacement(target)
}

