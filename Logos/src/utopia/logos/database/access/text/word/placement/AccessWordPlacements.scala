package utopia.logos.database.access.text.word.placement

import utopia.logos.database.reader.text.WordPlacementDbReader
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessWordPlacements extends AccessManyRoot[AccessWordPlacementRows[WordPlacement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = AccessWordPlacementRows(AccessManyRows(WordPlacementDbReader))
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessWordPlacements[_, _]): AccessWordPlacementValues = access.values
}

/**
  * Used for accessing multiple word placements from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessWordPlacements[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessWordPlacement[A]] with FilterWordPlacements[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible word placements
	  */
	lazy val values = AccessWordPlacementValues(wrapped)
}

/**
  * Provides access to row-specific word placement -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessWordPlacementRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessWordPlacements[A, AccessWordPlacementRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessWordPlacementRows[A], AccessWordPlacement[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessWordPlacementRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessWordPlacement(target)
}

/**
  * Used for accessing word placement items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedWordPlacements[A](wrapped: TargetingMany[A]) 
	extends AccessWordPlacements[A, AccessCombinedWordPlacements[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedWordPlacements[A], AccessWordPlacement[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedWordPlacements(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessWordPlacement(target)
}

