package utopia.paradigm.shape.template

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.enumeration.Axis
import Axis._
import utopia.flow.operator.EqualsFunction
import utopia.flow.operator.EqualsExtensions._

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{IndexedSeqOps, mutable}

/**
  * Represents a set of dimensions (X, Y, Z, ...)
  * @author Mikko Hilpinen
  * @since 5.11.2022, v1.2
  */
case class Dimensions[+A](zero: A, values: Vector[A])
	extends IndexedSeq[A] with IndexedSeqOps[A, IndexedSeq, Dimensions[A]]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return The x-component of these dimensions
	  */
	def x = apply(X)
	/**
	  * @return The y-component of these dimensions
	  */
	def y = apply(Y)
	/**
	  * @return The z-component of these dimensions
	  */
	def z = apply(Z)
	
	/**
	  * @return The x and y dimensions as a pair
	  */
	def xyPair = Pair(x, y)
	
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
	  * @return A map based on these dimensions, where axes are used as keys.
	  */
	def toMap = Axis.values.zip(values).toMap
	
	
	// IMPLEMENTED  --------------------------
	
	override def length = values.length
	
	override def empty = copy(values = Vector())
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		copy(values = Vector.from(coll))
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, Dimensions[A]] =
		new DimensionsBuilder[A](zero)
	
	override def toVector = values
	
	override def toString() = s"[${values.mkString(", ")}]"
	
	override def slice(from: Int, until: Int) = copy(values = values.slice(from, until))
	
	override def apply(i: Int) = {
		if (i < 0 || i >= length)
			zero
		else
			values(i)
	}
	
	override def map[B](f: A => B) = Dimensions(f(zero), values.map(f))
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return Dimension of this item along that axis
	  */
	def apply(axis: Axis): A = apply(axis.index)
	
	/**
	  * @param length New length to apply to this set of dimensions
	  * @return A copy of this set of dimensions with exactly that many dimensions
	  */
	def withLength(length: Int) = {
		if (this.length == length)
			this
		else if (this.length < length)
			copy(values = values.padTo(length, zero))
		else
			copy(values = values.take(length))
	}
	
	/**
	  * @param other Another set of dimensions
	  * @tparam B Type of values in the other set of dimensions
	  * @return An iterator that iterates over the values from both these dimensions
	  */
	def zipIteratorWith[B](other: Dimensions[B]) =
		values.iterator.zipPad(other.values.iterator, zero, other.zero)
	/**
	  * Combines these dimensions with other dimensions
	  * @param other Other set of dimensions
	  * @tparam B Type of values in other dimensions
	  * @return A combination of these dimensions, where each value is a tuple
	  */
	def zip[B](other: Dimensions[B]) =
		Dimensions((zero, other.zero), values.iterator.zipPad(other.values.iterator, zero, other.zero).toVector)
	/**
	  * Combines these dimensions with other dimensions
	  * @param other Other set of dimensions
	  * @tparam B Type of values in other dimensions
	  * @return A combination of these dimensions, where each value is a [[Pair]]
	  */
	def pairWith[B >: A](other: Dimensions[B]) =
		Dimensions(Pair(zero, other.zero),
			values.iterator.zipPad(other.iterator, zero, other.zero).map { case (a, b) => Pair(a, b) }.toVector)
	/**
	  * Combines these dimensions with other dimensions
	  * @param other Other set of dimensions
	  * @param merge A merging function to combine the two values
	  * @tparam B Type of values in other dimensions
	  * @tparam C Type of merge result
	  * @return A set of dimensions consisting of merge results
	  */
	def mergeWith[B, C >: A](other: Dimensions[B])(merge: (A, B) => C) =
		Dimensions(zero, values.iterator.zipPad(other.values.iterator, zero, other.zero)
			.map { case (a, b) => merge(a, b) }.toVector)
	
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
	
	def withDimension[B >: A](axis: Axis, dimension: B) =
		copy(values = (values.take(axis.index) :+ dimension) ++ values.drop(axis.index + 1))
	def withDimensions[B >: A](dimensions: Map[Axis, B]) = {
		dimensions.keysIterator.maxByOption { _.index } match {
			case Some(maxAxis) =>
				val overlap = zipWithAxis.map { case (v, axis) => dimensions.getOrElse(axis, v) }
				if (maxAxis.index < length)
					copy(values = overlap)
				else
					copy(values = overlap ++ Axis.values.drop(length).map { a => dimensions.getOrElse(a, zero) })
			case None => this
		}
	}
	// TODO: Continue with mapDimension etc.
}