package utopia.paradigm.shape.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.operator.MayBeZero
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.enumeration.{Axis, Axis2D}
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.paradigm.shape.shape1d.Dimension

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{IndexedSeqOps, mutable}
import scala.language.implicitConversions

object Dimensions
{
	// TYPES    -----------------------
	
	type DoubleDimensions = Dimensions[Double]
	
	
	// ATTRIBUTES   -------------------
	
	/**
	  * A factory for building dimensions that consist of double numbers
	  */
	val double = apply(0.0)
	/**
	  * A factory for building dimensions that consists of integer numbers
	  */
	val int = apply(0)
	
	private val distanceFactoryCache = Cache { unit: DistanceUnit => new DistanceDimensionsFromDoublesFactory(unit) }
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return A factory for constructing distance-based dimensions (not predefining any distance unit)
	  * @see [[in]]
	  */
	def distance = DistanceDimensionsFactory
	
	
	// IMPLICIT -----------------------
	
	implicit def doubleFactory(d: Dimensions.type): DimensionsFactory[Double] = d.double
	// implicit def doubleDimensions(d: Iterable[Double]): Dimensions[Double] = double.from(d)
	
	
	// OTHER    -----------------------
	
	/**
	  * @param zero A zero dimension amount
	  * @tparam A Type of dimensions used
	  * @return A factory for building dimension sets
	  */
	def apply[A](zero: A) = new DimensionsFactory[A](zero)
	
	/**
	  * @param unit A unit of distance
	  * @return A factory for building dimension sets in / defaulting to that distance unit
	  * @see [[distance]]
	  */
	def in(unit: DistanceUnit) = distanceFactoryCache(unit)
	
	
	// NESTED   -----------------------
	
	/**
	  * A factory used for converting dimension sequences into a set of dimensions
	  * @param zeroValue Zero dimension value (lazily called)
	  * @tparam A Type of dimension values applied
	  */
	class DimensionsFactory[A](zeroValue: => A) extends DimensionalFactory[A, Dimensions[A]]
	{
		// ATTRIBUTES   -------------------------
		
		private val lazyZero = Lazy { zeroValue }
		
		/**
		  * An empty set of dimensions (0 length)
		  */
		override lazy val empty = apply(Empty)
		/**
		  * A set of dimensions with length 1 with 0 value
		  */
		lazy val zero1D = apply(zero)
		/**
		  * A set of dimensions with length of 2 with zeros as values
		  */
		lazy val zero2D = apply(Pair.twice(zero))
		/**
		  * A set of dimensions with length of 3 with zeros as values
		  */
		lazy val zero3D = apply(Vector.fill(3)(zero))
		
		
		// COMPUTED ------------------------------
		
		/**
		  * @return The zero value used by this factory
		  */
		def zero = lazyZero.value
		
		
		// IMPLEMENTED ---------------------------
		
		override def newBuilder = new DimensionsBuilder[A](lazyZero)
		
		override def apply(values: IndexedSeq[A]) = new Dimensions(lazyZero, values)
		override def apply(values: Map[Axis, A]): Dimensions[A] = {
			if (values.isEmpty)
				empty
			else
				apply(Axis.values.take(values.keysIterator.map { _.index }.max + 1).map { a => values.getOrElse(a, zero) })
		}
		override def from(values: IterableOnce[A]) = values match {
			case d: Dimensions[A] => d
			case s: IndexedSeq[A] => apply(s)
			case o => apply(IndexedSeq.from(o))
		}
		
		
		// OTHER    ----------------------------
		
		/**
		  * @param axis Axis that will contain the assigned value
		  * @param value Value to assign to the specified axis
		  * @return A set of dimensions with 0-1 non-zero values
		  */
		def apply(axis: Axis, value: A): Dimensions[A] = {
			if (axis.index == 0)
				new Dimensions[A](lazyZero, Single(value))
			else
				apply(Vector.fill(axis.index)(zero) :+ value)
		}
	}
	
	object DistanceDimensionsFactory extends DimensionalFactory[Distance, Dimensions[Distance]]
	{
		// IMPLEMENTED  ---------------------
		
		override def newBuilder: DimensionalBuilder[Distance, Dimensions[Distance]] =
			new DimensionsBuilder[Distance](Lazy.initialized(Distance.zero))
		
		override def apply(values: IndexedSeq[Distance]): Dimensions[Distance] = {
			new Dimensions[Distance](Lazy {
				values.headOption match {
					case Some(x) => Distance(0.0, x.unit)
					case None => Distance.zero
				}
			}, values)
		}
		override def apply(values: Map[Axis, Distance]): Dimensions[Distance] = {
			if (values.isEmpty)
				empty
			else {
				val zero = Lazy { Distance(0.0, values.valuesIterator.next().unit) }
				new Dimensions[Distance](zero,
					Axis.values.take(values.keysIterator.map { _.index }.max + 1).map { values.getOrElse(_, zero.value) })
			}
		}
		
		override def from(values: IterableOnce[Distance]): Dimensions[Distance] = values match {
			case d: Dimensions[Distance] => d
			case i => apply(OptimizedIndexedSeq.from(i))
		}
	}
	
	class DistanceDimensionsFromDoublesFactory(unit: DistanceUnit)
		extends DimensionsWrapperFactory[Double, Dimensions[Distance]]
	{
		// ATTRIBUTES   ---------------------
		
		override val zeroDimension: Double = 0.0
		private val lazyZeroValue = Lazy { Distance(0.0, unit) }
		
		
		// IMPLEMENTED  ---------------------
		
		override def newBuilder: DimensionalBuilder[Double, Dimensions[Distance]] =
			double.newBuilder.mapResult { d => apply(d.dimensions) }
		
		override def apply(dimensions: Dimensions[Double]): Dimensions[Distance] =
			new Dimensions[Distance](lazyZeroValue, dimensions.values.map { Distance(_, unit) })
		override def apply(values: IndexedSeq[Double]): Dimensions[Distance] =
			new Dimensions[Distance](lazyZeroValue, values.map { Distance(_, unit) })
		
		override def from(other: HasDimensions[Double]): Dimensions[Distance] = apply(other.dimensions)
		override def from(values: IterableOnce[Double]): Dimensions[Distance] =
			new Dimensions(lazyZeroValue, values.iterator.map { Distance(_, unit) }.toOptimizedSeq)
	}
}

/**
  * Represents a set of dimensions (X, Y, Z, ...)
  * @author Mikko Hilpinen
  * @since 5.11.2022, v1.2
  */
class Dimensions[+A](val lazyZeroValue: Lazy[A], val values: IndexedSeq[A])
	extends IndexedSeq[A] with IndexedSeqOps[A, IndexedSeq, Dimensions[A]]
		with HasDimensions[A] with MayBeZero[Dimensions[A]] with Equals
{
	// COMPUTED ------------------------------
	
	/**
	  * @return A value that represents a dimension with the value of zero
	  */
	def zeroValue = lazyZeroValue.value
	
	/**
	  * @return A 2D copy of these dimensions. Will only contain the X and Y values.
	  */
	def in2D = withLength(2)
	/**
	  * @return A 3D copy of these dimensions. Will contain X, Y and Z values.
	  */
	def in3D = withLength(3)
	
	/**
	  * @return Dimensions, coupled with their correlating axes
	  */
	def zipWithAxis = values zip Axis.values
	/**
	  * @return 0-2 first dimensions, coupled with their correlating axes
	  */
	def zipWithAxis2D = values zip Axis2D.values
	/**
	  * @return An iterator that returns dimensions along with their axes
	  */
	def zipWithAxisIterator = values.iterator zip Axis.values
	/**
	  * @return An iterator that returns the first 0-2 dimensions, along with their axes
	  */
	def zipWithAxis2DIterator = values.iterator zip Axis2D.values
	/**
	  * @return A map based on these dimensions, where axes are used as keys.
	  */
	def toMap = Axis.values.zip(values).toMap
	
	/**
	  * @param equals An implicit equals function to utilize
	  * @return Whether this set of dimensions is equal to a set of dimensions where each value is zero,
	  *         when using the implied equals function
	  */
	def isAboutZero(implicit equals: EqualsFunction[A]) = values.forall { _ ~== zeroValue }
	/**
	  * @param equals An implicit equals function to utilize
	  * @return Whether this set of dimensions is NOT equal to a set of dimensions where each value is zero,
	  *         when using the implied equals function
	  */
	def isNotAboutZero(implicit equals: EqualsFunction[A]) = !isAboutZero
	
	
	// IMPLEMENTED  --------------------------
	
	override def self = this
	
	override def empty = withDimensions(Empty)
	override def zero: Dimensions[A] = withDimensions[A](Vector.fill[A](length)(zeroValue))
	override def isZero = values.forall { _ == zeroValue }
	
	override def dimensions: Dimensions[A] = this
	/**
	  * @return Components of these dimensions
	  */
	override def components: IndexedSeq[Dimension[A]] =
		zipWithAxis.map { case (v, axis) => Dimension(axis, v, lazyZeroValue) }
	
	override def length = values.length
	
	override def toVector = values.toVector
	override def toString = s"[${values.mkString(", ")}]"
	
	override def hashCode = foldLeft(1)((result, property) => 31 * result + property.hashCode())
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		withDimensions(OptimizedIndexedSeq.from(coll))
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, Dimensions[A]] =
		new DimensionsBuilder[A](lazyZeroValue)
	
	override def apply(i: Int) = {
		if (i < 0 || i >= length)
			zeroValue
		else
			values(i)
	}
	/**
	  * @param axis Targeted axis
	  * @return Dimension of this item along that axis
	  */
	override def apply(axis: Axis): A = apply(axis.index)
	
	override def slice(from: Int, until: Int) = withDimensions(values.slice(from, until))
	
	override def map[B](f: A => B) = new Dimensions(lazyZeroValue.map(f), values.map(f))
	
	override def padTo[B >: A](len: Int, elem: B) = withDimensions(super.padTo(len, elem))
	
	// TODO: Implement
	override def canEqual(that: Any) = that.isInstanceOf[Dimensions[_]] || that.isInstanceOf[HasDimensions[_]]
	override def sameElements[B >: A](o: IterableOnce[B]) = {
		val myIter = iterator
		val theirIter = o.iterator
		
		var areSame = true
		while (areSame && myIter.hasNext && theirIter.hasNext) {
			areSame = myIter.next() == theirIter.next()
		}
		
		areSame && myIter.forall { _ == zeroValue } && theirIter.forall { _ == zeroValue }
	}
	override def equals(o: Any) = o match {
		case d: Dimensions[_] => sameElements(d) && (zeroValue: Any) == d.zeroValue
		case d: HasDimensions[_] =>
			val otherDimensions = d.dimensions
			sameElements(otherDimensions) && (zeroValue: Any) == otherDimensions.zeroValue
		case _ => false
	}
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param length Targeted number of dimensions
	  * @return An iterator of these dimensions that contains 'length' many items
	  */
	def iteratorOfLength(length: Int) = {
		if (this.length == length)
			iterator
		else if (this.length < length)
			iterator.padTo(length, zeroValue)
		else
			iterator.take(length)
	}
	
	/**
	  * @param component A component / dimension
	  * @return Whether these dimensions contains an equal dimension
	  */
	def contains(component: Dimension[Any]) = apply(component.axis) == component.value
	
	/**
	  * @param length New length to apply to this set of dimensions
	  * @return A copy of this set of dimensions with exactly that many dimensions
	  */
	def withLength(length: Int) = {
		if (this.length == length)
			this
		else if (this.length < length)
			withDimensions(values.padTo(length, zeroValue))
		else
			withDimensions(values.take(length))
	}
	def padTo(length: Int): Dimensions[A] = padTo(length, zeroValue)
	
	/**
	  * @param other Another set of dimensions
	  * @tparam B Type of values in the other set of dimensions
	  * @return An iterator that iterates over the values from both these dimensions
	  */
	def zipIteratorWith[B](other: Dimensions[B]) =
		values.iterator.zipPad(other.values.iterator, zeroValue, other.zeroValue)
	/**
	  * Combines these dimensions with other dimensions
	  * @param other Other set of dimensions
	  * @tparam B Type of values in other dimensions
	  * @return A combination of these dimensions, where each value is a tuple
	  */
	def zip[B](other: Dimensions[B]) =
		new Dimensions(Lazy { (zeroValue, other.zeroValue) },
			values.iterator.zipPad(other.values.iterator, zeroValue, other.zeroValue).toOptimizedSeq)
	/**
	  * Combines these dimensions with other dimensions
	  * @param other Other set of dimensions
	  * @tparam B Type of values in other dimensions
	  * @return A combination of these dimensions, where each value is a [[Pair]]
	  */
	def pairWith[B >: A](other: Dimensions[B]) =
		new Dimensions(Lazy { Pair(zeroValue, other.zeroValue) },
			values.iterator.zipPad(other.iterator, zeroValue, other.zeroValue)
				.map { case (a, b) => Pair(a, b) }.toOptimizedSeq)
	/**
	  * Combines these dimensions with other dimensions
	  * @param other Other set of dimensions
	  * @param merge A merging function to combine the two values
	  * @tparam B Type of values in other dimensions
	  * @tparam C Type of merge result
	  * @return A set of dimensions consisting of merge results
	  */
	def mergeWith[B, C >: A](other: HasDimensions[B])(merge: (A, B) => C) = {
		val d2 = other.dimensions
		new Dimensions(lazyZeroValue, values.iterator.zipPad(d2.values.iterator, zeroValue, d2.zeroValue)
			.map { case (a, b) => merge(a, b) }.toOptimizedSeq)
	}
	/**
	  * Combines these dimensions with other dimensions
	  * @param other Other set of dimensions
	  * @param zero A zero value to use in the new set of dimensions (lazily called)
	  * @param merge A merging function to combine the two values
	  * @tparam B Type of values in other dimensions
	  * @tparam C Type of merge result
	  * @return A set of dimensions consisting of merge results
	  */
	def mergeWith[B, C](other: HasDimensions[B], zero: => C)(merge: (A, B) => C) = {
		val d2 = other.dimensions
		new Dimensions(Lazy { zero },
			values.iterator.zipPad(d2.values.iterator, zeroValue, d2.zeroValue)
				.map { case (a, b) => merge(a, b) }.toOptimizedSeq)
	}
	
	/**
	  * @param other Another set of dimensions
	  * @param equals Equality function for testing values
	  * @tparam B Type of values in the other item
	  * @return Whether these two sets of dimensions are equal in terms of the specified equality function
	  */
	def ~==[B >: A](other: Dimensions[B])(implicit equals: EqualsFunction[B]) =
		zipIteratorWith(other).forall { case (a, b) => a ~== b }
	/**
	  * @param other Another set of dimensions
	  * @param equals Equality function for testing values
	  * @tparam B Type of values in the other item
	  * @return Whether these two sets of dimensions are NOT equal in terms of the specified equality function
	  */
	def !~==[B >: A](other: Dimensions[B])(implicit equals: EqualsFunction[B]) = !(this ~== other)
	
	/**
	  * Replaces one of the dimensions in this set
	  * @param axis Targeted axis
	  * @param dimension New dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of these dimensions with that dimension
	  */
	def withDimension[B >: A](axis: Axis, dimension: B) = {
		if (axis.index < length)
			withDimensions((values.take(axis.index) :+ dimension) ++ values.drop(axis.index + 1))
		else
			withDimensions(values.padTo(axis.index, zeroValue) :+ dimension)
	}
	/**
	  * Replaces this item's x-dimension
	  * @param x The new x-dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of this item with that dimension
	  */
	def withX[B >: A](x: B) = withDimension(X, x)
	/**
	  * Replaces this item's y-dimension
	  * @param y The new y-dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of this item with that dimension
	  */
	def withY[B >: A](y: B) = withDimension(Y, y)
	/**
	  * Replaces this item's z-dimension
	  * @param z The new z-dimension to assign
	  * @tparam B Type of new dimension
	  * @return A copy of this item with that dimension
	  */
	def withZ[B >: A](z: B) = withDimension(Z, z)
	/**
	  * Creates a new set of dimensions that uses the same zero value
	  * @param dimensions New dimensions
	  * @tparam B Type of new dimensions
	  * @return A new set of dimensions with the same zero value
	  */
	def withDimensions[B >: A](dimensions: IndexedSeq[B]) = new Dimensions[B](lazyZeroValue, dimensions)
	/**
	  * Replaces 0-n dimensions in this set
	  * @param dimensions Dimensions to assign (axis -> dimension)
	  * @tparam B Type of new dimensions
	  * @return A copy of these dimensions with those assignments applied
	  */
	def withDimensions[B >: A](dimensions: Map[Axis, B]): Dimensions[B] = {
		dimensions.keysIterator.maxByOption { _.index } match {
			case Some(maxAxis) =>
				val overlap = zipWithAxis.map { case (v, axis) => dimensions.getOrElse(axis, v) }
				if (maxAxis.index < length)
					withDimensions(overlap)
				else
					withDimensions(overlap ++ Axis.values.drop(length).map { a => dimensions.getOrElse(a, zeroValue) })
			case None => this
		}
	}
	
	/**
	  * @param newZero New zero value to use
	  * @param f Mapping function for specified values
	  * @tparam B New dimension type
	  * @return A mapped copy of these dimensions
	  */
	def mapWithZero[B](newZero: => B)(f: A => B) = new Dimensions[B](Lazy(newZero), values.map(f))
	/**
	  * Maps all dimensions in this set
	  * @param f A mapping function that accepts a dimension and the axis on which the dimension applies.
	  *          Returns a modified dimension.
	  * @tparam B Type of new dimensions.
	  * @return A modified copy of these dimensions.
	  */
	def mapWithAxes[B >: A](f: (A, Axis) => B) =
		new Dimensions(lazyZeroValue, zipWithAxis.map { case (d, a) => f(d, a) })
	/**
	  * Alters the value of a single dimension in this set
	  * @param axis Targeted axis
	  * @param f A function that accepts the current dimension and returns a modified copy
	  * @tparam B Type of new dimension
	  * @return A copy of these dimensions where the original dimension has been replaced with the modified copy
	  */
	def mapDimension[B >: A](axis: Axis)(f: A => B) = withDimension(axis, f(apply(axis)))
	/**
	  * Modifies the x-dimension of this item
	  * @param f A mapping function called for the x-dimension
	  * @tparam B Type of mapping result
	  * @return A copy of this item with the mapped dimension
	  */
	def mapX[B >: A](f: A => B) = mapDimension[B](X)(f)
	/**
	  * Modifies the y-dimension of this item
	  * @param f A mapping function called for the y-dimension
	  * @tparam B Type of mapping result
	  * @return A copy of this item with the mapped dimension
	  */
	def mapY[B >: A](f: A => B) = mapDimension[B](Y)(f)
	/**
	  * Modifies the z-dimension of this item
	  * @param f A mapping function called for the z-dimension
	  * @tparam B Type of mapping result
	  * @return A copy of this item with the mapped dimension
	  */
	def mapZ[B >: A](f: A => B) = mapDimension[B](Z)(f)
	/**
	  * Alters the value of 0-n dimensions in this set
	  * @param axes Targeted axes
	  * @param f A function that accepts the current dimension and returns a modified copy
	  * @tparam B Type of new dimension
	  * @return A copy of these dimensions where the original dimensions have been replaced with the modified copies,
	  *         where applicable
	  */
	def mapDimensions[B >: A](axes: IterableOnce[Axis])(f: A => B) =
		withDimensions(Set.from(axes).map { a => a -> f(apply(a)) }.toMap)
	/**
	  * Alters the value of 2-n dimensions in this set
	  * @param axis1 First targeted axis
	  * @param axis2 Second targeted axis
	  * @param more More targeted axes
	  * @param f A function that accepts the current dimension and returns a modified copy
	  * @tparam B Type of new dimension
	  * @return A copy of these dimensions where the original dimensions have been replaced with the modified copies,
	  *         where applicable
	  */
	def mapDimensions[B >: A](axis1: Axis, axis2: Axis, more: Axis*)(f: A => B): Dimensions[B] =
		mapDimensions[B](Set(axis1, axis2) ++ more)(f)
}