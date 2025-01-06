package utopia.paradigm.shape.shape3d

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.generic.ParadigmDataType.Vector3DType
import utopia.paradigm.motion.motion3d.Velocity3D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorFactory, DoubleVectorLike}
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

import scala.concurrent.duration.Duration

object Vector3D extends DoubleVectorFactory[Vector3D] with SureFromModelFactory[Vector3D]
{
    // ATTRIBUTES    --------------------
    
    /**
     * The identity vector. Scaling or dividing with this vector will not affect other vectors.
     */
    val identity = Vector3D(1, 1, 1)
    /**
     * The zero vector (0, 0, 0)
     */
    override val zero = empty
    /**
     * A vector with the length of 1
     */
    val unit = Vector3D(1)
    
    
    // IMPLEMENTED    ---------------------
	
	override def apply(dimensions: Dimensions[Double]) = new Vector3D(dimensions.withLength(3))
	
	override def from(other: HasDimensions[Double]) = other match {
		case v: Vector3D => v
		case o => apply(o.dimensions)
	}
	
	override def parseFrom(model: template.ModelLike[Property]) =
		Vector3D(model("x").getDouble, model("y").getDouble, model("z").getDouble)
    
    
    // OTHER METHODS    -----------------
    
    /**
     * Converts a coordinate map into a vector
     */
    @deprecated("Please use apply instead", "v1.2")
    def of(map: Map[Axis, Double]) = apply(map)
	
	/**
	  * @param dimensions An array of dimensions (x, y, z, ...)
	  * @return A 3D vector based on the specified dimensions
	  */
	@deprecated("Please use apply instead", "v1.2")
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
     * Combines two vectors into a third vector using a binary operator
     * @param first The first vector used at the left hand side of the operator
     * @param second the second vector used at the right hand side of the operator
     * @param f The binary operator that determines the coordinates of the returned vector
     * @return A vector with values combined from the two vectors using the provided operator
     */
    @deprecated("Please use first.mergeWith(second)(f) instead", "v1.2")
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
    @deprecated("Deprecated for removal", "v1.2")
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
    @deprecated("Deprecated for removal", "v1.2")
    def forall(first: Vector3D, second: Vector3D, condition: (Double, Double) => Boolean) = 
            !exists(first, second, { !condition(_, _) })
}

/**
 * This class represents a vector in 3 dimensional space. A vector has a direction and length but 
 * no specified origin point. Vectors have value semantics and are immutable.
 * @author Mikko Hilpinen
 * @since Genesis 24.12.2016
 */
class Vector3D private(override val dimensions: Dimensions[Double])
	extends DoubleVectorLike[Vector3D] with DoubleVector with ValueConvertible with ModelConvertible with EqualsBy
{
	// ATTRIBUTES   --------------------
	
	override lazy val length = super.length
	
	
    // COMPUTED PROPERTIES    ----------
	
	/**
	  * @return A 2D-copy of this vector
	  */
	def in2D = Vector2D.from(this)
	
    /**
     * A normal for this vector
     */
    def normal = if (x == 0 && y == 0 && z != 0) Vector3D(1) else normal2D.toVector3D
	
	/**
	  * The x, y and z components of this vector
	  */
	@deprecated("Please rather use .dimensions", "v1.2")
	def toVector = Vector(x, y, z)
    
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	override protected def factory = Vector3D
	override protected def equalsProperties = Single(dimensions)
	
	override def zero = Vector3D.zero
	
	override def toString = s"($x, $y, $z)"
	override def toValue = new Value(Some(this), Vector3DType)
	override def toModel = Model.fromMap(Map("x" -> x, "y" -> y, "z" -> z).filterNot { _._2 ~== 0.0 })
    
    
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
	def withoutDimension(axis: Axis) = axis match {
		case X => Vector2D(y, z)
		case Y => Vector2D(x, z)
		case Z => Vector2D(x, y)
	}
	/**
	  * @param index Index of the targeted dimension [0, 2]
	  * @return A 2D copy of this vector with the specified dimension dropped
	  */
	def withoutDimensionAtIndex(index: Int) = withoutDimension(Axis(index))
    
    /**
     * Creates a new vector with the same length as this vector
     * @param direction the new direction of the vector on the x-z plane
     */
    def withYDirection(direction: Angle) = 
    {
        val zRotated = toVector2D.withDirection(direction)
        Vector3D(zRotated.x, y, zRotated.y)
    }
    
    /**
     * Rotates the vector around a certain origin point
     * @param rotationRads The amount of rotation in radians
     * @param origin The point this vector is rotated around (defaults to zero)
     * @return The rotated vector
     */
    @deprecated("Deprecated for removal", "v1.2")
    def rotatedRads(rotationRads: Double, origin: Vector3D = Vector3D.zero) =
	    rotated(Rotation ofRadians rotationRads)
    /**
     * Rotates the vector around a certain origin point
     * @param rotationDegs The amount of rotation in degrees
     * @param origin The point this vector is rotated around (default to zero)
     * @return The rotated vector
     */
    @deprecated("Deprecated for removal", "v1.2")
    def rotatedDegs(rotationDegs: Double, origin: Vector3D = Vector3D.zero) =
	    rotated(Rotation ofDegrees rotationDegs)
	
	/**
	  * Converts this vector to a velocity vector
	  * @param time Duration of this transition
	  * @return A velocity vector
	  */
	def traversedIn(time: Duration) = Velocity3D(this, time)
}