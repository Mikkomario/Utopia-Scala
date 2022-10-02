package utopia.paradigm.shape.template

import utopia.flow.operator.{ApproximatelyZeroable, Combinable, EqualsFunction, LinearMeasurable, LinearScalable}
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.paradigm.angular.Angle
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.shape.shape1d.Vector1D

import scala.math.Ordering.Double.TotalOrdering

object VectorLike
{
	/**
	  * A type alias representing a vector of any length with any 'Repr'
	  */
	type V = VectorLike[_]
	
	/**
	  * Approximate equals function for VectorLike elements
	  */
	implicit val equals: EqualsFunction[VectorLike[_]] = _ ~== _
	private implicit def doubleEquals: EqualsFunction[Double] = EqualsFunction.approxDouble
	
	/**
	  * Forms the average of the provided vectorLike elements. Elements must be non-empty.
	  * @param elements Elements
	  * @tparam A The result type
	  * @return The average of the provided VectorLike elements
	  */
	def average[A <: VectorLike[A]](elements: Iterable[A]) =
	{
		val total = elements.reduce { _ + _ }
		total / elements.size
	}
}

/**
  * This trait is implemented by simple shape classes that can be represented as an vector of double numbers, each
  * matching an axis (X, Y, Z, ...)
  * @tparam Repr the concrete implementing class
  */
trait VectorLike[+Repr <: VectorLike[Repr]]
	extends LinearScalable[Repr] with Combinable[Repr, Dimensional[Double]] with LinearMeasurable
		with Dimensional[Double] with VectorProjectable[Repr]
		with ApproximatelyZeroable[Dimensional[Double], Repr]
{
	// ABSTRACT	---------------------
	
	/**
	  * Builds a new vectorlike instance from the provided dimensions
	  * @param dimensions A set of dimensions
	  * @return A parsed version of the dimensions
	  */
	def buildCopy(dimensions: IndexedSeq[Double]): Repr
	
	
	// IMPLEMENTED	-----------------
	
	override def isZero = dimensions.forall { _ == 0.0 }
	override def isAboutZero = dimensions.forall { _ ~== 0.0 }
	override def zeroDimension = 0.0
	
	override def length = math.sqrt(this dot this)
	
	override def +(other: Dimensional[Double]) = combineWith(other) { _ + _ }
	
	def -(other: Dimensional[Double]) = combineWith(other) { _ - _ }
	
	override def *(n: Double) = map { _ * n }
	
	// ab = (a . b) / (b . b) * b
	override def projectedOver[V <: VectorLike[V]](other: VectorLike[V]) =
		buildCopy((other * (dot(other) / (other dot other))).dimensions)
	
	/**
	  * Calculates the scalar projection of this vector over the other vector. This is the same as
	  * the length of this vector's projection over the other vector
	  */
	def scalarProjection(other: Dimensional[Double] with LinearMeasurable) = dot(other) / other.length
	
	override def ~==(other: Dimensional[Double]) = super[Dimensional].~==(other)
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return This vector separated to individual 1-dimensional components
	  */
	def components =
		dimensions.zipWithIndex.map { case (length, index) => Vector1D(length, Axis(index)) }
	
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
	
	/**
	  * @return Smallest of this vector's dimensions
	  */
	def minDimension = dimensions.min
	/**
	  * @return Largest of this vector's dimensions
	  */
	def maxDimension = dimensions.max
	
	
	// OPERATORS	----------------------
	
	/**
	  * @param dimensions Dimensions to append
	  * @return A combination of this vector and specified dimensions
	  */
	def +(dimensions: Iterable[Double]) = combineWithDimensions(dimensions) { _ + _ }
	/**
	  * @param directionalAdjust Axis adjust combo
	  * @return A copy of this vector with specified dimension appended
	  */
	@deprecated("Please use +(Vector1D) instead", "v1.1")
	def +(directionalAdjust: (Axis, Double)) = mapAxis(directionalAdjust._1) { _ + directionalAdjust._2 }
	
	/**
	  * @param directionalAdjust An axis adjust combo
	  * @return A copy of this vector with specified dimension subtracted
	  */
	@deprecated("Please use -(Vector1D) instead", "v1.1")
	def -(directionalAdjust: (Axis, Double)) = mapAxis(directionalAdjust._1) { _ - directionalAdjust._2 }
	/**
	  * @param dimensions Dimensions to subtract
	  * @return A copy of this vector with specified dimensions subtracted
	  */
	def -(dimensions: Iterable[Double]) = combineWithDimensions(dimensions) { _ - _ }
	
	/**
	  * @param dimensions Dimensions to use as multipliers (missing dimensions will be treated as 1)
	  * @return A multiplied copy of this vector
	  */
	def *(dimensions: Iterable[Double]) = combineWithDimensions(dimensions) { _ * _ }
	/**
	  * @param other Another vectorlike element
	  * @return This element multiplied on each axis of the provided element
	  */
	def *(other: Dimensional[Double]) = combineWith(other) { _ * _ }
	/**
	  * @param directedMultiplier Amount axis combo
	  * @return A copy of this vector multiplied only along the specified axis
	  */
	@deprecated("Please use scaledAlong(Double, Axis) instead", "v1.1")
	def *(directedMultiplier: (Axis, Double)) = mapAxis(directedMultiplier._1) { _ * directedMultiplier._2 }
	
	/**
	  * @param dimensions Dimensions to use as dividers. 0s and missing dimensions are ignored (treated as 1)
	  * @return A divided copy of this vector
	  */
	def /(dimensions: Iterable[Double]) = combineWithDimensions(dimensions) { (a, b) => if (b == 0) a else a / b }
	/**
	  * @param other Another vectorlike element
	  * @return This element divided on each axis of the provided element. Dividing by 0 is ignored
	  */
	def /(other: Dimensional[Double]): Repr = this / other.dimensions
	/**
	  * @param directedDivider A divider axis combination
	  * @return A copy of this vector divided along the specified axis
	  */
	@deprecated("Please use dividedAlong(Double, Axis) instead", "v1.1")
	def /(directedDivider: (Axis, Double)) = if (directedDivider._2 == 0) repr else
		mapAxis(directedDivider._1) { _ / directedDivider._2 }
	
	
	// OTHER	--------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return A component of this vector applicable to that axis
	  */
	def componentAlong(axis: Axis) = Vector1D(dimensions.getOrElse(axis.index, zeroDimension), axis)
	
	/**
	  * The dot product between this and another vector
	  */
	def dot(other: Dimensional[Double]) = (this * other).dimensions.sum
	
	/**
	  * Maps all dimensions of this vectorlike element
	  * @param f A mapping function
	  * @return A mapped version of this element
	  */
	def map(f: Double => Double) = buildCopy(dimensions.map(f))
	/**
	  * @param f A mapping function that also takes dimension index (0 for the first dimension)
	  * @return A mapped copy of this vector
	  */
	def mapWithIndex(f: (Double, Int) => Double) =
		buildCopy(dimensions.zipWithIndex.map { case (d, i) => f(d, i) })
	/**
	  * Maps all dimensional components of this vector, forming a new vector
	  * @param f A mapping function applied for all components of this vector
	  * @return A mapped copy of this vector
	  */
	def mapComponents(f: Vector1D => Double) = buildCopy(components.map(f))
	/**
	  * @param f A mapping function that also takes targeted axis
	  * @return A mapped copy of this vector
	  */
	@deprecated("Please use mapComponents instead", "v1.1")
	def mapWithAxis(f: (Double, Axis) => Double) = buildCopy(
		dimensions.zip(Vector[Axis](X, Y, Z)).map { case (d, a) => f(d, a) })
	/**
	  * Maps a single coordinate in this vectorlike element
	  * @param axis Targeted axis
	  * @param f A mapping function
	  * @return A copy of this vectorlike element with mapped coordinate
	  */
	def mapAxis(axis: Axis)(f: Double => Double) = {
		val myDimensions = dimensions
		val mapIndex = axis.index
		
		if (myDimensions.size <= mapIndex)
			buildCopy(myDimensions.padTo(mapIndex, 0.0) :+ f(0.0))
		else {
			val firstPart = myDimensions.take(mapIndex) :+ f(myDimensions(mapIndex))
			buildCopy(firstPart ++ myDimensions.drop(mapIndex + 1))
		}
	}
	
	/**
	  * Scales this vector along a singular axis. The other axes remain unaffected.
	  * @param vector A vector that determines the scaling factor (via length) and the affected axis (vector's axis)
	  * @return A copy of this vector where one component has been scaled using the specified modifier
	  */
	def scaledAlong(vector: Vector1D) = mapAxis(vector.axis) { _ * vector.length }
	/**
	  * Divides this vector along a singular axis. The other axes remain unaffected.
	  * Please note that this vector will remain unaffected if divided by zero.
	  * @param vector A vector that determines the dividing factor (via length) and the affected axis (vector's axis)
	  * @return A copy of this vector where one component has been divided with the specified modifier
	  */
	def dividedAlong(vector: Vector1D) = if (vector.isZero) repr else mapAxis(vector.axis) { _ / vector.length }
	
	/**
	  * Merges this vectorlike element with another element using a merge function. Dimensions not present in one of the
	  * elements will be treated as 0
	  * @param other Another vectorlike element
	  * @param merge A merge function
	  * @return A new element with merged or copied dimensions
	  */
	def combineWith[A](other: Dimensional[A])(merge: (Double, A) => Double) =
		buildCopy(dimensions.zipPad(other.dimensions, zeroDimension, other.zeroDimension)
			.map { case (a, b) => merge(a, b) })
	/**
	  * Merges this vectorlike element with specified dimensions using a merge function.
	  * Dimensions not present in one of the elements will be treated as 0
	  * @param dimensions a set of dimensions
	  * @param merge A merge function
	  * @return A new element with merged or copied dimensions
	  */
	def combineWithDimensions(dimensions: Iterable[Double])(merge: (Double, Double) => Double) =
		buildCopy(this.dimensions.zipPad(dimensions, 0.0).map { case (a, b) => merge(a, b) })
	
	/**
	  * @param other Another vectorlike element
	  * @return The minimum combination of these two elements where each dimension is taken from the smaller alternative
	  */
	def min(other: Dimensional[Double]) = combineWith(other) { _ min _ }
	/**
	  * The top left corner of a bounds between these two elements. In other words,
	  * creates a vector that has the smallest available value on each axis from the two candidates
	  * @param other Another element
	  * @return a minimum of these two elements on each axis
	  */
	def topLeft(other: Dimensional[Double]) = this min other
	/**
	  * @param other Another vectorlike element
	  * @return A maximum combination of these two elements where each dimension is taken from the larger alternative
	  */
	def max(other: Dimensional[Double]) = combineWith(other) { _ max _ }
	/**
	  * The bottom right corner of a bounds between the two vertices. In other words,
	  * creates a vector that has the largest available value on each axis from the two candidates
	  * @param other Another element
	  * @return A maximum of these two elements on each axis
	  */
	def bottomRight(other: Dimensional[Double]) = combineWith(other) { _ max _ }
	
	/**
	  * @param dimension A dimension to replace the matching dimension on this vector
	  * @return A copy of this vector with the specific component replacing one of this vector's components
	  */
	def withDimension(dimension: Vector1D) = {
		val targetIndex = dimension.axis.index
		val myDimensions = dimensions
		val newDimensions = {
			if (targetIndex < myDimensions.size)
				myDimensions.updated(targetIndex, dimension.length)
			else
				myDimensions.padTo(targetIndex, zeroDimension) :+ dimension.length
		}
		buildCopy(newDimensions)
	}
	/**
	  * Creates a copy of this vectorlike instance with a single dimension replaced
	  * @param amount New amount for the specified dimension
	  * @param axis Axis that determines target dimension
	  * @return A copy of this vectorlike instance with specified dimension replaced
	  */
	@deprecated("Please use withDimension(Vector1D) instead", "v1.1")
	def withDimension(amount: Double, axis: Axis): Repr = withDimension(Vector1D(amount, axis))
	
	/**
	  * @param axis Target axis
	  * @return Whether this vectorlike instance has a positive value for specified dimension
	  */
	def isPositiveAlong(axis: Axis) = along(axis) >= 0
	/**
	  * @param axis Target axis
	  * @return A copy of this vectorlike instance with a non-negative value for the specified dimension
	  */
	def positiveAlong(axis: Axis) = if (isPositiveAlong(axis)) repr else withDimension(Vector1D.zeroAlong(axis))
	
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
	
	/**
	  * @param other Another vector
	  * @return The distance between the points represented by these two vectors
	  */
	def distanceFrom(other: Dimensional[Double]) = (this - other).length
	
	/**
	  * Calculates the directional difference between these two vectors. The difference is
	  * absolute (always positive) and doesn't specify the direction of the difference.
	  */
	def angleDifference(other: VectorLike[_ <: VectorLike[_]]) = {
		// This vector is used as the 'x'-axis, while a perpendicular vector is used as the 'y'-axis
		// The other vector is then measured against these axes
		val x: VectorLike[_] = other.projectedOver[VectorLike[Repr]](this)
		val y = other - x
		
		Angle.ofRadians(math.atan2(y.length, x.length).abs)
	}
	
	/**
	  * The length of the cross product of these two vectors. |a||b|sin(a, b)
	  */
	// = |a||b|sin(a, b)e, |e| = 1 (in this we skip the e)
	def crossProductLength(other: VectorLike[_ <: VectorLike[_]]) =
		length * other.length * angleDifference(other).sine
	
	/**
	  * Checks whether this vector is parallel with another vector (has same or opposite direction)
	  */
	def isParallelWith(other: VectorLike[_ <: VectorLike[_]]) = crossProductLength(other) ~== 0.0
	/**
	  * @param axis Target axis
	  * @return Whether this vector is parallel to the specified axis
	  */
	def isParallelWith(axis: Axis): Boolean = isParallelWith(axis.unit)
	
	/**
	  * Checks whether this vector is perpendicular to another vector (ie. (1, 0) vs. (0, 1))
	  */
	def isPerpendicularTo(other: Dimensional[Double]) = dot(other) ~== 0.0
	/**
	  * @param axis Target axis
	  * @return Whether this vector is perpendicular to the specified axis
	  */
	def isPerpendicularTo(axis: Axis): Boolean = isPerpendicularTo(axis.unit)
	
	/**
	  * @param other Another dimensional item
	  * @return True if this vector is smaller or equal on all dimensions, compared to the other item.
	  *         False if this vector is larger on all dimensions, compared to the other item.
	  *         Undefined otherwise.
	  */
	def <=(other: Dimensional[Double]) = compareDimensions(other) { _ <= _ }
	/**
	  * @param other Another dimensional item
	  * @return True if this vector is larger or equal on all dimensions, compared to the other item.
	  *         False if this vector is smaller on all dimensions, compared to the other item.
	  *         Undefined otherwise.
	  */
	def >=(other: Dimensional[Double]) = compareDimensions(other) { _ >= _ }
	/**
	  * @param other Another dimensional item
	  * @return True if this vector is larger on all dimensions, compared to the other item.
	  *         False if this vector is smaller or equal on all dimensions, compared to the other item.
	  *         Undefined otherwise.
	  */
	def <(other: Dimensional[Double]) = compareDimensions(other) { _ < _ }
	/**
	  * @param other Another dimensional item
	  * @return True if this vector is larger on all dimensions, compared to the other item.
	  *         False if this vector is smaller or equal on all dimensions, compared to the other item.
	  *         Undefined otherwise.
	  */
	def >(other: Dimensional[Double]) = compareDimensions(other) { _ > _ }
}