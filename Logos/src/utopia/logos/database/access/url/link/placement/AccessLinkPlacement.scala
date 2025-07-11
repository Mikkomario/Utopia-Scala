package utopia.logos.database.access.url.link.placement

import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessLinkPlacement extends AccessOneRoot[AccessLinkPlacement[LinkPlacement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessLinkPlacements.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessLinkPlacement[_]): AccessLinkPlacementValue = access.values
}

/**
  * Used for accessing individual link placements from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessLinkPlacement[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessLinkPlacement[A]] 
		with FilterLinkPlacements[AccessLinkPlacement[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible link placement
	  */
	lazy val values = AccessLinkPlacementValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessLinkPlacement(newTarget)
}

