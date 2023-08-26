package utopia.paradigm.shape.shape1d

import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.EqualsFunction
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.enumeration.{Axis, Direction2D}
import utopia.paradigm.shape.template.{Dimensions, NumericVectorFactory}

trait Vector1DFactoryLike[D, +V] extends NumericVectorFactory[D, V]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Approximate equality function used for the specified dimensions
	  */
	implicit def dimensionApproxEquals: EqualsFunction[D]
	
	/**
	  * @param length Length of this vector
	  * @param axis Axis along which this vector will run
	  * @return A new vector that runs along the specified axis
	  */
	def apply(length: D, axis: Axis): V
	
	
	// COMPUTED   ----------------------
	
	/**
	  * A unit (1.0) vector (along the X-axis)
	  */
	def unit = apply(n.fromInt(1), X)
	
	
	// IMPLEMENTED  ----------------------
	
	/**
	  * A zero vector (along the X-axis)
	  */
	override def zero = apply(n.zero, X)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param axis Axis along which this vector should run
	  * @return A unit vector along the specified axis
	  */
	def unitAlong(axis: Axis) = apply(n.fromInt(1), axis)
	/**
	  * @param direction Direction of the resulting vector
	  * @return A unit vector pointing towards the specified direction
	  */
	def unitTowards(direction: Direction2D) = apply(n.fromInt(direction.sign.modifier), direction.axis)
	
	/**
	  * @param axis Axis along which this vector should run
	  * @return A zero length vector along the specified axis
	  */
	def zeroAlong(axis: Axis) = apply(n.zero, axis)
	
	
	// IMPLEMENTED  -------------------
	
	override def apply(dimensions: Dimensions[D]): V = {
		dimensions.zipWithAxis.find { _._1 !~== n.zero } match {
			case Some((length, axis)) => apply(length, axis)
			case None => zero
		}
	}
	
	override def apply(values: Map[Axis, D]) =
		values.find { _._2 !~== n.zero }.orElse { values.find { _._2 != n.zero } }.orElse { values.headOption } match {
			case Some((axis, length)) => apply(length, axis)
			case None => zero
		}
}


