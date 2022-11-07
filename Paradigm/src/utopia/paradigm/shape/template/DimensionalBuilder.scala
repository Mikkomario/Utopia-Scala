package utopia.paradigm.shape.template

import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.template.DimensionalBuilder.MappedDimensionalBuilder

import scala.collection.mutable

object DimensionalBuilder
{
	// NESTED   ---------------------------
	
	private class MappedDimensionalBuilder[D, Mid, To](builder: DimensionalBuilder[D, Mid])(f: Mid => To)
		extends DimensionalBuilder[D, To]
	{
		override def update(axis: Axis, value: D) = builder(axis) = value
		
		override def clear() = builder.clear()
		
		override def result() = f(builder.result())
		
		override def addOne(elem: D) = {
			builder += elem
			this
		}
	}
}

/**
  * Used for building items with dimensions
  * @author Mikko Hilpinen
  * @since 5.11.2022, v1.2
  */
trait DimensionalBuilder[-D, +To] extends mutable.Builder[D, To]
{
	// ABSTRACT ---------------------------
	
	/**
	  * Assigns a value on a specific axis
	  * @param axis Targeted axis
	  * @param value Value to assign for that axis
	  */
	def update(axis: Axis, value: D): Unit
	
	
	// IMPLEMENTED  ----------------------
	
	override def mapResult[NewTo](f: To => NewTo): DimensionalBuilder[D, NewTo] =
		new MappedDimensionalBuilder[D, To, NewTo](this)(f)
	
	
	// OTHER    --------------------------
	
	def +=(assignment: (Axis, D)) = update(assignment._1, assignment._2)
	def ++=(assignments: IterableOnce[(Axis, D)]) = assignments.iterator.foreach { this += _ }
}
