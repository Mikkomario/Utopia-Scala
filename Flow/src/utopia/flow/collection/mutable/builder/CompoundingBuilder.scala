package utopia.flow.collection.mutable.builder

import utopia.flow.view.mutable.caching.ResettableLazy

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable

/**
  * A builder (wrapper) that provides access to immediate build states.
  * @author Mikko Hilpinen
  * @since 29.9.2022, v2.0
  * @param initialState The initial "intermediate" value of this builder
  * @tparam A Type of items that will be added to the resulting collection
  * @tparam B Type of builder used for building the intermediate collection
  * @tparam C Type of intermediate collection (builder result)
  * @tparam To The type of collection that results from this builder
  */
abstract class CompoundingBuilder[A, +B <: mutable.Builder[A, C], C, +To <: Iterable[A]](initialState: To)
	extends mutable.Builder[A, To] with Iterable[A]
{
	// ATTRIBUTES   --------------------------
	
	private val builderPointer = ResettableLazy { newBuilder() }
	private var _lastResult: To @uncheckedVariance = initialState
	
	
	// ABSTRACT ------------------------------
	
	/**
	  * @return An empty result collection
	  */
	protected def clearState: To
	
	/**
	  * @return A new empty builder
	  */
	protected def newBuilder(): B
	
	/**
	  * Appends new items to the last result (accessible through .lastResult)
	  * @param newItems New items to add
	  * @return A copy of the cached collection with the new items added.
	  *         If the cached collection is mutable, may return that collection.
	  */
	protected def append(newItems: C): To
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The last generated result (may not be up to date)
	  */
	protected def lastResult = _lastResult
	
	/**
	  * @return The current result of this builder. Calling this function doesn't interfere with the building process,
	  *         although it might cause the construction of a new intermediate Vector
	  */
	def currentState: To = builderPointer.current match {
		// Case: New additions since last 'currentState' call => creates a new state
		case Some(b) =>
			_lastResult = append(b.result())
			builderPointer.reset()
			_lastResult
		// Case: No new additions => reuses last calculated state
		case None => _lastResult
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
	def minSize = Some(knownSize).filter { _ >= 0 }.getOrElse(_lastResult.size)
	
	
	// IMPLEMENTED  --------------------------
	
	override def iterator: Iterator[A] = new BuilderIterator()
	
	/**
	  * @return Whether this builder is empty (i.e. contains 0 items)
	  */
	override def isEmpty = _lastResult.isEmpty && (builderPointer.nonInitialized || currentState.isEmpty)
	
	/**
	  * @return The first item introduced to this builder
	  */
	override def head = if (_lastResult.isEmpty) currentState.head else _lastResult.head
	/**
	  * @return The first item introduced to this builder. None if no such item was found
	  */
	override def headOption = {
		if (_lastResult.isEmpty) {
			if (builderPointer.isInitialized)
				currentState.headOption
			else
				None
		}
		else
			_lastResult.headOption
	}
	override def last = currentState.last
	override def lastOption = currentState.lastOption
	
	override def size = builderPointer.current match {
		case Some(b) =>
			val ks = b.knownSize
			if (ks < 0) currentState.size else _lastResult.size + ks
		case None => _lastResult.size
	}
	override def knownSize = builderPointer.current match {
		case Some(b) =>
			val builderSize = b.knownSize
			if (builderSize < 0) -1 else builderSize + _lastResult.size
		case None => _lastResult.size
	}
	
	override def clear() = {
		builderPointer.reset()
		_lastResult = clearState
	}
	
	override def result() = currentState
	
	override def addOne(elem: A) = {
		builderPointer.value.addOne(elem)
		this
	}
	
	override def addAll(xs: IterableOnce[A]) = {
		val iter = xs.iterator
		if (iter.hasNext)
			builderPointer.value.addAll(iter)
		this
	}
	
	
	// NESTED   -----------------------------
	
	private class BuilderIterator extends Iterator[A]
	{
		// ATTRIBUTES   ---------------------
		
		private var currentlyIteratingResult: Iterable[A] = _lastResult
		private var currentSource = currentlyIteratingResult.iterator
		
		
		// IMPLEMENTED  ---------------------
		
		// Has next if:
		// a) Currently material has more items remaining
		// b) More items are available
		// c) Current material doesn't span the whole cached material
		override def hasNext =
			currentSource.hasNext || builderPointer.isInitialized ||
				currentlyIteratingResult.sizeCompare(_lastResult) < 0
		
		override def next() = {
			// Case: Current material has more to iterate => continues iteration
			if (currentSource.hasNext)
				currentSource.next()
			// Case: Current material ended but more material has been cached since => updates current material
			else if (currentlyIteratingResult.sizeCompare(_lastResult) < 0) {
				val skipCount = currentlyIteratingResult.size
				currentlyIteratingResult = _lastResult
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
