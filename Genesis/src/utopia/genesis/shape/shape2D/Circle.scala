package utopia.genesis.shape.shape2D

import utopia.genesis.util.Extensions._
import java.awt.geom.Ellipse2D

import utopia.flow.generic.ValueConvertible
import utopia.genesis.generic.CircleType
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.FromModelFactory
import utopia.genesis.generic.GenesisValue._
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.genesis.shape.{Angle, Vector3D}

import scala.util.Success

object Circle extends FromModelFactory[Circle]
{
    override def apply(model: template.Model[Property]) = Success(Circle(model("origin").getPoint, model("radius").getDouble))
}

/**
 * Circles are shapes that are formed by an origin and a radius
 * @author Mikko Hilpinen
 * @since 1.1.2017
 */
case class Circle(origin: Point, radius: Double) extends ShapeConvertible with Area2D with
        ValueConvertible with ModelConvertible with Projectable
{
    // COMPUTED PROPERTIES    ---------
    
    override def toShape = new Ellipse2D.Double(origin.x - radius, origin.y - radius, radius * 2, radius * 2)
    
    override def toValue = new Value(Some(this), CircleType)
    
    override def toModel = Model(Vector("origin" -> origin, "radius" -> radius))
    
    /**
     * The diameter of this circle, from one side to another
     */
    def diameter = radius * 2
    
    /**
     * The perimeter of this circle
     */
    def perimeter = 2 * math.Pi * radius
    
    /**
     * The area of the circle in square pixels
     */
    def area = math.Pi * radius * radius
    
    /**
      * @return The bounds around this circle
      */
    def bounds = Bounds.between(origin - (radius, radius), origin + (radius, radius))
    
    
    // OPERATORS    -------------------
    
    /**
     * Scales the circle's radius by the provided amount
     */
    def *(d: Double) = copy(radius = radius * d)
    
    /**
      * @param angle Target angle
      * @return A point on this circle's edge at the specified angle
      */
    def apply(angle: Angle) = origin + Vector3D.lenDir(radius, angle)
    
    
    // IMPLEMENTED METHODS    ---------
    
    override def contains(point: Point) = point.distanceFrom(origin) <= radius
    
    override def projectedOver(axis: Vector3D) =
    {
        val projectedOrigin = origin.toVector.projectedOver(axis).toPoint
        Line(projectedOrigin - axis.withLength(radius), projectedOrigin + axis.withLength(radius))
    }
    
    
    // OTHER METHODS    ---------------
    
    /**
     * Checks whether the sphere fully contains the provided line
     */
    def contains(line: Line): Boolean = contains(line.start) && contains(line.end)
    
    /**
     * Checks whether the circle contains the provided rectangle
     */
    def contains(poly: Polygonic): Boolean = poly.corners.forall(contains)
    
    /**
     * Checks whether the other circle is contained within this circle's area
     */
    def contains(other: Circle) = origin.distanceFrom(other.origin) <= radius - other.radius
    
    /**
     * Checks whether the two circles intersect with each other
     */
    def intersectsWith(other: Circle) = origin.distanceFrom(other.origin) <= radius + other.radius
    
    // check sphere intersection 
    // http://stackoverflow.com/questions/5048701/finding-points-of-intersection-when-two-spheres-intersect
    
    /**
     * Finds the intersection points between this and another circle
     * @return Empty vector if there is no intersection (no contact or containment), one point if 
     * there is only a single intersection point, two points otherwise
     */
    def circleIntersection(other: Circle) = 
    {
        // References: http://paulbourke.net/geometry/circlesphere/
        // Distance vector D with length of d
        val distanceVector = (other.origin - this.origin).toVector
        val distance = distanceVector.length
        
        // If there is containment (d < |r0 - r1|), there are no collision points
	    // Also, if the circles are identical, there are infinite number of collision 
		// points (they cannot be calculated)
        if (distance > radius + other.radius || distance < (radius - other.radius).abs || this == other)
        {
            Vector[Point]()
        }
        else
        {
            /* We can form triangles using points P0, P1, P2 and P3(s)
			 * Where
			 * 		P0 = Center of the first circle
			 * 		P1 = Center of the second circle
			 * 		P2 = A point at the intersection of D and a line formed by the collision points
			 * 		P3 = The collision points at each side of P2
			 * 
			 * From this we get
			 * 		a^2 + h^2 = r0^2 (Pythagoras)
			 * 		b^2 + h^2 = r1^2 (Pythagoras)
			 * 		d = a + b (P2 divides D in two)
			 * Where
			 * 		a = |P2 - P0|
			 * 		b = |P2 - P1|
			 * 		r0 = first circle radius
			 * 		r1 = second circle radius
			 * 		d = |P0 - P1|
			 * 
			 * From these we get
			 * a = (r0^2 - r1^2 + d^2) / (2*d)
			 */
            val a = (math.pow(radius, 2) - math.pow(other.radius, 2) + math.pow(distance, 2)) / (2 * distance)
            /*
			 * From this we can solve h (|P3 - P2|) with
			 * 		h^2 = r0^2 - a^2 
			 * 	->	h = sqrt(r0^2 - a^2)
			 */
            val h = math.sqrt(math.pow(radius, 2) - math.pow(a, 2))
            /*
			 * We can also solve P2 with
			 * 		P2 = P0 + a*(P1 - P0) / d
			 * 	->	P2 = P0 + D * (a / d)
			 */
            val P2 = origin + distanceVector.withLength(a)
            
            // If may be that there is only a single collision point on the distance vector
            if (h ~== 0.0)
            {
                Vector(P2)
            }
            else
            {
                /*
        		 * From we see that H (P3 - P2) is perpendicular to D and has length h.
        		 * From these we can calculate
        		 * 		P3 = P2 +- H
        		 */
                val heightVector = distanceVector.normal2D.withLength(h)
                Vector(P2 + heightVector, P2 - heightVector)
            }
        }
    }
    
    /**
     * Checks collision between two circles
     * @return if there is collision, the minimum translation vector that gets this circle out 
     * of the collision. None otherwise.
     */
    def collisionMtvWith(other: Circle): Option[Vector3D] = collisionMtvWith(other, Vector((other.origin - origin).toVector))
    
    /**
     * Calculates two collision points using a very simple algorithm. The collision points will be 
     * along the minimum translation vector and one of them is at the edge of this circle.
     * @param mtv The circle's minimum translation vector in the collision
     */
    def simpleCollisionPoints(mtv: Vector3D) = 
    {
        val firstPoint = origin - mtv.withLength(radius)
        Vector(firstPoint, firstPoint + mtv)
    }
    
    /*
     * Finds the intersection points for the circle when a minimum translation vector is known
     * @param mtv the minimum translation vector for the circle in a collision situation. The 
     * minimum translation must be <b>towards the center of the circle</b> from the collision area
     * @return the collision points in a collision with the specified minimum translation vector
     */
    // Doesn't work as it is. Maybe possible to fix?
    /*
    def collisionPoints(mtv: Vector3D) = 
    {
        // The collision points form a line that cuts the circle in two pieces
        // First finds out the vector from circle origin to where collision point line and the 
        // translation vector / line intersect
        val separatorLength = radius - mtv.length / 2
        
        if (separatorLength == 0)
        {
            // There's a special case where the collision point line runs through the circle origin
            val normal = mtv.normal2D.withLength(radius)
            Vector(origin + normal, origin - normal)
        }
        else
        {
            val separator = origin - mtv.withLength(separatorLength)
            
            // Next calculates the length from the center of the line to the collision points using 
            // Pythagoras and known radius (the collision point line is perpendicular to the separator line)
            val normalLength = math.pow(radius, 2) - math.pow(separatorLength, 2)
            val separatorNormal = separator.normal2D.withLength(normalLength)
            
            Vector(origin + separator + separatorNormal, origin + separator - separatorNormal)
        }
    }*/
}