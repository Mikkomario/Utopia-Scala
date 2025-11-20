package utopia.logos.database.access.text.word.placement

import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}

import scala.language.implicitConversions

object AccessWordPlacement extends AccessOneRoot[AccessWordPlacement[WordPlacement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessWordPlacements.root.head
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessWordPlacement[_]): AccessWordPlacementValue = access.values
}

/**
  * Used for accessing individual word placements from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessWordPlacement[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessWordPlacement[A]] 
		with FilterWordPlacements[AccessWordPlacement[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible word placement
	  */
	lazy val values = AccessWordPlacementValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessWordPlacement(newTarget)
}

