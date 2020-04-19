package utopia.reflection.controller.data

import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.{Refreshable, RefreshableWithPointer}

/**
  * ContentManagers update content on a component. Please note that when using ContentManagers, you shouldn't modify
  * the underlying displays through other means. Implementing classes should call <i>setup()</i> on initialization
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1+
  */
trait ContentManager[A, C <: Refreshable[A]] extends RefreshableWithPointer[Vector[A]]
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
	  * Checks whether these two items should be considered equal by this content manager's standards
	  * @param a First item
	  * @param b Second item
	  * @return Whether the two items should be considered equal in this context
	  */
	protected def itemsAreEqual(a: A, b: A): Boolean
	
	
	// OTHER	--------------------
	
	/**
	  * Sets up this manager once other attributes have been initialized. Enables content change listening.
	  */
	protected def setup() = contentPointer.addListener(ContentUpdateListener, Some(Vector()))
	
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
	def displayFor(item: A): Option[C] = displayMatching(item)(itemsAreEqual)
	
	/**
	  * Replaces a single item in content
	  * @param oldItem Item to be replaced
	  * @param newItem The item that will replace the old item
	  */
	def replace(oldItem: A, newItem: A) =
	{
		content.indexWhereOption { itemsAreEqual(oldItem, _) }.foreach { targetIndex =>
			content = content.updated(targetIndex, newItem)
		}
	}
	
	/**
	  * Updates a single item in this display's content (only useful for mutable entities)
	  * @param item Item to be updated
	  */
	def updateSingle(item: A) =
	{
		content.indexWhereOption { itemsAreEqual(item, _) } match
		{
			case Some(index) =>
				val targetedDisplay = displays(index)
				targetedDisplay.content = item
				content = content.updated(index, item)
			case None => content :+= item
		}
	}
	
	
	// NESTED CLASSES	-------------
	
	private object ContentUpdateListener extends ChangeListener[Vector[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[A]]) =
		{
			setContent(event.newValue)
			finalizeRefresh()
		}
		
		private def setContent(newValues: Vector[A]) =
		{
			val d = displays
			val oldContentSize = d.size
			val newContentSize = newValues.size
			//println()
			//println(s"Updating content from $oldContentSize to $newContentSize items\nOld: ${d.map { _.content }}\nNew: $newValues")
			
			val sizeDifference = newContentSize - oldContentSize
			
			// Finds similar start and end portions (if present)
			val identicalStart = d.zip(newValues).takeWhile { case (display, newVal) =>
				itemsAreEqual(display.content, newVal) }
			val identicalEnd =
			{
				// Compared parts (old vs new) must have identical size
				val comparedPart =
				{
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
					case (display, newVal) => itemsAreEqual(display.content, newVal) }.toVector
			}
			
			val skipFirst = identicalStart.size
			val skipLast = identicalEnd.size
			
			//println(s"$skipFirst identical items in the beginning, $skipLast at the end.")
			
			// Size difference is positive => new items added somewhere
			if (sizeDifference > 0)
			{
				// Case: New items are added to the end
				if (skipFirst == oldContentSize)
				{
					//println(s"Adding $sizeDifference items (right side of $newContentSize) to index $oldContentSize")
					addDisplaysFor(newValues.takeRight(sizeDifference), oldContentSize)
				}
				// Case: New items are added to the beginning
				else if (skipLast == oldContentSize)
				{
					//println(s"Adding $sizeDifference items (left of $newContentSize) to start")
					addDisplaysFor(newValues.take(sizeDifference), 0)
				}
				// Case: New items are added to the middle
				else if (skipFirst + skipLast == oldContentSize)
				{
					//println(s"Adding indices $skipFirst-${skipFirst + sizeDifference} from $newContentSize to index $skipFirst")
					addDisplaysFor(newValues.slice(skipFirst, skipFirst + sizeDifference), skipFirst)
				}
				// Case: New items added in multiple locations
				else
				{
					// Adds items either to the beginning of collection or to the end of the updated area
					val insertToEnd = identicalStart.nonEmpty || identicalEnd.isEmpty
					val updateRange = skipFirst until (oldContentSize - skipLast)
					val updatedValues = if (insertToEnd) newValues.slice(updateRange) else
						newValues.dropRight(skipLast).takeRight(updateRange.size)
					val insertedValues = if (insertToEnd) newValues.takeRight(sizeDifference) else newValues.take(sizeDifference)
					val insertIndex = if (insertToEnd) updateRange.last + 1 else 0
					
					// Updates first, then adds new displays
					update(updateRange, updatedValues)
					//println(s"Inserts $insertedValues to index $insertIndex")
					addDisplaysFor(insertedValues, insertIndex)
				}
			}
			// Size difference is negative => old items removed somewhere
			else if (sizeDifference < 0)
			{
				val removeCount = -sizeDifference
				
				// Case: Items removed from the end
				if (skipFirst == newContentSize)
				{
					//println(s"Dropping items $newContentSize-${oldContentSize - 1}")
					dropDisplaysAt(newContentSize until oldContentSize)
				}
				// Case: Items removed from the beginning
				else if (skipLast == newContentSize)
				{
					//println(s"Dropping $removeCount items from the beginning")
					dropDisplaysAt(0 until removeCount)
				}
				// Case: Items removed from the middle
				else if (skipFirst + skipLast == newContentSize)
				{
					//println(s"Dropping $removeCount items from index $skipFirst")
					dropDisplaysAt(skipFirst until (skipFirst + removeCount))
				}
				// Case: Items removed from multiple places
				else
				{
					// Drops rows either from the beginning of the collection or at the end of the updated area
					val dropFromEnd = identicalStart.nonEmpty || identicalEnd.isEmpty
					val updatedRange = skipFirst until (newContentSize - skipLast)
					
					// Drops first, then updates
					if (dropFromEnd)
					{
						//println(s"Dropping items at ${oldContentSize - skipLast - removeCount}-${oldContentSize - skipLast - 1}")
						dropDisplaysAt((oldContentSize - skipLast - removeCount) until (oldContentSize - skipLast))
					}
					else
					{
						//println(s"Dropping $removeCount items from the beginning")
						dropDisplaysAt(0 until removeCount)
					}
					
					update(updatedRange, newValues.slice(updatedRange))
				}
			}
			// Sizes are identical => content swapped somewhere
			else
			{
				// Updates the content between identical areas
				val updateRange = skipFirst until (newContentSize - skipLast)
				update(updateRange, newValues.slice(updateRange))
			}
		}
		
		private def update(targetRange: Range, items: Vector[A]) =
		{
			//println(s"Updating range: $targetRange")
			displays.slice(targetRange).foreachWith(items) { (d, i) => d.content = i }
		}
	}
}
