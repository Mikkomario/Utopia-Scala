package utopia.terra.model.angular

import utopia.paradigm.angular.Rotation
import utopia.terra.model.enumeration.CompassDirection.NorthSouth

object NorthSouthRotation
{
	// ATTRIBUTES   ------------------
	
	/**
	  * A zero degree rotation along this axis
	  */
	val zero = apply(Rotation.clockwise.zero)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param rotation A rotation to wrap
	  * @return Specified rotation along the North-to-South axis
	  */
	def apply(rotation: Rotation) = new NorthSouthRotation(rotation)
}

/**
  * Represents rotation along the North-to-South axis.
  * Wraps a Rotation instance.
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
class NorthSouthRotation(override val wrapped: Rotation)
	extends CompassRotationLike[NorthSouthRotation] with CompassRotation
{
	override def self: NorthSouthRotation = this
	override def zero: NorthSouthRotation = NorthSouthRotation.zero
	
	override def compassAxis = NorthSouth
	override def direction = NorthSouth(wrapped.direction)
	
	override def +(other: Rotation): NorthSouthRotation = NorthSouthRotation(wrapped + other)
	override def *(mod: Double): NorthSouthRotation = NorthSouthRotation(wrapped * mod)
}
