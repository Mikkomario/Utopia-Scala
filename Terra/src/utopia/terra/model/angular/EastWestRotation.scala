package utopia.terra.model.angular

import utopia.paradigm.angular.Rotation
import utopia.terra.model.enumeration.CompassDirection.EastWest

object EastWestRotation
{
	// ATTRIBUTES   --------------------
	
	/**
	  * A zero degree rotation along this axis
	  */
	val zero = apply(Rotation.clockwise.zero)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param rotation A rotation to wrap
	  * @return Specified rotation along the East-to-West axis
	  */
	def apply(rotation: Rotation) = new EastWestRotation(rotation)
}

/**
  * Represents a rotation along the East-to-West axis.
  * Wraps a rotation instance
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
class EastWestRotation(override val wrapped: Rotation)
	extends CompassRotationLike[EastWestRotation] with CompassRotation
{
	// WET WET
	override def self: EastWestRotation = this
	override def zero: EastWestRotation = EastWestRotation.zero
	
	override def compassAxis = EastWest
	override def direction = EastWest(wrapped.direction)
	
	override def +(other: Rotation): EastWestRotation = EastWestRotation(wrapped + other)
	override def *(mod: Double): EastWestRotation = EastWestRotation(wrapped * mod)
}
