package utopia.vault.database.value

import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.database.Connection
import utopia.vault.database.value.LazyDbValue.MappedLazyDbValue

object LazyDbValue
{
	// OTHER    ------------------------
	
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
	
	
	// NESTED   -------------------------
	
	private class MappedLazyDbValue[A, B](original: LazyDbValue[A], f: A => B) extends LazyDbValue[B]
	{
		// ATTRIBUTES   -----------------
		
		private var _current: Option[B] = original.current.map(f)
		
		
		// IMPLEMENTED  -----------------
		
		override def value: B = _current.getOrElse {
			val value = f(original.value)
			_current = Some(value)
			value
		}
		override def current: Option[B] = _current
		
		override def connectedValue(implicit connection: Connection): B = _current.getOrElse {
			val value = f(original.connectedValue)
			_current = Some(value)
			value
		}
	}
}

/**
 * Common trait for values that may be initialized lazily through DB interaction
 * @author Mikko Hilpinen
 * @since 17.11.2025, v2.1
 */
trait LazyDbValue[+A] extends Lazy[A]
{
	// ABSTRACT ------------------------
	
	/**
	 * @param connection Implicit DB connection to utilize, if necessary
	 * @return Wrapped value
	 */
	def connectedValue(implicit connection: Connection): A
	
	
	// IMPLEMENTED  -------------------
	
	override def map[B](f: A => B): LazyDbValue[B] = current match {
		case Some(value) => LazyDbValue.initialized(f(value))
		case None => new MappedLazyDbValue[A, B](this, f)
	}
	
	override def lightMap[B](f: A => B) = map(f)
	override def mapValue[B](f: A => B) = map(f)
}
