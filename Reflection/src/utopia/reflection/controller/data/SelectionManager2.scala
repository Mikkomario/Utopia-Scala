package utopia.reflection.controller.data

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.ChangingLike
import utopia.reflection.component.template.display.Refreshable
import utopia.reflection.component.template.input.{InteractionWithPointer, SelectionWithPointers}

/**
  * Used for displaying content and managing selection
  * @author Mikko Hilpinen
  * @since 19.12.2020, v2
  * @tparam A Type of content in a display
  * @tparam S Type of selection (E.g. Option[A] for 0-1 selections, A for 1 selection or Vector[A] for multiple selection)
  * @tparam C Type of display component
  * @tparam PA Type of pointer that contains the displayed content
  */
trait SelectionManager2[A, S, C <: Refreshable[A], +PA <: ChangingLike[Vector[A]]]
	extends ContentDisplayer[A, C, PA] with SelectionWithPointers[S, PointerWithEvents[S], Vector[A], PA]
		with InteractionWithPointer[S]
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return A pointer to the currently selected display / displays
	  */
	def selectedDisplayPointer: ChangingLike[Iterable[C]]
	
	/**
	  * @param item A displayed item
	  * @return A selection value for that item
	  */
	protected def itemToSelection(item: A): S
	
	/**
	  * @param item A displayed item
	  * @param selection Previous selection
	  * @return A selection containing both
	  */
	protected def itemInSelection(item: A, selection: S): S
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return The currently selected display. None if no item is currently selected
	  */
	def selectedDisplay = selectedDisplayPointer.value
	
	
	// OTHER	----------------------
	
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
				val oldIndices = selectedDisplay.flatMap{ displays.optionIndexOf(_) }
				
				if (oldIndices.nonEmpty)
				{
					val oldIndex = if (amount < 0) oldIndices.min else oldIndices.max
					// Moves the selection by one
					val newIndex = (oldIndex + amount) % displays.size
					
					if (newIndex < 0)
						value = itemToSelection(displays(newIndex + displays.size).content)
					else
						value = itemToSelection(displays(newIndex).content)
				}
				// If no item was selected previously, either selects the first or last item
				else if (amount > 0)
					selectOnlyDisplay(displays.head)
				else
					selectOnlyDisplay(displays.last)
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
	  * Selects another display
	  * @param display A display to select
	  */
	def selectDisplay(display: C) =
	{
		if (selectedDisplay.forall { _ != display })
			value = itemInSelection(display.content, value)
	}
	
	/**
	  * Selects the specified display only
	  * @param display A display to select
	  */
	def selectOnlyDisplay(display: C) =
	{
		if (selectedDisplay.size != 1 || selectedDisplay.forall { _ != display })
			value = itemToSelection(display.content)
	}
}
