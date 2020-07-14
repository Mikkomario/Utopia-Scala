package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.shape1D.Angle
import utopia.genesis.shape.shape3D.Vector3D

import scala.concurrent.duration.Duration

object Vector2D
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * A (0,0) vector
	  */
	val zero = Vector2D()
	
	/**
	  * A (1,1) vector
	  */
	val identity = Vector2D(1, 1)
	
	/**
	  * A (1,0) vector
	  */
	val unit = Vector2D(1)
	
	
	// OTHER	------------------------------
	
	/**
	  * Creates a new vector with specified length and direction
	  */
	def lenDir(length: Double, direction: Angle) = Vector2D(direction.cosine * length, direction.sine * length)
	
	/**
	  * Converts a coordinate map into a vector
	  */
	def of(map: Map[Axis2D, Double]) = Vector2D(map.getOrElse(X, 0), map.getOrElse(Y, 0))
}

/**
  * A 2 dimensional vector
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  */
case class Vector2D(override val x: Double = 0.0, override val y: Double = 0.0) extends Vector2DLike[Vector2D]
{
	// COMPUTED	---------------------------------
	
	/**
	  * @return A three dimensional copy of this vector
	  */
	def in3D = Vector3D(x, y)
	
	/**
	  * @return A point that matches this vector
	  */
	def toPoint = Point(x, y)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def in2D = this
	
	override val dimensions = Vector(x, y)
	
	override def buildCopy(dimensions: Vector[Double]) =
	{
		if (dimensions.size >= 2)
			Vector2D(dimensions(0), dimensions(1))
		else if (dimensions.nonEmpty)
			Vector2D(dimensions.head)
		else
			Vector2D.zero
	}
	
	override def repr = this
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param z A z-coordinate
	  * @return A copy of this vector with an added z-coordinate
	  */
	def withZ(z: Double) = Vector3D(x, y, z)
	
	/**
	  * @param duration Time period
	  * @return A velocity that represents this movement in specified duration
	  */
	def traversedIn(duration: Duration) = Velocity2D(this, duration)
}
