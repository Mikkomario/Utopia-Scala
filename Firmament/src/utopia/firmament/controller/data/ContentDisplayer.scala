package utopia.firmament.controller.data

import utopia.firmament.component.display.{PoolWithPointer, Refreshable}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.template.eventful.Changing

/**
  * Updates the displayed content on a component. Please note that when using ContentDisplayers, you shouldn't modify
  * the underlying displays through other means. Implementing classes should call <i>setup()</i> on initialization
  * @author Mikko Hilpinen
  * @since 9.5.2020, Reflection v1.2
  * @tparam A Type of item displayed in a single display component
 *  @tparam C Type of display component
 *  @tparam P Type of reflected content source
  */
trait ContentDisplayer[A, +C <: Refreshable[A], +P <: Changing[Vector[A]]] extends PoolWithPointer[Vector[A], P]
{
	// ABSTRACT	----------------------
	
	/**
	  * @return The currently used displays
	  */
	def displays: Vector[C]
	
	/**
	  * Adds new displays for new values
	  * @param values New values that need to be displayed
	  * @param index Index where to add these displays
	  */
	protected def addDisplaysFor(values: Vector[A], index: Int): Unit
	
	/**
	  * Removes unnecessary displays
	  * @param range Range of indices to drop
	  */
	protected def dropDisplaysAt(range: Range): Unit
	
	/**
	  * This method will be called at the end of each refresh
	  */
	protected def finalizeRefresh(): Unit
	
	/**
	  * Checks whether these two items should be considered equal by this content manager's standards. If this
	  * method returns true, content updated may be completely skipped.
	  * @param a First item
	  * @param b Second item
	  * @return Whether the two items should be considered equal in this context
	  */
	protected def itemsAreEqual(a: A, b: A): Boolean
	
	/**
	  * Checks whether these two items represent same or different state of a the same item whereas itemsAreEqual checks
	  * whether the two states are exactly equal (or equal display-wise). For content managers with immutable stateless
	  * content, this is same as calling itemsAreEqual.
	  * @param a First item
	  * @param b Second item
	  * @return Whether these two items represent same or different versions of the same instance
	  */
	protected def representSameItem(a: A, b: A): Boolean
	
	/**
	  * @return Whether the contents managed by this manager don't have any states - that each managed item represents
	  *         an individual immutable object and not a state of some other object. True if the managed items
	  *         don't represent any other item (representsSameItem is same as itemsAreEqual). False otherwise.
	  */
	protected def contentIsStateless: Boolean
	
	
	// OTHER	--------------------
	
	/**
	  * Sets up this manager once other attributes have been initialized. Enables content change listening.
	  */
	protected def setup() = contentPointer.addListenerAndSimulateEvent(Vector())(ContentUpdateListener)
	
	/**
	  * Finds a display currently showing provided element
	  * @param item A searched item
	  * @param equals A function for testing equality between contents
	  * @tparam B Type of tested item
	  * @return The display currently showing the provided item. None if no such display was found.
	  */
	def displayMatching[B](item: B)(equals: (A, B) => Boolean) = displays.find { d => equals(d.content, item) }
	
	/**
	  * Finds a display currently showing provided element (uses equals to find the element)
	  * @param item A searched item
	  * @return The display currently showing the provided item. None if no such display was found.
	  */
	def displayFor(item: A): Option[C] = {
		// May perform additional filtering in case there are duplicates
		val options = displays.filter { d => representSameItem(item, d.content) }
		if (options.size < 2)
			options.headOption
		else
			options.find { d => itemsAreEqual(item, d.content) }.orElse(options.headOption)
	}
	
	
	// NESTED CLASSES	-------------
	
	private object ContentUpdateListener extends ChangeListener[Vector[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[A]]) = {
			setContent(event.newValue)
			finalizeRefresh()
		}
		
		private def setContent(newValues: Vector[A]) = {
			val d = displays
			val oldContentSize = d.size
			val newContentSize = newValues.size
			
			val sizeDifference = newContentSize - oldContentSize
			
			// Finds similar start and end portions (if present)
			val identicalStart = d.zip(newValues).takeWhile { case (display, newVal) =>
				representSameItem(display.content, newVal) }
			val identicalEnd = {
				// Compared parts (old vs new) must have identical size
				val comparedPart = {
					// Case: Addition -> new value start skipped
					if (sizeDifference > 0)
						d.zip(newValues.drop(sizeDifference))
					// Case: Deletion -> old value start skipped
					else if (sizeDifference < 0)
						d.drop(-sizeDifference).zip(newValues)
					// Case: Update -> no need to skip anything
					else
						d.zip(newValues)
				}
				comparedPart.reverseIterator.takeWhile {
					case (display, newVal) => representSameItem(display.content, newVal) }.toVector
			}
			
			val skipFirst = identicalStart.size
			val skipLast = identicalEnd.size
			
			// Size difference is positive => new items added somewhere
			if (sizeDifference > 0) {
				// Case: New items are added to the end
				if (skipFirst == oldContentSize)
					addDisplaysFor(newValues.takeRight(sizeDifference), oldContentSize)
				// Case: New items are added to the beginning
				else if (skipLast == oldContentSize)
					addDisplaysFor(newValues.take(sizeDifference), 0)
				// Case: New items are added to the middle
				else if (skipFirst + skipLast == oldContentSize)
					addDisplaysFor(newValues.slice(skipFirst, skipFirst + sizeDifference), skipFirst)
				// Case: New items added in multiple locations
				else {
					// Adds items either to the beginning of collection or to the end of the updated area
					val insertToEnd = identicalStart.nonEmpty || identicalEnd.isEmpty
					val updateRange = skipFirst until (oldContentSize - skipLast)
					val updatedValues = if (insertToEnd) newValues.slice(updateRange) else
						newValues.dropRight(skipLast).takeRight(updateRange.size)
					val insertedValues = if (insertToEnd) newValues.dropRight(skipLast).takeRight(sizeDifference) else newValues.take(sizeDifference)
					val insertIndex = if (insertToEnd) updateRange.last + 1 else 0
					
					// Updates first, then adds new displays
					update(updateRange, updatedValues)
					addDisplaysFor(insertedValues, insertIndex)
				}
			}
			// Size difference is negative => old items removed somewhere
			else if (sizeDifference < 0) {
				val removeCount = -sizeDifference
				
				// Case: Items removed from the end
				if (skipFirst == newContentSize)
					dropDisplaysAt(newContentSize until oldContentSize)
				// Case: Items removed from the beginning
				else if (skipLast == newContentSize)
					dropDisplaysAt(0 until removeCount)
				// Case: Items removed from the middle
				else if (skipFirst + skipLast == newContentSize)
					dropDisplaysAt(skipFirst until (skipFirst + removeCount))
				// Case: Items removed from multiple places
				else {
					// Drops rows either from the beginning of the collection or at the end of the updated area
					val dropFromEnd = identicalStart.nonEmpty || identicalEnd.isEmpty
					val updatedRange = skipFirst until (newContentSize - skipLast)
					
					// Drops first, then updates
					if (dropFromEnd)
						dropDisplaysAt((oldContentSize - skipLast - removeCount) until (oldContentSize - skipLast))
					else
						dropDisplaysAt(0 until removeCount)
					
					update(updatedRange, newValues.slice(updatedRange))
				}
			}
			// Sizes are identical => content swapped somewhere
			else {
				// Updates the content between identical areas
				val updateRange = skipFirst until (newContentSize - skipLast)
				update(updateRange, newValues.slice(updateRange))
			}
			
			// May also update the state of identical items, in case their mutable state was altered (optional)
			if (!contentIsStateless) {
				(identicalStart.iterator ++ identicalEnd.iterator)
					.filterNot { case (display, value) => itemsAreEqual(display.content, value) }
					.foreach { case (display, value) => display.content = value }
			}
		}
		
		private def update(targetRange: Range, items: Vector[A]) =
			displays.slice(targetRange).foreachWith(items) { (d, i) => d.content = i }
	}
}
