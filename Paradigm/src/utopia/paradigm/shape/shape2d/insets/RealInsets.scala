package utopia.paradigm.shape.shape2d.insets

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.point.RealPoint
import utopia.paradigm.shape.shape2d.vector.size.RealSize

object RealInsets extends SidesFactory[Distance, RealInsets]
{
	override def withSides(sides: Map[Direction2D, Distance]): RealInsets = apply(sides)
}

/**
  * Defines real life inner margins for 0-4 sides
  * @author Mikko Hilpinen
  * @since 02.01.2025, v1.7.1
  */
case class RealInsets(sides: Map[Direction2D, Distance]) extends ScalableSidesLike[Distance, RealSize, RealInsets]
{
	// ATTRIBUTES   --------------
	
	override protected lazy val zeroLength: Distance = sides.valuesIterator.nextOption() match {
		case Some(d) => Distance(0.0, d.unit)
		case None => Distance.zero
	}
	lazy override val dimensions = super.dimensions
	
	override lazy val total: RealSize = RealSize(totalAlong(X), totalAlong(Y))
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Top left coordinate inside these insets
	  */
	def toPoint = RealPoint(left, top)
	
	/**
	  * @return A copy of these insets where all sides are at least 0 (i.e. with negative values removed)
	  */
	def positive = RealInsets(sides.filter { _._2.isPositive })
	
	
	// IMPLEMENTED  -------------------
	
	override def self: RealInsets = this
	
	override protected def multiply(length: Distance, mod: Double): Distance = length * mod
	override protected def subtract(from: Distance, amount: Distance): Distance = from - amount
	override protected def join(a: Distance, b: Distance): Distance = a + b
	
	override protected def withSides(sides: Map[Direction2D, Distance]): RealInsets = RealInsets(sides)
}