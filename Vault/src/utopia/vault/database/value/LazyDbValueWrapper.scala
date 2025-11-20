package utopia.vault.database.value

import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.database.Connection

object LazyDbValueWrapper
{
	// OTHER    --------------------------
	
	/**
	 * @param getValue A function that will yield the DB value to wrap
	 * @tparam A Type of the wrapped value's value
	 * @return A lazily initialized value wrapper
	 */
	def apply[A](getValue: => LazyDbValue[A]): LazyDbValueWrapper[A] = apply(Lazy(getValue))
	/**
	 * @param lazyValue A lazily initialized DB value to wrap
	 * @tparam A Type of the wrapped value's value
	 * @return A lazily initialized value wrapper
	 */
	def apply[A](lazyValue: Lazy[LazyDbValue[A]]): LazyDbValueWrapper[A] = new _LazyDbValueWrapper[A](lazyValue)
	
	
	// NESTED   --------------------------
	
	private class _LazyDbValueWrapper[+A](override val lazyWrapped: Lazy[LazyDbValue[A]]) extends LazyDbValueWrapper[A]
}

/**
 * Common trait for implementations of [[LazyDbValue]], which are based on lazily wrapping another such instance.
 * @author Mikko Hilpinen
 * @since 17.11.2025, v2.1
 */
trait LazyDbValueWrapper[+A] extends LazyDbValue[A]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return A lazy container that will yield the value to wrap
	 */
	protected def lazyWrapped: Lazy[LazyDbValue[A]]
	
	
	// IMPLEMENTED  --------------------
	
	override def value: A = lazyWrapped.value.value
	override def connectedValue(implicit connection: Connection): A = lazyWrapped.value.connectedValue
	
	override def current: Option[A] = lazyWrapped.current.flatMap { _.current }
}
