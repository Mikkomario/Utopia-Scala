package utopia.paradigm.shape.shape1d

import utopia.flow.collection.immutable.Single
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

object Dimension
{
	// OTHER    --------------------
	
	/**
	  * @param axis  An axis
	  * @param value Value to assign for the specified axis
	  * @param zero  A value that represents zero (called lazily)
	  * @tparam A Type of wrapped value
	  * @return A new dimension
	  */
	def apply[A](axis: Axis, value: A, zero: => A): Dimension[A] = apply(axis, value, Lazy(zero))
	/**
	  * @param axis An axis
	  * @param value Value to assign for the specified axis
	  * @param zero A value that represents zero (lazy)
	  * @tparam A Type of wrapped value
	  * @return A new dimension
	  */
	def apply[A](axis: Axis, value: A, zero: Lazy[A]): Dimension[A] = new _Dimension(value, zero, axis)
	
	
	// NESTED   --------------------
	
	private class _Dimension[+A](override val value: A, lazyZero: Lazy[A], override val axis: Axis)
		extends Dimension[A] with EqualsBy
	{
		override def zeroValue: A = lazyZero.value
		override protected def equalsProperties: Seq[Any] = Vector(lazyZero.value, value, axis)
	}
}

/**
  * Represents a sub-component of Dimensions, a single dimensional component
  * @author Mikko Hilpinen
  * @since 10.11.2022, v1.2
  */
trait Dimension[+A] extends View[A] with HasDimensions[A]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The axis on which this dimension applies
	  */
	def axis: Axis
	
	/**
	  * @return A value that represents zero
	  */
	def zeroValue: A
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Whether this is a zero-dimension (value is zero)
	  */
	def isZero = value == zeroValue
	/**
	  * @return Whether this is not a zero-dimension (value is not zero)
	  */
	def nonZero = !isZero
	
	
	// IMPLEMENTED  ----------------------
	
	override def dimensions = Dimensions(zeroValue)(Vector.fill(axis.index)(zeroValue) :+ value)
	
	override def components: Seq[Dimension[A]] = Single(this)
}
