package utopia.firmament.model.enumeration

import utopia.firmament.model.enumeration.GuiElementState.Activated
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

/**
  * An enumeration for different states / statuses a GUI element may have
  * @author Mikko Hilpinen
  * @since 10.4.2023, v1.0
  */
// TODO: Review use-cases of Activated and see if Pressed is more suitable
sealed trait GuiElementState
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The level / intensity of interaction / highlighting, where 0 is no interaction
	 */
	def level: Int
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
		override val level: Int = 1
		override val effect = Positive
		override protected val _impliedStates: Set[GuiElementState] = Set()
	}
	/**
	  * State where the component's primary function has been activated.
	  * E.g. a button being pressed.
	  */
	case object Activated extends GuiElementState
	{
		override val level: Int = 3
		override val effect = Positive
		override protected lazy val _impliedStates: Set[GuiElementState] = Set(Focused)
	}
	/**
	  * State where the component is not interactive
	  */
	case object Disabled extends GuiElementState
	{
		override val level: Int = -1
		override val effect = Negative
		override protected val _impliedStates: Set[GuiElementState] = Set()
	}
}

sealed trait MouseInteractionState extends SelfComparable[MouseInteractionState]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The level / scale of interaction, where 0 is no interaction
	 */
	def level: Int
	
	
	// IMPLEMENTED  --------------------
	
	override def self: MouseInteractionState = this
	
	override def compareTo(o: MouseInteractionState): Int = level - o.level
}

object MouseInteractionState
{
	/**
	 * A state with no mouse interaction
	 */
	case object NoInteraction extends MouseInteractionState
	{
		override val level: Int = 0
	}
	/**
	 * State that's applicable for components that have the mouse cursor hovering over them
	 */
	case object Hover extends MouseInteractionState with GuiElementState
	{
		override val level: Int = 1
		override val effect = Positive
		override protected val _impliedStates: Set[GuiElementState] = Set()
	}
	/**
	 * A state applied to components that are being triggered by a mouse press
	 */
	case object Pressed extends MouseInteractionState with GuiElementState
	{
		override val level: Int = 3
		override val effect: Sign = Positive
		override protected lazy val _impliedStates: Set[GuiElementState] = Set(Hover, Activated)
	}
}