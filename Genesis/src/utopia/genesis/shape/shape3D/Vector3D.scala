package utopia.genesis.shape.shape3D

import utopia.flow.datastructure
import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ValueConvertible}
import utopia.genesis.generic.Vector3DType
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.shape2D.{Point, Size, Vector2D, Vector2DLike}
import utopia.genesis.shape.Axis
import utopia.genesis.shape.shape1D.{Angle, Rotation}
import utopia.genesis.util.ApproximatelyEquatable
import utopia.genesis.util.Extensions._

import scala.collection.immutable.HashMap
import scala.concurrent.duration.Duration
import scala.util.Success

object Vector3D extends FromModelFactory[Vector3D]
{
    // ATTRIBUTES    --------------------
    
    /**
     * The identity vector. Scaling or dividing with this vector will not affect other vectors.
     */
    val identity = Vector3D(1, 1, 1)
    /**
     * The zero vector (0, 0, 0)
     */
    val zero = Vector3D()
    /**
     * A vector with the length of 1
     */
    val unit = Vector3D(1)
    
    
    // OPERATORS    ---------------------
    
    override def apply(model: datastructure.template.Model[Property]) = Success(Vector3D(
            model("x").getDouble, model("y").getDouble, model("z").getDouble))
    
    
    // OTHER METHODS    -----------------
    
    /**
     * Creates a new vector with specified length and direction
     */
    def lenDir(length: Double, direction: Angle) = Vector3D(
            math.cos(direction.radians) * length, math.sin(direction.radians) * length)
    
    /**
     * Converts a coordinate map into a vector
     */
    def of(map: Map[Axis, Double]) = Vector3D(map.getOrElse(X, 0), map.getOrElse(Y, 0), map.getOrElse(Z, 0))
	
	/**
	  * @param dimensions An array of dimensions (x, y, z, ...)
	  * @return A 3D vector based on the specified dimensions
	  */
	def withDimensions(dimensions: Seq[Double]) =
	{
		if (dimensions.size >= 3)
			Vector3D(dimensions.head, dimensions(1), dimensions(2))
		else if (dimensions.size == 2)
			Vector3D(dimensions.head, dimensions(1))
		else if (dimensions.isEmpty)
			Vector3D.zero
		else
			Vector3D(dimensions.head)
	}
	
    /**
     * Calculates a surface normal for two vectors. If this normal was called n, both n and -n are 
     * normals for this surface
     */
    def surfaceNormal(a: Vector3D, b: Vector3D) = 
    {
        val u = a.toUnit
        val v = b.toUnit
        
        /*
		    Nx = UyVz - UzVy
			Ny = UzVx - UxVz
			Nz = UxVy - UyVx
		 */
        Vector3D(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x)
    }
    
    /**
     * Calculates the average point of the provided vectors
     * @return The average point of the provided vectors
     * @throws UnsupportedOperationException If the collection is empty
     */
    @throws(classOf[UnsupportedOperationException])
    def average(vectors: Iterable[Vector3D]) = vectors.reduceLeft { _ + _ } / vectors.size
    
    /**
     * Calculates the average point of the provided vectors
     * @return The average point of the provided vectors. None if collection is empty
     */
    def averageOption(vectors: Iterable[Vector3D]) =
    {
        if (vectors.isEmpty)
            None
        else
            vectors.reduceLeftOption(_ + _).map(_ / vectors.size)
    }
	
    /**
     * The top left corner of a bounds between the two vertices. In other words,
     * creates a vector that has the smallest available value on each axis from the two candidates
     */
	@deprecated("Please call through the vector instance", "v2")
    def topLeft(first: Vector3D, second: Vector3D) = combine(first, second, (a, b) => if (a <= b) a else b)
    
    /**
     * The top left corner of a bounds around the vertices. In other words,
     * creates a vector that has the smallest available value on each axis from all the candidates.
     * None if the provided collection is empty
     */
    def topLeft(vectors: IterableOnce[Vector3D]): Option[Vector3D] = vectors.iterator.reduceOption { _ topLeft _ }
    
    /**
     * The bottom right corner of a bounds between the two vertices. In other words,
     * creates a vector that has the largest available value on each axis from the two candidates
     */
	@deprecated("Please call through the vector instance", "v2")
    def bottomRight(first: Vector3D, second: Vector3D) = combine(first, second, (a, b) => if (a >= b) a else b)
    
    /**
     * The bottom right corner of a bounds around the vertices. In other words,
     * creates a vector that has the largest available value on each axis from all the candidates.
     * None if the provided collection is empty
     */
    def bottomRight(vectors: IterableOnce[Vector3D]): Option[Vector3D] = vectors.iterator.reduceOption { _ bottomRight _ }
	
    /**
     * Combines two vectors into a third vector using a binary operator
     * @param first The first vector used at the left hand side of the operator
     * @param second the second vector used at the right hand side of the operator
     * @param f The binary operator that determines the coordinates of the returned vector
     * @return A vector with values combined from the two vectors using the provided operator
     */
    def combine(first: Vector3D, second: Vector3D, f: (Double, Double) => Double) = 
    {
        val v1 = first.toVector
        val v2 = second.toVector
        
        val combo = for { i <- 0 to 2 } yield f(v1(i), v2(i))
        Vector3D(combo(0), combo(1), combo(2))
    }
    
    /**
     * Performs a check over two vectors using a binary operator. Returns true if the condition 
     * holds for any coordinate pair between the two vectors.
     * @param first The first vector used at the left hand side of the operator
     * @param second the second vector used at the right hand side of the operator
     * @param condition The binary operator that determines the return value of the function
     * @return True if the condition returned true for any two coordinates, false otherwise
     */
    def exists(first: Vector3D, second: Vector3D, condition: (Double, Double) => Boolean): Boolean = 
    {
        val v1 = first.toVector
        val v2 = second.toVector
        
        for (i <- v1.indices)
        {
            if (condition(v1(i), v2(i))) { return true }
        }
        
        false
    }
    
    /**
     * Performs a check over two vectors using a binary operator. Returns true if the condition 
     * holds for all coordinate pair between the two vectors.
     * @param first The first vector used at the left hand side of the operator
     * @param second the second vector used at the right hand side of the operator
     * @param condition The binary operator that determines the return value of the function
     * @return True if the condition returned true for every two coordinates, false otherwise
     */
    def forall(first: Vector3D, second: Vector3D, condition: (Double, Double) => Boolean) = 
            !exists(first, second, { !condition(_, _) })
}

/**
 * This class represents a vector in 3 dimensional space. A vector has a direction and length but 
 * no specified origin point. Vectors have value semantics and are immutable.
 * @author Mikko Hilpinen
 * @since 24.12.2016
 */
case class Vector3D(override val x: Double = 0.0, override val y: Double = 0.0, override val z: Double = 0.0)
	extends Vector2DLike[Vector3D] with ValueConvertible with ModelConvertible with ApproximatelyEquatable[Vector3D]
		with ThreeDimensional[Double]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * The x, y and z components of this vector
	  */
	lazy val toVector = Vector(x, y, z)
	
	
    // COMPUTED PROPERTIES    ----------
    
    /**
     * Converts this vector to a point
     */
    def toPoint = Point(x, y)
    
    /**
     * Converts this vector to a size
     */
    def toSize = Size(x, y)
    
    /**
     * This vector's direction on the z-y plane
     */
    def xDirection = Angle ofRadians calculateDirection(z, y)
    
    /**
     * This vector's direction on the x-z plane
     */
    def yDirection = Angle ofRadians calculateDirection(x, z)
    
    /**
     * Calculates this vectors direction around the specified axis
     */
    def directionAround(axis: Axis) = axis match
	{
		case X => xDirection
		case Y => yDirection
		case Z => direction
	}
    
    /**
     * A normal for this vector
     */
    def normal = if (x == 0 && y == 0 && z != 0) Vector3D(1) else normal2D.in3D
	
	/**
	  * @return A 3x3 matrix based on this 3d vector. The natural 1x3 matrix representation [x,y,z] of this vector
	  *         is expanded to 3x3 by adding missing numbers from the identity matrix.
	  */
	def to3DMatrix = Matrix3D(
		x, y, z,
		0, 1, 0,
		0, 0, 1
	)
    
	
	// IMPLEMENTED	--------------------
	
	override def toString = s"($x, $y, $z)"
	
	override def repr = this
	
	override def toValue = new Value(Some(this), Vector3DType)
	
	override def toModel = Model.fromMap(HashMap("x" -> x, "y" -> y, "z" -> z).filterNot { _._2 ~== 0.0 })
	
	/**
	  * @return The X, Y, Z ... dimensions of this vectorlike instance. No specific length required, however.
	  */
	override def dimensions = toVector
	
	override def buildCopy(vector: Vector2D) = vector.in3D
	
	override def buildCopy(vector: Vector3D) = vector
	
	/**
	  * Builds a new vectorlike instance from the provided dimensions
	  * @param dimensions A set of dimensions
	  * @return A parsed version of the dimensions
	  */
	override def buildCopy(dimensions: Vector[Double]) = Vector3D.withDimensions(dimensions)
    
    
    // OTHER METHODS    ----------------
    
    /**
     * The cross product between this and another vector. The cross product is parallel with a
     * normal for a surface created by these two vectors
     */
    def cross(other: Vector3D) = Vector3D.surfaceNormal(this, other).withLength(crossProductLength(other))
	
	/**
	  * @param axis Target axis / dimension
	  * @return A 2D copy of this vector with the specified dimension dropped (Eg. (x, z) or (y, z))
	  */
	def withoutDimension(axis: Axis) = axis match
	{
		case X => Vector2D(y, z)
		case Y => Vector2D(x, z)
		case Z => Vector2D(x, y)
	}
	
	/**
	  * @param index Index of the targeted dimension [0, 2]
	  * @return A 2D copy of this vector with the specified dimension dropped
	  */
	def withoutDimensionAtIndex(index: Int) =
	{
		val dimension = index match
		{
			case 0 => X
			case 1 => Y
			case 2 => Z
			case _ => throw new IndexOutOfBoundsException(s"3D Vector doesn't have dimension with index $index")
		}
		withoutDimension(dimension)
	}
	
	/**
	  * A projection of this vector for the specified axis
	  */
	@deprecated("Please use ProjectedOver instead", "v2")
	def projectedAlong(axis: Axis) =
	{
		axis match
		{
			case X => xProjection
			case Y => yProjection
			case Z => zProjection
		}
	}
    
    /**
     * Calculates the directional difference between the two vectors in radians. The difference is 
     * absolute (always positive) and doesn't specify the direction of the difference.
     */
	@deprecated("Please use angleDifference(...) instead", "v2.3")
    def angleDifferenceRads(other: Vector3D) = 
    {
        // This vector is used as the 'x'-axis, while a perpendicular vector is used as the 'y'-axis
        // The other vector is then measured against these axes
        val x = other projectedOver this
        val y = other - x
        
        math.atan2(y.length, x.length).abs
    }
    
    /**
     * Creates a new vector with the same length as this vector
     * @param direction the new direction of the vector on the x-z plane
     */
    def withYDirection(direction: Angle) = 
    {
        val zRotated = in2D.withDirection(direction)
        Vector3D(zRotated.x, y, zRotated.y)
    }
    
    /**
     * Rotates the vector around a certain origin point
     * @param rotationRads The amount of rotation in radians
     * @param origin The point this vector is rotated around (defaults to zero)
     * @return The rotated vector
     */
    def rotatedRads(rotationRads: Double, origin: Vector3D = Vector3D.zero) = rotated(Rotation ofRadians rotationRads)
    
    /**
     * Rotates the vector around a certain origin point
     * @param rotationDegs The amount of rotation in degrees
     * @param origin The point this vector is rotated around (default to zero)
     * @return The rotated vector
     */
    def rotatedDegs(rotationDegs: Double, origin: Vector3D = Vector3D.zero) = rotated(Rotation ofDegrees rotationDegs)
	
	/**
	  * Converts this vector to a velocity vector
	  * @param time Duration of this transition
	  * @return A velocity vector
	  */
	def traversedIn(time: Duration) = Velocity3D(this, time)
	
    private def calculateDirection(x: Double, y: Double) = math.atan2(y, x)
}