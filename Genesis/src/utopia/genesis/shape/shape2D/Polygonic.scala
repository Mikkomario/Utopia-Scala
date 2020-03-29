package utopia.genesis.shape.shape2D

import java.awt.Shape

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.RotationDirection.Clockwise
import utopia.genesis.shape.{Rotation, Vector3D}

import scala.collection.immutable.VectorBuilder

/**
  * This trait is extended by 2D shapes that have 3 or more corners
  * @author Mikko Hilpinen
  * @since 14.4.2019, v2+
  */
trait Polygonic extends ShapeConvertible with Projectable with Area2D with TransformProjectable[Polygonic]
{
	// ABSTRACT	----------------
	
	/**
	  * @return The corners of this shape, in order
	  */
	def corners: Vector[Point]
	
	
	// COMPUTED	----------------
	
	/**
	  * @return The sides of this shape, in order
	  */
	def sides =
	{
		val c = corners
		if (c.size < 2)
			Vector()
		else
		{
			val buffer = new VectorBuilder[Line]()
			var start = c.head
			
			c.tail.foreach
			{
				corner =>
					buffer += Line(start, corner)
					start = corner
			}
			
			buffer += Line(start, c.head)
			buffer.result()
		}
	}
	
	/**
	  * @return The edges of this shape in order. Same as sides, except in vector form
	  */
	def edges = sides.map { _.vector }
	
	/**
	  * @return The rotations at each corner of this polygonic shape
	  */
	def rotations =
	{
		val e = sides
		
		if (e.size > 1)
		{
			val buffer = new VectorBuilder[Rotation]()
			
			buffer += e.head.direction - e.last.direction
			for (i <- 1 until e.size) buffer += e(i).direction - e(i - 1).direction
			
			buffer.result()
		}
		else
			Vector()
	}
	
	/**
	  * @return The angles at each corner of this polygonic shape
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
	def isConvex =
	{
		val dir = rotationDirection
		rotations.forall { _.direction == dir }
	}
	
	/**
	  * @return The collision axes that should be considered when testing this instance
	  */
	def collisionAxes = edges.distinctWith { _ isParallelWith _ }.map { _.normal2D }
	
	/**
	  * @return The bounds around this polygonic instance
	  */
	def bounds =
	{
		val c = corners
		val topLeft = Point.topLeft(c)
		val bottomRigth = Point.bottomRight(c)
		
		Bounds(topLeft, (bottomRigth - topLeft).toSize)
	}
	
	/**
	  * @return The center point of this shape
	  */
	def center =
	{
		val c = corners
		if (c.isEmpty)
			Point.origin
		else
		{
			val total = c.reduce { _ + _ }
			total / c.size
		}
	}
	
	/**
	  * The smallest possible circle that contains all the vertices in this polygon
	  */
	def circleAround =
	{
		val origin = center
		val radius = corners.map { c => (c - origin).toVector.length }.max
		Circle(origin, radius)
	}
	
	/**
	  * The largest possible circle that fits inside this polygon
	  */
	def circleInside =
	{
		val origin = center
		val radius = corners.map { vertex => (vertex - origin).toVector.length }.min
		Circle(origin, radius)
	}
	
	/**
	  * Divides this polygon into convex portions. Each of the returned parts is convex and can
	  * be used in collision checks
	  */
	def convexParts: Vector[Polygonic] =
	{
		val c = corners
		
		if (c.size < 3 || isConvex)
			Vector(this)
		else
		{
			val dir = rotationDirection
			val r = rotations
			
			val firstBrokenIndex = r.indexWhere { _.direction != dir }
			
			// Tries to find another (non-sequential) broken index
			val secondBrokenIndex =
			{
				if (firstBrokenIndex < c.size - 1)
					rotations.indexWhere({ _.direction != dir }, firstBrokenIndex + 1)
				else
					-1
			}
			
			if (secondBrokenIndex >= 0)
			{
				// If a second index was found, cuts the polygon between the two indices
				val (firstPart, secondPart) = cutBetween(firstBrokenIndex, secondBrokenIndex)
				firstPart.convexParts ++ secondPart.convexParts
			}
			else
			{
				// If there is only one broken index, cuts the polygon so that the part becomes convex
				val brokenVertex = vertex(firstBrokenIndex)
				val incomeAngle = side(firstBrokenIndex - 1).direction
				
				val remainingOutcomeIndex =
				{
					if (firstBrokenIndex < c.size - 2)
						c.indexWhere(vertex => { (Line(brokenVertex, vertex).direction - incomeAngle).direction == rotationDirection },
							firstBrokenIndex + 2)
					else
						-1
				}
				
				if (remainingOutcomeIndex >= 0)
				{
					val (firstPart, secondPart) = cutBetween(firstBrokenIndex, remainingOutcomeIndex)
					firstPart.convexParts ++ secondPart.convexParts
				}
				else
				{
					val outcomeIndex = c.indexWhere {
						vertex => (Line(brokenVertex, vertex).direction - incomeAngle).direction == rotationDirection }
					val (firstPart, secondPart) = cutBetween(outcomeIndex, firstBrokenIndex)
					firstPart.convexParts ++ secondPart.convexParts
				}
			}
		}
	}
	
	
	// IMPLEMENTED	------------
	
	override def toShape: Shape =
	{
		val c = corners
		val x = c.map { _.x.toInt }.toArray
		val y = c.map { _.y.toInt }.toArray
		
		new java.awt.Polygon(x, y, c.size)
	}
	
	override def projectedOver(axis: Vector3D) =
	{
		val projectedCorners = corners.map { _.toVector.projectedOver(axis).toPoint }
		val start = projectedCorners.min
		val end = projectedCorners.max
		
		Line(start, end)
	}
	
	override def contains(point: Point) = collisionAxes.forall { containsProjection(point, _) }
	
	override def transformedWith(transformation: Transformation): Polygonic = Polygon(corners.map { transformation(_) } )
	
	
	// OTHER	---------------
	
	/**
	  * @param index Index of the vertex (may even be negative or out of bounds, in which case loops around)
	  * @return A vertex (corner) of this polygonic instance from the specified index
	  */
	def vertex(index: Int) =
	{
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
	def sidesFrom(index: Int) =
	{
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
	def rotation(index: Int) =
	{
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
	
	/**
	  * Calculates the minimum translation vector that would get these two projectable shapes out of
	  * a collision situation
	  * @param other another polygonic instance
	  * @return The minimum translation vector that gets these two shapes out of a collision situation
	  * or none if there is no collision
	  */
	def collisionMtvWith(other: Polygonic): Option[Vector3D] = collisionMtvWith(other,
		(collisionAxes ++ other.collisionAxes).distinctWith { _ isParallelWith _ })
	
	/**
	  * Slices this polygon to two pieces. The cut is made between the two vertices so that both
	  * polygon pieces will contain those vertices.
	  * @param index1 The index of the first common index (< index2 - 1)
	  * @param index2 The index of the second common index (> index 1 + 1)
	  * @return Two polygon pieces
	  */
	def cutBetween(index1: Int, index2: Int) =
	{
		val c = corners
		
		val cutVertices = c.slice(index1, index2 + 1)
		val remainingVertices = c.take(index1 + 1) ++ c.drop(index2)
		Polygon(remainingVertices) -> Polygon(cutVertices)
	}
}
