package utopia.flow.event.model

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.event.model.ChangeResponse.ConditionalResponse
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View

import scala.annotation.unused
import scala.language.implicitConversions

/**
  * Common trait for different responses that may be given for a change event
  * @author Mikko Hilpinen
  * @since 24.7.2023, v2.2
  */
sealed trait ChangeResponse
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Whether the associated change event -recipient should continue to receive change events
	  *         (from this source) in the future.
	  */
	def shouldContinueListening: Boolean
	/**
	  * @return Effects that should be performed as a response to the original change event.
	 *
	 *         The timing when these events will be triggered is dependent on their priority.
	 *         For example, events with High priority will be triggered right after other high-priority listeners
	 *         and effects have been resolved (i.e. before informing the regular change listeners)
	  */
	def afterEffects: IterableOnce[AfterEffect]
	
	/**
	 * @return A response "opposite" to this one in terms of the choice to detach or continue to receive further events.
	 *         E.g. if called for Continue, this will yield Detach.
	 *         All original after-effects will also be included.
	 */
	def opposite: ChangeResponse
	
	/**
	  * Creates a copy of this response that also includes the specified after-effect
	  * @param afterEffect An after-effect that should be triggered after this response has been given
	  * @return Copy of this response including the specified after-effect
	  */
	def and(afterEffect: AfterEffect): ChangeResponse
	/**
	 * @param afterEffects After-effects to trigger as a change response
	 * @return Copy of this response including the specified after-effects
	 */
	def ++(afterEffects: IterableOnce[AfterEffect]): ChangeResponse
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Whether the associated change event recipient should no longer continue to receive
	  *         change events (from this source)
	  */
	def shouldDetach = !shouldContinueListening
	
	/**
	 * Alias for [[opposite]]
	 */
	def unary_! = opposite
	
	
	// OTHER    ----------------------
	
	/**
	 * @param priority Priority at which the specified effect should be triggered
	 * @param effect A function called as an after-effect
	 * @return A copy of this response including the specified after-effect
	 */
	def and(priority: ChangeResponsePriority)(effect: => Unit): ChangeResponse = and(AfterEffect(priority)(effect))
	/**
	 * @param afterEffects After-effects to trigger as a change response
	 * @return Copy of this response including the specified after-effects
	 */
	def andAll(afterEffects: IterableOnce[AfterEffect]) = this ++ afterEffects
	
	/**
	 * @param condition A condition for this detachment choice. Call-by-name.
	 * @return A copy of this response that reverses this response's detachment choice,
	 *         if the specified condition is not met (at that time)
	 */
	def onlyIf(condition: => Boolean): ChangeResponse = onlyIf(View(condition))
	/**
	 * @param conditionView A condition for this detachment choice, wrapped in a View.
	 * @return A copy of this response that reverses this response's detachment choice,
	 *         if the specified condition is not met (at that time)
	 */
	def onlyIf(conditionView: View[Boolean]) = new ConditionalResponse(this, conditionView)
	/**
	 * @param condition A condition for reversing this detachment choice. Call-by-name.
	 * @return A copy of this response that reverses this response's detachment choice,
	 *         if the specified condition is met (at that time)
	 */
	def unless(condition: => Boolean): ChangeResponse = unless(View(condition))
	/**
	 * @param conditionView A condition for reversing this detachment choice, wrapped in a View.
	 * @return A copy of this response that reverses this response's detachment choice,
	 *         if the specified condition is met (at that time)
	 */
	def unless(conditionView: View[Boolean]) = new ConditionalResponse(this, conditionView, not = true)
}

object ChangeResponse
{
	// IMPLICIT --------------------------
	
	// Unit response (i.e. response not specified) is implicitly interpreted as Continue
	implicit def continueByDefault(@unused u: Unit): ChangeResponse = Continue
	
	
	// OTHER    --------------------------
	
	/**
	  * @param detachmentCondition Whether the change event recipient should be detached and no longer receive
	  *                            change events in the future
	  * @return A new change response matching the specified condition parameter
	  */
	@deprecated("Please use Continue.unless(Boolean) instead", "v2.8")
	def continueUnless(detachmentCondition: Boolean): ChangeResponse = Continue.unless(detachmentCondition)
	/**
	  * @param continueCondition Whether the change event recipient should continue to receive change events
	  *                          in the future as well.
	  * @return A change response matching the specified condition parameter
	  */
	@deprecated("Please use Continue.onlyIf(Boolean) instead", "v2.8")
	def continueIf(continueCondition: Boolean): ChangeResponse = Continue.onlyIf(continueCondition)
	
	
	// NESTED   --------------------------
	
	/**
	 * Common trait for change responses that already specify after-effects.
	 * Used for simplifying response-implementation.
	 */
	trait ChangeResponseWithEffects extends ChangeResponse
	{
		// ABSTRACT ----------------------
		
		protected def verb: String
		
		protected def withEffects(effects: IterableOnce[AfterEffect]): ChangeResponse
		
		
		// IMPLEMENTED  ------------------
		
		override def toString = afterEffects match {
			case _: scala.collection.View[_] => s"$verb and possibly trigger effects"
			case i: Iterable[_] =>
				if (i.isEmpty)
					verb
				else {
					i.sizeIfKnown match {
						case Some(size) => s"$verb and trigger $size effects"
						case None => s"$verb and trigger effects"
					}
				}
			case _ => s"$verb and possibly trigger effects"
		}
		
		override def and(afterEffect: AfterEffect): ChangeResponse = {
			val newEffects: IterableOnce[AfterEffect] = afterEffects match {
				case s: Seq[AfterEffect] => s :+ afterEffect
				case v: scala.collection.View[AfterEffect] => scala.collection.View.concat(v, Single(afterEffect))
				case i: Iterable[AfterEffect] =>
					if (i.isEmpty) Single(afterEffect) else scala.collection.View.concat(i, Single(afterEffect))
				case i => i.iterator ++ Single(afterEffect)
			}
			withEffects(newEffects)
		}
		override def ++(afterEffects: IterableOnce[AfterEffect]): ChangeResponse = {
			val newEffects = this.afterEffects match {
				case i: Iterable[AfterEffect] => i ++ afterEffects
				case _ => this.afterEffects.iterator ++ afterEffects
			}
			withEffects(newEffects)
		}
	}
	
	/**
	  * Change response used when the event recipient wishes to continue receiving change events
	  */
	case object Continue extends ChangeResponse
	{
		override val shouldContinueListening: Boolean = true
		override val afterEffects = Empty
		
		override def opposite: ChangeResponse = Detach
		
		override def and(afterEffect: AfterEffect): ChangeResponse = ContinueAnd(Single(afterEffect))
		override def ++(afterEffects: IterableOnce[AfterEffect]): ChangeResponse = ContinueAnd(afterEffects)
	}
	/**
	  * Change response used when the event recipient wishes to withdraw from event-receiving
	  */
	case object Detach extends ChangeResponse
	{
		override val shouldContinueListening: Boolean = false
		override val afterEffects = Empty
		
		override def opposite: ChangeResponse = Continue
		
		override def and(afterEffect: AfterEffect): ChangeResponse = DetachAnd(Single(afterEffect))
		override def ++(afterEffects: IterableOnce[AfterEffect]): ChangeResponse = DetachAnd(afterEffects)
	}
	
	/**
	  * Change response used when the event receiver wishes to continue receiving change events,
	  * and also needs to trigger certain effects after the event has been resolved.
	  * @param afterEffects Effects to trigger after the change event has been resolved
	  */
	case class ContinueAnd(afterEffects: IterableOnce[AfterEffect]) extends ChangeResponseWithEffects
	{
		override val shouldContinueListening: Boolean = true
		
		override def opposite: ChangeResponse = DetachAnd(afterEffects)
		
		override protected def verb: String = "Continue"
		
		override protected def withEffects(effects: IterableOnce[AfterEffect]): ChangeResponse = ContinueAnd(effects)
	}
	/**
	  * Change response used when the event receiver wishes to stop receiving change events,
	  * but also needs to trigger certain effects after the event has been resolved.
	  * @param afterEffects Effects to trigger after the change event has been resolved
	  */
	case class DetachAnd(afterEffects: IterableOnce[AfterEffect]) extends ChangeResponseWithEffects
	{
		override val shouldContinueListening: Boolean = false
		
		override def opposite: ChangeResponse = ContinueAnd(afterEffects)
		
		override protected def verb: String = "Detach"
		
		override protected def withEffects(effects: IterableOnce[AfterEffect]): ChangeResponse = DetachAnd(effects)
	}
	
	class ConditionalResponse(wrapped: ChangeResponse, condition: View[Boolean], not: Boolean = false)
		extends ChangeResponse
	{
		// IMPLEMENTED  --------------------------
		
		override def shouldContinueListening: Boolean = {
			// Depending on the wrapped condition, may reverse the detachment choice
			val default = wrapped.shouldContinueListening
			if (not) {
				if (condition.value) !default else default
			}
			else if (condition.value) default else !default
		}
		
		override def afterEffects: IterableOnce[AfterEffect] = wrapped.afterEffects
		
		override def opposite: ChangeResponse = new ConditionalResponse(wrapped, condition, !not)
		
		override def and(afterEffect: AfterEffect): ChangeResponse = mapWrapped { _.and(afterEffect) }
		override def ++(afterEffects: IterableOnce[AfterEffect]): ChangeResponse = mapWrapped { _ ++ afterEffects }
		
		
		// OTHER    ----------------------------
		
		private def mapWrapped(f: Mutate[ChangeResponse]) = new ConditionalResponse(f(wrapped), condition)
	}
}