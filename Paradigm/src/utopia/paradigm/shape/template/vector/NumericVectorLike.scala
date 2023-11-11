package utopia.paradigm.shape.template.vector

import utopia.flow.collection.immutable.range.HasInclusiveOrderedEnds
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{CanBeAboutZero, Combinable, EqualsFunction, HasLength, Reversible, Scalable}
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{Dimensional, Dimensions, FromDimensionsFactory, HasDimensions, VectorProjectable}
import utopia.paradigm.transform.Transformable

import scala.math.Fractional.Implicits.infixFractionalOps

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
  * @tparam FromDoubles Type of vector returned in case of double number input / precision
  */
trait NumericVectorLike[D, +Repr <: HasDimensions[D] with HasLength, +FromDoubles]
	extends Dimensional[D, Repr]
		with Scalable[D, Repr] with Combinable[HasDimensions[D], Repr] with Reversible[Repr] with HasLength
		with VectorProjectable[FromDoubles] with CanBeAboutZero[HasDimensions[D], Repr] with Transformable[FromDoubles]
{
	// ABSTRACT ---------------------
	
	/**
	  * @return Approximate function used for comparing individual dimensions
	  */
	implicit def dimensionApproxEquals: EqualsFunction[D]
	
	/**
	  * @return Factory used for building more copies of this vector
	  */
	protected def factory: NumericVectorFactory[D, Repr]
	/**
	  * @return Factory used for building transformed vectors that are based on double precision numbers
	  */
	protected def fromDoublesFactory: FromDimensionsFactory[Double, FromDoubles]
	
	/**
	  * @return A copy of this vector that uses double number precision
	  */
	def toDoublePrecision: FromDoubles
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return Numeric implementation for the dimensions used in this vector
	  */
	implicit def n: Fractional[D] = factory.n
	
	/**
	  * @return Direction of this vector in x-y -plane
	  */
	def direction = Angle.radians(math.atan2(n.toDouble(y), n.toDouble(x)))
	/**
	  * This vector's direction on the z-y plane
	  */
	def xDirection = Angle.radians(calculateDirection(n.toDouble(z), n.toDouble(y)))
	/**
	  * This vector's direction on the x-z plane
	  */
	def yDirection = Angle.radians(calculateDirection(n.toDouble(x), n.toDouble(z)))
	
	/**
	  * A 2D normal for this vector
	  */
	def normal2D = {
		val scaling = 1.0 / length
		fromDoublesFactory(Dimensions.double(-n.toDouble(y) * scaling, n.toDouble(x) * scaling))
	}
	
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
	def toUnit = dividedBy(length)
	
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
	override def projectedOver(vector: DoubleVector) =
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
	  * @param f A mapping function applied for all dimensions of this element
	  * @return A mapped copy of this element
	  */
	def mapToDouble(f: D => Double) = fromDoublesFactory(dimensions.mapWithZero(0.0)(f))
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
	  * @param other Another vector-like element
	  * @return This element multiplied on each axis of the provided element
	  */
	def *(other: HasDimensions[D]): Repr = mergeWith(other)(n.times)
	/**
	  * @param mod mod scaling modifier
	  * @return A copy of this vector where each dimension is scaled by the specified amount
	  */
	def scaledBy(mod: Double) = mapToDouble { n.toDouble(_) * mod }
	/**
	  * @param other A scaling vector
	  * @return A copy of this vector where each dimension is scaled using the specified vector's matching dimension
	  */
	def scaledBy(other: HasDoubleDimensions) =
		fromDoublesFactory(dimensions.mergeWith(other, 0.0) { n.toDouble(_) * _ })
	/**
	  * Scales this vector along a singular axis. The other axes remain unaffected.
	  * @param mod A one-dimensional scaling modifier
	  * @return A copy of this vector scaled along the specified dimension by the specified amount
	  */
	def scaledAlong(mod: Dimension[D]) = mapDimension(mod.axis) { _ * mod.value }
	/**
	  * @param div A dividing factor
	  * @return Copy of this vector where each dimension is divided using the specified division factor
	  */
	def /(div: D) = map { n.div(_, div) }
	/**
	  * @param other Another vector-like element
	  * @return This element divided on each axis of the provided element.
	  */
	def /(other: HasDimensions[D]): Repr = mergeWith(other)(n.div)
	/**
	  * @param div A dividing factor
	  * @return Copy of this vector where each dimension is divided using the specified division factor
	  */
	def dividedBy(div: Double) = mapToDouble { n.toDouble(_) / div }
	/**
	  * @param other A dividing vector
	  * @return Copy of this vector where each dimension is divided using the specified vector.
	  *         Zero dimensions (of the other vector) are ignored.
	  */
	def dividedBy(other: HasDoubleDimensions) =
		fromDoublesFactory(
			dimensions.mergeWith(other, 0.0) { (a, d) => if (d == 0.0) n.toDouble(a) else n.toDouble(a) / d })
	/**
	  * Divides this vector along a singular axis. The other axes remain unaffected.
	  * @param div A one-dimensional divider
	  * @return A copy of this vector divided along the specified axis by the specified amount
	  */
	def dividedAlong(div: Dimension[D]) = mapDimension(div.axis) { _ / div.value }
	
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
	def crossProductLength[V <: VectorProjectable[V] with Reversible[V] with Combinable[V, HasLength] with HasLength](other: V) =
		length * other.length * angleDifference[V](other).sine
	/**
	  * Calculates the directional difference between these two vectors. The difference is
	  * absolute (always positive) and doesn't specify the direction of the difference.
	  */
	def angleDifference[V <: VectorProjectable[V] with Reversible[V] with Combinable[V, HasLength] with HasLength](other: V) = {
		// This vector is used as the 'x'-axis, while a perpendicular vector is used as the 'y'-axis
		// The other vector is then measured against these axes
		val x = other.projectedOver(DoubleVector(dimensions.map(n.toDouble)))
		val y = other - x
		
		Angle.radians(math.atan2(y.length, x.length).abs)
	}
	/**
	  * @param other Another vector
	  * @return The distance between the points represented by these two vectors
	  */
	def distanceFrom(other: HasDimensions[D]) = (this - other).length
	
	/**
	  * Checks whether this vector is parallel with another vector (has same or opposite direction)
	  */
	def isParallelWith[V <: VectorProjectable[V] with Reversible[V] with Combinable[V, HasLength] with HasLength](other: V) =
		crossProductLength[V](other) ~== 0.0
	/**
	  * @param axis Target axis
	  * @return Whether this vector is parallel to the specified axis
	  */
	def isParallelWith(axis: Axis): Boolean = isParallelWith(axis.unit.in3D)
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
	def withLength(length: Double) = scaledBy(length / this.length)
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
			fromDoublesFactory(
				Dimensions.double(twoDimensional.dimensions.withLength(2) ++ dimensions.drop(2).map(n.toDouble)))
		else
			fromDoublesFactory.from(twoDimensional)
	}
	/**
	  * Moves this vector into the specified area with minimal movement
	  * @param area An area to which this vector shall remain within
	  * @return A copy of this vector that lies within the specified area
	  */
	def shiftedInto(area: HasDimensions[HasInclusiveOrderedEnds[D]]) =
		mergeWith(area) { (p, area) => area.restrict(p) }
	/**
	  * Moves this vector into a specific area with minimal movement
	  * @param area A 1-dimensional area
	  * @return A copy of this vector that lies within that area along that dimension
	  */
	def shiftedInto(area: Dimension[HasInclusiveOrderedEnds[D]]) = mapDimension(area.axis)(area.value.restrict)
	
	private def calculateDirection(x: Double, y: Double) = math.atan2(y, x)
}