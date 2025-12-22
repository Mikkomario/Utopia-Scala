package utopia.flow.view.mutable.eventful

import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.eventful.{Changing, Flag}

/**
 * A combination of [[Lazy]] and [[Changing]], which initializes its value lazily, firing a change event in the process.
 * @author Mikko Hilpinen
 * @since 21.12.2025, v2.8
 */
trait GeneratesLike[+A, +Repr[X] <: Changing[X] with Lazy[X]] extends Changing[A] with Lazy[A]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @return A flag that contains true while this pointer contains a generated value.
	 */
	def nonEmptyFlag: Flag
	/**
	 * @return A flag that contains true while this pointer has not yet been initialized
	 */
	def emptyFlag: Flag
	
	/**
	 * @return A pointer that contains None while this pointer is not initialized.
	 *         Once this pointer is initialized, contains the same value.
	 */
	def currentPointer: Changing[Option[A]]
	
	/**
	 * @param f A mapping function to apply
	 * @tparam B Type of the mapping results
	 * @return A changing item that:
	 *              - Initializes lazily, like this pointer
	 *              - Contains a mapped result
	 *              - Generates change events when/if this pointer changes
	 *              - If applicable, resets when this pointer is reset
	 */
	protected def _map[B](f: A => B): Repr[B]
	
	
	// IMPLEMENTED  -------------------------
	
	override def map[B](f: A => B): Repr[B] = _map(f)
	override def lightMap[B](f: A => B): Repr[B] = map(f)
}
