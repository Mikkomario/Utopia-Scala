package utopia.terra.model

import utopia.flow.operator.{CanBeAboutZero, Combinable, EqualsBy, LinearScalable}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape1d.Dimension
import utopia.terra.controller.coordinate.world.VectorDistanceConversion
import utopia.terra.model.enumeration.CompassDirection.CompassAxis
import utopia.terra.model.world.WorldDistance

/**
  * Represents a travel distance along a specific compass axis.
  * The travel may be linear or arcing, depending on the context.
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
class CompassTravel(val compassAxis: CompassAxis, val distance: WorldDistance)
                   (implicit worldView: VectorDistanceConversion)
	extends Combinable[Distance, CompassTravel] with LinearScalable[CompassTravel] with Dimension[WorldDistance]
		with EqualsBy with CanBeAboutZero[CompassTravel, CompassTravel]
{
	// IMPLEMENTED  ------------------------
	
	override def self: CompassTravel = this
	
	override def axis: Axis2D = compassAxis.axis
	override def value: WorldDistance = distance
	override def zeroValue: WorldDistance = WorldDistance.zero
	
	override protected def equalsProperties: Iterable[Any] = Vector(compassAxis, distance)
	
	override def zero: CompassTravel = new CompassTravel(compassAxis, WorldDistance.zero)
	override def isAboutZero: Boolean = distance.isAboutZero
	
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
