package utopia.logos.database.access.text.word.placement

import utopia.logos.database.factory.text.WordPlacementDbFactory
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessWordPlacements extends AccessManyRoot[AccessWordPlacements[WordPlacement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(AccessManyRows(WordPlacementDbFactory))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessWordPlacements[_]): AccessWordPlacementValues = access.values
}

/**
  * Used for accessing multiple word placements from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessWordPlacements[A](wrapped: TargetingManyRows[A]) 
	extends AccessRowsWrapper[A, AccessWordPlacements[A], AccessWordPlacement[A]] 
		with FilterWordPlacements[AccessWordPlacements[A]]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible word placements
	  */
	lazy val values = AccessWordPlacementValues(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessWordPlacements(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessWordPlacement(target)
}

