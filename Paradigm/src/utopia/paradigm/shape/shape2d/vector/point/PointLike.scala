package utopia.paradigm.shape.shape2d.vector.point

import utopia.flow.operator.{MayBeAboutZero, HasLength}
import utopia.paradigm.shape.template.HasDimensions
import utopia.paradigm.shape.template.vector.NumericVectorLike

import java.awt.geom.Point2D

/**
* Common trait for classes that represent points in two-dimensional space
* @author Mikko Hilpinen
* @since 25.8.2023, v1.4
**/
trait PointLike[D, +Repr <: HasDimensions[D] with HasLength]
	extends NumericVectorLike[D, Repr, Repr] with MayBeAboutZero[HasDimensions[D], Repr]
{
    // IMPLEMENTED    -----------------
	
	override def toString = xyPair.toString()
	
	
	// COMPUTED	-----------------------
	
	/**
	  * An awt representation of this point
	  */
	def toAwtPoint = new java.awt.Point(n.toInt(x), n.toInt(y))
	/**
	  * An awt geom representation of this point
	  */
	def toAwtPoint2D = new Point2D.Double(n.toDouble(x), n.toDouble(y))
}