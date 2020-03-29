package utopia.reflection.controller.data

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.input.SelectableWithPointers
import utopia.reflection.component.Refreshable

/**
  * This manager handles displayed content AND selection
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1+
  */
trait SelectionManager[A, C <: Refreshable[A]] extends ContentManager[A, C] with SelectableWithPointers[Option[A], Vector[A]]
{
	// ATTRIBUTES	-------------------
	
	private var _selectedDisplay: Option[C] = None
	override val valuePointer = new PointerWithEvents[Option[A]](None)
	
	
	// INITIAL CODE	-------------------
	
	valuePointer.addListener(new ValueUpdateListener)
	contentPointer.addListener(new ContentUpdateSelectionHandler)
	
	
	// ABSTRACT	-----------------------
	
	/**
	  * Updates how selection is displayed
	  * @param oldSelected The old selected item (None if no item was selected before)
	  * @param newSelected The new selected item (None if no item is selected anymore)
	  */
	protected def updateSelectionDisplay(oldSelected: Option[C], newSelected: Option[C]): Unit
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return The currently selected display. None if no item is currently selected
	  */
	def selectedDisplay = _selectedDisplay
	
	
	// OTHER	-------------------
	
	/**
	  * Moves the selection (cursor) by specified amount (of items)
	  * @param amount Number of items to progress
	  */
	def moveSelection(amount: Int) =
	{
		if (amount != 0)
		{
			val displays = this.displays
			if (displays.nonEmpty)
			{
				val oldIndex = _selectedDisplay.flatMap{ displays.optionIndexOf(_) }
				
				if (oldIndex.isDefined)
				{
					// Moves the selection by one
					val newIndex = (oldIndex.get + amount) % displays.size
					
					if (newIndex < 0)
						selectDisplay(displays(newIndex + displays.size))
					else
						selectDisplay(displays(newIndex))
				}
				// If no item was selected previously, either selects the first or last item
				else if (amount > 0)
					selectDisplay(displays.head)
				else
					selectDisplay(displays.last)
			}
		}
	}
	
	/**
	  * Moves selection one step forward
	  */
	def selectNext() = moveSelection(1)
	
	/**
	  * Moves selection one step backwards
	  */
	def selectPrevious() = moveSelection(-1)
	
	/**
	  * Handles mouse click event
	  * @param displayAtMousePosition The display closest to / under the mouse cursor
	  */
	protected def handleMouseClick(displayAtMousePosition: C) = selectDisplay(displayAtMousePosition)
	
	private def updateSelection(newValue: Option[A]): Unit =
	{
		val oldSelected = _selectedDisplay
		_selectedDisplay = newValue.flatMap { v => displays.find { d => itemsAreEqual(v, d.content) } }
		
		if (oldSelected != _selectedDisplay)
			updateSelectionDisplay(oldSelected, _selectedDisplay)
	}
	
	private def selectDisplay(display: C) =
	{
		if (!_selectedDisplay.contains(display))
			value = Some(display.content)
	}
	
	
	// NESTED CLASSES	---------------------
	
	private class ContentUpdateSelectionHandler extends ChangeListener[Vector[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Vector[A]]) =
		{
			// Tries to preserve selection after refresh
			value.foreach { currentValue =>
				if (event.newValue.exists { newV => itemsAreEqual(currentValue, newV) })
					updateSelection(value)
				else
					value = None
			}
		}
	}
	
	private class ValueUpdateListener extends ChangeListener[Option[A]]
	{
		override def onChangeEvent(event: ChangeEvent[Option[A]]) = updateSelection(event.newValue)
	}
}
