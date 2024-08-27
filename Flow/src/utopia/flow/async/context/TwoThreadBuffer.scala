package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.util.{NotEmpty, UncertainBoolean}
import utopia.flow.util.UncertainBoolean.CertainBoolean
import utopia.flow.util.UncertainNumber.{CertainNumber, UncertainInt, zeroOrMore}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Flag

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

object TwoThreadBuffer
{
	/**
	  * Interface for pushing data into a buffer.
	  * Operations typically block while the buffer is full.
	  * @tparam A Type of items pushed to the buffer.
	  */
	sealed trait Output[A] extends AutoCloseable
	{
		// ABSTRACT ----------------------
		
		/**
		 * @return Interface that provides non-blocking methods for pushing data into this buffer
		 */
		def immediately: ImmediateOutput[A]
		
		/**
		 * @return Size of the input that is yet to be added to the buffer. May be uncertain.
		 */
		def remainingSize: UncertainInt
		def remainingSize_=(newSize: UncertainInt): Unit
		
		/**
		 * @return Whether all data has been read from this input.
		 */
		def closed: UncertainBoolean
		def closed_=(isClosed: UncertainBoolean): Unit
		
		/**
		 * Declares that this output will not close until otherwise declared.
		 * This allows the output to return true on 'hasNext' without yet having read values.
		 * @param allowReopen Whether this declaration should be applied even after this output has closed.
		 *                    Default = false = ignore this declaration if this output has closed.
		 */
		def declareNotClosing(allowReopen: Boolean = false): Unit
		/**
		 * Declares that this output might close without receiving any more values (which is the initial output state).
		 * This declaration may be cancelled by calling [[declareNotClosing()]].
		 * @param allowReopen Whether this declaration (of uncertainty) should override a possible closed state,
		 *                    i.e. to possibly declare this output as reopened.
		 *                    Default = false = output will remain closed, if closed before
		 */
		def declarePossiblyClosing(allowReopen: Boolean = false): Unit
		
		/**
		 * Specifies the number of items that will be read before this output closes.
		 * May involve uncertainty.
		 * @param remainingInputSize The number of items that will be read before this input closes.
		 *                           May be uncertain.
		 */
		def declareRemainingInputSize(remainingInputSize: UncertainInt): Unit
		
		/**
		 * Appends 0-n items to this buffer.
		 * Blocks until all of the specified items have been successfully placed in this buffer.
		 * If this buffer becomes full, some data has to be read before new items may be fit in.
		 * @param items Items to place within this buffer
		 */
		def push(items: Iterable[A]): Unit
		
		
		// OTHER    -----------------------
		
		/**
		 * Appends an item into this buffer.
		 * If this buffer is full, blocks until at least one element has been read first.
		 * @param item An item to add to this buffer
		 */
		def push(item: A): Unit = push(Single(item))
		
		/**
		 * Alias for [[push]]
		 */
		def ++=(items: Iterable[A]) = push(items)
		/**
		 * Alias for [[push]]
		 */
		def +=(item: A) = push(item)
	}
	
	/**
	  * An interface for pushing data into a buffer.
	  * Will not block, but instead may reject the proposed items.
	  * @tparam A Type of items added to the buffer.
	  */
	sealed trait ImmediateOutput[A]
	{
		// ABSTRACT ---------------------
		
		/**
		 * @return Whether this buffer is currently full.
		 *         Full buffers won't accept new items without blocking.
		 */
		def isFull: Boolean
		/**
		 * @return Size of the current available capacity in this buffer
		 */
		def availableCapacity: Int
		
		/**
		 * Adds 0-n items to this buffer without blocking.
		 * Some of the items may not get appended in this method call.
		 * @param items Items to append to this buffer, if possible
		 * @return Items that couldn't be appended at this time
		 */
		def push(items: Iterable[A]): Iterable[A]
		/**
		 * Appends an item to this buffer without blocking, if possible.
		 * If this buffer is full, the item won't get appended.
		 * @param item Item to append to this buffer
		 * @return Some if the item was rejected. None if it was accepted.
		 */
		def push(item: A): Option[A]
		
		
		// COMPUTED ---------------------
		
		/**
		 * @return Whether this buffer has free capacity available.
		 */
		def hasCapacity = !isFull
		
		
		// OTHER    ---------------------
		
		/**
		 * Alias for [[push]]
		 */
		def ++=(items: Iterable[A]) = push(items)
		/**
		 * Alias for [[push]]
		 */
		def +=(item: A) = push(item)
	}
	
	/**
	  * An interface for reading buffered data.
	  * Operations typically block while the buffer is empty.
	  * @tparam A Type of read data.
	  */
	sealed trait Input[+A] extends Iterator[A]
	{
		// ABSTRACT ------------------------
		
		/**
		 * @return An interface (iterator) that only returns immediately available (i.e. already buffered) items
		 */
		def immediately: ImmediateInput[A]
		
		/**
		 * @return Number of elements that are still available to be read (although not necessarily immediately).
		 *         May be uncertain.
		 */
		def sizeEstimate: UncertainInt
		
		
		// OTHER    ----------------------
		
		/**
		 * Collects the next min-max items from this input, if possible.
		 * If at least 'min' items are immediately available, returns the immediately available items, up
		 * to 'max' items.
		 * If less than 'min' items are available, blocks until they become available,
		 * or until no data may be read anymore.
		 * @param min The smallest number of items that may be returned from this function,
		 *            unless the end of input is reached.
		 * @param max The largest number of items that may be returned from this function.
		 * @return Next min-max items from this input.
		 */
		def collectNextBetween(min: Int, max: Int): IndexedSeq[A] = {
			val immediate = immediately.collectNext(max)
			if (immediate.hasSize < min)
				immediate ++ this.collectNext(min - immediate.size)
			else
				immediate
		}
		
		/**
		 * Returns the next available item, or multiple items if such are immediately available.
		 * Blocks if no items are currently available.
		 * @param max The maximum number of items to return
		 * @throws IllegalStateException If no more items are available
		 * @return Either Left: The next available item, or Right: Many immediately available items
		 */
		@throws[IllegalStateException]("If no more items are available")
		def nextOneOrMany(max: Int) = NotEmpty(immediately.collectNext(max)) match {
			case Some(immediateValues) =>
				if (immediateValues hasSize 1) Left(immediateValues.head) else Right(immediateValues)
			case None => Left(next())
		}
	}
	
	/**
	  * An interface for reading buffered data without blocking.
	  * Will not yield data while the buffer is empty. I.e. only returns immediately accessible values.
	  * @tparam A Type of read items.
	  */
	sealed trait ImmediateInput[+A] extends Iterator[A]
	{
		/**
		 * Retrieves the next 'n' immediately available items from this buffer.
		 * If less than 'n' items are available, returns those.
		 * @param n Maximum number of immediately available items to pull
		 * @return The next immediately available items (up to 'n' items). May be empty.
		 */
		def collectNext(n: Int): IndexedSeq[A]
	}
	
	/**
	  * An input interface to use / present in situations where there is no data to read.
	  */
	object EmptyInput extends Input[Nothing]
	{
		override def immediately = EmptyImmediateInput
		override def sizeEstimate = CertainNumber(0)
		override def hasNext: Boolean = false
		
		override def next(): Nothing = throw new IllegalStateException("next() called on empty input")
	}
	/**
	  * An immediate input interface to use / present in situations where there is no data to read.
	  */
	object EmptyImmediateInput extends ImmediateInput[Nothing]
	{
		override def hasNext: Boolean = false
		
		override def next(): Nothing = throw new IllegalStateException("next() called on empty input")
		override def collectNext(n: Int): IndexedSeq[Nothing] = Empty
	}
}

/**
  * A buffer that provides two interfaces:
  *     1) For pushing elements into the buffer, as long as there is space, and
  *     2) For pulling elements from the buffer, as long as there are some available
  *
  * This interface is intended to be used in a 2-threaded environment, where one thread pushes items into the
  * buffer and another pulls them from the buffer.
  * Each interface blocks if the other falls behind, making single-threaded use-case an unlikely fit.
  *
  * @author Mikko Hilpinen
  * @since 14.11.2023, v2.3
  *
  * @param capacity Maximum buffer size (must be > 0)
  * @param exc Implicit execution context used in certain read actions
  * @param log Implicit logging implementation used for non-direct errors (e.g. errors caused by pointer-listeners)
  */
class TwoThreadBuffer[A](capacity: Int)(implicit exc: ExecutionContext, log: Logger)
{
	if (capacity <= 0)
		throw new IllegalArgumentException(s"Capacity must be positive. Currently $capacity")
	
	// ATTRIBUTES   ------------------------
	
	// Contains true when Input is closed
	private val _declaredFilledPointer = Volatile[UncertainBoolean](UncertainBoolean)
	// Contains the remaining input size / length. Actively tracked, but may be uncertain.
	// Based on declarations and appended input size
	private val _remainingInputSize = Volatile[UncertainInt](zeroOrMore)
	/**
	  * Pointer that contains whether this buffer is receiving more elements.
	  * May contain an uncertain value.
	  */
	val isFillingFlag = _declaredFilledPointer.mergeWith(_remainingInputSize) { (declaredFilling, remainingSize) =>
		declaredFilling.exact match {
			case Some(filled) => CertainBoolean(!filled)
			case None => remainingSize.isPositive
		}
	}
	
	private val buffer = VolatileList[A]()
	
	/**
	  * Flag that contains true while this buffer contains unread elements
	  */
	val nonEmptyFlag: Flag = buffer.mapValue { _.nonEmpty }
	/**
	  * Flag that contains true while this buffer is empty
	  */
	lazy val isEmptyFlag = !nonEmptyFlag
	/**
	  * Flag that contains true once/when this buffer has been completely read and the input has been closed.
	  * May contain an uncertain value.
	  */
	lazy val closedFlag = _declaredFilledPointer.mergeWith(nonEmptyFlag) { (filled, nonEmpty) =>
		if (nonEmpty) CertainBoolean(false) else filled
	}
	
	// Reset whenever becomes empty
	private val _nonEmptyFuture = ResettableLazy { buffer.futureWhere { _.nonEmpty } }
	// Reset whenever becomes full
	private val _nonFullFuture = ResettableLazy { buffer.futureWhere { _.hasSize < capacity } }
	// Reset when filled state becomes inexact or changes
	private val _knownFilledStateFuture = ResettableLazy { _declaredFilledPointer.findMapFuture { _.exact } }
	// Reset if reopened
	private val _filledFuture = ResettableLazy {
		_declaredFilledPointer.findMapFuture { filled => if (filled.isCertainlyTrue) Some(()) else None }
	}
	
	
	// INITIAL CODE ------------------------
	
	// Resets the known filled state future whenever that state is no longer valid
	_declaredFilledPointer.addContinuousListener { e =>
		if (e.oldValue.isCertain) {
			_knownFilledStateFuture.reset()
			_filledFuture.reset()
		}
	}
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Interface for pushing items to this buffer
	  */
	def output = BufferOutput
	/**
	  * @return Interface for reading values from this buffer
	  */
	def input = BufferInput
	
	/**
	  * @return Current number of items in this buffer
	  */
	def currentSize = buffer.size
	/**
	  * @return Currently available capacity in this buffer
	  */
	def currentlyAvailableCapacity = capacity - currentSize
	
	/**
	  * @return Whether this buffer is currently full.
	  *         Full buffers don't accept new items before some are read.
	  */
	def isCurrentlyFull = buffer.hasSize >= capacity
	/**
	  * @return Whether this buffer still has available capacity at this time.
	  */
	def isNotCurrentlyFull = buffer.hasSize < capacity
	
	/**
	  * @return Whether this buffer is currently empty.
	  *         Empty buffers block reading operations.
	  */
	def isCurrentlyEmpty = buffer.isEmpty
	/**
	  * @return Whether this buffer currently contains items.
	  */
	def isNotCurrentlyEmpty = buffer.nonEmpty
	
	/**
	  * @return Whether this buffer will still receive new items. May be uncertain.
	  */
	def isStillFilling = isFillingFlag.value
	/**
	  * @return Whether this buffer has stopped filling (i.e. input has been fully consumed). May be uncertain.
	  */
	def isNoLongerFilling = !isStillFilling
	
	/**
	  * @return Whether all items from the input have been read. May be uncertain.
	  */
	def hasClosed = closedFlag.value
	/**
	  * @return Whether some items from the input have yet to be read. May be uncertain.
	  */
	def hasNotClosed = !hasClosed
	
	/**
	  * @return Future that resolves once this buffer will not receive new input any more
	  */
	def noLongerFillingFuture = _filledFuture.value
	/**
	  * @return Future that resolves once the input has been completely read
	  */
	def closedFuture = closedFlag
		.findMapFuture { closed => if (closed.isCertainlyTrue) Some(()) else None }
	
	
	// NESTED   ----------------------------
	
	object BufferOutput extends TwoThreadBuffer.Output[A]
	{
		// INITIAL CODE -------------------
		
		// If the declared input size becomes 0 (or less), declares this input as closed
		_remainingInputSize.addListener { e =>
			if (e.newValue.isCertainlyNotPositive)
				close()
		}
		
		
		// IMPLEMENTED  -------------------
		
		override def immediately = Immediate
		
		override def remainingSize = _remainingInputSize.value
		override def remainingSize_=(newSize: UncertainInt) = _remainingInputSize.value = newSize
		
		override def closed = _declaredFilledPointer.value
		override def closed_=(isClosed: UncertainBoolean) = _declaredFilledPointer.value = isClosed
		
		override def close() = _declaredFilledPointer.value = true
		
		override def declareNotClosing(allowReopen: Boolean = false) = {
			if (allowReopen)
				_declaredFilledPointer.value = false
			else
				_declaredFilledPointer.update { filled => if (filled.isCertainlyTrue) filled else false }
		}
		override def declarePossiblyClosing(allowReopen: Boolean = false) = {
			if (allowReopen)
				_declaredFilledPointer.value = UncertainBoolean
			else
				_declaredFilledPointer
					.update { filled => if (filled.isCertainlyTrue) filled else UncertainBoolean }
		}
		
		override def declareRemainingInputSize(remainingInputSize: UncertainInt) =
			_remainingInputSize.value = remainingInputSize
		
		@tailrec
		override final def push(items: Iterable[A]): Unit = {
			if (items.nonEmpty) {
				val immediatelyAvailableCapacity = immediately.availableCapacity
				val wasFull = immediatelyAvailableCapacity <= 0
				// Waits until there is space within the buffer for at least one item
				val availableCapacity =
					if (wasFull) capacity - _nonFullFuture.value.waitFor().get.size else immediatelyAvailableCapacity
				
				// Appends as many items as possible to the buffer
				val (nextAppend, remaining) = items.splitAt(availableCapacity)
				buffer.update { _ ++ nextAppend }
				_remainingInputSize.update { _ - nextAppend.size }
				
				// If there are items which couldn't fit in the buffer, handles them recursively
				// (i.e. completes another wait iteration)
				if (remaining.nonEmpty) {
					_nonFullFuture.reset()
					push(remaining)
				}
			}
		}
		
		
		// NESTED   ------------------------------
		
		object Immediate extends TwoThreadBuffer.ImmediateOutput[A]
		{
			override def isFull = buffer.hasSize >= capacity
			
			override def availableCapacity = capacity - buffer.size
			
			override def push(items: Iterable[A]): Iterable[A] = {
				if (items.nonEmpty) {
					// Appends as many items as possible without overfilling the buffer
					val immediateCapacity = immediately.availableCapacity
					val (append, reject) = items.splitAt(immediateCapacity)
					buffer.update { _ ++ append }
					_remainingInputSize.update { _ - append.size }
					// Returns the remaining items
					reject
				}
				else
					items
			}
			override def push(item: A) = {
				// Case: Buffer is full => Rejects the item
				if (isFull)
					Some(item)
				// Case: Buffer is not full => Appends the item to the buffer
				else {
					buffer.update { _ :+ item }
					_remainingInputSize.update { _ - 1 }
					None
				}
			}
		}
	}
	
	object BufferInput extends TwoThreadBuffer.Input[A]
	{
		// IMPLEMENTED  -------------------------
		
		override def immediately = Immediate
		
		override def sizeEstimate: UncertainInt = output.remainingSize + buffer.size
		
		override def knownSize = sizeEstimate.exact.getOrElse(-1)
		override def size = sizeEstimate.exact.getOrElse(super.size)
		
		override def hasNext: Boolean = {
			// Case: There are items in the buffer => Has more items
			if (isNotCurrentlyEmpty)
				true
			else
				isNoLongerFilling.exact match {
					// Case: It is known whether the buffer has been filled or not => Returns based on that knowledge
					case Some(hasBeenFilled) => !hasBeenFilled
					// Case: It is still uncertain whether iteration has finished or not
					// => Waits until it is no longer uncertain
					case None =>
						// Uncertainty ends when filledPointer is updated or when buffer acquires a value
						_nonEmptyFuture.value.or(_knownFilledStateFuture.value).waitFor().get match {
							// Case: Buffer acquired a value => Has more elements
							case Left(_) => true
							// Case: Filled status updated => Continues if not yet filled
							case Right(filled) => !filled
						}
				}
		}
		
		override def next(): A = {
			// Returns the immediately available item, if possible
			immediately.nextOption().getOrElse {
				// Case: No more items available => Throws
				if (isNoLongerFilling.isCertainlyTrue)
					noMoreItems()
				// Case: Items may be available => Waits
				else
					_nonEmptyFuture.value.or(noLongerFillingFuture).waitFor().get match {
						// Case: Next item is now available => Recursively acquires that item
						case Left(_) => next()
						// Case: Filled without items available => Throws
						case Right(_) => noMoreItems()
					}
			}
		}
		
		
		// OTHER    ---------------------
		
		private def noMoreItems(): Nothing = throw new IllegalStateException(
			"next() called on an empty iterator; There won't be any more items in this buffer.")
			
		
		// NESTED   ---------------------
		
		object Immediate extends TwoThreadBuffer.ImmediateInput[A]
		{
			// IMPLEMENTED  -------------
			
			override def knownSize = buffer.size
			override def size = buffer.size
			
			override def hasNext: Boolean = isNotCurrentlyEmpty
			
			override def next(): A = nextOption().getOrElse { throw new IllegalStateException(
				"next() called on an empty iterator; No more items are immediately available") }
			
			override def nextOption() = takeFromBuffer[Option[A]](None) { b => Some(b.head) -> b.tail }
			
			override def collectNext(n: Int) =
				if (n <= 0) Empty else takeFromBuffer[IndexedSeq[A]](Empty) { _.splitAt(n) }
			
			
			// OTHER    ---------------
			
			private def takeFromBuffer[R](ifEmpty: => R)(ifNonEmpty: IndexedSeq[A] => (R, IndexedSeq[A])) =
				buffer.mutate { b =>
					// Case: No immediate items available
					if (b.isEmpty)
						ifEmpty -> b
					// Case: Immediate items available => Extracts one from the buffer
					else {
						val (result, remaining) = ifNonEmpty(b)
						// Updates the future pointers
						if (remaining.isEmpty)
							_nonEmptyFuture.reset()
						result -> remaining
					}
				}
		}
	}
}
