package utopia.logos.database.access.url.link

import utopia.logos.database.access.url.link.AccessLinks.placementModel
import utopia.logos.database.access.url.link.placement.{AccessLinkPlacementValues, FilterByLinkPlacement}
import utopia.logos.database.reader.url.{DetailedLinkDbReader, LinkDbReader}
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.targeting.many._
import utopia.vault.nosql.targeting.one.TargetingOne

import scala.language.implicitConversions

object AccessLinks extends AccessManyRoot[AccessLinkRows[StoredLink]]
{
	// ATTRIBUTES	--------------------
	
	protected val placementModel = LinkPlacementDbModel
	
	override lazy val root = apply(LinkDbReader)
	
	/**
	 * Access to links, including request path & domain information
	 */
	lazy val detailed = apply(DetailedLinkDbReader)
	
	
	// IMPLICIT	--------------------
	
	/**
	  * Provides implicit access to an access point's .values property
	  * @param access Access point whose values are accessed
	  */
	implicit def accessValues(access: AccessLinks[_, _]): AccessLinkValues = access.values
	
	
	// OTHER    --------------------
	
	def apply[A](reader: DbRowReader[A]) = AccessLinkRows(AccessManyRows(reader))
}

/**
  * Used for accessing multiple links from the DB at a time
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
abstract class AccessLinks[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingManyLike[A, Repr, AccessLink[A]] with FilterLinks[Repr]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to the values of accessible links
	  */
	lazy val values = AccessLinkValues(wrapped)
	
	lazy val joinedToPlacements = join(placementModel.table)
	lazy val wherePlacements = FilterByLinkPlacement(joinedToPlacements)
	lazy val placements = AccessLinkPlacementValues(joinedToPlacements)
}

/**
  * Provides access to row-specific link -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessLinkRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessLinks[A, AccessLinkRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessLinkRows[A], AccessLink[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessLinkRows(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessLink(target)
}

/**
  * Used for accessing link items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessCombinedLinks[A](wrapped: TargetingMany[A]) 
	extends AccessLinks[A, AccessCombinedLinks[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedLinks[A], AccessLink[A]]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedLinks(newTarget)
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessLink(target)
}

