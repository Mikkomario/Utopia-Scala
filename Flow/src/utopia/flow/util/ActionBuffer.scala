package utopia.flow.util

import utopia.flow.datastructure.mutable.ResettableLazy

import scala.collection.immutable.VectorBuilder

object ActionBuffer
{
	/**
	 * Creates a new action buffer
	 * @param bufferSize Number of items stored in this buffer before the specified action is called
	 * @param action Action that is performed for each group of items
	 * @tparam A Type of stored item
	 * @return A new buffer
	 */
	def apply[A](bufferSize: Int)(action: Vector[A] => Unit) = new ActionBuffer[A](bufferSize)(action)
}

/**
 * Used for performing pre-specified actions on a number of items at once, the action being performed when a certain
 * number of items is reached. Remember to call flush() for the remaining items at the end to make sure they are
 * acted upon also.
 * @author Mikko Hilpinen
 * @since 5.4.2021, v1.9
 */
class ActionBuffer[A](bufferSize: Int)(action: Vector[A] => Unit)
{
	// ATTRIBUTES   ------------------------
	
	private val builderPointer = ResettableLazy { new VectorBuilder[A]() }
	private var counter = 0
	
	
	// COMPUTED ----------------------------
	
	def remainingCapacity = bufferSize - counter
	
	
	// OTHER    ----------------------------
	
	/**
	 * Adds a new item to this buffer
	 * @param item Item to add
	 */
	def +=(item: A) =
	{
		builderPointer.value += item
		counter += 1
		if (counter >= bufferSize)
			flush()
	}
	
	/**
	 * Adds multiple new items to this buffer
	 * @param items Items to add
	 */
	def ++=(items: Seq[A]) =
	{
		// Checks whether the buffer will overflow
		val capacity = remainingCapacity
		val overflow = items.size - capacity
		// Case: Buffer becomes full but won't overflow => Fills and flushes the buffer
		if (overflow == 0)
		{
			builderPointer.value ++= items
			counter = bufferSize
			flush()
		}
		// Case: There is overflow
		else if (overflow > 0)
		{
			// Prepares the actionable items
			val nextActionItems = builderPointer.pop().result() ++ items.take(capacity)
			// Prepares possible additional actions based on additional overflows
			val additionalActionItems = (0 until (overflow / bufferSize))
				.map { iteration =>
					items.slice(capacity + iteration * bufferSize, capacity + iteration * bufferSize + bufferSize)
				}
			// Sets up the buffer to represent the state after all of these items have been acted upon
			val remainingItems = items.takeRight(overflow % bufferSize)
			builderPointer.value ++= remainingItems
			counter = remainingItems.size
			// Calls each action in sequence
			(nextActionItems +: additionalActionItems.map { _.toVector }).foreach(action)
		}
		// Case: No overflow => fills the buffer
		else
		{
			builderPointer.value ++= items
			counter += items.size
		}
	}
	
	/**
	 * Flushes this buffer, calling the specified action on all the items that were in this buffer before the flush
	 */
	def flush() =
	{
		// Resets the builder and calls the action (No OP if no items have been collected yet)
		if (counter > 0)
		{
			counter = 0
			action(builderPointer.pop().result())
		}
	}
}
