package utopia.firmament.model

import utopia.firmament.model.enumeration.GuiElementState
import utopia.flow.operator.MaybeEmpty
import utopia.paradigm.color.Color

object GuiElementStatus
{
	// ATTRIBUTES   -------------------
	
	/**
	  * Status with no affecting states
	  */
	val identity = apply(Set[GuiElementState]())
	
	
	// OTHER    -----------------------
	
	/**
	  * @param state A GUI element state
	  * @return A status with only that state
	  */
	def apply(state: GuiElementState): GuiElementStatus = apply(Set(state))
	
	/**
	  * @param first  First state to include
	  * @param second Second state to include
	  * @param more   More states to include
	  * @return A status with those states
	  */
	def apply(first: GuiElementState, second: GuiElementState, more: GuiElementState*): GuiElementStatus =
		apply(more.toSet + first + second)
}

/**
  * Represents a snapshot of a GUI element's interactive status
  * @author Mikko Hilpinen
  * @since 10.4.2023, v1.0
  */
case class GuiElementStatus(states: Set[GuiElementState]) extends MaybeEmpty[GuiElementStatus]
{
	// COMPUTED -----------------------
	
	/**
	  * @return The states included in this status, including the implied states
	  */
	def implicitStates = states ++ states.flatMap { _.impliedStates }
	
	/**
	  * @return The intensity of this status, where 0 is no effect.
	  *         May be positive or negative.
	  */
	def intensity = implicitStates.map { _.effect.modifier }.sum
	
	/**
	  * @return Hover effect alpha value to use with this status
	  */
	def hoverAlpha = {
		val i = intensity
		if (i > 0)
			0.1 + i * 0.05
		else
			0
	}
	
	
	// IMPLEMENTED  -------------------
	
	override def self: GuiElementStatus = this
	
	override def isEmpty: Boolean = states.isEmpty
	
	override def toString = {
		if (states.isEmpty)
			"Default"
		else
			states.mkString(" & ")
	}
	
	
	// OTHER    -----------------------
	
	/**
	  * @param state A state
	  * @return Whether the described element currently has that state
	  */
	def is(state: GuiElementState) = states.contains(state)
	/**
	  * @param state A state
	  * @return Whether the described element doesn't currently have that state
	  */
	def isNot(state: GuiElementState) = !is(state)
	
	/**
	  * Modifies provided color to represent this component state
	  * @param color Original color
	  * @param intensityMod Effect intensity modifier. Default = 1.
	  * @return A modified color based on this state
	  */
	def modify(color: Color, intensityMod: Double = 1) = {
		val i = intensity
		if (i == 0)
			color
		else if (i > 0)
			color.highlightedBy(i * intensityMod)
		else
			color.timesAlpha(math.max(0.8 + i * 0.2 * intensityMod, 0.2))
	}
	
	/**
	  * @param state A state to assign
	  * @return A copy of this status with that state included
	  */
	def +(state: GuiElementState) = copy(states + state)
	/**
	  * @param state A state to subtract
	  * @return A copy of this status with that state subtracted
	  */
	def -(state: GuiElementState) = copy(states - state)
	/**
	  * @param state Targeted state and whether to add it (true) or to subtract it (false)
	  * @return A copy of this status with the modified state
	  */
	def +(state: (GuiElementState, Boolean)): GuiElementStatus = if (state._2) this + state._1 else this - state._1
	/**
	  * @param status States to include
	  * @return A copy of this status with those states added
	  */
	def ++(status: GuiElementStatus) = copy(states ++ status.states)
	/**
	  * @param status States to exclude
	  * @return A copy of this status with those states removed
	  */
	def --(status: GuiElementStatus) = copy(states -- status.states)
}
