package utopia.paradigm.shape.template.vector

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.angular.Angle
import utopia.paradigm.shape.template.{Dimensional, DimensionsWrapperFactory, HasDimensions}

/**
  * A common trait for factories used for building numeric vectors.
  * These factories are expected to handle double number calculations, also.
  * @author Mikko Hilpinen
  * @since 9.11.2022, v1.2
  */
trait NumericVectorFactory[D, +V] extends DimensionsWrapperFactory[D, V]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Numeric implementation for the wrapped dimensions
	  */
	implicit def n: Fractional[D]
	
	/**
	  * @param double A double number
	  * @return A dimension from the specified number.
	  *         Transformations and/or rounding may apply.
	  */
	def dimensionFrom(double: Double): D
	/**
	  * Scales the specified dimension with the specified double number factor
	  * @param d A dimension to scale
	  * @param mod A scaling modifier to apply
	  * @return A scaled copy of the specified dimension
	  */
	def scale(d: D, mod: Double): D
	/**
	  * Divides the specified dimension using the specified factor
	  * @param d A dimension to divide
	  * @param div A factor to divide the dimension with
	  * @return A divided copy of the specified dimension
	  */
	def div(d: D, div: Double): D
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return A zero vector (where each axis is set to 0)
	  */
	def zero = apply()
	
	
	// IMPLEMENTED  ---------------------
	
	override def zeroDimension = n.zero
	
	
	// OTHER    -------------------------
	
	/**
	  * @param v A vector containing double numbers
	  * @return A vector of this type containing converted values
	  */
	def fromDoubles(v: HasDimensions[Double]) = apply(v.dimensions.map(dimensionFrom))
	
	/**
	  * Creates a new vector with specified length and direction
	  */
	def lenDir(length: D, direction: Angle) = apply(scale(length, direction.cosine), scale(length, direction.sine))
	
	/**
	  * @param items A set of vectors
	  * @return The average between those vectors.
	  *         Empty vector if the specified set of items was empty.
	  */
	def average[V2 <: Dimensional[D, V2]](items: Iterable[V2]) = {
		if (items.size == 1)
			from(items.head)
		else if (items.nonEmpty) {
			val sum = items.reduce { _.mergeWith(_)(n.plus) }
			val div = items.size
			from(sum.mapEachDimension { this.div(_, div) })
		}
		else
			empty
	}
	/**
	  * @param items A set of vectors
	  * @return The average between those vectors. None if the set was empty.
	  */
	def averageOption[V2 <: Dimensional[D, V2]](items: Iterable[V2]) =
		if (items.isEmpty) None else Some(average(items))
	/**
	  * @param items A set of vectors and their relative weights
	  * @return A weighed average between those vectors.
	  *         Empty vector if the specified set of items was empty.
	  */
	def weighedAverage[V2 <: Dimensional[D, V2]](items: Iterable[(V2, Double)]) = {
		if (items.size == 1)
			from(items.head._1)
		else if (items.nonEmpty) {
			val sum = items.map { case (v, weight) =>
				v.mapEachDimension { scale(_, weight) } }.reduce { _.mergeWith(_)(n.plus)
			}
			val divider = items.map { _._2 }.sum
			from(sum.mapEachDimension { div(_, divider) })
		}
		else
			empty
	}
	/**
	  * @param items A set of vectors and their relative weights
	  * @return A weighed average between those vectors. None if the set was empty.
	  */
	def weighedAverageOption[V2 <: Dimensional[D, V2]](items: Iterable[(V2, Double)]) =
		if (items.isEmpty) None else Some(weighedAverage(items))
	
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the minimum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def topLeft[V2 <: Dimensional[D, V2]](points: IterableOnce[V2]) = merge(points) { _ topLeft _ }
	/**
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the minimum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def topLeft[V2 <: Dimensional[D, V2]](first: V2, second: V2, more: V2*): V = topLeft(Pair(first, second) ++ more)
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the minimum between these points.
	  *         None if the specified set of items was empty.
	  */
	def topLeftOption[V2 <: Dimensional[D, V2]](points: Iterable[V2]) =
		if (points.isEmpty) None else Some(topLeft(points))
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the maximum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def bottomRight[V2 <: Dimensional[D, V2]](points: IterableOnce[V2]) = merge(points) { _ bottomRight _ }
	/**
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the maximum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def bottomRight[V2 <: Dimensional[D, V2]](first: V2, second: V2, more: V2*): V =
		bottomRight(Pair(first, second) ++ more)
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the maximum between these points.
	  *         None if the specified set of items was empty.
	  */
	def bottomRightOption[V2 <: Dimensional[D, V2]](points: Iterable[V2]) =
		if (points.isEmpty) None else Some(bottomRight(points))
	
	/**
	  * Combines a set of items
	  * @param items A set of items
	  * @param f A reduce function
	  * @tparam V2 Type of items
	  * @return A vector based on the reduce results.
	  *         Empty vector if the specified set of items was empty.
	  */
	def merge[V2 <: HasDimensions[D]](items: IterableOnce[V2])(f: (V2, V2) => V2) = {
		items.iterator.reduceLeftOption(f) match {
			case Some(merged) => from(merged)
			case None => empty
		}
	}
}
