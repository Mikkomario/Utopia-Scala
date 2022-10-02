package utopia.flow.collection.mutable.builder

import scala.collection.immutable.VectorBuilder

/**
  * A VectorBuilder wrapper that provides access to intermediate vector states
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  * @param initialState The initial "intermediate" value of this builder
  * @tparam A Type of items that will be added to the resulting Vector
  */
class CompoundingVectorBuilder[A](initialState: Vector[A] = Vector.empty)
	extends CompoundingBuilder[A, VectorBuilder[A], Vector[A], Vector[A]](initialState) with Seq[A]
{
	// IMPLEMENTED  --------------------------
	
	override protected def clearState = Vector.empty
	
	override protected def append(newItems: Vector[A]) = lastResult ++ newItems
	
	override def length = super[CompoundingBuilder].size
	override def isEmpty = super[CompoundingBuilder].isEmpty
	
	override def toIndexedSeq = currentState
	override def toVector = currentState
	
	override protected def newBuilder() = new VectorBuilder[A]()
	
	/**
	  * @param index Targeted index
	  * @return The item in this builder at that index
	  */
	override def apply(index: Int) = {
		if (index < 0)
			throw new IllegalArgumentException(s"apply with index $index")
		else if (index < lastResult.size)
			lastResult(index)
		else
			currentState(index)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param index Targeted index
	  * @return The item in this builder at that index. None the index was not in range of this builder's contents.
	  */
	def getOption(index: Int) = {
		if (index < 0)
			None
		else if (index < lastResult.size)
			Some(lastResult(index))
		else {
			val v = currentState
			v.lift(index)
		}
	}
}
