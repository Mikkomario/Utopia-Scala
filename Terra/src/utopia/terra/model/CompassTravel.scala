package utopia.terra.model

import utopia.flow.operator.combine.{Combinable, LinearScalable}
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.MayBeAboutZero
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape1d.Dimension
import utopia.terra.controller.coordinate.world.VectorDistanceConversion
import utopia.terra.model.enumeration.CompassDirection
import utopia.terra.model.enumeration.CompassDirection.CompassAxis
import utopia.terra.model.world.WorldDistance

object CompassTravel
{
	/**
	  * Creates a new compass travel instance
	  * @param axis Axis along which the travel occurs
	  * @param distance Distance traveled
	  * @param worldView Implicit world view used in distance conversions
	  * @return A new travel instance
	  */
	def apply(axis: CompassAxis, distance: WorldDistance)(implicit worldView: VectorDistanceConversion) =
		new CompassTravel(axis, distance)(worldView)
	/**
	  * @param direction Targeted direction
	  * @param distance Distance traveled towards the specified direction
	  * @param worldView Implicit world view used in distance conversions
	  * @return A new travel instance
	  */
	def apply(direction: CompassDirection, distance: WorldDistance)
	         (implicit worldView: VectorDistanceConversion): CompassTravel =
		apply(direction.axis, distance * direction.sign)
}

/**
  * Represents a travel distance along a specific compass axis.
  * The travel may be linear or arcing, depending on the context.
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
class CompassTravel(val compassAxis: CompassAxis, val distance: WorldDistance)
                   (implicit worldView: VectorDistanceConversion)
	extends Combinable[Distance, CompassTravel] with LinearScalable[CompassTravel] with Dimension[WorldDistance]
		with EqualsBy with MayBeAboutZero[CompassTravel, CompassTravel]
{
	// IMPLEMENTED  ------------------------
	
	override def self: CompassTravel = this
	
	override def axis: Axis2D = compassAxis.axis
	override def value: WorldDistance = distance
	override def zeroValue: WorldDistance = WorldDistance.zero
	
	override protected def equalsProperties: Iterable[Any] = Vector(compassAxis, distance)
	
	override def zero: CompassTravel = new CompassTravel(compassAxis, WorldDistance.zero)
	override def isAboutZero: Boolean = distance.isAboutZero
	override def nonZero = distance.nonZero
	
	override def ~==(other: CompassTravel): Boolean = compassAxis == other.compassAxis && (distance ~== other.distance)
	
	override def +(other: Distance): CompassTravel = new CompassTravel(compassAxis, distance + other)
	override def *(mod: Double): CompassTravel = new CompassTravel(compassAxis, distance * mod)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param vectorLength Increased travel amount, in vector coordinates
	  * @return Increased copy of this travel
	  */
	def +(vectorLength: Double) = new CompassTravel(compassAxis, distance + vectorLength)
	/**
	  * @param vectorLength Travel amount decrease, in vector coordinates
	  * @return Decreased copy of this travel
	  */
	def -(vectorLength: Double) = new CompassTravel(compassAxis, distance - vectorLength)
}
