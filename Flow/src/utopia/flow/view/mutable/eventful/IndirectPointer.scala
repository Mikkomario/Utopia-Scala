package utopia.flow.view.mutable.eventful

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
	def apply[A](view: Changing[A])(mutate: A => Unit): IndirectPointer[A] = new _IndirectPointer[A](view, mutate)
	
	/**
	  * Creates a n indirect pointer that provides mutability to a map-result pointer
	  * @param origin The original pointer (mutable)
	  * @param mapped The map result pointer (immutable)
	  * @param reverseMap A function that reverses the effects of the original mapping
	  * @tparam A Type of original (non-mapped) values
	  * @tparam B Type of mapped/viewed values
	  * @return A new indirect pointer
	  */
	def reverseMapped[A, B](origin: Pointer[A], mapped: Changing[B])(reverseMap: B => A) =
		apply(mapped) { a => origin.value = reverseMap(a) }
	
	
	// NESTED   -------------------------
	
	private class _IndirectPointer[A](override val wrapped: Changing[A], set: A => Unit) extends IndirectPointer[A]
	{
		override def value_=(newValue: A): Unit = set(newValue)
	}
}

/**
  * A pointer that reflects the value of another pointer, allowing indirect mutations.
  * This may be suitable, for example, when one wants to provide a mutable interface to a mapping result pointer
  * or another reflective pointer.
  * @author Mikko Hilpinen
  * @since 12.4.2023, v2.1
  */
trait IndirectPointer[A] extends Pointer[A] with ChangingWrapper[A]
