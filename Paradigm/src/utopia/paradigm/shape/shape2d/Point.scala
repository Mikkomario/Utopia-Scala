package utopia.paradigm.shape.shape2d

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.EqualsBy
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.generic.ParadigmDataType.PointType
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.{Dimensions, DoubleVector, DoubleVectorLike, HasDimensions, VectorFactory}

import java.awt.geom.Point2D
import scala.collection.immutable.HashMap
import scala.util.Success

object Point extends VectorFactory[Point] with FromModelFactory[Point]
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * (0,0) location
	  */
    val origin = empty
	
    
	// IMPLEMENTED  ---------------------------
	
	override def apply(dimensions: Dimensions[Double]) = new Point(dimensions.withLength(2))
	override def from(other: HasDimensions[Double]) = other match {
		case p: Point => p
		case o => apply(o.dimensions)
	}
	
	def apply(model: template.ModelLike[Property]) = Success(
            Point(model("x").getDouble, model("y").getDouble))
	
	
	// OTHER    -------------------------------
    
    /**
     * Converts an awt point to Utopia point
     */
    def of(point: java.awt.Point) = Point(point.getX, point.getY)
    /**
     * Converts an awt geom point to Utopia point
     */
    def of(point: Point2D) = Point(point.getX, point.getY)
    /**
     * Converts a coordinate map into a point
     */
    @deprecated("Please use apply instead", "v1.2")
    def of[K >: Axis2D](map: Map[K, Double]) = Point(map.getOrElse(X, 0), map.getOrElse(Y, 0))
	
	/**
	  * Creates a new point by calling specified function for both axes (X and Y)
	  * @param f A function that is called for specified axes
	  * @return Point with function results as values
	  */
	def calculateWith(f: Axis2D => Double) = Point(f(X), f(Y))
}

/**
* A point represents a coordinate pair in a 2 dimensional space
* @author Mikko Hilpinen
* @since Genesis 20.11.2018
**/
class Point private(override val dimensions: Dimensions[Double])
	extends DoubleVectorLike[Point] with DoubleVector with ValueConvertible with ModelConvertible
		with EqualsBy
{
    // IMPLEMENTED    -----------------
	
	override def zero = Point.origin
	override def repr = this
	
	override protected def equalsProperties = dimensions
	
	override def toValue = new Value(Some(this), PointType)
	override def toModel = Model.fromMap(HashMap("x" -> x, "y" -> y))
	
	override def toString = xyPair.toString()
	
	override protected def factory = Point
	
	
	// COMPUTED	-----------------------
	
	/**
	  * A vector representation of this point
	  */
	def toVector = toVector2D
	/**
	  * @return A 3D vector representation of this point
	  */
	@deprecated("Please use .toVector3D instead", "v1.2")
	def in3D = Vector3D(x, y)
	
	/**
	  * An awt representation of this point
	  */
	def toAwtPoint = new java.awt.Point(x.round.toInt, y.round.toInt)
	/**
	  * An awt geom representation of this point
	  */
	def toAwtPoint2D = new Point2D.Double(x, y)
    
    
    // OTHER    -----------------------
	
	/**
	 * Connects this point with another, forming a line
	 */
	def lineTo(other: Point) = Line(this, other)
	
    /**
     * A copy of this point with specified coordinate
     */
	@deprecated("Please use the more generic .withDimension(...) instead", "v2.3")
    def withCoordinate(c: Double, axis: Axis2D) = withDimension(axis(c))
}