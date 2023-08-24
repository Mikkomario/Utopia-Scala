package utopia.paradigm.shape.template

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{CanBeAboutZero, Combinable, EqualsFunction, HasLength, Reversible, Scalable}
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.shape.shape1d.{Dimension, Vector1D}
import utopia.paradigm.shape.shape2d.{Matrix2D, Vector2D}
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.Transformable

/**
  * This trait is implemented by simple shape classes that can be represented as an vector of numbers,
  * each matching an axis (X, Y, Z, ...).
  * All implementing classes are expected to handle double number inputs, like double scaling,
  * but they may apply rounding and other conversions to the results.
  *
  * @author Mikko Hilpinen
  * @since 24.8.2023 v1.4
  *
  * @tparam D Type of dimensions used by this vector
  * @tparam Repr the concrete implementing class
  * @tparam Transformed Type of vector returned in matrix-based transformation functions and direction changes
  */
trait NumericVectorLike[D, +Repr <: HasDimensions[D] with HasLength, +Transformed]
	extends Dimensional[D, Repr]
		with Scalable[D, Repr] with Combinable[HasDimensions[D], Repr] with Reversible[Repr] with HasLength
		with VectorProjectable[Transformed] with CanBeAboutZero[HasDimensions[D], Repr] with Transformable[Transformed]
{
	// ABSTRACT ---------------------
	
	/**
	  * @return Factory used for building more copies of this vector
	  */
	protected def factory: NumericVectorFactory[D, Repr]
	/**
	  * @return Factory used for building transformed vectors that are based on double precision numbers
	  */
	protected def fromDoublesFactory: DimensionsWrapperFactory[Double, Transformed]
	
	/**
	  * @return Approximate function used for comparing individual dimensions
	  */
	implicit def dimensionApproxEquals: EqualsFunction[D]
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return Numeric implementation for the dimensions used in this vector
	  */
	implicit def n: Numeric[D] = factory.n
	
	/*
	/**
	  * @return A 2D-copy of this vector
	  */
	def toVector2D = Vector2D.from(this)
	/**
	  * @return A 3D-copy of this vector
	  */
	def toVector3D = Vector3D.from(this)
	/**
	  * @return A point based on this vector
	  */
	def toPoint = Point.from(this)
	/**
	  * @return A size based on this vector
	  */
	def toSize = Size.from(this)
	*/
	
	/**
	  * @return Direction of this vector in x-y -plane
	  */
	def direction = Angle.ofRadians(math.atan2(n.toDouble(y), n.toDouble(x)))
	/**
	  * This vector's direction on the z-y plane
	  */
	def xDirection = Angle ofRadians calculateDirection(n.toDouble(z), n.toDouble(y))
	/**
	  * This vector's direction on the x-z plane
	  */
	def yDirection = Angle ofRadians calculateDirection(n.toDouble(x), n.toDouble(z))
	
	/**
	  * A 2D normal for this vector
	  */
	def normal2D = Vector2D(-n.toDouble(y), n.toDouble(x)).toUnit
	
	/**
	  * @return A 2x2 matrix representation of this vector (1x2 matrix [x,y] extended to 2x2).
	  *         The resulting matrix will match the identity matrix outside the 1x2 defined range.
	  */
	def to2DMatrix = Matrix2D(
		n.toDouble(x), n.toDouble(y),
		0, 1
	)
	/**
	  * @return A 3x3 matrix based on this 3d vector. The natural 1x3 matrix representation [x,y,z] of this vector
	  *         is expanded to 3x3 by adding missing numbers from the identity matrix.
	  */
	def to3DMatrix = Matrix3D(
		n.toDouble(x), n.toDouble(y), n.toDouble(z),
		0, 1, 0,
		0, 0, 1
	)
	
	/**
	  * @return Whether this vector is an identity vector (1, 1, ...)
	  */
	def isIdentity: Boolean = dimensions.forall { _ == n.fromInt(1) }
	
	/**
	  * This vector with length of 1
	  */
	def toUnit = this / length
	
	/**
	  * A version of this vector where all values are at least 0
	  */
	def positive = map { d => n.max(d, n.zero) }
	
	
	// IMPLEMENTED	-----------------
	
	override def length = math.sqrt(n.toDouble(this dot this))
	
	override def isAboutZero = dimensions.forall { _ ~== n.zero }
	
	override def unary_- : Repr = map(n.negate)
	
	override def withDimensions(newDimensions: Dimensions[D]) = factory(newDimensions)
	
	override def +(other: HasDimensions[D]) = mergeWith(other)(n.plus)
	override def *(mod: D): Repr = map { n.times(_, mod) }
	
	override def ~==(other: HasDimensions[D]) = super[Dimensional].~==(other)
	
	// ab = (a . b) / (b . b) * b
	override def projectedOver[V <: DoubleVectorLike[V]](vector: V) =
		fromDoublesFactory.from(vector * (doubleDot(vector) / (vector dot vector)))
	
	override def transformedWith(transformation: Matrix2D) =
		fromDoublesFactory.from(transformation(dimensions.map(n.toDouble).padTo(2, 1.0)))
	override def transformedWith(transformation: Matrix3D) =
		fromDoublesFactory.from(transformation(dimensions.map(n.toDouble).padTo(3, 1.0)))
	
	
	// OTHER	--------------------------
	
	/**
	  * @param dimension A dimension to replace the matching dimension on this vector
	  * @return A copy of this vector with the specific component replacing one of this vector's components
	  */
	def withDimension(dimension: Dimension[D]): Repr = withDimension(dimension.axis, dimension.value)
	
	/**
	  * Maps all dimensions of this vector-like element
	  * @param f A mapping function
	  * @return A mapped version of this element
	  */
	def map(f: D => D) = mapEachDimension(f)
	/**
	  * @param f A mapping function that also takes dimension index (0 for the first dimension)
	  * @return A mapped copy of this vector
	  */
	def mapWithIndex(f: (D, Int) => D): Repr = factory(dimensions.zipWithIndex.map { case (d, i) => f(d, i) })
	
	/**
	  * This vector with increased length
	  */
	def +(n: Double) = withLength(length + n)
	/**
	  * This vector with decreased length (the direction may change to opposite)
	  */
	def -(n: Double) = this + (-n)
	def -(other: HasDimensions[D]) = mergeWith(other)(n.minus)
	
	/**
	  * @param n A scaling modifier
	  * @return A copy of this vector where each dimension is scaled by the specified amount
	  */
	def *(n: Double) = map { factory.scale(_, n) }
	/**
	  * @param other Another vector-like element
	  * @return This element multiplied on each axis of the provided element
	  */
	def *(other: HasDimensions[D]): Repr = mergeWith(other)(n.times)
	/**
	  * @param div A dividing factor
	  * @return Copy of this vector where each dimension is divided using the specified division factor
	  */
	def /(div: Double) = map { factory.div(_, div) }
	/**
	  * @param other Another vector-like element
	  * @return This element divided on each axis of the provided element.
	  */
	def /(other: HasDoubleDimensions): Repr = mergeWith(other)(factory.div)
	
	/**
	  * Scales this vector along a singular axis. The other axes remain unaffected.
	  * @param vector A vector that determines the scaling factor (via length) and the affected axis (vector's axis)
	  * @return A copy of this vector where one component has been scaled using the specified modifier
	  */
	def scaledAlong(vector: Vector1D) = mapDimension(vector.axis) { factory.scale(_, vector.length) }
	/**
	  * Divides this vector along a singular axis. The other axes remain unaffected.
	  * Please note that this vector will remain unaffected if divided by zero.
	  * @param vector A vector that determines the dividing factor (via length) and the affected axis (vector's axis)
	  * @return A copy of this vector where one component has been divided with the specified modifier
	  */
	def dividedAlong(vector: Vector1D) =
		if (vector.isZero) self else mapDimension(vector.axis) { factory.div(_, vector.length) }
	
	/**
	  * Calculates this vectors direction around the specified axis
	  */
	def directionAround(axis: Axis) = axis match {
		case X => xDirection
		case Y => yDirection
		case Z => direction
	}
	
	/**
	  * The dot product between this and another vector
	  */
	def dot(other: HasDimensions[D]) = mergeWith(other)(n.times).dimensions.sum
	/**
	  * @param other Another vector
	  * @return The dot product between these vectors, in double precision
	  */
	def doubleDot(other: HasDoubleDimensions) =
		dimensions.mergeWith(other, 0.0) { n.toDouble(_) * _ }.sum
	/**
	  * Calculates the scalar projection of this vector over the other vector.
	  * This is the same as the length of this vector's projection over the other vector
	  */
	def scalarProjection(other: HasDoubleDimensions with HasLength) = doubleDot(other) / other.length
	/**
	  * The length of the cross product of these two vectors. |a||b|sin(a, b)
	  */
	// = |a||b|sin(a, b)e, |e| = 1 (in this we skip the e)
	def crossProductLength[
		V <: VectorProjectable[V2] with Combinable[V2, HasLength] with HasLength,
		V2 <: Reversible[V2] with HasLength](other: V) =
		length * other.length * angleDifference[V, V2](other).sine
	/**
	  * Calculates the directional difference between these two vectors. The difference is
	  * absolute (always positive) and doesn't specify the direction of the difference.
	  */
	def angleDifference[V <: VectorProjectable[V2] with Combinable[V2, HasLength], V2 <: Reversible[V2] with HasLength](other: V) = {
		// This vector is used as the 'x'-axis, while a perpendicular vector is used as the 'y'-axis
		// The other vector is then measured against these axes
		val x = other.projectedOver(DoubleVector(dimensions.map(n.toDouble)))
		val y = other - x
		
		Angle.ofRadians(math.atan2(y.length, x.length).abs)
	}
	/**
	  * @param other Another vector
	  * @return The distance between the points represented by these two vectors
	  */
	def distanceFrom(other: HasDimensions[D]) = (this - other).length
	
	/**
	  * Checks whether this vector is parallel with another vector (has same or opposite direction)
	  */
	def isParallelWith[
		V <: VectorProjectable[V2] with Combinable[V2, HasLength] with HasLength,
		V2 <: Reversible[V2] with HasLength](other: V) =
		crossProductLength[V, V2](other) ~== 0.0
	/**
	  * @param axis Target axis
	  * @return Whether this vector is parallel to the specified axis
	  */
	def isParallelWith(axis: Axis): Boolean = isParallelWith[Vector1D, Vector1D](axis.unit)
	/**
	  * Checks whether this vector is perpendicular to another vector (ie. (1, 0) vs. (0, 1))
	  */
	// Vectors are perpendicular when their dot product is zero.
	def isPerpendicularTo(other: HasDoubleDimensions) = doubleDot(other) ~== 0.0
	/**
	  * @param axis Target axis
	  * @return Whether this vector is perpendicular to the specified axis
	  */
	def isPerpendicularTo(axis: Axis): Boolean = isPerpendicularTo(axis.unit)
	
	/**
	  * Creates a new vector with the same direction with this vector
	  * @param length The length of the new vector
	  */
	def withLength(length: Double) = this * (length / this.length)
	/**
	  * Creates a new vector with the same length as this vector
	  * @param direction The direction of the new vector (on the x-y -plane)
	  */
	def withDirection(direction: Angle) = {
		val l = length
		fromDoublesFactory(Dimensions.double(direction.cosine * l, direction.sine * l))
	}
	/**
	  * Rotates this vector around a certain origin point
	  * @param rotation The amount of rotation
	  * @param origin   The point this vector is rotated around
	  * @return The rotated version of this vector
	  */
	def rotatedAround(rotation: Rotation, origin: HasDoubleDimensions) = {
		val separator = Vector2D.from(dimensions.mergeWith(origin, 0.0) { n.toDouble(_) - _ })
		val twoDimensional = separator.withDirection(separator.direction + rotation) + origin
		
		if (dimensions.size > 2)
			fromDoublesFactory(twoDimensional.dimensions.withLength(2) ++ dimensions.drop(2).map(n.toDouble))
		else
			fromDoublesFactory.from(twoDimensional)
	}
	/**
	  * Moves this vector into the specified area with minimal movement
	  * @param area An area to which this vector shall remain within
	  * @return A copy of this vector that lies within the specified area
	  */
	def shiftedInto(area: HasDimensions[HasInclusiveEnds[D]]) = mergeWith(area) { (p, area) => area.restrict(p) }
	/**
	  * Moves this vector into a specific area with minimal movement
	  * @param area A 1-dimensional area
	  * @return A copy of this vector that lies within that area along that dimension
	  */
	def shiftedInto(area: Dimension[HasInclusiveEnds[D]]) = mapDimension(area.axis)(area.value.restrict)
	
	private def calculateDirection(x: Double, y: Double) = math.atan2(y, x)
}