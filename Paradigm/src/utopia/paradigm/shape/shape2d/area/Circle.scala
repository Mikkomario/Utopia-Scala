package utopia.paradigm.shape.shape2d.area

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.Mutate
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.generic.ParadigmDataType.CircleType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}
import utopia.paradigm.shape.shape2d.area.polygon.{Polygon, Polygonic}
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.{LineProjectable, ShapeConvertible}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.paradigm.transform.LinearSizeAdjustable

import java.awt.geom.Ellipse2D
import scala.util.Success

object Circle extends FromModelFactory[Circle]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A circle with zero radius and origin at (0,0)
	  */
	lazy val zero = Circle(Point.origin, 0.0)
	
	
	// IMPLEMENTED  -----------------------
	
	override def apply(model: template.ModelLike[Property]) =
		Success(Circle(model("origin").getPoint, model("radius").getDouble))
	
	
	// OTHER    --------------------------
	
	/**
	  * Creates a circle that just fits within the specified bounds
	  * @param bounds Bounds within which the resulting circle is placed
	  * @return A circle placed at the center of the specified bounds,
	  *         filling as large an area as possible without exiting the bounds.
	  */
	def within(bounds: Bounds) = apply(bounds.center, bounds.size.minDimension / 2)
	
	/**
	  * @param a First point to enclose
	  * @param b Second point to enclose
	  * @return A circle which just encloses these two points (both will lie at the circle edge)
	  */
	def enclosing(a: DoubleVector, b: DoubleVector): Circle = {
		// Determines the radius vector and uses that to determine the origin and the radius length
		val r = (b - a) / 2.0
		Circle((a + r).toPoint, r.length)
	}
	/**
	  * @param a First point to enclose
	  * @param b Second point to enclose
	  * @param c Third point to enclose
	  * @return Smallest circle which encloses all the specified 3 points
	  */
	def enclosing(a: DoubleVector, b: DoubleVector, c: DoubleVector) =
		enclosing3OrLess(Vector(a, b, c).distinct)
	/**
	  * Creates the smallest possible circle which encloses the specified n points
	  * @param points Points to enclose
	  * @return A circle which encloses the specified n points
	  */
	def enclosing(points: Seq[DoubleVector]): Circle = {
		val distinctPoints = points.distinct
		if (distinctPoints.hasSize <= 3)
			enclosing3OrLess(distinctPoints)
		else
			_enclosing(distinctPoints, Empty, 0)
	}
	
	/**
	  * Finds the smallest circle that contains all the specified circles
	  * @param circles Circles that need to be contained within the resulting circle
	  * @param errorMargin Largest allowed error.
	  *                    Smaller values will require more iterations while larger values may yield inexact results.
	  *                    Default = Epsilon = Very very small.
	  * @return The smallest circle which contains all of the specified circles.
	  */
	def enclosingCircles(circles: Iterable[Circle], errorMargin: Double = 1e-6) = {
		if (circles.isEmpty)
			zero
		else if (circles.hasSize(1))
			circles.head
		else {
			// Places a circle over the weighed centroid of the proposed circles and iterates
			// in order to adjust the radius and the origin
			// Initial radius is the radius of the largest circle
			Iterator
				.iterate(Circle(weighedCentroidOf(circles),
					circles.view.map { _.radius }.max) -> false) { case (proposed, _) =>
					// Adjusts the proposed circle for each circle value
					circles
						.foldLeft(proposed -> true) { case ((proposed, wasValid), circle) =>
							val relativeOrigin = circle.origin - proposed.origin
							val maxDistanceFromOrigin = relativeOrigin.length + circle.radius
							
							// Case: Circle is not enclosed => Adjusts the enclosing circle and tries again
							if (maxDistanceFromOrigin > proposed.radius) {
								val adjustment = relativeOrigin.withLength((maxDistanceFromOrigin - proposed.radius) / 2)
								val adjustedOrigin = proposed.origin + adjustment
								val adjusted = Circle(adjustedOrigin,
									(circle.origin - adjustedOrigin).length + circle.radius)
								
								adjusted -> (wasValid && (adjustment.length < errorMargin))
							}
							// Case: Circle is enclosed => Continues
							else
								proposed -> wasValid
						}
				}
				.find { _._2 }.get._1
		}
	}
	
	/**
	  * Calculates the weighed centroid (i.e. a weighed central position) or the specified circles
	  * @param circles A collection of circles. Not empty.
	  * @throws UnsupportedOperationException if the specified collection is empty
	  * @return Weighed central position of these circles
	  */
	@throws[UnsupportedOperationException]("If 'circles' is empty")
	def weighedCentroidOf(circles: Iterable[Circle]) =
		circles.view.map { c => c.origin * c.radius }.reduce { _ + _ } / circles.view.map { _.radius }.sum
	
	private def _enclosing(points: Seq[DoubleVector], perimeterPoints: Seq[DoubleVector], processed: Int): Circle =
	{
		// Returns once all points have been processed or when 3 perimeter points have been identified
		if (points.hasSize(processed) || perimeterPoints.size == 3) {
			println("All points have been processed, or 3 perimeter points acquired => Encloses 3 or less points")
			println(s"Perimeter: ${ perimeterPoints.mkString(", ") }")
			enclosing3OrLess(perimeterPoints)
		}
		else {
			println(s"\n${ points.size - processed } points remain to be processed. Current perimeter = ${ perimeterPoints.size } points")
			
			// Creates a circle without a specific point and checks whether it will be consequently left outside
			// (Uses recursion to get the circle)
			val testedCircle = _enclosing(points, perimeterPoints, processed + 1)
			val testedPoint = points(processed)
			
			println(s"Tests whether $testedPoint fits within $testedCircle")
			
			// Case: The tested point is contained within the circle => No modification needed
			if (testedCircle.contains(testedPoint)) {
				println("Yes => Proceed")
				testedCircle
			} // Case: The tested point is outside the circle => It shall be considered a perimeter point
			else {
				println(s"No => Adds new perimeter point (now ${ perimeterPoints.size }) and moves to the next point (${ processed + 2 }/${ points.size })")
				_enclosing(points, perimeterPoints :+ testedPoint, processed + 1)
			}
		}
	}
	
	// Assumes that the size of 'points' is 3 or less
	private def enclosing3OrLess(points: Seq[DoubleVector]): Circle = points.size match {
		case 0 =>
			println("Enclosing 0 points => Returns zero Circle")
			zero
		case 1 =>
			println(s"Enclosing 1 point (${ points.head }) => Returns 0 radius circle")
			Circle(points.head.toPoint, 0.0)
		case 2 =>
			println("Enclosing 2 points")
			enclosing(points.head, points(1))
		case _ =>
			println("Enclosing 3 points")
			_enclosing3(points)
	}
	
	// Assumes that 'points' contains exactly 3 items
	private def _enclosing3(points: Seq[DoubleVector]): Circle = {
		// Checks whether this circle may be defined with only 2 points instead
		points.indices
			.findMap { removedIndex =>
				val remaining = points.withoutIndex(removedIndex)
				Some(enclosing(remaining.head, remaining(1))).filter { _.contains(points(removedIndex)) }
			}
			// If not, encloses the three points instead
			.getOrElse { _enclosing3(points.head, points(1), points(2)) }
	}
	
	// NB: Doesn't optimize by testing if can enclose 2 instead (use _enclosing3(Seq) for that)
	private def _enclosing3(a: DoubleVector, b: DoubleVector, c: DoubleVector): Circle = {
		// Calculates A-B and A-C vectors and their dot products
		val ab = b - a
		val ac = c - a
		val dotAb = ab doubleDot ab
		val dotAc = ac doubleDot ac
		// Uses these to calculate A-O vector, which is the vector from A to the circle origin O
		val d2 = (ab.x * ac.y - ab.y * ac.x) * 2.0
		val ao = Vector2D((ac.y * dotAb - ab.y * dotAc) / d2, (ab.x * dotAc - ac.x * dotAb) / d2)
		val origin = a + ao
		
		// Uses this information to form the circle
		Circle(origin.toPoint, ao.length)
	}
}

/**
  * Circles are shapes that are formed by an origin and a radius
  * @author Mikko Hilpinen
  * @since Genesis 1.1.2017
  */
case class Circle(origin: Point = Point.origin, radius: Double)
	extends ShapeConvertible with Area2D with ValueConvertible with ModelConvertible with LineProjectable
		with LinearSizeAdjustable[Circle] with Combinable[HasDoubleDimensions, Circle] with HasBounds
{
	// COMPUTED PROPERTIES    ---------
	
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
	
	
	// OPERATORS    -------------------
	
	/**
	  * @param angle Target angle
	  * @return A point on this circle's edge at the specified angle
	  */
	def apply(angle: Angle) = origin + Vector2D.lenDir(radius, angle)
	
	
	// IMPLEMENTED METHODS    ---------
	
	override def self = this
	
	/**
	  * @return The bounds around this circle
	  */
	override def bounds = Bounds.between(origin - Vector2D(radius, radius), origin + Vector2D(radius, radius))
	
	override def toShape = new Ellipse2D.Double(origin.x - radius, origin.y - radius, radius * 2, radius * 2)
	
	override def toValue = new Value(Some(this), CircleType)
	
	override def toModel = Model(Pair("origin" -> origin, "radius" -> radius))
	
	override def contains(point: DoubleVector) = point.distanceFrom(origin) <= radius
	
	override def projectedOver(axis: DoubleVector) = {
		val projectedOrigin = origin.projectedOver(axis)
		val radiusVector = axis.withLength(radius)
		Line(projectedOrigin - radiusVector, projectedOrigin + radiusVector)
	}
	
	def -(another: HasDoubleDimensions) = copy(origin = origin - another)
	
	/**
	  * Scales the circle's radius by the provided amount
	  */
	override def *(d: Double) = copy(radius = radius * d)
	
	/**
	  * @param translation A translation amount
	  * @return A copy of this circle where the origin has been translated by specified amount
	  */
	override def +(translation: HasDoubleDimensions) = copy(origin = origin + translation)
	
	
	// OTHER METHODS    ---------------
	
	/**
	  * @param newOrigin New origin to assign
	  * @return Copy of this circle with the specified origin
	  */
	def withOrigin(newOrigin: Point) = copy(origin = newOrigin)
	/**
	  * @param newRadius New radius to assign
	  * @return Copy of this circle with the specified radius
	  */
	def withRadius(newRadius: Double) = copy(radius = newRadius)
	
	/**
	  * @param f A mapping function applied to this circle's origin coordinates
	  * @return Copy of this circle with mapped origin coordinates
	  */
	def mapOrigin(f: Mutate[Point]) = withOrigin(f(origin))
	/**
	  * @param f A mapping function applied to this circle's radius
	  * @return Copy of this circle with mapped radius
	  */
	def mapRadius(f: Mutate[Double]) = withRadius(f(radius))
	
	/**
	  * Checks whether the sphere fully contains the provided line
	  */
	def contains(line: Line): Boolean = contains(line.start) && contains(line.end)
	
	/**
	  * Checks whether the circle contains the provided rectangle
	  */
	def contains(poly: Polygonic): Boolean = poly.corners.forall { contains(_) }
	
	/**
	  * Checks whether the other circle is contained within this circle's area
	  */
	def contains(other: Circle) = origin.distanceFrom(other.origin) <= radius - other.radius
	
	/**
	  * Converts this circle to a "circular" polygon, such as a hexagon
	  * @param sidesCount Number of sides in the resulting polygon
	  * @param startAngle Angle of the first corner (default = 270 degrees = up)
	  * @return A polygon based on this circle
	  */
	def toPolygon(sidesCount: Int, startAngle: Angle = Angle.up) = {
		val increment = Rotation.clockwise.circle / sidesCount
		Polygon(Iterator.iterate(startAngle) { _ + increment }.take(sidesCount - 1)
			.map { angle => origin + Vector2D.lenDir(radius, angle) }.toVector)
	}
	
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
	def circleIntersection(other: Circle) = {
		// References: http://paulbourke.net/geometry/circlesphere/
		// Distance vector D with length of d
		val distanceVector = (other.origin - this.origin).toVector
		val distance = distanceVector.length
		
		// If there is containment (d < |r0 - r1|), there are no collision points
		// Also, if the circles are identical, there are infinite number of collision
		// points (they cannot be calculated)
		if (distance > radius + other.radius || distance < (radius - other.radius).abs || this == other)
			Empty
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
				Single(P2)
			else {
				/*
				 * From we see that H (P3 - P2) is perpendicular to D and has length h.
				 * From these we can calculate
				 * 		P3 = P2 +- H
				 */
				val heightVector = distanceVector.normal2D.withLength(h)
				Pair(P2 + heightVector, P2 - heightVector)
			}
		}
	}
	
	/**
	  * Checks collision between two circles
	  * @return if there is collision, the minimum translation vector that gets this circle out
	  * of the collision. None otherwise.
	  */
	def collisionMtvWith(other: Circle): Option[Vector2D] =
		collisionMtvWith(other, Single((other.origin - origin).toVector))
	
	/**
	  * Calculates two collision points using a very simple algorithm. The collision points will be
	  * along the minimum translation vector and one of them is at the edge of this circle.
	  * @param mtv The circle's minimum translation vector in the collision
	  */
	def simpleCollisionPoints(mtv: Vector2D) = {
		val firstPoint = origin - mtv.withLength(radius)
		Pair(firstPoint, firstPoint + mtv)
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