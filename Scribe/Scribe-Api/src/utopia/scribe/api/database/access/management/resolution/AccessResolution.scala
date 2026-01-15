package utopia.scribe.api.database.access.management.resolution

import utopia.scribe.core.model.stored.management.Resolution
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneDeprecatingRoot, AccessOneWrapper, TargetingOne}

object AccessResolution extends AccessOneDeprecatingRoot[AccessResolution[Resolution]]
{
	// ATTRIBUTES	--------------------
	
	override val all: AccessResolution[Resolution] = AccessResolutions.all.head
}

/**
  * Used for accessing individual resolutions from the DB at a time
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessResolution[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessResolution[A]] with HasValues[AccessResolutionValue] 
		with FilterResolutions[AccessResolution[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessResolutionValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessResolution(newTarget)
}

