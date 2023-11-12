package utopia.terra.model.angular

import utopia.paradigm.angular.{DirectionalRotationFactory, Rotation}
import utopia.terra.model.enumeration.CompassDirection

object CompassRotation extends DirectionalRotationFactory[CompassDirection, CompassRotation]
{
	// IMPLEMENTED  ------------------
	
	override protected def _apply(absolute: Rotation, direction: CompassDirection): CompassRotation =
		new _CompassRotation(absolute, direction)
	
	
	// NESTED   ----------------------
	
	private class _CompassRotation(override val absolute: Rotation, override val direction: CompassDirection)
		extends CompassRotation
	{
		override def self: CompassRotation = this
		override def zero: CompassRotation = new _CompassRotation(Rotation.zero, direction)
		
		override protected def copy(amount: Rotation, reverseDirection: Boolean) =
			new _CompassRotation(amount, if (reverseDirection) -direction else direction)
	}
}

/**
  * Common trait for Northward, Southward, Eastward and Westward rotations.
  * These are typically applicable in the latitude-longitude coordinate systems,
  * as well as in systems that assume the Earth to be (perfectly) spherical.
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
trait CompassRotation extends CompassRotationLike[CompassDirection, CompassRotation]