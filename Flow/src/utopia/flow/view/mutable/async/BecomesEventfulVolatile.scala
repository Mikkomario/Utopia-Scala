package utopia.flow.view.mutable.async

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.LoggingPointerFactory
import utopia.flow.view.mutable.eventful.BecomesEventfulPointer

object BecomesEventfulVolatile extends LoggingPointerFactory[BecomesEventfulVolatile]
{
	// IMPLEMENTED  ---------------------
	
	/**
	 * @param initialValue Initially contained value
	 * @param log Implicit logging implementation used in event-handling
	 * @tparam A Type of assigned values
	 * @return A new volatile pointer that has the capacity to provide an eventful interface
	 */
	override def apply[A](initialValue: A)(implicit log: Logger) =
		wrapping[A](initialValue) { EventfulVolatile(_) }
	
	
	// OTHER    -------------------------
	
	/**
	 * @param initialValue Initially contained value
	 * @param toEventful A constructor for the wrapped eventful pointer
	 * @tparam A Type of assigned values
	 * @return A new volatile pointer that lazily provides an eventful interface
	 */
	def wrapping[A](initialValue: A)(toEventful: A => EventfulVolatile[A]): BecomesEventfulVolatile[A] =
		new _BecomesEventfulVolatile[A](initialValue, toEventful)
	
	
	// NESTED   -------------------------
	
	private class _BecomesEventfulVolatile[A](initialValue: A, toEventful: A => EventfulVolatile[A])
		extends BecomesEventfulVolatile[A]
	{
		// ATTRIBUTES   ----------------
		
		@volatile private var _value: Option[A] = Some(initialValue)
		private val lazyEventful = Lazy {
			this.synchronized {
				val p = toEventful(_value.get)
				_value = None
				p
			}
		}
		
		
		// IMPLEMENTED  ----------------
		
		override def value: A = _value.getOrElse { lazyEventful.value.value }
		
		override def eventful: EventfulVolatile[A] = lazyEventful.value
		
		override def mutate[B](mutate: A => (B, A)): B = lazyEventful.current match {
			case Some(p) => p.mutate(mutate)
			case None => super.mutate(mutate)
		}
		
		override def lockWhile[B](operation: => B): B = lazyEventful.current match {
			case Some(p) => p.lockWhile(operation)
			case None => super.lockWhile(operation)
		}
		override def viewLocked[B](operation: A => B): B = lazyEventful.current match {
			case Some(p) => p.viewLocked(operation)
			case None => super.viewLocked(operation)
		}
		
		override protected def assign(oldValue: A, newValue: A): Seq[() => Unit] = lazyEventful.current match {
			case Some(p) =>
				p.setAndQueueEvent(newValue).iterator.map[() => Unit] { e => { () => e.trigger() } }.toOptimizedSeq
			case None =>
				_value = Some(newValue)
				Empty
		}
	}
}

/**
 * Common trait for volatile (i.e. thread-safe) pointer implementations that can lazily provide an eventful interface.
 * @author Mikko Hilpinen
 * @since 22.12.2025, v2.8
 */
trait BecomesEventfulVolatile[A] extends Volatile[A] with BecomesEventfulPointer[A]
{
	// ABSTRACT ------------------------
	
	override def eventful: EventfulVolatile[A]
}