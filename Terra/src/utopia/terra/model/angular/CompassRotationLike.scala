package utopia.terra.model.angular

import utopia.flow.operator.{Combinable, EqualsBy, LinearScalable, SignOrZero, SignedOrZero}
import utopia.flow.view.template.Extender
import utopia.paradigm.angular.Rotation
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.Dimension
import utopia.terra.model.enumeration.CompassDirection
import utopia.terra.model.enumeration.CompassDirection.CompassAxis

/**
  * Common trait for Northward, Southward, Eastward and Westward rotations.
  * These are typically applicable in the latitude-longitude coordinate systems,
  * as well as in systems that assume the Earth to be (perfectly) spherical.
  *
  * This trait allows a custom 'Repr' type.
  * Extending classes are expected to extend [[CompassRotation]] as well.
  *
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  *
  * @tparam Repr Type of the implementing class
  */
trait CompassRotationLike[+Repr]
	extends Extender[Rotation] with Dimension[Rotation]
		with Combinable[Rotation, Repr] with LinearScalable[Repr] with SignedOrZero[Repr]
		with EqualsBy
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The axis along which this rotation occurs
	  */
	def compassAxis: CompassAxis
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return The direction of this rotation
	  */
	def direction: CompassDirection = compassAxis(wrapped.direction)
	
	/**
	  * @return A 2-dimensional version of this one-dimensional rotation
	  */
	def in2D = LatLongRotation(this)
	
	
	// IMPLEMENTED  ---------------------
	
	override def value: Rotation = wrapped
	override def axis: Axis = compassAxis.axis
	override def sign: SignOrZero = wrapped.sign
	
	override def zeroValue: Rotation = Rotation.clockwise.zero
	override def isZero = wrapped.isZero
	override def nonZero = !isZero
	
	override def toString = s"${wrapped.absolute.degrees} $direction"
	
	override protected def equalsProperties: Iterable[Any] = Vector(axis, wrapped)
}
