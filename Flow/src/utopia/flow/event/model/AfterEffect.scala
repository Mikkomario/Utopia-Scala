package utopia.flow.event.model

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.model.ChangeResponsePriority.{After, High, Normal}

import scala.language.implicitConversions

object AfterEffect
{
	// ATTRIBUTES   ---------------------
	
	private val factories = ChangeResponsePriority.descending.iterator.map { p => p -> new AfterEffectFactory(p) }.toMap
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Factory for constructing after-effects that trigger *after* the other effects have resolved
	 */
	def after = apply(After)
	/**
	 * @return Factory for constructing after-effects that trigger right after the regular change-listeners have
	 *         been informed, which is before the regular after-effects.
	 */
	def before = apply(Normal)
	/**
	 * @return Factory for constructing after-effects that trigger as soon as possible, even before regular
	 *         change listeners get informed of the originating event (if possible).
	 */
	def asap = apply(High)
	
	
	// IMPLICIT -------------------------
	
	// Implicitly applies the "After" priority level
	implicit def applyDefaultPriority(o: AfterEffect.type): AfterEffectFactory = o.after
	
	
	// OTHER    -------------------------
	
	/**
	 * @param priority Priority of the after effect that's being created
	 * @return Access to a factory for constructing after effects of that priority
	 */
	def apply(priority: ChangeResponsePriority) = factories(priority)
	
	
	// NESTED   -------------------------
	
	class AfterEffectFactory(priority: ChangeResponsePriority)
	{
		// IMPLICIT ---------------------
		
		/**
		 * @param f A function called when it's time to trigger this effect
		 * @return A new after-effect which calls that function
		 * @tparam U Arbitrary result type of 'f'
		 * @see [[chaining]], if you need to trigger further after-effects
		 */
		implicit def apply[U](f: => U): AfterEffect = chaining { f; Empty }
		
		
		// OTHER    --------------------
		
		/**
		 * @param f A function called when it's time to trigger this effect.
		 *          Yields further effects that should also be triggered.
		 * @return A new after-effect that calls the specified function, potentially generating further after-effects.
		 */
		def chaining(f: => IterableOnce[AfterEffect]): AfterEffect = new _AfterEffect(priority, f)
	}
	
	private class _AfterEffect(override val priority: ChangeResponsePriority, f: => IterableOnce[AfterEffect])
		extends AfterEffect
	{
		override def trigger(): IterableOnce[AfterEffect] = f
	}
}

/**
 * Common trait for change-response after-effects,
 * which are triggered after the listeners / primary responses (of that and higher priority levels) have resolved.
 * @author Mikko Hilpinen
 * @since 14.12.2025, v2.8
 */
trait AfterEffect
{
	/**
	 * @return The priority of this after-effect, which determines when it should be triggered.
	 */
	def priority: ChangeResponsePriority
	
	/**
	 * A function called once it's time to trigger this after effect.
	 * @return Further after-effects to trigger.
	 */
	def trigger(): IterableOnce[AfterEffect]
}
