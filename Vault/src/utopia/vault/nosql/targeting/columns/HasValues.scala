package utopia.vault.nosql.targeting.columns

import scala.language.implicitConversions

object HasValues
{
	// Implicitly accesses the wrapped values
	implicit def autoAccessValues[V](a: HasValues[V]): V = a.values
}

/**
 * Common trait for interfaces which provide (implicit) access to a set of values.
 * @author Mikko Hilpinen
 * @since 18.08.2025, v2.0
 */
trait HasValues[+Values]
{
	/**
	 * @return Access to values of the accessible items
	 */
	def values: Values
}
