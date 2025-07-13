package utopia.logos.database.access.url.link.placement

import utopia.logos.database.reader.url.{DetailedLinkPlacementDbReader, LinkPlacementDbReader}
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many.{AccessManyRoot, AccessManyRows, AccessRowsWrapper, AccessWrapper, TargetingMany, TargetingManyLike, TargetingManyRows}
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessLinkPlacements extends AccessManyRoot[AccessLinkPlacementRows[LinkPlacement]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val root = apply(LinkPlacementDbReader)
	
	/**
	 * Access to link placements, including full link information
	 */
	lazy val detailed = apply(DetailedLinkPlacementDbReader)
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessLinkPlacements[_, _]): AccessLinkPlacementValues = access.values
	
	
	// OTHER    --------------------
	
	def apply[A](reader: DbRowReader[A]) = AccessLinkPlacementRows(AccessManyRows(reader))
}

/**
  * Used for accessing multiple link placements from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessLinkPlacements[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessLinkPlacement[A]] with FilterLinkPlacements[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible link placements
	  */
	lazy val values = AccessLinkPlacementValues(wrapped)
}

/**
  * Provides access to row-specific link placement -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessLinkPlacementRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessLinkPlacements[A, AccessLinkPlacementRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessLinkPlacementRows[A], AccessLinkPlacement[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessLinkPlacementRows(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessLinkPlacement(target)
}

/**
  * Used for accessing link placement items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedLinkPlacements[A](wrapped: TargetingMany[A]) 
	extends AccessLinkPlacements[A, AccessCombinedLinkPlacements[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedLinkPlacements[A], AccessLinkPlacement[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedLinkPlacements(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessLinkPlacement(target)
}

