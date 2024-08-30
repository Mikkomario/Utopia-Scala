package utopia.paradigm.shape.template

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.paradigm.enumeration.Axis

import scala.collection.mutable

/**
  * Used for building sets of dimensions
  * @author Mikko Hilpinen
  * @since 5.11.2022, v1.2
  */
class DimensionsBuilder[A](zero: Lazy[A]) extends DimensionalBuilder[A, Dimensions[A]]
{
	// ATTRIBUTES   -----------------------
	
	private val linearBuilder = OptimizedIndexedSeq.newBuilder[A]
	private val assigned = ResettableLazy { mutable.Map[Axis, A]() }
	
	
	// IMPLEMENTED  -----------------------
	
	override def clear() = {
		linearBuilder.clear()
		assigned.reset()
	}
	
	override def result() = {
		val values = assigned.current.filter { _.nonEmpty } match {
			case Some(assignments) =>
				val iter = linearBuilder.result().iterator
				Axis.values.take(assignments.keysIterator.map { _.index }.max + 1)
					.map { a => assignments.getOrElse(a, iter.nextOption().getOrElse(zero.value)) } ++ iter
			case None => linearBuilder.result()
		}
		new Dimensions[A](zero, values)
	}
	
	override def addOne(elem: A) = {
		linearBuilder += elem
		this
	}
	
	override def update(axis: Axis, value: A) = assigned.value(axis) = value
}
