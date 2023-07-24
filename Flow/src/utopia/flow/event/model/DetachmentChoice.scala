package utopia.flow.event.model

import scala.language.implicitConversions

@deprecated("Replaced with ChangeResponse", "v2.2")
object DetachmentChoice
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * Represents a choice to continue listening for new events
	  */
	@deprecated("Replaced with ChangeResponse.Continue", "v2.2")
	val continue = apply(shouldContinue = true)
	/**
	  * Represents a choice to detach from an event source
	  */
	@deprecated("Replaced with ChangeResponse.Detach", "v2.2")
	val detach = apply(shouldContinue = false)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param condition A condition
	  * @return Continues if 'condition' is true, otherwise detaches
	  */
	@deprecated("Replaced with ChangeResponse.continueIf(Boolean)", "v2.2")
	def continueIf(condition: Boolean) = apply(condition)
	/**
	  * @param condition A condition
	  * @return Continues if the 'condition' is false, otherwise detaches
	  */
	@deprecated("Replaced with ChangeResponse.continueUnless(Boolean)", "v2.2")
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
@deprecated("Replaced with ChangeResponse", "v2.2")
case class DetachmentChoice(shouldContinue: Boolean) extends ChangeResponse
{
	override def shouldContinueListening: Boolean = shouldContinue
	override def afterEffects: Iterable[() => Unit] = Vector.empty
	
	override def and[U](afterEffect: => U): ChangeResponse = ChangeResponse.continueIf(shouldContinue).and(afterEffect)
}
