package utopia.flow.collection.mutable.builder

import utopia.flow.collection.immutable.OptimizedIndexedSeq.OptimizedSeqBuilder
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}

/**
  * A builder that provides access to intermediate collection states
  * @author Mikko Hilpinen
  * @since 20.11.2025, v2.8, based on CompoundingVectorBuilder written 24.7.2022 for v1.16
  * @param initialState The initial "intermediate" value of this builder
  * @tparam A Type of items that will be added to the resulting Seq
  */
class CompoundingSeqBuilder[A](initialState: Seq[A] = Empty)
	extends CompoundingBuilder[A, OptimizedSeqBuilder[A], IndexedSeq[A], Seq[A]](initialState) with Seq[A]
{
	// IMPLEMENTED  --------------------------
	
	override protected def clearState = Empty
	
	override protected def append(newItems: IndexedSeq[A]) = lastResult ++ newItems
	
	override def length = super[CompoundingBuilder].size
	override def isEmpty = super[CompoundingBuilder].isEmpty
	
	override def toIndexedSeq = OptimizedIndexedSeq.from(currentState)
	override def toVector = Vector.from(currentState)
	
	override protected def newBuilder() = OptimizedIndexedSeq.newBuilder
	
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
	def lift(index: Int) = {
		if (index < 0)
			None
		else if (index < lastResult.size)
			Some(lastResult(index))
		else {
			val v = currentState
			v.lift(index)
		}
	}
	/**
	  * @param index Targeted index
	  * @return The item in this builder at that index. None the index was not in range of this builder's contents.
	  */
	@deprecated("Renamed to lift", "v2.8")
	def getOption(index: Int) = lift(index)
}
