package utopia.flow.view.mutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.model.AfterEffect
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

object IndirectPointer
{
	// OTHER    -------------------------
	
	/**
	  * Creates a new indirect pointer
	  * @param view The wrapped view pointer
	  * @param mutate A function that (indirectly) mutates the value of the viewed pointer
	  * @tparam A Type of viewed and accepted values
	  * @return A new indirect pointer
	  */
	// TODO: Possibly add support for queued change events. The current version fires them immediately.
	def apply[A](view: Changing[A])(mutate: A => Unit): IndirectPointer[A] =
		new _IndirectPointer[A](view, mutate, v => {
			mutate(v)
			Empty
		})
	
	/**
	  * Creates an indirect pointer that provides mutability to a map-result pointer
	  * @param origin The original pointer (mutable)
	  * @param mapped The map result pointer (immutable)
	  * @param reverseMap A function that reverses the effects of the original mapping
	  * @tparam A Type of original (non-mapped) values
	  * @tparam B Type of mapped/viewed values
	  * @return A new indirect pointer
	  */
	def reverseMapped[A, B](origin: Pointer[A], mapped: Changing[B])(reverseMap: B => A) =
		origin match {
			case o: EventfulPointer[A] =>
				new _IndirectPointer[B](mapped, a => o.value = reverseMap(a), a => o.setAndQueueEvent(reverseMap(a)))
			case o => apply(mapped) { a => o.value = reverseMap(a) }
		}
	
	
	// NESTED   -------------------------
	
	private class _IndirectPointer[A](override val wrapped: Changing[A], set: A => Unit,
	                                  setAndQueue: A => IterableOnce[AfterEffect])
		extends IndirectPointer[A]
	{
		override implicit def listenerLogger: Logger = wrapped.listenerLogger
		
		override def value_=(newValue: A): Unit = set(newValue)
		override def setAndQueueEvent(newValue: A): IterableOnce[AfterEffect] = setAndQueue(newValue)
		
		override def toString = s"$wrapped.indirect"
	}
}

/**
  * A pointer that reflects the value of another pointer, allowing indirect mutations.
  * This may be suitable, for example, when one wants to provide a mutable interface to a mapping result pointer
  * or another reflective pointer.
  * @author Mikko Hilpinen
  * @since 12.4.2023, v2.1
  */
trait IndirectPointer[A] extends EventfulPointer[A] with ChangingWrapper[A]