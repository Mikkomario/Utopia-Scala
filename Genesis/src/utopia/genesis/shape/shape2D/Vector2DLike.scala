package utopia.genesis.shape.shape2D

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.shape1D.{Angle, Rotation}
import utopia.genesis.shape.template.{Dimensional, VectorLike}

/**
  * A common trait for vectors which have the both x and y dimensions. May contain more dimensions.
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  */
trait Vector2DLike[+Repr <: Vector2DLike[Repr]] extends VectorLike[Repr] with TwoDimensional[Double]
{
	// COMPUTED	--------------------------
	
	/**
	  * @return Direction of this vector in x-y -plane
	  */
	def direction = Angle.ofRadians(math.atan2(y, x))
	
	/**
	  * A 2D normal for this vector
	  */
	def normal2D = Vector2D(-y, x).toUnit
	
	/**
	  * @return A copy of this vector where all dimensions higher than 2 are set to 0
	  */
	def in2D = Vector2D(x, y)
	
	
	// OTHER	--------------------------
	
	/**
	  * Creates a new vector with the same length as this vector
	  * @param direction The direction of the new vector (on the x-y -plane)
	  */
	def withDirection(direction: Angle) =
	{
		val l = length
		buildCopy(Vector(direction.cosine * l, direction.sine * l))
	}
	
	/**
	  * Rotates this vector around a certain origin point
	  * @param rotation The amount of rotation
	  * @param origin The point this vector is rotated around (defaults to (0,0))
	  * @return The rotated version of this vector
	  */
	def rotated(rotation: Rotation, origin: Dimensional[Double] = Vector2D.zero) =
	{
		val separator = (this - origin).in2D
		val twoDimensional = separator.withDirection(separator.direction + rotation) + origin
		
		buildCopy(twoDimensional.dimensions.take(2) ++ dimensions.drop(2))
	}
	
	/**
	  * A copy of this point with specified x
	  */
	def withX(x: Double) = withDimension(x, X)
	
	/**
	  * A copy of this point with specified y
	  */
	def withY(y: Double) = withDimension(y, Y)
	
	/**
	  * @param f A mapping function
	  * @return A copy of this vector with mapped x-coordinate
	  */
	def mapX(f: Double => Double) = mapAxis(X)(f)
	
	/**
	  * @param f A mapping function
	  * @return A copy of this vector with mapped y-coordinate
	  */
	def mapY(f: Double => Double) = mapAxis(Y)(f)
	
	/**
	  * Point translated over X axis
	  */
	def plusX(increase: Double) = mapX { _ + increase }
	
	/**
	  * Point translated over Y axis
	  */
	def plusY(increase: Double) = mapY { _ + increase }
	
	/**
	  * @param decrease Amount of translation to left
	  * @return Translated point
	  */
	def minusX(decrease: Double) = plusX(-decrease)
	
	/**
	  * @param decrease Amount of translation to right
	  * @return Translated point
	  */
	def minusY(decrease: Double) = plusY(-decrease)
}
