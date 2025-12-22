package utopia.flow.view.mutable.eventful

import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.BecomesEventfulVolatile
import utopia.flow.view.mutable.{LoggingPointerFactory, Pointer}

object BecomesEventfulPointer extends LoggingPointerFactory[BecomesEventfulPointer]
{
	// COMPUTED ----------------------
	
	/**
	 * @return Access to constructors for volatile (i.e. thread-safe) pointer implementations
	 */
	def volatile = BecomesEventfulVolatile
	
	
	// IMPLEMENTED  ------------------
	
	/**
	 * @param initialValue Initially assigned pointer value
	 * @param log Implicit logging implementation used in change-event handling
	 * @tparam A Type of the assigned values
	 * @return A new pointer that can (lazily) provide an eventful interface
	 */
	override def apply[A](initialValue: A)(implicit log: Logger) =
		wrapping(initialValue) { EventfulPointer(_) }
	
	
	// OTHER    ----------------------
	
	/**
	 * @param initialValue Initially assigned pointer value
	 * @param toEventful A function which creates the eventful pointer to wrap
	 * @tparam A Type of the assigned values
	 * @return A new pointer that lazily generates the wrapped pointer
	 */
	def wrapping[A](initialValue: A)(toEventful: A => EventfulPointer[A]): BecomesEventfulPointer[A] =
		new _BecomesEventfulPointer[A](initialValue, toEventful)
	
	
	// NESTED   ----------------------
	
	private class _BecomesEventfulPointer[A](initialValue: A, toEventful: A => EventfulPointer[A])
		extends BecomesEventfulPointer[A]
	{
		// ATTRIBUTES   --------------
		
		private var _value: Option[A] = Some(initialValue)
		private val lazyEventful = Lazy {
			val p = toEventful(_value.get)
			_value = None
			p
		}
		
		
		// IMPLEMENTED  --------------
		
		override def eventful: EventfulPointer[A] = lazyEventful.value
		
		override def value: A = _value.getOrElse { lazyEventful.value.value }
		override def value_=(newValue: A): Unit = lazyEventful.current match {
			case Some(p) => p.value = newValue
			case None => _value = Some(newValue)
		}
		
		override def mutate[B](mutate: A => (B, A)): B = _value match {
			case Some(value) =>
				val (result, newValue) = mutate(value)
				_value = Some(newValue)
				result
			case None => lazyEventful.value.mutate(mutate)
		}
		override def update(f: A => A): Unit = _value match {
			case Some(value) => _value = Some(f(value))
			case None => lazyEventful.value.update(f)
		}
	}
}

/**
 * Common trait for pointer implementations that can produce an eventful interface on-demand / lazily.
 * @author Mikko Hilpinen
 * @since 22.12.2025, v2.8
 */
trait BecomesEventfulPointer[A] extends Pointer[A]
{
	// ABSTRACT ----------------------
	
	/**
	 * @return An eventful interface to this pointer
	 */
	def eventful: EventfulPointer[A]
}
