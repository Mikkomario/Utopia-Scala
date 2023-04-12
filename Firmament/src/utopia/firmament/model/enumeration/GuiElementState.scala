package utopia.firmament.model.enumeration

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * An enumeration for different states / statuses a GUI element may have
  * @author Mikko Hilpinen
  * @since 10.4.2023, v1.0
  */
trait GuiElementState
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Whether this state is affecting the interactivity positively or negatively.
	  *         For example, a hover state is considered positive and a disabled state is considered negative.
	  */
	def effect: Sign
	
	/**
	  * @return GUI element states that are directly implied by the presence of this state.
	  *         E.g. An activated GUI element is considered to be in focus and hovered.
	  */
	protected def _impliedStates: Set[GuiElementState]
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return GUI element states that are directly or indirectly implied by the presence of this state.
	  *         E.g. An activated GUI element is considered to be in focus and hovered.
	  */
	def impliedStates: Set[GuiElementState] = {
		val impl = _impliedStates
		if (impl.nonEmpty)
			impl ++ impl.flatMap { _.impliedStates }
		else
			impl
	}
}

object GuiElementState
{
	// VALUES   -----------------------
	
	/**
	  * State that's applicable for focused components
	  */
	case object Focused extends GuiElementState
	{
		override def effect = Positive
		override protected def _impliedStates: Set[GuiElementState] = Set()
	}
	/**
	  * State that's applicable for components that have the mouse cursor hovering over them
	  */
	case object Hover extends GuiElementState
	{
		override def effect = Positive
		override protected def _impliedStates: Set[GuiElementState] = Set()
	}
	/**
	  * State where the component's primary function has been activated.
	  * E.g. a button being pressed.
	  */
	case object Activated extends GuiElementState
	{
		override def effect = Positive
		override protected def _impliedStates: Set[GuiElementState] = Set(Focused, Hover)
	}
	/**
	  * State where the component is not interactive
	  */
	case object Disabled extends GuiElementState
	{
		override def effect = Negative
		override protected def _impliedStates: Set[GuiElementState] = Set()
	}
}