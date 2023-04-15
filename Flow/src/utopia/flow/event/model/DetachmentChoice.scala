package utopia.flow.event.model

import scala.language.implicitConversions

object DetachmentChoice
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * Represents a choice to continue listening for new events
	  */
	val continue = apply(shouldContinue = true)
	/**
	  * Represents a choice to detach from an event source
	  */
	val detach = apply(shouldContinue = false)
	
	
	// IMPLICIT --------------------------
	
	// Unit implicitly converts to a choice to continue attached
	implicit def continueByDefault(u: Unit): DetachmentChoice = continue
	// Boolean is converted implicitly, so that true marks a desire to continue as attached
	// while false leads to detachment
	implicit def convertBoolean(shouldContinue: Boolean): DetachmentChoice = apply(shouldContinue)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param condition A condition
	  * @return Continues if 'condition' is true, otherwise detaches
	  */
	def continueIf(condition: Boolean) = apply(condition)
	/**
	  * @param condition A condition
	  * @return Continues if the 'condition' is false, otherwise detaches
	  */
	def continueUntil(condition: Boolean) = apply(!condition)
}

/**
  * Represents a choice (from a listener) on whether they wish to continue receiving events or whether they'd rather
  * disconnect from the source of those events.
  *
  * These are then evaluated by the event sources in order to remove event listeners.
  *
  * @author Mikko Hilpinen
  * @since 14.9.2022, v1.17
  *
  * @constructor Creates a new detachment choice
  * @param shouldContinue Whether the listener should be kept attached to the event source
  */
case class DetachmentChoice(shouldContinue: Boolean)
{
	def shouldDetach = !shouldContinue
}
