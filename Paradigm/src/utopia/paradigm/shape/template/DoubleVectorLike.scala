package utopia.paradigm.shape.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{CanBeAboutZero, Combinable, HasLength, LinearScalable}
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.shape.shape1d.{Span1D, Vector1D}
import utopia.paradigm.shape.shape2d.{Matrix2D, Point, Size, Vector2D}
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.Transformable

import scala.math.Ordering.Double.TotalOrdering

/**
  * This trait is implemented by simple shape classes that can be represented as an vector of double numbers, each
  * matching an axis (X, Y, Z, ...)
  * @tparam Repr the concrete implementing class
  */
trait DoubleVectorLike[+Repr <: DoubleVectorLike[Repr]]
	extends Dimensional[Double, Repr]
		with LinearScalable[Repr] with Combinable[HasDoubleDimensions, Repr] with HasLength
		with VectorProjectable[Repr] with CanBeAboutZero[HasDoubleDimensions, Repr] with Transformable[Repr]
{
	// ABSTRACT ---------------------
	
	/**
	  * @return Factory used for building more copies of this vector
	  */
	protected def factory: DoubleVectorFactory[Repr]
	
	
	// COMPUTED ---------------------
	
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
	
	/**
	  * @return Direction of this vector in x-y -plane
	  */
	def direction = Angle.ofRadians(math.atan2(y, x))
	/**
	  * This vector's direction on the z-y plane
	  */
	def xDirection = Angle ofRadians calculateDirection(z, y)
	/**
	  * This vector's direction on the x-z plane
	  */
	def yDirection = Angle ofRadians calculateDirection(x, z)
	
	/**
	  * A 2D normal for this vector
	  */
	def normal2D = Vector2D(-y, x).toUnit
	
	/**
	  * @return A 2x2 matrix representation of this vector (1x2 matrix [x,y] extended to 2x2).
	  *         The resulting matrix will match the identity matrix outside the 1x2 defined range.
	  */
	def to2DMatrix = Matrix2D(
		x, y,
		0, 1
	)
	/**
	  * @return A 3x3 matrix based on this 3d vector. The natural 1x3 matrix representation [x,y,z] of this vector
	  *         is expanded to 3x3 by adding missing numbers from the identity matrix.
	  */
	def to3DMatrix = Matrix3D(
		x, y, z,
		0, 1, 0,
		0, 0, 1
	)
	
	/**
	  * @return Whether this vector is an identity vector (1, 1, ...)
	  */
	def isIdentity = dimensions.forall { _ == 1 }
	
	/**
	  * @return This vector separated to individual 1-dimensional components
	  */
	override def components: IndexedSeq[Vector1D] =
		dimensions.zipWithAxis.map { case (length, axis) => Vector1D(length, axis) }
	
	/**
	  * This vector with length of 1
	  */
	def toUnit = this / length
	
	/**
	  * a copy of this element where the coordinate values have been cut to integer numbers.
	  * This operation always rounds the numbers down, never up.
	  */
	def floor = map(math.floor)
	/**
	  * a copy of this element where the coordinate values have been increased to whole integer
	  * numbers. This operation always rounds the numbers up, never down.
	  */
	def ceil = map(math.ceil)
	/**
	  * a copy of this element where the coordinates have been rounded to nearest integer / long
	  * numbers.
	  */
	def round = map { math.round(_).toDouble }
	/**
	  * a copy of this element where the coordinates have been rounded to nearest integer / long
	  * numbers.
	  */
	@deprecated("Please use .round instead", "v2.4")
	def rounded = map { math.round(_).toDouble }
	
	/**
	  * A version of this vector where all values are at least 0
	  */
	def positive = map { _ max 0 }
	
	
	// IMPLEMENTED	-----------------
	
	override def withDimensions(newDimensions: Dimensions[Double]) = factory(newDimensions)
	
	override def isAboutZero = dimensions.forall { _ ~== 0.0 }
	
	override def length = math.sqrt(this dot this)
	
	override def +(other: HasDoubleDimensions) = mergeWith(other) { _ + _ }
	def -(other: HasDoubleDimensions) = mergeWith(other) { _ - _ }
	
	override def *(n: Double) = map { _ * n }
	
	// ab = (a . b) / (b . b) * b
	override def projectedOver[V <: DoubleVectorLike[V]](vector: V) =
		factory.from(vector * (dot(vector) / (vector dot vector)))
	
	/**
	  * Calculates the scalar projection of this vector over the other vector. This is the same as
	  * the length of this vector's projection over the other vector
	  */
	def scalarProjection(other: HasDoubleDimensions with HasLength) = dot(other) / other.length
	
	override def ~==(other: HasDoubleDimensions) = super[Dimensional].~==(other)
	
	override def transformedWith(transformation: Matrix2D) =
		factory.from(transformation(dimensions.padTo(2, 1.0)))
	override def transformedWith(transformation: Matrix3D) =
		factory.from(transformation(dimensions.padTo(3, 1.0)))
	
	override def scaled(xScaling: Double, yScaling: Double) = this * Dimensions.double(xScaling, yScaling)
	override def scaled(modifier: Double) = this * modifier
	
	override def along(axis: Axis) = Vector1D(apply(axis), axis)
	
	override def translated(translation: HasDoubleDimensions) = this + translation
	
	/**
	  * Creates a new vector with the same direction with this vector
	  * @param length The length of the new vector
	  */
	def withLength(length: Double) = toUnit * length
	
	/**
	  * This vector with increased length
	  */
	def +(n: Double) = withLength(length + n)
	
	/**
	  * This vector with decreased length (the direction may change to opposite)
	  */
	def -(n: Double) = this + (-n)
	
	
	// OTHER	--------------------------
	
	/**
	  * @param other Another vectorlike element
	  * @return This element multiplied on each axis of the provided element
	  */
	def *(other: HasDoubleDimensions) = mergeWith(other) { _ * _ }
	/**
	  * @param other Another vectorlike element
	  * @return This element divided on each axis of the provided element. Dividing by 0 is ignored
	  */
	def /(other: HasDoubleDimensions): Repr = mergeWith(other) { (a, b) => if (b == 0) a else a / b }
	
	/**
	  * @param axis Targeted axis
	  * @return A component of this vector applicable to that axis
	  */
	@deprecated("Replaced with .along(Axis)", "v1.2")
	def componentAlong(axis: Axis) = Vector1D(dimensions.getOrElse(axis.index, dimensions.zeroValue), axis)
	
	/**
	  * The dot product between this and another vector
	  */
	def dot(other: HasDoubleDimensions) = (this * other).dimensions.sum
	
	/**
	  * Maps all dimensions of this vectorlike element
	  * @param f A mapping function
	  * @return A mapped version of this element
	  */
	def map(f: Double => Double) = mapEachDimension(f)
	/**
	  * @param f A mapping function that also takes dimension index (0 for the first dimension)
	  * @return A mapped copy of this vector
	  */
	def mapWithIndex(f: (Double, Int) => Double): Repr =
		withDimensions(Dimensions.double(dimensions.zipWithIndex.map { case (d, i) => f(d, i) }))
	/**
	  * Maps all dimensional components of this vector, forming a new vector
	  * @param f A mapping function applied for all components of this vector
	  * @return A mapped copy of this vector
	  */
	def mapComponents(f: Vector1D => Double) = withDimensions(Dimensions.double(components.map(f)))
	/**
	  * Maps a single coordinate in this vectorlike element
	  * @param axis Targeted axis
	  * @param f A mapping function
	  * @return A copy of this vectorlike element with mapped coordinate
	  */
	@deprecated("Replaced with mapDimension", "v1.2")
	def mapAxis(axis: Axis)(f: Double => Double) = mapDimension(axis)(f)
	
	/**
	  * Scales this vector along a singular axis. The other axes remain unaffected.
	  * @param vector A vector that determines the scaling factor (via length) and the affected axis (vector's axis)
	  * @return A copy of this vector where one component has been scaled using the specified modifier
	  */
	def scaledAlong(vector: Vector1D) = mapDimension(vector.axis) { _ * vector.length }
	/**
	  * Divides this vector along a singular axis. The other axes remain unaffected.
	  * Please note that this vector will remain unaffected if divided by zero.
	  * @param vector A vector that determines the dividing factor (via length) and the affected axis (vector's axis)
	  * @return A copy of this vector where one component has been divided with the specified modifier
	  */
	def dividedAlong(vector: Vector1D) = if (vector.isZero) self else mapDimension(vector.axis) { _ / vector.length }
	
	/**
	  * Merges this vectorlike element with another element using a merge function. Dimensions not present in one of the
	  * elements will be treated as 0
	  * @param other Another vectorlike element
	  * @param merge A merge function
	  * @return A new element with merged or copied dimensions
	  */
	@deprecated("Replaced with mergeWith", "v1.2")
	def combineWith[A](other: HasDimensions[A])(merge: (Double, A) => Double) = mergeWith(other)(merge)
	/**
	  * Merges this vectorlike element with specified dimensions using a merge function.
	  * Dimensions not present in one of the elements will be treated as 0
	  * @param dimensions a set of dimensions
	  * @param merge A merge function
	  * @return A new element with merged or copied dimensions
	  */
	@deprecated("Replaced with mergeWith", "v1.2")
	def combineWithDimensions(dimensions: Iterable[Double])(merge: (Double, Double) => Double) =
		mergeWith(Dimensions.from(dimensions))(merge)
	
	/**
	  * @param other Another vectorlike element
	  * @return The minimum combination of these two elements where each dimension is taken from the smaller alternative
	  */
	@deprecated("Please use topLeft instead", "v1.2")
	def min(other: HasDoubleDimensions) = topLeft(other)
	/**
	  * @param other Another vectorlike element
	  * @return A maximum combination of these two elements where each dimension is taken from the larger alternative
	  */
	@deprecated("Please use bottomRight instead", "v1.2")
	def max(other: HasDoubleDimensions) = bottomRight(other)
	
	/**
	  * @param dimension A dimension to replace the matching dimension on this vector
	  * @return A copy of this vector with the specific component replacing one of this vector's components
	  */
	def withDimension(dimension: Vector1D): Repr = withDimension(dimension.axis, dimension.length)
	
	/**
	  * @param other Another vector
	  * @return The distance between the points represented by these two vectors
	  */
	def distanceFrom(other: HasDoubleDimensions) = (this - other).length
	
	/**
	  * Calculates the directional difference between these two vectors. The difference is
	  * absolute (always positive) and doesn't specify the direction of the difference.
	  */
	def angleDifference[V <: DoubleVectorLike[V]](other: V) = {
		// This vector is used as the 'x'-axis, while a perpendicular vector is used as the 'y'-axis
		// The other vector is then measured against these axes
		val x = other.projectedOver(DoubleVector(dimensions))
		val y = other - x
		
		Angle.ofRadians(math.atan2(y.length, x.length).abs)
	}
	
	/**
	  * The length of the cross product of these two vectors. |a||b|sin(a, b)
	  */
	// = |a||b|sin(a, b)e, |e| = 1 (in this we skip the e)
	def crossProductLength[V <: DoubleVectorLike[V]](other: V) =
		length * other.length * angleDifference(other).sine
	
	/**
	  * Checks whether this vector is parallel with another vector (has same or opposite direction)
	  */
	def isParallelWith[V <: DoubleVectorLike[V]](other: V) = crossProductLength(other) ~== 0.0
	/**
	  * @param axis Target axis
	  * @return Whether this vector is parallel to the specified axis
	  */
	def isParallelWith(axis: Axis): Boolean = isParallelWith(axis.unit)
	
	/**
	  * Checks whether this vector is perpendicular to another vector (ie. (1, 0) vs. (0, 1))
	  */
	def isPerpendicularTo(other: HasDoubleDimensions) = dot(other) ~== 0.0
	/**
	  * @param axis Target axis
	  * @return Whether this vector is perpendicular to the specified axis
	  */
	def isPerpendicularTo(axis: Axis): Boolean = isPerpendicularTo(axis.unit)
	
	/**
	  * Creates a new vector with the same length as this vector
	  * @param direction The direction of the new vector (on the x-y -plane)
	  */
	def withDirection(direction: Angle) = {
		val l = length
		withDimensions(Dimensions.double(direction.cosine * l, direction.sine * l))
	}
	/**
	  * Calculates this vectors direction around the specified axis
	  */
	def directionAround(axis: Axis) = axis match {
		case X => xDirection
		case Y => yDirection
		case Z => direction
	}
	
	/**
	  * Rotates this vector around a certain origin point
	  * @param rotation The amount of rotation
	  * @param origin The point this vector is rotated around
	  * @return The rotated version of this vector
	  */
	def rotatedAround(rotation: Rotation, origin: HasDoubleDimensions) =
	{
		val separator = Vector2D.from(this - origin)
		val twoDimensional = separator.withDirection(separator.direction + rotation) + origin
		
		if (dimensions.size > 2)
			withDimensions(Dimensions.double(twoDimensional.dimensions.withLength(2) ++ dimensions.drop(2)))
		else
			factory.from(twoDimensional)
	}
	
	/**
	  * Moves this vector into the specified area with minimal movement
	  * @param area An area to which this vector shall remain within
	  * @return A copy of this vector that lies within the specified area
	  */
	def shiftedInto(area: HasDimensions[HasInclusiveEnds[Double]]) =
		mergeWith(area) { (p, area) => area.restrict(p) }
	/**
	  * Moves this vector into a specific area with minimal movement
	  * @param area A 1-dimensional area
	  * @return A copy of this vector that lies within that area along that dimension
	  */
	def shiftedInto(area: Span1D) = mapDimension(area.axis)(area.restrict)
	
	private def calculateDirection(x: Double, y: Double) = math.atan2(y, x)
}