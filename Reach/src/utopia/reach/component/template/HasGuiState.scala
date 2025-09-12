package utopia.reach.component.template

import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.{Activated, Disabled, Focused}
import utopia.firmament.model.enumeration.MouseInteractionState
import utopia.firmament.model.enumeration.MouseInteractionState.{Hover, NoInteraction, Pressed}
import utopia.reach.focus.FocusTracking

/**
  * Common trait for components that can define their GUI element state
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
trait HasGuiState extends FocusTracking
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Current state of this GUI element
	  */
	def state: GuiElementStatus
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this element is currently enabled (i.e. may be interacted with)
	  */
	def enabled = !disabled
	/**
	  * @return Whether this element is currently disabled (i.e. may not be interacted with)
	  */
	def disabled = state is Disabled
	
	/**
	  * @return Whether The mouse is currently over this element
	  */
	def isMouseOver = state is Hover
	/**
	  * @return Whether the mouse cursor is currently outside of this element's area
	  */
	def isMouseOutside = !isMouseOver
	
	/**
	  * @return Whether this element is currently being activated
	  */
	def isActivated = state is Activated
	/**
	  * @return Whether this element is currently not being activated
	  */
	def isNotActivated = !isActivated
	
	/**
	 * @return The currently applicable level of mouse interaction with this component
	 */
	def mouseInteractionLevel: MouseInteractionState = {
		if (state is Pressed)
			Pressed
		else if (state is Hover)
			Hover
		else
			NoInteraction
	}
	
	
	// IMPLEMENTED  -----------------------------
	
	override def hasFocus: Boolean = state is Focused
}
