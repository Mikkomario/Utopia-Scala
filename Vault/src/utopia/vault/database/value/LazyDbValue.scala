package utopia.vault.database.value

import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.database.Connection

object LazyDbValue
{
	/**
	 * @return Access to look-up value constructors
	 */
	def lookUp = LazyLookUpDbValue
	
	/**
	 * @param value A preinitialized value
	 * @tparam A Type of the specified value
	 * @return A preinitialized DB value
	 */
	def initialized[A](value: A): LazyDbValue[A] = InitializedDbValue(value)
	
	/**
	 * @param lazyValue A lazy container that will yield a lazy DB value
	 * @tparam A Type of the wrapped value's value
	 * @return A lazily initialized wrapper, based on the specified lazy container
	 */
	def wrap[A](lazyValue: Lazy[LazyDbValue[A]]): LazyDbValue[A] = lazyValue.current match {
		case Some(value) => value
		case None => LazyDbValueWrapper(lazyValue)
	}
	/**
	 * @param getValue A function that yields a lazy DB value
	 * @tparam A Type of the value's value
	 * @return A lazily initialized value wrapper
	 */
	def wrapLazily[A](getValue: => LazyDbValue[A]): LazyDbValue[A] = LazyDbValueWrapper(getValue)
}

/**
 * Common trait for values that may be initialized lazily through DB interaction
 * @author Mikko Hilpinen
 * @since 17.11.2025, v2.0.1
 */
trait LazyDbValue[+A] extends Lazy[A]
{
	/**
	 * @param connection Implicit DB connection to utilize, if necessary
	 * @return Wrapped value
	 */
	def connectedValue(implicit connection: Connection): A
}
