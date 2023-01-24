package utopia.paradigm.shape.template

import utopia.paradigm.angular.Angle
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

/**
  * A common trait for factories used for building (double) vectors
  * @author Mikko Hilpinen
  * @since 9.11.2022, v1.2
  */
trait VectorFactory[+V] extends DimensionsWrapperFactory[Double, V]
{
	// IMPLEMENTED  ---------------------
	
	override def zeroDimension = 0.0
	override protected def dimensionsFactory = Dimensions.double
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a new vector with specified length and direction
	  */
	def lenDir(length: Double, direction: Angle) = apply(direction.cosine * length, direction.sine * length)
	
	/**
	  * @param items A set of vectors
	  * @return The average between those vectors.
	  *         Empty vector if the specified set of items was empty.
	  */
	def average(items: Iterable[DoubleVector]) = {
		if (items.size == 1)
			from(items.head)
		else if (items.nonEmpty) {
			val sum = items.reduce { _ + _ }
			from(sum / items.size)
		}
		else
			empty
	}
	/**
	  * @param items A set of vectors
	  * @return The average between those vectors. None if the set was empty.
	  */
	def averageOption(items: Iterable[DoubleVector]) = if (items.isEmpty) None else Some(average(items))
	/**
	  * @param items A set of vectors and their relative weights
	  * @return A weighed average between those vectors.
	  *         Empty vector if the specified set of items was empty.
	  */
	def weighedAverage(items: Iterable[(DoubleVector, Double)]) = {
		if (items.size == 1)
			from(items.head._1)
		else if (items.nonEmpty) {
			val sum = items.map { case (v, weight) => v * weight }.reduce { _ + _ }
			from(sum / items.map { _._2 }.sum)
		}
		else
			empty
	}
	/**
	  * @param items A set of vectors and their relative weights
	  * @return A weighed average between those vectors. None if the set was empty.
	  */
	def weighedAverageOption(items: Iterable[(DoubleVector, Double)]) =
		if (items.isEmpty) None else Some(weighedAverage(items))
	
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the minimum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def topLeft[V2 <: Dimensional[Double, V2]](points: IterableOnce[V2]) = merge(points) { _ topLeft _ }
	/**
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the minimum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def topLeft[V2 <: Dimensional[Double, V2]](first: V2, second: V2, more: V2*): V =
		topLeft(Vector(first, second) ++ more)
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the minimum between these points.
	  *         None if the specified set of items was empty.
	  */
	def topLeftOption[V2 <: Dimensional[Double, V2]](points: Iterable[V2]) =
		if (points.isEmpty) None else Some(topLeft(points))
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the maximum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def bottomRight[V2 <: Dimensional[Double, V2]](points: IterableOnce[V2]) = merge(points) { _ bottomRight _ }
	/**
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the maximum between these points.
	  *         Empty vector if the specified set of items was empty.
	  */
	def bottomRight[V2 <: Dimensional[Double, V2]](first: V2, second: V2, more: V2*): V =
		bottomRight(Vector(first, second) ++ more)
	/**
	  * @param points A set of points
	  * @tparam V2 Type of points
	  * @return A vector where each dimension is the maximum between these points.
	  *         None if the specified set of items was empty.
	  */
	def bottomRightOption[V2 <: Dimensional[Double, V2]](points: Iterable[V2]) =
		if (points.isEmpty) None else Some(bottomRight(points))
	
	/**
	  * Combines a set of items
	  * @param items A set of items
	  * @param f A reduce function
	  * @tparam V2 Type of items
	  * @return A vector based on the reduce results.
	  *         Empty vector if the specified set of items was empty.
	  */
	def merge[V2 <: HasDoubleDimensions](items: IterableOnce[V2])(f: (V2, V2) => V2) = {
		items.iterator.reduceLeftOption(f) match {
			case Some(merged) => from(merged)
			case None => empty
		}
	}
}
