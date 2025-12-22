package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.BecomesEventfulVolatile
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper, Flag}

import scala.language.implicitConversions

object GeneratesAgain
{
	// ATTRIBUTES -----------------------------
	
	/**
	 * Access to generates again -constructors for regular pointers
	 * @see [[volatile]], if you want to construct thread-safe pointers
	 */
	val normal = new GeneratesAgainFactory(volatile = false)
	/**
	 * Access to constructors of thread-safe pointer versions
	 */
	val volatile = new GeneratesAgainFactory(volatile = true)
	
	
	// IMPLICIT ------------------------------
	
	// Implicitly accesses the regular constructors
	implicit def objectAsFactory(o: GeneratesAgain.type): GeneratesAgainFactory = o.normal
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param volatile Whether to construct volatile pointers
	 * @return A factory used for constructing pointers of the targeted type
	 */
	def volatileIf(volatile: Boolean) = if (volatile) this.volatile else normal
	
	
	// NESTED   ------------------------------
	
	class GeneratesAgainFactory(volatile: Boolean)
	{
		/**
		 * Creates a new pointer that initializes when its value is called.
		 * The pointer will fire a change event whenever it initializes.
		 * Supports resetting.
		 *
		 * @param generate A function that will generate a new value, once it is needed.
		 * @param log Implicit logging implementation used in change-event handling
		 * @tparam A Type of the generated value
		 * @return A new pointer that will initialize once its value is called
		 */
		def apply[A](generate: => A)(implicit log: Logger): GeneratesAgain[A] =
			new _GeneratesAgain[A](None, generate, volatile)
		/**
		 * Creates a new pointer that initializes when its value is called.
		 * The pointer will fire a change event whenever it initializes.
		 * Supports resetting.
		 *
		 * @param oldValue A function that will generate the old value, used when generating change events.
		 *                 May be called 0-n times.
		 * @param generate A function that will generate a new value, once it is needed.
		 * @param log Implicit logging implementation used in change-event handling
		 * @tparam A Type of the generated value
		 * @return A new pointer that will initialize once its value is called
		 */
		def simulatingOldValue[A](oldValue: => A)(generate: => A)(implicit log: Logger): GeneratesAgain[A] =
			new _GeneratesAgain[A](Some(oldValue), generate, volatile)
	}
	
	private class _GeneratesAgain[A](simulateOldValue: => Option[A], generate: => A, volatile: Boolean = false)
	                                (implicit log: Logger)
		extends AbstractChanging[A] with GeneratesAgain[A]
	{
		// ATTRIBUTES   ----------------------
		
		override val destiny: Destiny = ForeverFlux
		
		/**
		 * Stores the last generated value, so that it may be used as the "old value" in the next generated change event
		 */
		private var lastValue: Option[A] = None
		
		private val wrapped = {
			if (volatile)
				BecomesEventfulVolatile.empty[A]
			else
				BecomesEventfulPointer.empty[A]
		}
		override lazy val emptyFlag: Flag = wrapped.eventful.emptyFlag
		override lazy val nonEmptyFlag: Flag = !emptyFlag
		
		override lazy val readOnly: Changing[A] = ChangingWrapper(this)
		
		
		// IMPLEMENTED  ----------------------
		
		override def value: A = {
			// Updates the wrapped pointer, if necessary
			val (result, wasGenerated) = wrapped.mutate {
				// Case: A value was already generated => Yields that value without changes
				case value @ Some(initialized) => (initialized, false) -> value
				// Case: A value was not yet generated => Generates a new value and assigns that to the wrapped pointer
				case None =>
					val newValue = generate
					(newValue, true) -> Some(newValue)
			}
			
			// Case: Generated a new value => Updates "last value" and fires a change event, if needed
			if (wasGenerated) {
				val last = lastValue
				lastValue = Some(result)
				
				if (hasListeners)
					fireEvent(ChangeEvent(last.getOrElse { simulateOldValue.getOrElse(result) }, result))
			}
			
			result
		}
		
		override def current: Option[A] = wrapped.value
		override def currentPointer: Changing[Option[A]] = wrapped.eventful.readOnly
		
		override def reset(): Boolean = wrapped.reset()
		
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
		
		override protected def _map[B](f: A => B): GeneratesAgain[B] = {
			// Generates a mapped lazily initialized pointer
			val mapped = new _GeneratesAgain[B](lastValue.orElse(simulateOldValue).map(f), f(generate), volatile)
			
			// Whenever this pointer is initialized, cascades the change events from the mapped pointer, also
			// Whenever this pointer is reset, also resets the other pointer
			wrapped.eventful.addListener { e =>
				if (e.newValue.isDefined) {
					if (mapped.hasListeners)
						mapped.value
				}
				else
					mapped.reset()
				
				Continue
			}
			
			mapped
		}
	}
}

/**
 * A [[Changing]] version of [[ResettableLazy]].
 * Generates the value and change-event only on demand, and may be reset when needed.
 * @author Mikko Hilpinen
 * @since 22.12.2025, v2.8
 */
trait GeneratesAgain[+A] extends GeneratesLike[A, GeneratesAgain] with ResettableLazy[A]
{

}
