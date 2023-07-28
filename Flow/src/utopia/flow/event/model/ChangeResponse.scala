package utopia.flow.event.model

import scala.language.implicitConversions

/**
  * Common trait for different responses that may be given for a change event
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
trait ChangeResponse
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Whether the associated change event recipient should continue to receive change events
	  *         (from this source) in the future.
	  */
	def shouldContinueListening: Boolean
	/**
	  * @return Effects that should be performed once the other primary effects of the associated event
	  *         have been resolved.
	  */
	def afterEffects: Iterable[() => Unit]
	
	/**
	  * Creates a copy of this response that also includes the specified after-effect
	  * @param afterEffect A function that should be run after the change event has resolved (call-by-name)
	  * @tparam U Arbitrary function return type
	  * @return Copy of this response with the specified after-effect included
	  */
	def and[U](afterEffect: => U): ChangeResponse
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Whether the associated change event recipient should no longer continue to receive
	  *         change events (from this source)
	  */
	def shouldDetach = !shouldContinueListening
}

object ChangeResponse
{
	// IMPLICIT --------------------------
	
	// Unit response (i.e. response not specified) is implicitly interpreted as Continue
	implicit def continueByDefault(u: Unit): ChangeResponse = Continue
	
	
	// OTHER    --------------------------
	
	/**
	  * @param detachmentCondition Whether the change event recipient should be detached and no longer receive
	  *                            change events in the future
	  * @return A new change response matching the specified condition parameter
	  */
	def continueUnless(detachmentCondition: Boolean): ChangeResponse = if (detachmentCondition) Detach else Continue
	/**
	  * @param continueCondition Whether the change event recipient should continue to receive change events
	  *                          in the future as well.
	  * @return A change response matching the specified condition parameter
	  */
	def continueIf(continueCondition: Boolean): ChangeResponse = if (continueCondition) Continue else Detach
	
	
	// NESTED   --------------------------
	
	/**
	  * Change response used when the event recipient wishes to continue receiving change events
	  */
	case object Continue extends ChangeResponse
	{
		override val shouldContinueListening: Boolean = true
		override val afterEffects: Iterable[() => Unit] = Vector.empty
		
		override def and[U](afterEffect: => U): ChangeResponse = ContinueAnd(Vector(() => afterEffect))
	}
	
	/**
	  * Change response used when the event recipient wishes to withdraw from event-receiving
	  */
	case object Detach extends ChangeResponse
	{
		override val shouldContinueListening: Boolean = false
		override val afterEffects: Iterable[() => Unit] = Vector.empty
		
		override def and[U](afterEffect: => U): ChangeResponse = DetachAnd(Vector(() => afterEffect))
	}
	
	/**
	  * Change response used when the event receiver wishes to continue receiving change events,
	  * and also needs to trigger certain effects after the event has been resolved.
	  * @param afterEffects Effects to trigger after the change event has been resolved
	  */
	case class ContinueAnd(afterEffects: Seq[() => Unit]) extends ChangeResponse
	{
		override def shouldContinueListening: Boolean = true
		
		override def toString = {
			if (afterEffects.isEmpty)
				"Continue"
			else
				s"Continue and perform ${afterEffects.size} actions"
		}
		
		override def and[U](afterEffect: => U): ChangeResponse = copy(afterEffects :+ { () => afterEffect })
	}
	
	/**
	  * Change response used when the event receiver wishes to stop receiving change events,
	  * but also needs to trigger certain effects after the event has been resolved.
	  * @param afterEffects Effects to trigger after the change event has been resolved
	  */
	case class DetachAnd(afterEffects: Seq[() => Unit]) extends ChangeResponse
	{
		override def shouldContinueListening: Boolean = false
		
		override def toString = {
			if (afterEffects.isEmpty)
				"Detach"
			else
				s"Detach and perform ${ afterEffects.size } actions"
		}
		
		// WET WET
		override def and[U](afterEffect: => U): ChangeResponse = copy(afterEffects :+ { () => afterEffect })
	}
}