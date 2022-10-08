package utopia.paradigm.shape.shape2d

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.generic.ParadigmDataType.PointType
import utopia.paradigm.shape.shape3d.Vector3D

import java.awt.geom.Point2D
import scala.collection.immutable.HashMap
import scala.util.Success

object Point extends FromModelFactory[Point]
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * (0,0) location
	  */
    val origin = apply()
	
    
	// IMPLEMENTED  ---------------------------
	
    def apply(model: template.ModelLike[Property]) = Success(
            Point(model("x").getDouble, model("y").getDouble))
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param x An x-coordinate (default = 0)
	  * @param y An y-coordinate (default = 0)
	  * @return A new point with those coordinates
	  */
	def apply(x: Double = 0.0, y: Double = 0.0): Point = apply(Pair(x, y))
	
	/**
	  * @param l Position length-wise
	  * @param b Position breadth-wise
	  * @param axis Target axis that determines which direction is length
	  * @return A new point
	  */
	def apply(l: Double, b: Double, axis: Axis2D): Point = axis match
	{
		case X => Point(l, b)
		case Y => Point(b, l)
	}
    
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
    def of[K >: Axis2D](map: Map[K, Double]) = Point(map.getOrElse(X, 0), map.getOrElse(Y, 0))
	
	/**
	  * Creates a new point by calling specified function for both axes (X and Y)
	  * @param f A function that is called for specified axes
	  * @return Point with function results as values
	  */
	def calculateWith(f: Axis2D => Double) = Point(f(X), f(Y))
    
    /**
     * A combination of the points with minimum x and y coordinates
     */
	@deprecated("Please call this method through the point instance", "v2")
    def topLeft(a: Point, b: Point) = Point(Math.min(a.x, b.x), Math.min(a.y, b.y))
    /**
     * A combination of the points with minimum x and y coordinates. None if collection is empty
     */
    def topLeftOption(points: IterableOnce[Point]) = points.iterator.reduceLeftOption { _ topLeft _ }
    /**
     * A combination of the points with minimum x and y coordinates
     */
    def topLeft(points: IterableOnce[Point]): Point = topLeftOption(points).getOrElse(origin)
    
    /**
     * A combination of the points with maximum x and y coordinates
     */
	@deprecated("Please call this method through the point instance", "v2")
    def bottomRight(a: Point, b: Point) = Point(Math.max(a.x, b.x), Math.max(a.y, b.y))
    /**
     * A combination of the points with maximum x and y coordinates. None if collection is empty
     */
    def bottomRightOption(points: IterableOnce[Point]) = points.iterator.reduceLeftOption { _ bottomRight _ }
    /**
     * A combination of the points with maximum x and y coordinates
     */
    def bottomRight(points: IterableOnce[Point]): Point = bottomRightOption(points).getOrElse(origin)
}

/**
* A point represents a coordinate pair in a 2 dimensional space
* @author Mikko Hilpinen
* @since Genesis 20.11.2018
**/
case class Point(override val dimensions2D: Pair[Double])
	extends Vector2DLike[Point] with ValueConvertible with ModelConvertible
		with TwoDimensional[Double]
{
    // IMPLEMENTED    -----------------
	
	override def zero = Point.origin
	override def repr = this
	
	override def toValue = new Value(Some(this), PointType)
	override def toModel = Model.fromMap(HashMap("x" -> x, "y" -> y))
	
	override def toString = dimensions2D.toString()
	
	override def buildCopy(vector: Vector2D) = Point(vector.dimensions2D)
	override def buildCopy(vector: Vector3D) = Point(vector.dimensions2D)
	
	override def buildCopy(dimensions: IndexedSeq[Double]) =
	{
		if (dimensions.size >= 2)
			Point(dimensions.head, dimensions(1))
		else if (dimensions.isEmpty)
			Point.origin
		else
			Point(dimensions.head)
	}
	
	
	
	
	// COMPUTED	-----------------------
	
	/**
	  * A vector representation of this point
	  */
	def toVector = Vector2D(dimensions2D)
	/**
	  * @return A 3D vector representation of this point
	  */
	def in3D = Vector3D(x, y)
	/**
	  * @return A size representation of this point
	  */
	def toSize = Size(dimensions2D)
	
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