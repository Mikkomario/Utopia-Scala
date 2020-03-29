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
	// ATTRIBUTES	------------------
	
	private var updatingOnly: Option[A] = None
	
	
	// ABSTRACT	----------------------
	
	/**
	  * @return The currently used displays
	  */
	def displays: Vector[C]
	
	/**
	  * Adds new displays for new values
	  * @param values New values that need to be displayed
	  */
	protected def addDisplaysFor(values: Vector[A]): Unit
	
	/**
	  * Removes unnecessary displays
	  * @param dropped The displays to be dropped
	  */
	protected def dropDisplays(dropped: Vector[C])
	
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
			updatingOnly = Some(newItem)
			content = content.updated(targetIndex, newItem)
			updatingOnly = None
		}
	}
	
	/**
	  * Updates a single item in this display's content (only useful for mutable entities)
	  * @param item Item to be updated
	  */
	def updateSingle(item: A) =
	{
		updatingOnly = Some(item)
		content.indexWhereOption { itemsAreEqual(item, _) } match
		{
			case Some(index) =>
				val oldContent = content
				val newContent = content.updated(index, item)
				content = newContent
				// Will have to manually trigger change event if the two content's are equal
				if (oldContent == newContent)
					ContentUpdateListener.onChangeEvent(new ChangeEvent[Vector[A]](oldContent, newContent))
			case None => content :+= item
		}
		updatingOnly = None
	}
	
	
	// NESTED CLASSES	-------------
	
	private object ContentUpdateListener extends ChangeListener[Vector[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[A]]) =
		{
			val d = displays
			val newContent = event.newValue
			
			// Existing rows are updated (may skip update if targeting only a single row)
			updatingOnly match
			{
				// The individual target row is found with content's index
				case Some(onlyTarget) => newContent.optionIndexOf(onlyTarget).filter { _ < d.size }
					.foreach { i => d(i).content = onlyTarget }
				case None => d.foreachWith(newContent) { _.content = _ }
			}
			
			val size = d.size
			
			// Unnecessary rows are removed and new rows may be added
			if (size > newContent.size)
				dropDisplays(d.drop(newContent.size))
			else if (size < newContent.size)
				addDisplaysFor(newContent.drop(size))
			
			finalizeRefresh()
		}
	}
}
