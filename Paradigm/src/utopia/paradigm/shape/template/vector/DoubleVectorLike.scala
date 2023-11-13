package utopia.paradigm.shape.template.vector

import utopia.flow.operator.equality.{EqualsExtensions, EqualsFunction}
import utopia.flow.operator.HasLength
import utopia.flow.operator.combine.LinearScalable
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.vector.Vector1D
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

/**
  * This trait is implemented by simple shape classes that can be represented as an vector of double numbers, each
  * matching an axis (X, Y, Z, ...)
  * @tparam Repr the concrete implementing class
  */
trait DoubleVectorLike[+Repr <: HasDoubleDimensions with HasLength]
	extends NumericVectorLike[Double, Repr, Repr] with LinearScalable[Repr]
{
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
	@deprecated("Please use .round instead", "v1.4")
	def rounded = map { math.round(_).toDouble }
	
	
	// IMPLEMENTED	-----------------
	
	override implicit def dimensionApproxEquals: EqualsFunction[Double] = EqualsExtensions.doubleEquals
	
	override protected def fromDoublesFactory = factory
	override def toDoublePrecision: Repr = self
	
	/**
	  * @return This vector separated to individual 1-dimensional components
	  */
	override def components: IndexedSeq[Vector1D] =
		dimensions.zipWithAxis.map { case (length, axis) => Vector1D(length, axis) }
	
	override def along(axis: Axis) = Vector1D(apply(axis), axis)
	
	override def /(div: Double) = super[NumericVectorLike]./(div)
	
	override def scaledBy(mod: Double) = this * mod
	override def scaledBy(other: HasDoubleDimensions) = this * other
	override def dividedBy(div: Double) = this / div
	override def dividedBy(other: HasDoubleDimensions) = this / other
	
	override def scaled(xScaling: Double, yScaling: Double) = this * Dimensions.double(xScaling, yScaling)
	override def scaled(modifier: Double) = this * modifier
	override def translated(translation: HasDoubleDimensions) = this + translation
	
	// Slightly optimized overrides
	override def transformedWith(transformation: Matrix2D) =
		factory.from(transformation(dimensions.padTo(2, 1.0)))
	override def transformedWith(transformation: Matrix3D) =
		factory.from(transformation(dimensions.padTo(3, 1.0)))
	
	
	// OTHER	--------------------------
	
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
}