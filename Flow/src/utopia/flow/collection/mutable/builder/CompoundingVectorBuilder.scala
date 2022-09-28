package utopia.flow.collection.mutable.builder


import utopia.flow.view.mutable.caching.ResettableLazy

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

/**
  * A VectorBuilder wrapper that provides access to intermediate vector states
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  * @param initialState The initial "intermediate" value of this builder
  * @tparam A Type of items that will be added to the resulting Vector
  */
class CompoundingVectorBuilder[A](initialState: Vector[A] = Vector.empty)
	extends mutable.Builder[A, Vector[A]] with Iterable[A]
{
	// ATTRIBUTES   --------------------------
	
	private val builderPointer = ResettableLazy { new VectorBuilder[A]() }
	private var lastResult = initialState
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The current result of this builder. Calling this function doesn't interfere with the building process,
	  *         although it might cause the construction of a new intermediate Vector
	  */
	def currentState: Vector[A] = builderPointer.current match {
		// Case: New additions since last 'currentState' call => creates a new state
		case Some(b) =>
			lastResult ++= b.result()
			builderPointer.reset()
			lastResult
		// Case: No new additions => reuses last calculated state
		case None => lastResult
	}
	
	/**
	  * @return The current size of this builder's results
	  */
	def currentSize = {
		val default = knownSize
		if (default < 0) currentState.size else default
	}
	/**
	  * @return The currently known minimum size of this builder.
	  *         The actual size may be larger, but not smaller, than the returned size.
	  */
	def minSize = Some(knownSize).filter { _ >= 0 }.getOrElse(lastResult.size)
	
	
	// IMPLEMENTED  --------------------------
	
	override def iterator: Iterator[A] = new BuilderIterator()
	
	/**
	  * @return Whether this builder is empty (i.e. contains 0 items)
	  */
	override def isEmpty = lastResult.isEmpty && (builderPointer.nonInitialized || currentState.isEmpty)
	
	/**
	  * @return The first item introduced to this builder
	  */
	override def head = if (lastResult.isEmpty) currentState.head else lastResult.head
	/**
	  * @return The first item introduced to this builder. None if no such item was found
	  */
	override def headOption = {
		if (lastResult.isEmpty) {
			if (builderPointer.isInitialized)
				currentState.headOption
			else
				None
		}
		else
			lastResult.headOption
	}
	override def last = currentState.last
	override def lastOption = currentState.lastOption
	
	override def size = builderPointer.current match {
		case Some(b) =>
			val ks = b.knownSize
			if (ks < 0) currentState.size else lastResult.size + ks
		case None => lastResult.size
	}
	override def knownSize = builderPointer.current match {
		case Some(b) =>
			val builderSize = b.knownSize
			if (builderSize < 0) -1 else builderSize + lastResult.size
		case None => lastResult.size
	}
	
	override def toVector = currentState
	override def toSeq = currentState
	override def toIndexedSeq = currentState
	
	override def clear() = {
		builderPointer.reset()
		lastResult = Vector.empty
	}
	
	override def result() = currentState
	
	override def addOne(elem: A) = {
		builderPointer.value.addOne(elem)
		this
	}
	
	override def addAll(xs: IterableOnce[A]) = {
		builderPointer.value.addAll(xs)
		this
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param index Targeted index
	  * @return The item in this builder at that index
	  */
	def apply(index: Int) = {
		if (index < 0)
			throw new IllegalArgumentException(s"apply with index $index")
		else if (index < lastResult.size)
			lastResult(index)
		else
			currentState(index)
	}
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
	
	
	// NESTED   -----------------------------
	
	private class BuilderIterator extends Iterator[A]
	{
		// ATTRIBUTES   ---------------------
		
		private var currentlyIteratingResult = lastResult
		private var currentSource = currentlyIteratingResult.iterator
		
		
		// IMPLEMENTED  ---------------------
		
		// Has next if:
		// a) Currently material has more items remaining
		// b) More items are available
		// c) Current material doesn't span the whole cached material
		override def hasNext =
			currentSource.hasNext || builderPointer.isInitialized ||
				currentlyIteratingResult.sizeCompare(lastResult) < 0
		
		override def next() = {
			// Case: Current material has more to iterate => continues iteration
			if (currentSource.hasNext)
				currentSource.next()
			// Case: Current material ended but more material has been cached since => updates current material
			else if (currentlyIteratingResult.sizeCompare(lastResult) < 0) {
				val skipCount = currentlyIteratingResult.size
				currentlyIteratingResult = lastResult
				currentSource = currentlyIteratingResult.drop(skipCount).iterator
				currentSource.next()
			}
			// Case: All cached material ended => builds new state based on recently added items and updates
			// current material to match the whole available material
			else {
				val skipCount = currentlyIteratingResult.size
				currentlyIteratingResult = currentState
				currentSource = currentlyIteratingResult.drop(skipCount).iterator
				currentSource.next()
			}
		}
	}
}
