package utopia.scribe.api.database.access.management.resolution

import utopia.scribe.api.database.reader.management.{DetailedResolutionDbReader, ResolutionDbReader}
import utopia.scribe.api.database.storable.management.ResolutionDbModel
import utopia.scribe.core.model.stored.management.Resolution
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many._
import utopia.vault.nosql.targeting.one.TargetingOne

object AccessResolutions 
	extends DeprecatingWrapRowAccess[AccessResolutionRows](ResolutionDbModel)
		with WrapOneToManyAccess[AccessCombinedResolutions]
		with AccessManyDeprecatingRoot[AccessResolutionRows[Resolution]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val all: AccessResolutionRows[Resolution] = apply(ResolutionDbReader).all
	
	/**
	 * Access to resolutions, including comment and notification information, where applicable.
	 * Note: Includes historical notifications, even when limited to active resolutions.
	 */
	lazy val detailed = apply(DetailedResolutionDbReader)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrap[I](access: TargetingManyRows[I]): AccessResolutionRows[I] = AccessResolutionRows(access)
	override def apply[A](access: TargetingMany[A]) = AccessCombinedResolutions(access)
}

/**
  * Used for accessing multiple resolutions from the DB at a time
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
abstract class AccessResolutions[A, +Repr <: TargetingManyLike[_, Repr, _]](wrapped: AccessManyColumns) 
	extends TargetingTimeline[A, Repr, AccessResolution[A]] with HasValues[AccessResolutionValues] 
		with FilterResolutions[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessResolutionValues(wrapped)
}

/**
  * Provides access to row-specific resolution -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessResolutionRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessResolutions[A, AccessResolutionRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessResolutionRows[A], AccessResolution[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessResolutionRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessResolution(target)
}

/**
  * Used for accessing resolution items that have been combined with one-to-many combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessCombinedResolutions[A](wrapped: TargetingMany[A]) 
	extends AccessResolutions[A, AccessCombinedResolutions[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedResolutions[A], AccessResolution[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedResolutions(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessResolution(target)
}

