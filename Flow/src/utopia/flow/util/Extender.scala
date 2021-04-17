package utopia.flow.util

import scala.language.implicitConversions

object Extender
{
	/**
	 * Allows implicit access to the wrapped item
	 * @param extender A wrapper
	 * @tparam A Type of wrapped item
	 * @return Item wrapped by the extender / wrapper
	 */
	implicit def autoAccess[A](extender: Extender[A]): A = extender.wrapped
}

/**
 * Common trait for wrapper classes which allow implicit access to the wrapped item
 * @author Mikko Hilpinen
 * @since 5.4.2021, v1.9
 */
trait Extender[+A]
{
	/**
	 * @return Item wrapped by this extender
	 */
	def wrapped: A
}
