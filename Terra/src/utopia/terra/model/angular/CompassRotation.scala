package utopia.terra.model.angular

import utopia.paradigm.angular.Rotation
import utopia.terra.model.enumeration.CompassDirection.{CompassAxis, NorthSouth}

object CompassRotation
{
	// ATTRIBUTES   ------------------
	
	/**
	  * A zero degree rotation (north-to-south)
	  */
	lazy val zero = apply(NorthSouth, Rotation.clockwise.zero)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param axis Targeted rotational axis
	  * @param rotation The amount of rotation applied
	  * @return Specified amount of rotation along the specified axis
	  */
	def apply(axis: CompassAxis, rotation: Rotation): CompassRotation = new _CompassRotation(axis, rotation)
	
	
	// NESTED   ----------------------
	
	private class _CompassRotation(override val compassAxis: CompassAxis, override val wrapped: Rotation)
		extends CompassRotation
	{
		override def self: CompassRotation = this
		override def zero: CompassRotation = new _CompassRotation(compassAxis, Rotation.clockwise.zero)
		
		override def +(other: Rotation): CompassRotation = new _CompassRotation(compassAxis, wrapped + other)
		override def *(mod: Double): CompassRotation = new _CompassRotation(compassAxis, wrapped * mod)
	}
}

/**
  * Common trait for Northward, Southward, Eastward and Westward rotations.
  * These are typically applicable in the latitude-longitude coordinate systems,
  * as well as in systems that assume the Earth to be (perfectly) spherical.
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
trait CompassRotation extends CompassRotationLike[CompassRotation]