package utopia.genesis.shape.template

import utopia.genesis.shape.Axis
import utopia.genesis.shape.Axis.{X, Y, Z}
import utopia.genesis.shape.shape1D.Angle
import utopia.genesis.util.Extensions._
import utopia.genesis.util.{ApproximatelyEquatable, Arithmetic, DistanceLike}

import scala.collection.immutable.VectorBuilder
import scala.math.Ordering.Double.TotalOrdering

object VectorLike
{
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
trait VectorLike[+Repr <: VectorLike[Repr]] extends Arithmetic[Dimensional[Double], Repr] with DistanceLike
	with Dimensional[Double] with VectorProjectable[Repr] with ApproximatelyEquatable[Dimensional[Double]]
{
	// ABSTRACT	---------------------
	
	/**
	  * @return The X, Y, Z ... dimensions of this vectorlike instance. No specific length required, however.
	  */
	def dimensions: Vector[Double]
	
	/**
	  * Builds a new vectorlike instance from the provided dimensions
	  * @param dimensions A set of dimensions
	  * @return A parsed version of the dimensions
	  */
	def buildCopy(dimensions: Vector[Double]): Repr
	
	
	// IMPLEMENTED	-----------------
	
	override protected def zeroDimension = 0.0
	
	override def length = math.sqrt(this dot this)
	
	override def +(other: Dimensional[Double]) = combineWith(other) { _ + _ }
	
	override def -(other: Dimensional[Double]) = combineWith(other) { _ - _ }
	
	override def *(n: Double) = map { _ * n }
	
	// ab = (a . b) / (b . b) * b
	override def projectedOver[V <: VectorLike[V]](other: VectorLike[V]) =
		buildCopy((other * (dot(other) / (other dot other))).dimensions)
	
	/**
	  * Calculates the scalar projection of this vector over the other vector. This is the same as
	  * the length of this vector's projection over the other vector
	  */
	def scalarProjection(other: VectorLike[_]) = dot(other) / other.length
	
	override def ~==(other: Dimensional[Double]) =
	{
		val myDim = dimensions
		val theirDim = other.dimensions
		
		if (myDim.size > theirDim.size && myDim.drop(theirDim.size).exists { _ !~== 0.0 })
			false
		else if (theirDim.size > myDim.size && theirDim.drop(myDim.size).exists { _ !~== 0.0 })
			false
		else
			myDim.zip(theirDim).forall { case (a, b) => a ~== b }
	}
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return Whether all of this instance's dimensions are zero
	  */
	def isZero = dimensions.forall { _ == 0 }
	
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
	def +(dimensions: Seq[Double]) = combineDimensions(dimensions) { _ + _ }
	
	/**
	  * @param x X translation
	  * @param more Y, Z, ... translation
	  * @return A translated version of this element
	  */
	@deprecated("This method will be removed. Please use another variation of + instead", "v2.3")
	def +(x: Double, more: Double*) = combineDimensions(x +: more) { _ + _ }
	
	/**
	  * @param adjust Translation on target axis
	  * @param axis Target axis
	  * @return A copy of this element with one dimension translated
	  */
	def +(adjust: Double, axis: Axis) = mapAxis(axis) { _ + adjust }
	
	/**
	  * @param directionalAdjust Axis adjust combo
	  * @return A copy of this vector with specified dimension appended
	  */
	def +(directionalAdjust: (Axis, Double)) = mapAxis(directionalAdjust._1) { _ + directionalAdjust._2 }
	
	/**
	  * @param adjust Translation on target axis
	  * @param axis Target axis
	  * @return A copy of this element with one dimension translated
	  */
	def -(adjust: Double, axis: Axis) = this.+(-adjust, axis)
	
	/**
	  * @param directionalAdjust An axis adjust combo
	  * @return A copy of this vector with specified dimension subtracted
	  */
	def -(directionalAdjust: (Axis, Double)) = mapAxis(directionalAdjust._1) { _ - directionalAdjust._2 }
	
	/**
	  * @param dimensions Dimensions to subtract
	  * @return A copy of this vector with specified dimensions subtracted
	  */
	def -(dimensions: Seq[Double]) = combineDimensions(dimensions) { _ - _ }
	
	/**
	  * @param x X translation (negative)
	  * @param more Y, Z, ... translation (negative)
	  * @return A translated version of this element
	  */
	@deprecated("This method will be removed. Please use another variation of - instead", "v2.3")
	def -(x: Double, more: Double*) = combineDimensions(x +: more) { _ - _ }
	
	/**
	  * @param dimensions Dimensions to use as multipliers (missing dimensions will be treated as 1)
	  * @return A multiplied copy of this vector
	  */
	def *(dimensions: Seq[Double]) = combineDimensions(dimensions) { _ * _ }
	
	/**
	  * @param other Another vectorlike element
	  * @return This element multiplied on each axis of the provided element
	  */
	def *(other: Dimensional[Double]) = combineWith(other) { _ * _ }
	
	/**
	  * @param directedMultiplier Amount axis combo
	  * @return A copy of this vector multiplied only along the specified axis
	  */
	def *(directedMultiplier: (Axis, Double)) = mapAxis(directedMultiplier._1) { _ * directedMultiplier._2 }
	
	/**
	  * @param n A multiplier for specified axis
	  * @param axis Target axis
	  * @return A copy of this element with one dimension multiplied
	  */
	def *(n: Double, axis: Axis) = mapAxis(axis) { _ * n }
	
	/**
	 * @param x X modifier
	 * @param y Y modifier
	 * @param more Z, etc. modifiers (optional)
	 * @return A modified copy of this vectorlike instance
	 */
	@deprecated("This method will be removed, please use another variation of *", "v2.3")
	def *(x: Double, y: Double, more: Double*) = combineDimensions(Vector(x, y) ++ more) { _ * _ }
	
	/**
	  * @param dimensions Dimensions to use as dividers. 0s and missing dimensions are ignored (treated as 1)
	  * @return A divided copy of this vector
	  */
	def /(dimensions: Seq[Double]) = combineDimensions(dimensions) { (a, b) => if (b == 0) a else a / b }
	
	/**
	  * @param other Another vectorlike element
	  * @return This element divided on each axis of the provided element. Dividing by 0 is ignored
	  */
	def /(other: Dimensional[Double]): Repr = this / other.dimensions
	
	/**
	  * @param directedDivider A divider axis combination
	  * @return A copy of this vector divided along the specified axis
	  */
	def /(directedDivider: (Axis, Double)) = if (directedDivider._2 == 0) repr else
		mapAxis(directedDivider._1) { _ / directedDivider._2 }
	
	/**
	  * @param n A divider for target axis
	  * @param axis Target axis
	  * @return This element divided on specified axis
	  */
	def /(n: Double, axis: Axis) = if (n == 0) repr else mapAxis(axis) { _ / n }
	
	
	// OTHER	--------------------------
	
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
	def mapWithIndex(f: (Double, Int) => Double) = buildCopy(
		dimensions.zipWithIndex.map { case (d, i) => f(d, i) })
	
	/**
	  * @param f A mapping function that also takes targeted axis
	  * @return A mapped copy of this vector
	  */
	def mapWithAxis(f: (Double, Axis) => Double) = buildCopy(
		dimensions.zip(Vector(X, Y, Z)).map { case (d, a) => f(d, a) })
	
	/**
	  * Transforms a coordinate of this vectorlike element and returns the transformed element
	  * @param f The map function that maps a current coordinate into a new coordinate
	  * @param along the axis that specifies the mapped coordinate
	  * @return A copy of this element with the mapped coordinate
	  */
	@deprecated("Replaced with mapAxis", "v2.1")
	def map(f: Double => Double, along: Axis) =
	{
		val myDimensions = dimensions
		val mapIndex = indexForAxis(along)
		
		if (myDimensions.size <= mapIndex)
			repr
		else
		{
			val firstPart = myDimensions.take(mapIndex) :+ f(myDimensions(mapIndex))
			buildCopy(firstPart ++ myDimensions.drop(mapIndex + 1))
		}
	}
	
	/**
	  * Maps a single coordinate in this vectorlike element
	  * @param axis Targeted axis
	  * @param f A mapping function
	  * @return A copy of this vectorlike element with mapped coordinate
	  */
	def mapAxis(axis: Axis)(f: Double => Double) =
	{
		val myDimensions = dimensions
		val mapIndex = indexForAxis(axis)
		
		if (myDimensions.size <= mapIndex)
			buildCopy(myDimensions.padTo(mapIndex, 0.0) :+ f(0.0))
		else
		{
			val firstPart = myDimensions.take(mapIndex) :+ f(myDimensions(mapIndex))
			buildCopy(firstPart ++ myDimensions.drop(mapIndex + 1))
		}
	}
	
	/**
	  * Merges this vectorlike element with another element using a merge function. Dimensions not present in one of the
	  * elements will be treated as 0
	  * @param other Another vectorlike element
	  * @param merge A merge function
	  * @return A new element with merged or copied dimensions
	  */
	def combineWith(other: Dimensional[Double])(merge: (Double, Double) => Double) =
		combineDimensions(other.dimensions)(merge)
	
	private def combineDimensions(dimensions: Seq[Double])(merge: (Double, Double) => Double) =
	{
		val myDimensions = this.dimensions
		val otherDimensions = dimensions
		
		val builder = new VectorBuilder[Double]()
		
		// Merges common indices
		myDimensions.zip(otherDimensions).foreach { case (a, b) => builder += merge(a, b) }
		// Adds pairless items
		if (myDimensions.size > otherDimensions.size)
			myDimensions.drop(otherDimensions.size).foreach { builder += merge(_, 0.0) }
		else if (otherDimensions.size > myDimensions.size)
			otherDimensions.drop(myDimensions.size).foreach { builder += merge(0.0, _) }
		
		buildCopy(builder.result())
	}
	
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
	  * Creates a copy of this vectorlike instance with a single dimension replaced
	  * @param amount New amount for the specified dimension
	  * @param axis Axis that determines target dimension
	  * @return A copy of this vectorlike instance with specified dimension replaced
	  */
	def withDimension(amount: Double, axis: Axis) =
	{
		val targetIndex = indexForAxis(axis)
		val myDimensions = dimensions
		val newDimensions =
		{
			if (targetIndex < myDimensions.size)
				myDimensions.updated(targetIndex, amount)
			else
				myDimensions.padTo(targetIndex, 0.0) :+ amount
		}
		buildCopy(newDimensions)
	}
	
	/**
	  * @param axis Target axis
	  * @return Whether this vectorlike instance has a positive value for specified dimension
	  */
	def isPositiveAlong(axis: Axis) = along(axis) >= 0
	
	/**
	  * @param axis Target axis
	  * @return A copy of this vectorlike instance with a non-negative value for the specified dimension
	  */
	def positiveAlong(axis: Axis) =
	{
		if (isPositiveAlong(axis))
			repr
		else
			withDimension(0.0, axis)
	}
	
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
	def angleDifference(other: VectorLike[_ <: VectorLike[_]]) =
	{
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
	def crossProductLength(other: VectorLike[_ <: VectorLike[_]]) = length * other.length * angleDifference(other).sine
	
	/**
	  * Checks whether this vector is parallel with another vector (has same or opposite direction)
	  */
	def isParallelWith(other: VectorLike[_ <: VectorLike[_]]) = crossProductLength(other) ~== 0.0
	
	/**
	  * @param axis Target axis
	  * @return Whether this vector is parallel to the specified axis
	  */
	def isParallelWith(axis: Axis): Boolean = isParallelWith(axis.toUnitVector)
	
	/**
	  * Checks whether this vector is perpendicular to another vector (ie. (1, 0) vs. (0, 1))
	  */
	def isPerpendicularTo(other: Dimensional[Double]) = dot(other) ~== 0.0
	
	/**
	  * @param axis Target axis
	  * @return Whether this vector is perpendicular to the specified axis
	  */
	def isPerpendicularTo(axis: Axis): Boolean = isPerpendicularTo(axis.toUnitVector)
}