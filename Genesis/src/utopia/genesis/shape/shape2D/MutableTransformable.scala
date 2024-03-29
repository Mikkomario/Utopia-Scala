package utopia.genesis.shape.shape2D

import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.{AffineTransformation, LinearTransformation}

/**
  * A common trait for mutable items that have a mutating transformation state
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
trait MutableTransformable
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Current transformation of this item
	  */
	def transformation: AffineTransformation
	/**
	  * @param newTransformation New transformation state for this item
	  */
	def transformation_=(newTransformation: AffineTransformation): Unit
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Current position of this instance
	  */
	def position = transformation.translation.toPoint
	def position_=(newPosition: Point) = transformation = transformation.withPosition(newPosition)
	
	/**
	  * @return Current x-coordinate of this instance
	  */
	def x = transformation.translation.x
	def x_=(newX: Double) = position = position.withX(newX)
	/**
	  * @return Current y-coordinate of this instance
	  */
	def y = transformation.translation.y
	def y_=(newY: Double) = position = position.withY(newY)
	
	/**
	  * @return Current direction / angle of this instance
	  */
	def angle = transformation.rotation.toAngle
	def angle_=(newAngle: Angle) = transformation = transformation.withRotation(newAngle.toShortestRotation)
	
	/**
	  * @return Current scaling of this instance (x and y scaling separately)
	  */
	def scaling = transformation.scaling
	def scaling_=(newScaling: Double) = transformation = transformation.withScaling(newScaling)
	def scaling_=(newScaling: Vector2D) = transformation = transformation.withScaling(newScaling)
	
	/**
	  * @return Current horizontal scaling of this instance
	  */
	def xScaling = scalingAlong(X)
	def xScaling_=(newScaling: Double) = setScalingAlong(X, newScaling)
	/**
	  * @return Current vertical scaling of this instance
	  */
	def yScaling = scalingAlong(Y)
	def yScaling_=(newScaling: Double) = setScalingAlong(Y, newScaling)
	
	
	// OTHER	-------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return This instance's position on specified axis
	  */
	def positionAlong(axis: Axis) = transformation.translation(axis)
	/**
	  * Changes this instance's position on the specified axis
	  * @param axis Targeted axis
	  * @param amount New position along specified axis
	  */
	def setPositionAlong(axis: Axis, amount: Double) = transformation = transformation.withTranslation(
		transformation.translation.withDimension(axis(amount)))
	
	/**
	  * @param axis Targeted axis
	  * @return This instance's scaling along specified axis
	  */
	def scalingAlong(axis: Axis) = scaling(axis)
	/**
	  * Changes this instance's scaling along specified axis
	  * @param axis Targeted axis
	  * @param amount New scaling amount for the specified axis
	  */
	def setScalingAlong(axis: Axis, amount: Double) = scaling = scaling.withDimension(axis(amount))
	
	/**
	  * Transforms this item using specified transformation. Please note that the provided transformation is applied
	  * <b>over</b> the existing transformation. If you wish to instead overwrite the current transformation,
	  * use transformation = ...
	  * @param appliedTransformation Transformation applied over this instance
	  */
	def transform(appliedTransformation: AffineTransformation) = transformation += appliedTransformation
	
	/**
	  * Translates this instance the specified amount. Please note that the current scaling may affect the applied translation
	  * @param amount Amount of translation (position change) applied
	  */
	def translate(amount: HasDoubleDimensions) = transformation += AffineTransformation.translation(amount)
	
	/**
	  * Rotates this instance the specified amount
	  * @param amount Amount of rotation applied to this instance
	  */
	def rotate(amount: Rotation) = transformation += amount
	
	/**
	  * Scales this instance, applied in addition to existing scaling
	  * @param amount Amount of scaling applied
	  */
	def scale(amount: Double) = transformation += LinearTransformation.scaling(amount)
	
	/**
	  * Scales this instance, applied in addition to existing scaling
	  * @param amount Amount of scaling applied (for each axis separately)
	  */
	def scale(amount: Vector2D) = transformation += LinearTransformation.scaling(amount)
}
