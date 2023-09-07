package utopia.terra.model

import utopia.flow.operator.{Combinable, LinearScalable}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape1d.Dimension
import utopia.terra.model.enumeration.CompassDirection.CompassAxis

/**
  * Represents a travel distance along a specific compass axis.
  * The travel may be linear or arcing, depending on the context.
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
case class CompassTravel(compassAxis: CompassAxis, distance: Distance)
	extends Combinable[Distance, CompassTravel] with LinearScalable[CompassTravel] with Dimension[Distance]
{
	// IMPLEMENTED  ------------------------
	
	override def self: CompassTravel = this
	
	override def axis: Axis2D = compassAxis.axis
	override def value: Distance = distance
	override def zeroValue: Distance = Distance.zero
	
	override def +(other: Distance): CompassTravel = copy(distance = distance + other)
	override def *(mod: Double): CompassTravel = copy(distance = distance * mod)
}
