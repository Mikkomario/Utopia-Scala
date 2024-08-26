package utopia.paradigm.shape.shape2d.area.polygon

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.util.Mutate
import utopia.paradigm.angular.DirectionalRotation
import utopia.paradigm.enumeration.RotationDirection
import utopia.paradigm.enumeration.RotationDirection.Clockwise
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}
import utopia.paradigm.shape.shape2d.area.{Area2D, Circle}
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.{LineProjectable, Matrix2D, ShapeConvertible}
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.VectorProjectable
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.paradigm.transform.Transformable

import java.awt.Shape

object Polygon
{
	// OTHER    ------------------------------
	
	/**
	  * @param p1 Point 1
	  * @param p2 Point 2
	  * @param p3 Point 3
	  * @param more Additional points
	  * @return A polygon with specified corners
	  */
	def apply(p1: Point, p2: Point, p3: Point, more: Point*): Polygon = apply(Vector(p1, p2, p3) ++ more)
	
	/**
	  * @param corners Corners of this polygon
	  * @return A polygon with those corner points
	  */
	def apply(corners: Seq[Point]): Polygon =
		if (corners.hasSize(3)) Triangle.withCorners(corners.head, corners(1), corners(2)) else _Polygon(corners)
	
	// NB: Assumes that:
	//      1) 'polygon' doesn't cross itself
	//      2) 'polygon' is of size 4 or greater
	private def convexPartsFrom(polygon: Polygon, angles: Seq[DirectionalRotation],
	                            direction: RotationDirection, originIndex: Int = 0): Seq[Polygon] =
	{
		val indexCount = angles.size
		// Converts a relative index to an actual polygon index
		def index(relativeIndex: Int) = relativeToAbsolute(originIndex, indexCount, relativeIndex)
		def isConvex(relativeIndex: Int) = this.isConvex(angles(index(relativeIndex)), direction)
		
		// Case: Origin is convex => Finds the first non-convex index and uses that as the splitting index
		if (isConvex(0))
			(1 until indexCount).find { !isConvex(_) } match {
				case Some(advanceToNonConvex) => splitFrom(polygon, index(advanceToNonConvex), angles, direction)
				
				// Case: No non-convex index was found => This polygon is convex
				case None => Single(polygon)
			}
		// Case: Origin is non-convex => Uses it as the primary splitting index
		// Case: The next index is convex => starts scanning for more indices towards that direction
		else if (isConvex(1))
			splitFrom(polygon, originIndex, angles, direction, splitDirection = Positive)
		// Case: Two consecutive non-convex indices to the positive direction
		//       => Moves to the negative (i.e. default) direction instead
		else if (isConvex(-1))
			splitFrom(polygon, originIndex, angles, direction)
		// Case: Three consecutive non-convex indices
		//       => Finds the next convex index and places the primary split index next to that
		//          (instead of at the origin), so that the first index after the split will be convex
		else
			(2 until indexCount).find(isConvex) match {
				case Some(advanceToConvex) =>
					// splitFrom(polygon, originIndex, rotations, direction, advanceToConvex - 1, Positive)
					splitFrom(polygon, index(advanceToConvex - 1), angles, direction, splitDirection = Positive)
				
				// Case: All the rotations are "non-convex" => Indicates a miscalculated rotation direction (error)
				case None =>
					throw new IllegalArgumentException(
						s"Rotation direction of polygon $polygon is not $direction, as claimed")
			}
	}
	
	// 'splitDirection' is the direction from origin, towards which the secondary split index is estimated to be.
	// Should be on the opposite side of the primary split index, relative to the origin.
	// NB: Doesn't work if the first index in the 'splitDirection' is non-convex
	// TODO: In some semi-rare circumstances, there's still the possibility to encounter crossing, which messes up this logic
	private def splitFrom(polygon: Polygon, originIndex: Int, angles: Seq[DirectionalRotation],
	                      polygonDirection: RotationDirection,
	                      relativeSplitIndex: Int = 0, splitDirection: Sign = Negative) =
	{
		val indexCount = angles.size
		def index(relativeIndex: Int) = relativeToAbsolute(originIndex, indexCount, relativeIndex)
		
		lazy val absoluteSplitIndex = index(relativeSplitIndex)
		lazy val splitPoint = polygon.corners(index(relativeSplitIndex))
		// Reference point is the next point from the split point
		// towards direction to that is being tested / iterated
		lazy val referencePoint = polygon.corners(index(relativeSplitIndex + splitDirection.modifier))
		lazy val referenceDirection = (referencePoint - splitPoint).direction
		
		// Finds the first index from the origin, where either:
		//      1) That index is non-convex
		//      2) Angle at the split point, when joined to that index, would become larger than 180 degrees
		//          - In these cases, the previous index is selected instead
		//            (i.e. would select the last possible index which keeps the angle low enough)
		// Note: The index for triggering case 2 is always at least 3 steps away from the splitting point,
		//       as the resulting shape will always contain at least 4 corners.
		//       Also, the two splitting indices are never consecutive, meaning that absolute minimum advance is 2.
		val minimumAdvanceForAngle = (3 - relativeSplitIndex.abs) min 2
		(2 until indexCount).findMap { advance =>
			val absoluteIndex = index(splitDirection * advance)
			
			// Case: Found a non-convex index => Splits at that
			if (!isConvex(angles(absoluteIndex), polygonDirection))
				Some(Left(absoluteIndex))
			else if (advance >= minimumAdvanceForAngle) {
				val targetPoint = polygon.corners(absoluteIndex)
				val angle = (targetPoint - splitPoint).direction - referenceDirection
				
				// Case: Angle is still smaller than 180 degrees => Moves on to the next index
				if (angle.isZero || angle.direction == polygonDirection * splitDirection)
					None
				// Case: Angle became larger than 180 degrees => Returns the previous step
				else
					Some(Right(advance - 1))
			}
			// Case: Neither condition is fulfilled => Moves on to the next index
			else
				None
		} match {
			// Case: The other split point was found
			//       => Separates the area between these points as a convex polygons
			//          and moves on to process the remainder
			case Some(otherSplitIndex) =>
				val absoluteOtherSplitIndex = otherSplitIndex.leftOrMap { advance => index(splitDirection * advance) }
				val cutOffRange = splitDirection match {
					case Positive => NumericSpan(absoluteSplitIndex, absoluteOtherSplitIndex)
					case Negative => NumericSpan(absoluteOtherSplitIndex, absoluteSplitIndex)
				}
				val cutOffAndRemainder = {
					val ascendingRange = cutOffRange.ascending
					val ascendingRangePolygon = apply(polygon.corners.slice(ascendingRange))
					val outsideAscendingRangePolygon = apply(polygon.corners.slice(ascendingRange.end, indexCount) ++
						polygon.corners.slice(0, ascendingRange.start + 1))
					
					if (cutOffRange.isAscending)
						Pair(ascendingRangePolygon, outsideAscendingRangePolygon)
					else
						Pair(outsideAscendingRangePolygon, ascendingRangePolygon)
				}
				
				// Case: The remainder is small enough to always be convex => Finishes
				if (cutOffAndRemainder.second.corners.hasSize <= 4)
					cutOffAndRemainder
				// Case: The remainder may be non-convex => Splits it, if necessary
				else {
					// If the secondary split was made at a non-convex index, selects that as the next origin
					val nextOrigin = polygon.corners(
						if (otherSplitIndex.isLeft) absoluteOtherSplitIndex else absoluteSplitIndex)
					cutOffAndRemainder.flatMapSecond { p =>
						convexPartsFrom(p, p.rotations, polygonDirection, p.corners.indexOf(nextOrigin))
					}
				}
			
			// Case: No non-convex point was found => The remainder is convex
			case None => Single(polygon)
		}
	}
	
	private def relativeToAbsolute(originIndex: Int, indexCount: Int, relativeIndex: Int) = {
		indexInRange(originIndex + relativeIndex, indexCount)
	}
	private def indexInRange(index: Int, indexCount: Int) = {
		if (index >= indexCount)
			index - indexCount
		else if (index < 0)
			indexCount + index
		else
			index
	}
	
	private def isConvex(angle: DirectionalRotation, polygonDirection: RotationDirection) =
		angle.isZero || angle.direction == polygonDirection
	
	
	// NESTED   ------------------------------
	
	private case class _Polygon(corners: Seq[Point]) extends Polygon
	{
		// ATTRIBUTES	--------------------
		
		// Some more calculation extensive operations are cached in lazy variables
		override lazy val sides = super.sides
		override lazy val rotationDirection = super.rotationDirection
		override lazy val isConvex = super.isConvex
		override lazy val collisionAxes = super.collisionAxes
		override lazy val convexParts = super.convexParts
		override lazy val center = super[Polygon].center
	}
}

/**
  * This trait is extended by 2D shapes that have 3 or more corners
  * @author Mikko Hilpinen
  * @since Genesis 14.4.2019, v2+
  */
trait Polygon
	extends ShapeConvertible with LineProjectable with Transformable[Polygon] with HasBounds with Area2D
		with MaybeEmpty[Polygon] with Combinable[HasDoubleDimensions, Polygon]
{
	// ABSTRACT	----------------
	
	/**
	  * @return The corners of this shape, ordered
	  */
	def corners: Seq[Point]
	
	
	// COMPUTED	----------------
	
	/**
	  * @return The sides of this shape as lines, in order
	  */
	def sides = sidesIterator.toOptimizedSeq
	/**
	  * @return An iterator that returns the sides of this shape as lines, in order
	  */
	def sidesIterator = corners.notEmpty match {
		case Some(corners) => corners.iterator.pairedTo(corners.head).map { Line(_) }
		case None => Iterator.empty
	}
	/**
	  * @return The edges of this shape in order. Same as sides, except in vector form
	  */
	def edges = edgesIterator.toOptimizedSeq
	/**
	  * @return An iterator that returns the edges of this shape in order.
	  *         Same as sidesIterator, except in vector form
	  */
	def edgesIterator = sidesIterator.map { _.vector }
	/**
	  * @return The rotations at each corner of this shape.
	  *         The first rotation is the one happening in corner / vertex at index 0
	  *         and the last rotation is the one happening at the last corner / vertex.
	  *         The resulting collection is of equal size to the number of corners.
	  *
	  *         Rotations are different from corner [[angles]],
	  *         in that they represent how much each side turns at each corner.
	  *         E.g. If we have a corner of almost 180 degrees, the rotation at that corner is close to 0.
	  * @see [[angles]]
	  */
	def rotations = sides.notEmpty match {
		case Some(sides) =>
			val directions = sides.map { _.direction }
			directions.pairedFrom(directions.last).map { _.merge { (incoming, outgoing) => outgoing - incoming } }
		case None => Empty
	}
	/**
	  * @return The angles at each corner of this shape, starting from the corner at vertex 0.
	  *         Angles are always less than 180 degrees, also containing directional information.
	  *
	  *         If you want to acquire the angles inside this polygon,
	  *         convert these angles to this polygon's [[rotationDirection]].
	  *         If you want the angles outside this polygon,
	  *         convert them to the direction opposite to this polygon's rotation direction.
	  *
	  *         Please also note that angles are different from [[rotations]].
	  *         An angle of near 180 degrees would correspond to a rotation of nearly 0.
	  */
	def angles = sides.notEmpty match {
		case Some(sides) =>
			val directions = sides.map { _.direction }
			directions.pairedFrom(directions.last)
				.map { _.merge { (incoming, outgoing) => incoming.opposite - outgoing } }
		case None => Empty
	}
	
	/**
	  * @return The rotation direction of this Polygon shape (whether the corners of this polygon are listed in
	  *         clockwise or counterclockwise order)
	  */
	def rotationDirection = rotations.reduceOption { _ + _ }.map { _.direction }.getOrElse(Clockwise)
	
	/**
	  * Whether this polygon is convex. Convex polygons only need to turn clockwise or
	  * counter-clockwise when traversing through the polygon. They don't have holes or dips, so to speak.
	  */
	def isConvex = {
		lazy val dir = rotationDirection
		angles.forall { r => r.isZero || r.direction == dir }
	}
	
	/**
	  * @return The total surface area of this polygon
	  */
	// Calculates polygon area by splitting it to triangles and summing the area of those
	// NB: Relies on an override in Triangle
	def area: Double = toTriangles.view.map { _.area }.sum
	
	/**
	  * @return The collision axes that should be considered when testing this instance
	  */
	def collisionAxes: Seq[Vector2D] = edges.distinctWith { _ isParallelWith _ }.map { _.normal2D }
	
	/**
	  * @return The length of the longest edge in this polygon
	  */
	def maxEdgeLength = edgesIterator.map { _.length }.maxOption.getOrElse(0.0)
	/**
	  * @return The length of the shortest edge in this polygon
	  */
	def minEdgeLength = edgesIterator.map { _.length }.minOption.getOrElse(0.0)
	
	/**
	  * The smallest possible circle that contains all the vertices in this polygon
	  */
	def circleAround = {
		val origin = center
		val radius = corners.view.map { c => (c - origin).length }.max
		Circle(origin, radius)
	}
	/**
	  * The largest possible circle that fits inside this polygon
	  */
	def circleInside = {
		val origin = center
		val radius = sidesIterator.map { side => (side.center - origin).length }.min
		Circle(origin, radius)
	}
	
	/**
	  * @return This polygon as a collection of triangles.
	  *         Empty if this polygon contained 2 or fewer corners.
	  */
	// First must convert this polygon into its convex parts
	def toTriangles = convexParts.flatMap { polygon =>
		val corners = polygon.corners
		// Case: Polygon with less than 3 corners => Can't form a single triangle
		if (corners.hasSize < 3)
			Empty
		// Case: Already a triangle
		else if (corners.hasSize(3))
			Single(Triangle.withCorners(corners.head, corners(1), corners(2)))
		// Case: 4 or more corners => Splits into triangles by connecting other vertices to a single "anchor" vertex
		else {
			val anchor = corners.head
			corners.tail.paired.map { others => Triangle.withCorners(anchor, others.first, others.second) }
		}
	}
	
	/**
	  * @return This polygon divided into convex polygons.
	  *         If this polygon is convex already, returns self.
	  */
	def convexParts: Seq[Polygon] = {
		val c = corners
		// Case: 3 or fewer corners => Always convex, without there existing a possibility for crossing either
		if (c.hasSize <= 3)
			Single(this)
		else {
			// Tests whether there exists crossing within this polygon
			val lines = sides
			val lineCount = lines.size
			(0 until (lineCount - 2)).findMap { startIndex =>
				val line1 = lines(startIndex)
				val lastTargetIndex = if (startIndex == 0) lineCount - 2 else lineCount - 1
				((startIndex + 2) to lastTargetIndex).reverseIterator.findMap { otherIndex =>
					val line2 = lines(otherIndex)
					line1.intersection(line2).map { intersectionPoint =>
						(startIndex, otherIndex, intersectionPoint)
					}
				}
			} match {
				// Case: There exists crossing within this polygon => Splits at the intersection point
				case Some((firstLineIndex, secondLineIndex, intersectionPoint)) =>
					// This first polygon certainly doesn't cross itself
					val part1 = Polygon((corners.take(firstLineIndex + 1) :+ intersectionPoint) ++
						corners.drop(secondLineIndex + 1))
					// This second polygon may still cross itself
					val part2 = Polygon(intersectionPoint +: corners.slice(firstLineIndex + 1, secondLineIndex + 1))
					
					part1._convexParts ++ part2.convexParts
				
				// Case: No crossing exists
				case None => _convexParts
			}
		}
	}
	// This version assumes that crossing has been tested and ruled out and that this polygon has 4 or more corners
	private def _convexParts = Polygon.convexPartsFrom(this, angles, rotationDirection)
	
	
	// IMPLEMENTED	------------
	
	override def self: Polygon = this
	override def identity = this
	
	override def isEmpty: Boolean = corners.isEmpty
	
	/**
	  * @return The center point of this shape
	  */
	override def center = {
		// Converts this polygon into a set of triangles
		// and calculates the weighed average of those triangles' center points, where weights are triangle areas
		val c = corners
		if (c.hasSize <= 3)
			Point.average(c)
		else
			Point.weighedAverage(toTriangles.map { t => t.center -> t.area })
	}
	
	/**
	  * @return The bounds around this Polygon instance
	  */
	override def bounds = {
		val c = corners
		val topLeft = Point.topLeft(c)
		val bottomRight = Point.bottomRight(c)
		Bounds.between(topLeft, bottomRight)
	}
	
	override def toShape: Shape = {
		val c = corners
		val x = c.view.map { _.x.round.toInt }.toArray
		val y = c.view.map { _.y.round.toInt }.toArray
		
		new java.awt.Polygon(x, y, c.size)
	}
	
	override def toString = s"[${ corners.mkString(", ") }]"
	
	override def projectedOver(axis: DoubleVector) =
		Line(corners.view.map { _.toVector.projectedOver(axis).toPoint }.minMax)
	
	override def transformedWith(transformation: Matrix3D): Polygon = map { transformation(_).toPoint }
	override def transformedWith(transformation: Matrix2D): Polygon = map { transformation(_).toPoint }
	
	override def +(other: HasDoubleDimensions): Polygon = map { _ + other }
	
	override def contains(point: DoubleVector): Boolean = contains(point: VectorProjectable[HasDoubleDimensions])
	
	
	// OTHER	---------------
	
	/**
	  * @param index Index of the vertex (may even be negative or out of bounds, in which case loops around)
	  * @return A vertex (corner) of this Polygon instance from the specified index
	  */
	def vertex(index: Int) = {
		val c = corners
		if (index >= 0)
			c(index % c.size)
		else
			c(c.size + (index % c.size))
	}
	/**
	  * @param index Index of the starting vertex
	  * @return A side of this Polygon instance starting from the specified index
	  */
	def side(index: Int) = Line(vertex(index), vertex(index + 1))
	/**
	  * @param index index of the starting vertex
	  * @return An edge (same as side, except vector) of this Polygon shape starting from the specified index
	  */
	def edge(index: Int) = side(index).vector
	
	/**
	  * @param index Vertex index
	  * @return The two sides that are connected to the specified vertex, except that both will start from the specified vertex
	  */
	def sidesFrom(index: Int) = {
		val start = vertex(index)
		Line(start, vertex(index - 1)) -> Line(start, vertex(index + 1))
	}
	/**
	  * @param index Vertex index
	  * @return The 2 sides that are connected to the specified vertex
	  */
	def sidesConnectedTo(index: Int) = side(index - 1) -> side(index)
	
	/**
	  * @param index Index of the corner
	  * @return The rotation at the specified corner of this Polygon shape
	  */
	def rotation(index: Int) = {
		val v0 = vertex(index - 1)
		val v1 = vertex(index)
		val v2 = vertex(index + 1)
		
		Line(v1, v2).direction - Line(v0, v1).direction
	}
	
	/**
	  * @param index Index of the corner
	  * @return The angle at the specified corner of this Polygon shape
	  */
	def angle(index: Int) = rotation(index).toAngle
	
	def contains(point: VectorProjectable[HasDoubleDimensions]) =
		collisionAxes.forall { containsProjection(point, _) }
	
	/**
	  * Calculates the minimum translation vector that would get these two projectable shapes out of
	  * a collision situation
	  * @param other another Polygon instance
	  * @return The minimum translation vector that gets these two shapes out of a collision situation
	  * or none if there is no collision
	  */
	def collisionMtvWith(other: Polygon): Option[Vector2D] = collisionMtvWith(other,
		(collisionAxes ++ other.collisionAxes).distinctWith { _ isParallelWith _ })
	
	/**
	  * Slices this polygon to two pieces. The cut is made between the two vertices so that both
	  * polygon pieces will contain those vertices.
	  * @param index1 The index of the first common index (< index2 - 1)
	  * @param index2 The index of the second common index (> index 1 + 1)
	  * @return Two polygon pieces as a pair
	  */
	def cutBetween(index1: Int, index2: Int) = {
		val c = corners
		val cutVertices = c.slice(index1, index2 + 1)
		val remainingVertices = c.take(index1 + 1) ++ c.drop(index2)
		Pair(Polygon(remainingVertices), Polygon(cutVertices))
	}
	
	/**
	  * @param f A mapping function applied to all corners of this polygon
	  * @return Copy of this polygon with mapped corners
	  */
	def map(f: Mutate[Point]): Polygon = Polygon(corners.map(f))
	
	/**
	  * Returns a copy of this polygon with the specified rotation direction
	  */
	def withRotationDirection(direction: RotationDirection) =
		if (rotationDirection == direction) this else Polygon(corners.reverse)
}
