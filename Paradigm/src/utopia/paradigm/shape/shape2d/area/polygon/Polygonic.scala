package utopia.paradigm.shape.shape2d.area.polygon

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.paradigm.enumeration.RotationDirection.Clockwise
import utopia.paradigm.shape.shape2d.area.{Area2D, Circle, polygon}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}
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

/**
  * This trait is extended by 2D shapes that have 3 or more corners
  * @author Mikko Hilpinen
  * @since Genesis 14.4.2019, v2+
  */
// TODO: Handle cases where there are 0 corners
trait Polygonic extends ShapeConvertible with LineProjectable with Transformable[Polygonic] with HasBounds with Area2D
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
	def sides = {
		val c = corners
		if (c.isEmpty)
			Empty
		else
			(c :+ c.head).paired.map { Line(_) }
	}
	/**
	  * @return The edges of this shape in order. Same as sides, except in vector form
	  */
	def edges = sides.map { _.vector }
	/**
	  * @return The rotations at each corner of this shape.
	  *         The first rotation is the one happening in corner / vertex at index 0
	  *         and the last rotation is the one happening at the last corner / vertex.
	  *         The resulting collection is of equal size to the number of corners.
	  */
	def rotations = {
		val e = sides
		if (e.nonEmpty)
			(e.last +: e).paired.map { p => p.second.direction - p.first.direction }
		else
			Empty
	}
	/**
	  * @return The angles at each corner of this shape
	  */
	def angles = rotations.map { _.toAngle }
	
	/**
	  * @return The rotation direction of this polygonic shape (whether the corners of this polygon are listed in
	  *         clockwise or counterclockwise order)
	  */
	def rotationDirection = rotations.reduceOption { _ + _ }.map { _.direction } getOrElse Clockwise
	
	/**
	  * Whether this polygon is convex. Convex polygons only need to turn clockwise or
	  * counter-clockwise when traversing through the polygon. They don't have holes or dips, so to speak.
	  */
	def isConvex = {
		val dir = rotationDirection
		rotations.forall { r => r.nonZero && r.direction == dir }
	}
	
	/**
	  * @return The collision axes that should be considered when testing this instance
	  */
	def collisionAxes: Seq[Vector2D] = edges.distinctWith { _ isParallelWith _ }.map { _.normal2D }
	
	/**
	  * @return The length of the longest edge in this polygon
	  */
	def maxEdgeLength = edges.map { _.length }.maxOption.getOrElse(0.0)
	/**
	  * @return The length of the shortest edge in this polygon
	  */
	def minEdgeLength = edges.map { _.length }.minOption.getOrElse(0.0)
	
	/**
	  * The smallest possible circle that contains all the vertices in this polygon
	  */
	def circleAround = {
		val origin = center
		val radius = corners.map { c => (c - origin).length }.max
		Circle(origin, radius)
	}
	/**
	  * The largest possible circle that fits inside this polygon
	  */
	def circleInside = {
		val origin = center
		val radius = corners.map { vertex => (vertex - origin).length }.min
		Circle(origin, radius)
	}
	
	/**
	  * Divides this polygon into convex portions. Each of the returned parts is convex and can
	  * be used in collision checks
	  */
	def convexParts: Seq[Polygonic] = {
		val c = corners
		if (c.size < 3)
			Single(this)
		else {
			val dir = rotationDirection
			val r = rotations
			
			// Checks whether there are any spots within this polygon where the rotation direction changes
			// (indicates non-convexity)
			r.findIndexWhere { r => r.nonZero && r.direction != dir } match {
				case Some(firstBrokenIndex) =>
					// Tries to find another (non-sequential) broken index
					val secondBrokenIndex = {
						if (firstBrokenIndex < c.size - 1)
							Some(rotations.indexWhere({ r => r.nonZero && r.direction != dir }, firstBrokenIndex + 1))
								.filter { _ >= 0 }
						else
							None
					}
					
					secondBrokenIndex match {
						// Case: Second broken index was found =>
						// Cuts this polygon between these two indices and continues recursively
						case Some(secondBrokenIndex) =>
							val (firstPart, secondPart) = cutBetween(firstBrokenIndex, secondBrokenIndex)
							firstPart.convexParts ++ secondPart.convexParts
						// Case: Only one broken index found =>
						// Cuts this polygon at that location so that the remaining parts become convex
						case None =>
							val brokenVertex = vertex(firstBrokenIndex)
							val incomeAngle = side(firstBrokenIndex - 1).direction
							
							val remainingOutcomeIndex = {
								if (firstBrokenIndex < c.size - 2)
									c.indexWhere({ vertex =>
										val rotation = Line(brokenVertex, vertex).direction - incomeAngle
										rotation.isZero || rotation.direction == rotationDirection
									}, firstBrokenIndex + 2)
								else
									-1
							}
							
							if (remainingOutcomeIndex >= 0) {
								val (firstPart, secondPart) = cutBetween(firstBrokenIndex, remainingOutcomeIndex)
								firstPart.convexParts ++ secondPart.convexParts
							}
							else {
								// WET WET (needs refactoring)
								val outcomeIndex = c.indexWhere { vertex =>
									val rotation = Line(brokenVertex, vertex).direction - incomeAngle
									rotation.isZero || rotation.direction == rotationDirection
								}
								val (firstPart, secondPart) = cutBetween(outcomeIndex, firstBrokenIndex)
								firstPart.convexParts ++ secondPart.convexParts
							}
					}
				case None =>
					Single(this)
			}
		}
	}
	
	
	// IMPLEMENTED	------------
	
	override def identity = this
	
	/**
	  * @return The center point of this shape
	  */
	override def center = {
		val c = corners
		if (c.isEmpty)
			Point.origin
		else {
			val total = c.reduce { _ + _ }
			total / c.size
		}
	}
	
	/**
	  * @return The bounds around this polygonic instance
	  */
	override def bounds = {
		val c = corners
		val topLeft = Point.topLeft(c)
		val bottomRigth = Point.bottomRight(c)
		
		Bounds(topLeft, (bottomRigth - topLeft).toSize)
	}
	
	override def toShape: Shape = {
		val c = corners
		val x = c.map { _.x.round.toInt }.toArray
		val y = c.map { _.y.round.toInt }.toArray
		
		new java.awt.Polygon(x, y, c.size)
	}
	
	override def projectedOver(axis: DoubleVector) = {
		val projectedCorners = corners.map { _.toVector.projectedOver(axis).toPoint }
		val start = projectedCorners.min
		val end = projectedCorners.max
		Line(start, end)
	}
	
	override def transformedWith(transformation: Matrix3D): Polygonic =
		Polygon(corners.map { transformation(_).toPoint }.toVector)
	override def transformedWith(transformation: Matrix2D): Polygonic =
		polygon.Polygon(corners.map { transformation(_).toPoint }.toVector)
	
	override def contains(point: DoubleVector): Boolean = contains(point: VectorProjectable[HasDoubleDimensions])
	
	
	// OTHER	---------------
	
	/**
	  * @param index Index of the vertex (may even be negative or out of bounds, in which case loops around)
	  * @return A vertex (corner) of this polygonic instance from the specified index
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
	  * @return A side of this polygonic instance starting from the specified index
	  */
	def side(index: Int) = Line(vertex(index), vertex(index + 1))
	
	/**
	  * @param index index of the starting vertex
	  * @return An edge (same as side, except vector) of this polygonic shape starting from the specified index
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
	  * @return The rotation at the specified corner of this polygonic shape
	  */
	def rotation(index: Int) = {
		val v0 = vertex(index - 1)
		val v1 = vertex(index)
		val v2 = vertex(index + 1)
		
		Line(v1, v2).direction - Line(v0, v1).direction
	}
	
	/**
	  * @param index Index of the corner
	  * @return The angle at the specified corner of this polygonic shape
	  */
	def angle(index: Int) = rotation(index).toAngle
	
	def contains(point: VectorProjectable[HasDoubleDimensions]) =
		collisionAxes.forall { containsProjection(point, _) }
	
	/**
	  * Calculates the minimum translation vector that would get these two projectable shapes out of
	  * a collision situation
	  * @param other another polygonic instance
	  * @return The minimum translation vector that gets these two shapes out of a collision situation
	  * or none if there is no collision
	  */
	def collisionMtvWith(other: Polygonic): Option[Vector2D] = collisionMtvWith(other,
		(collisionAxes ++ other.collisionAxes).distinctWith { _ isParallelWith _ })
	
	/**
	  * Slices this polygon to two pieces. The cut is made between the two vertices so that both
	  * polygon pieces will contain those vertices.
	  * @param index1 The index of the first common index (< index2 - 1)
	  * @param index2 The index of the second common index (> index 1 + 1)
	  * @return Two polygon pieces
	  */
	def cutBetween(index1: Int, index2: Int) = {
		val c = corners
		
		val cutVertices = c.slice(index1, index2 + 1)
		val remainingVertices = c.take(index1 + 1) ++ c.drop(index2)
		Polygon(remainingVertices) -> Polygon(cutVertices)
	}
}
