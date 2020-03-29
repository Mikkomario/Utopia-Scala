package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Vector3D

/**
  * This trait is extended by 2D shapes that have 4 corners and where the opposite sides (left + right, top + bottom)
  * are always identical
  * @author Mikko Hilpinen
  * @since 14.4.2019
  */
trait Parallelogramic extends Polygonic with TransformProjectable[Parallelogram]
{
	// ABSTRACT	------------------
	
	/**
	  * @return The top left corner of this shape
	  */
	def topLeft: Point
	
	/**
	  * @return The top / bottom edge of this shape
	  */
	def top: Vector3D
	
	/**
	  * @return The left / right edge of this shape
	  */
	def left: Vector3D
	
	
	// COMPUTED	------------------
	
	def topRight = topLeft + top
	
	def bottomRight = topRight + left
	
	def bottomLeft = topLeft + left
	
	/**
	  * @return The top side of this shape (left to right)
	  */
	def topEdge = Line(topLeft, topRight)
	
	/**
	  * @return The right side of this shape (top to bottom)
	  */
	def rightEdge = Line(topRight, bottomRight)
	
	/**
	  * @return The bottom side of this shape (right to left)
	  */
	def bottomEdge = Line(bottomRight, bottomLeft)
	
	/**
	  * @return The left side of this shape (bottom to top)
	  */
	def leftEdge = Line(bottomLeft, topLeft)
	
	/**
	  * @return The area of this 2D shape
	  */
	def area = top.length * left.length
	
	
	// IMPLEMENTED	--------------
	
	override def center = topLeft + (top / 2) + (left / 2)
	
	override def corners = Vector(topLeft, topRight, bottomRight, bottomLeft)
	
	override def collisionAxes = Vector(top, left).map { _.normal2D }
	
	override def transformedWith(transformation: Transformation) =
	{
		val topLeft2 = transformation(topLeft)
		val topRight2 = transformation(topRight)
		val bottomLeft2 = transformation(bottomLeft)
		
		Parallelogram(topLeft2, (topRight2 - topLeft2).toVector, (bottomLeft2 - topLeft2).toVector)
	}
}
