package utopia.terra.model.angular

import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.sign.Signed
import utopia.paradigm.angular.{DirectionalRotationLike, Rotation}
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
  * @tparam Direction The type of compass direction used in this rotation
  * @tparam Repr Type of the implementing class
  */
trait CompassRotationLike[+Direction <: Signed[Direction] with CompassDirection, +Repr]
	extends DirectionalRotationLike[Direction, CompassRotationLike[_, _], Repr]
		with Dimension[Rotation] with EqualsBy
{
	// COMPUTED -------------------------
	
	/**
	  * @return The axis along which this rotation occurs
	  */
	def compassAxis: CompassAxis = direction.axis
	
	/**
	  * @return A 2-dimensional version of this one-dimensional rotation
	  */
	def in2D = LatLongRotation.from(this)
	
	
	// IMPLEMENTED  ---------------------
	
	override def axis: Axis = compassAxis.axis
	override def value: Rotation = unidirectional
	override def zeroValue = Rotation.zero
	
	override def isZero = value.isZero
	override def nonZero = value.nonZero
	
	override protected def equalsProperties: Seq[Any] = Vector(unidirectional)
}
