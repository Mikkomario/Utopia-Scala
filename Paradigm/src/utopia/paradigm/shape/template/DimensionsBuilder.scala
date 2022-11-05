package utopia.paradigm.shape.template

import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.paradigm.enumeration.Axis

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

/**
  * Used for building sets of dimensions
  * @author Mikko Hilpinen
  * @since 5.11.2022, v1.2
  */
class DimensionsBuilder[A](zero: A) extends mutable.Builder[A, Dimensions[A]]
{
	// ATTRIBUTES   -----------------------
	
	private val linearBuilder = new VectorBuilder[A]()
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
					.map { a => assignments.getOrElse(a, iter.nextOption().getOrElse(zero)) } ++ iter
			case None => linearBuilder.result()
		}
		Dimensions(zero, values)
	}
	
	override def addOne(elem: A) = {
		linearBuilder += elem
		this
	}
	
	
	// OTHER    --------------------------
	
	/**
	  * Assigns a value on a specific axis
	  * @param axis Targeted axis
	  * @param value Value to assign for that axis
	  */
	def update(axis: Axis, value: A) = assigned.value(axis) = value
	
	def +=(assignment: (Axis, A)) = update(assignment._1, assignment._2)
	def ++=(assignments: IterableOnce[(Axis, A)]) = assignments.iterator.foreach { this += _ }
}
