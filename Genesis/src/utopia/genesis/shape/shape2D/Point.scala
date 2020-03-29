package utopia.genesis.shape.shape2D

import utopia.genesis.util.Extensions._
import utopia.flow.generic.ValueConversions._
import java.awt.geom.Point2D

import scala.collection.immutable.HashMap
import utopia.flow.generic.ValueConvertible
import utopia.flow.datastructure.immutable.Value
import utopia.genesis.generic.PointType
import utopia.flow.generic.ModelConvertible
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.FromModelFactory
import utopia.flow.datastructure.template.Property
import utopia.genesis.util.ApproximatelyEquatable
import utopia.genesis.shape.{Axis, Axis2D, Vector3D, VectorLike}
import utopia.genesis.shape.Axis._

import scala.util.Success

object Point extends FromModelFactory[Point]
{
    val origin = Point(0, 0)
    
    def apply(model: utopia.flow.datastructure.template.Model[Property]) = Success(
            Point(model("x").getDouble, model("y").getDouble))
	
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
    def topLeftOption(points: TraversableOnce[Point]) = points.reduceLeftOption { _ topLeft _ }
    
    /**
     * A combination of the points with minimum x and y coordinates
     */
    def topLeft(points: TraversableOnce[Point]): Point = topLeftOption(points).getOrElse(origin)
    
    /**
     * A combination of the points with maximum x and y coordinates
     */
	@deprecated("Please call this method through the point instance", "v2")
    def bottomRight(a: Point, b: Point) = Point(Math.max(a.x, b.x), Math.max(a.y, b.y))
    
    /**
     * A combination of the points with maximum x and y coordinates. None if collection is empty
     */
    def bottomRightOption(points: TraversableOnce[Point]) = points.reduceLeftOption { _ bottomRight _ }
    
    /**
     * A combination of the points with maximum x and y coordinates
     */
    def bottomRight(points: TraversableOnce[Point]): Point = bottomRightOption(points).getOrElse(origin)
}

/**
* A point represents a coordinate pair in a 2 dimensional space
* @author Mikko Hilpinen
* @since 20.11.2018
**/
case class Point(override val x: Double, override val y: Double) extends VectorLike[Point]
	with ApproximatelyEquatable[Point] with ValueConvertible with ModelConvertible
{
    // IMPLEMENTED    -----------------
	
	override lazy val dimensions = Vector(x, y)
	
	override def buildCopy(dimensions: Vector[Double]) =
	{
		if (dimensions.size >= 2)
			Point(dimensions(0), dimensions(1))
		else if (dimensions.isEmpty)
			Point.origin
		else
			Point(dimensions(0), 0)
	}
	
	override def toValue = new Value(Some(this), PointType)
    
    override def toModel = Model.fromMap(HashMap("x" -> x, "y" -> y))
    
    override def ~==(other: Point) = (x ~== other.x) && (y ~== other.y)
	
	override protected def repr = this
	
	override def toString = s"($x, $y)"
	
	
	// COMPUTED	-----------------------
	
	
	/**
	  * A vector representation of this point
	  */
	def toVector = Vector3D(x, y)
	
	/**
	  * @return A size representation of this point
	  */
	def toSize = Size(x, y)
	
	/**
	  * An awt representation of this point
	  */
	def toAwtPoint = new java.awt.Point(x.toInt, y.toInt)
	
	/**
	  * An awt geom representation of this point
	  */
	def toAwtPoint2D = new Point2D.Double(x, y)
	
    
    // OPERATORS    -------------------
    
	/**
	 * Translated position over certain axis
	 */
	def +(increase: Double, axis: Axis2D) = axis match 
	{
        case X => plusX(increase)
        case Y => plusY(increase)
    }
	
	/**
	 * Translated position over certain axis
	 */
	def -(decrease: Double, axis: Axis2D) = this.+(-decrease, axis)
    
    
    // OTHER    -----------------------
	
	/**
	  * @param other Another point
	  * @return The distance between these two points
	  */
	def distanceFrom(other: Point) = (this - other).toVector.length
	
	/**
	 * Connects this point with another, forming a line
	 */
	def lineTo(other: Point) = Line(this, other)
    
    /**
     * A copy of this point with specified x
     */
    def withX(x: Double) = Point(x, y)
    
    /**
     * A copy of this point with specified y
     */
    def withY(y: Double) = Point(x, y)
    
    /**
     * A copy of this point with specified coordinate
     */
    def withCoordinate(c: Double, axis: Axis2D) = axis match 
    {
        case X => withX(c)
        case Y => withY(c)
    }
    
    /**
     * Point translated over X axis
     */
    def plusX(increase: Double) = Point(x + increase, y)
    
    /**
     * Point translated over Y axis
     */
    def plusY(increase: Double) = Point(x, y + increase)
}