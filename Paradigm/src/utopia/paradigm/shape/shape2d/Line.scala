package utopia.paradigm.shape.shape2d

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.template
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.HasLength
import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.generic.ParadigmDataType.LineType
import utopia.paradigm.path.LinearPathLike
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.Transformable

import java.awt.geom.Line2D
import scala.collection.mutable.ListBuffer
import scala.util.Success

object Line extends FromModelFactory[Line]
{
    // ATTRIBUTES   -------------------------
    
    /**
      * A line between (0, 0) and (0, 0)
      */
    val zero = Line(Point.origin, Point.origin)
    
    
    // OPERATORS    -------------------------
    
    override def apply(model: template.ModelLike[Property]) =
        Success(Line(model("start").getPoint, model("end").getPoint))
    
    
    // OTHER METHODS    ---------------------
    
    /**
      * @param start Line start point
      * @param end Line end point
      * @return A new line
      */
    def apply(start: Point, end: Point): Line = apply(Pair(start, end))
    
    /**
     * Creates a new line from position and vector combo
     * @param position The starting position of the line
     * @param vector The vector portion of the line
     * @return A line with the provided position and vector part
     */
    def ofVector(position: Point, vector: HasDoubleDimensions) = Line(position, position + vector)
    
    /**
     * Creates a set of edges for the provided vertices. The vertices are iterated in order and an 
     * edge is placed between them.
     * @param vertices An ordered collection of vertices
     * @param close Should the shape be closed by connecting the last and the first vertex. Defaults
     * to true
     * @return The edges that were formed between the vertices. Empty if there were less than 2
     * vertices
     */
    // TODO: Add a 3D version separately
    def edgesForVertices(vertices: Seq[Point], close: Boolean = true) =
    {
        if (vertices.size < 2)
            Vector()
        else
        {
            var lastVertex = vertices.head
            val buffer = ListBuffer[Line]()
            // TODO: Add 3D support for 3D version of this class
            vertices.foreach( vertex => { buffer += Line(lastVertex, vertex); lastVertex = vertex } )
            if (close)
            {
                buffer += Line(lastVertex, vertices.head)
            }
            
            buffer.toVector
        }
    }
}

/**
 * A line consists of a start and an end point. Basically it is a vector with a certain position.
 * @author Mikko Hilpinen
 * @since Genesis 13.12.2016
 */
case class Line(points: Pair[Point])
    extends ShapeConvertible with ValueConvertible with ModelConvertible with Projectable with LinearPathLike[Point]
        with HasLength with Transformable[Line] with HasBounds
{
    // ATTRIBUTES    -------------------
    
    /**
     * The vector portion of this line (position information not included)
     */
    lazy val vector = (end - start).toVector
    
    /**
     * A function for calculating the y-coordinate on this line when the x-coordinate is known
     */
    lazy val yForX =
    {
        // y = kx + a
        // Where k = Vy / Vx where V is the vector format of this line
        // a is the y of this function at 0 x
        val k = vector.y / vector.x
        val a = start.y - k * start.x
        x: Double => k * x + a
    }
    /**
     * A function for calculating the x-coordinate on this line when the y-coordinate is known
     */
    lazy val xForY =
    {
        val k = vector.x / vector.y
        val a = start.x - k * start.y
        y: Double => k * y + a
    }
    
    
    // COMPUTED PROPERTIES    ----------
    
    /**
     * This line with inverted / reversed direction
     */
    def reverse = Line(points.reverse)
    
    /**
     * The axes that are necessary to include when checking collisions for this line
     */
    def collisionAxes = Vector(vector, vector.normal2D)
    
    /**
     * The center of the line segment
     */
    def center = (start + end) / 2
    
    /**
      * @return The direction of this line
      */
    def direction = vector.direction
    
    
    // IMPLEMENTED METHODS    ----------
    
    override def self = this
    
    /**
      * @return The bounds around this line
      */
    override def bounds = Bounds.between(start, end)
    
    override def toShape = new Line2D.Double(start.x, start.y, end.x, end.y)
    
    override def toValue = new Value(Some(this), LineType)
    
    override def toModel = Model(Vector("start" -> start, "end" -> end))
    
    def ~==(other: Line) = (start ~== other.start) && (end ~== other.end)
    
    override def start = points.first
    
    override def end = points.second
    
    override def transformedWith(transformation: Matrix3D) = map { transformation(_).toPoint }
    
    override def transformedWith(transformation: Matrix2D) = map { transformation(_).toPoint }
    
    override def length = vector.length
    
    override def projectedOver(axis: Vector2D) = Line(start.toVector.projectedOver(axis).toPoint,
            end.toVector.projectedOver(axis).toPoint)
    
    
    // OTHER METHODS    ----------------
    
    /**
      * @param f A mapping function for both the start and end point of this line
      * @return A mapped copy of this line
      */
    def map(f: Point => Point) = Line(points.map(f))
    
    /**
     * Calculates the intersection point between this and another line
     * @param other the other line
     * @param onlyPointsInSegment Should the intersection be limited to line segment area. Defaults
     * to true
     * @return The intersection between the two lines or None if there is no intersection
     */
    def intersection(other: Line, onlyPointsInSegment: Boolean = true) = {
        val v1 = vector.toVector3D
        val v2 = other.vector.toVector3D
        
        // a (V1 x V2) = (P2 - P1) x V2
        // Where P is the start point and Vs are the vector parts
        val leftVector = v1 cross v2
        lazy val rightVector = (other.start - start).toVector3D cross v2
        
        // If the left hand side vector is a zero vector, there is no collision
        // The two vectors must also be parallel
        if (leftVector.isAboutZero || !(leftVector isParallelWith rightVector))
            None
        else {
            // a = |right| / |left|, negative if they have opposite directions
            val a = if (leftVector.direction ~== rightVector.direction) 
                rightVector.length / leftVector.length else -rightVector.length / leftVector.length
            lazy val intersectionPoint = apply(a)
            
            if (onlyPointsInSegment) {
                if (a >= 0 && a <= 1 && other.bounds.contains(intersectionPoint))
                    Some(intersectionPoint) else None
            }
            else
                Some(intersectionPoint)
        }
    }
    
    // Do a sphere intersection https://en.wikipedia.org/wiki/Line%E2%80%93sphere_intersection
    
    /**
     * Calculates the intersection points between a line and a sphere in 3D. There are from 0 to 2
     * intersection points
     * @param circle The circle / sphere this line may intersect with
     * @param onlyPointsInSegment Determines whether only points that lie in this specific line 
     * segment should be included. If false, finds the intersection points as if the line had 
     * infinite length. Defaults to true.
     * @return The intersection points between this line (segment) and the circle
     */
    def circleIntersection(circle: Circle, onlyPointsInSegment: Boolean = true) = 
    {
        /* Circle Equation: |x - c|^2 = r^2 
         * where x is a point on the circle, c is circle origin and r is circle radius
         * 
         * Line Equation: x = o + vt
         * where x is point on the line, o is the line start point and v is the line's vector
         * portion. t is a scale for the vector portion ([0, 1] are on the line segment)
         * 
         * From these we get: |o + vt - c|^2 = r^2
         * -> ... -> t^2(v . v) + 2t(v . (o - c)) + (o - c) . (o - c) - r^2 = 0
         * And if we add L = o - c...
         * 
         * Using quadratic formula we get the terms
         * a = v . v
         * b = 2(v . L)
         * c = L . L - r^2
         */
        val distanceVector = (start - circle.origin).toVector
        val a = vector dot vector
        val b = 2 * (vector dot distanceVector)
        val c = (distanceVector dot distanceVector) - math.pow(circle.radius, 2)
        
        // The discriminant portion of the equation determines the amount of intersection points (0 - 2)
        // d = b^2 - 4 * a * c
        val discriminant = math.pow(b, 2) - 4 * a * c
        //println(discriminant)
        
        if (discriminant < 0)
        {
            Vector[Point]()
        }
        else
        {
            var intersectionPoints = Vector[Point]()
            
            //println(s"a = $a")
            //println(s"b = $b")
            //println(s"discriminant = $discriminant -> (${math.sqrt(discriminant)})")
            
            // t = (-b +- sqrt(d)) / 2a
            val tEnter = (-b - math.sqrt(discriminant)) / (2 * a)
            //println(tEnter)
            if (!onlyPointsInSegment || (tEnter >= 0 && tEnter <= 1))
            {
                intersectionPoints :+= apply(tEnter)
            }
            
            if (!(discriminant ~== 0.0))
            {
                val tExit = (-b + math.sqrt(discriminant)) / (2 * a)
                //println(tExit)
                if (!onlyPointsInSegment || (tExit >= 0 && tExit <= 1))
                {
                    intersectionPoints :+= apply(tExit)
                }
            }
            
            //println(intersectionPoints)
            intersectionPoints
        }
    }
    
    /**
     * Clips the line along a certain axis
     * @param clippingPlanePoint a point on the clipping axis / plane
     * @param clippingPlaneNormal A normal vector for the clipping plane. The normal is perpendicular 
     * to the clipping plane itself. The positive direction of the normal is the portion / side 
     * that will be preserved of the line
     * @return The clipped line. None if this line is completely clipped off
     */
    def clipped(clippingPlanePoint: Point, clippingPlaneNormal: Vector2D) =
    {
        val origin = clippingPlanePoint.toVector dot clippingPlaneNormal
        val startDistance = start.toVector.dot(clippingPlaneNormal) - origin
        val endDistance = end.toVector.dot(clippingPlaneNormal) - origin
        
        if (startDistance < 0 && endDistance < 0)
        {
            // If both start and end were clipped off, there's no line left
            None
        }
        else if (startDistance >= 0 && endDistance >= 0)
        {
            // If neither were clipped, the line is preserved
            Some(this)
        }
        else
        {
            // Calculates the point where the clipping happens
            val t = startDistance / (startDistance - endDistance)
            val clippingPoint = start + vector * t
            
            if (startDistance > endDistance) Some(Line(start, clippingPoint)) else Some(Line(clippingPoint, end))
        }
    }
    
    /*
     * Finds the intersection points between this line and a circle. Only works in 2D.
     * @param circle a circle
     * @param onlyPointsInSegment determines whether only points between this line's start and end
     * point should be included. Defaults to true.
     * @return The intersection points between this line and the circle. Empty if there is no
     * intersection, one point if the line is tangential to the circle or starts / ends inside the
     * circle. Enter point and exit point (in that order) in case the line traverses through the 
     * circle
     */
    /*
    def circleIntersection(circle: Circle, onlyPointsInSegment: Boolean = true) = 
    {
        /*
		 * Terms for the quadratic equation
		 * --------------------------------
		 * 
		 * a = (x1 - x0)^2 + (y1 - y0)^2
		 * b = 2 * (x1 - x0) * (x0 - cx) + 2 * (y1 - y0) * (y0 - cy)
		 * c = (x0 - cx)^2 + (y0 - cy)^2 - r^2
		 * 
		 * Where (x1, y1) is the end point, (x0, y0) is the starting point, (cx, cy) is the 
		 * circle origin and r is the circle radius
		 * 
		 * Vx = (x1 - x0), The transition vector (end - start)
		 * Vy = (y1 - y0)
		 * 
		 * Lx = (x0 - cx), The transition vector from the circle origin to the line start
		 * Ly = (y0 - cy)	(start - origin)
		 * 
		 * With this added:
		 * a = Vx^2  +  Vy^2
		 * b = 2 * Vx * Lx  +  2 * Vy * Ly
		 * c = Lx^2  +  Ly^2  -  r^2
		 */
        val L = start - circle.origin
        
        val a = math.pow(vector.x, 2) + math.pow(vector.y, 2)
        val b = 2 * vector.x * L.x + 2 * vector.y * L.y
        val c = math.pow(L.x, 2) + math.pow(L.y, 2) - math.pow(circle.radius, 2)
        
        /*
		 * The equation
		 * ------------
		 * 
		 * t = (-b +- sqrt(b^2 - 4*a*c)) / (2*a)
		 * Where t is the modifier for the intersection points [0, 1] would be on the line
		 * Where b^2 - 4*a*c is called the discriminant and where a != 0
		 * 
		 * If The discriminant is negative, there is no intersection
		 * If the discriminant is 0, there is a single intersection point
		 * Otherwise there are two
		 */
        var intersectionPoints = Vector[Vector3D]()
        
        val discriminant = math.pow(b, 2) - 4 * a * c
        
        if (a != 0 && discriminant >= 0)
        {
            val discriminantRoot = math.sqrt(discriminant)
            val tEnter = (-b - discriminantRoot) / (2 * a)
            
            /*
			 * The intersection points
			 * -----------------------
			 * 
			 * The final intersection points are simply
			 * start + t * V
			 * Where start is the line start position, and V is the line translation vector 
			 * (end - start)
			 */
            if (!onlyPointsInSegment || (tEnter >= 0 && tEnter <= 1))
            {
                intersectionPoints :+= apply(tEnter)
            }
            
            if (!(discriminant ~== 0))
            {
                val tExit = (-b + discriminantRoot) / (2 * a)
                
                if (!onlyPointsInSegment || (tExit >= 0 && tExit <= 1))
                {
                    intersectionPoints :+= apply(tExit)
                }
            }
        }
        
        intersectionPoints
    }*/
}