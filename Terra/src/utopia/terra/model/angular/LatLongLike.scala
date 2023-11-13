package utopia.terra.model.angular

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.combine.Combinable
import utopia.paradigm.angular.Rotation
import utopia.paradigm.shape.template.{Dimensional, Dimensions}
import utopia.terra.model.enumeration.CompassDirection
import utopia.terra.model.enumeration.CompassDirection.{CompassAxis, EastWest, NorthSouth}

object LatLongLike
{
	// ATTRIBUTES   ---------------
	
	/**
	  * A factory used for constructing rotation dimensions
	  */
	val dimensionsFactory = Dimensions(zeroValue)
	
	
	// COMPUTED ------------------
	
	/**
	  * @return Zero degree rotation value
	  */
	def zeroValue = Rotation.zero
}

/**
  * Common trait for latitude-longitude coordinate -related models.
  * Each model contains a lateral (X) rotation, as well as a longitude (Y) rotation and/or angle
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  */
trait LatLongLike[+Repr] extends Dimensional[Rotation, Repr] with Combinable[LatLongRotation, Repr]
{
	import LatLongLike._
	
	// ABSTRACT ----------------------------
	
	/**
	  * @return The North-to-South (X) component of this item
	  */
	def northSouth: NorthSouthRotation
	/**
	  * @return The East-to-West (Y) component of this item
	  */
	def eastWest: EastWestRotation
	
	
	// COMPUTED   ---------------------------
	
	/**
	  * @return The applied rotation towards north
	  */
	def north = northSouth.north
	/**
	  * @return The applied rotation towards south
	  */
	def south = northSouth.south
	/**
	  * @return The applied rotation towards east
	  */
	def east = eastWest.east
	/**
	  * @return The applied rotation towards west
	  */
	def west = eastWest.west
	
	
	// IMPLEMENTED  ------------------------
	
	override def dimensions: Dimensions[Rotation] =
		dimensionsFactory(northSouth.unidirectional, eastWest.unidirectional)
	override def components = Pair(northSouth, eastWest)
	
	override def x = northSouth.unidirectional
	override def y = eastWest.unidirectional
	
	override def toString = s"($northSouth, $eastWest)"
	
	override def +(other: LatLongRotation): Repr = withDimensions(dimensions.mergeWith(other) { _ + _ })
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return The rotation component that applies to that axis
	  */
	def along(axis: CompassAxis) = axis match {
		case NorthSouth => northSouth
		case EastWest => eastWest
	}
	/**
	  * @param direction Targeted compass direction
	  * @return Amount of rotation applied towards that direction
	  */
	def apply(direction: CompassDirection) = along(direction.axis).towards(direction)
	
	/**
	  * @param amount Amount of directional rotation to apply
	  * @return Copy of this coordinate shifted by the specified amount
	  */
	def +(amount: CompassRotation) = mapDimension(amount.axis) { _ + amount.unidirectional }
	/**
	  * @param amount Amount of directional rotation to subtract
	  * @return Copy of this coordinate shifted by the specified amount (in reverse)
	  */
	def -(amount: CompassRotation) = this + (-amount)
}
