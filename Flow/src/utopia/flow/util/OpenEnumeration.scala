package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.operator.equality.EqualsFunction

/**
 * A base implementation for open enumeration access points.
 * Intended to be extended by the enumeration companion objects.
 * @param initialValues Values immediately present in this enumeration
 * @param identifiersMatch An equality function implementation used when matching enumeration identifiers
 * @tparam V Type of enumeration values
 * @tparam A Type of identifiers used to match to the enumeration values
 * @author Mikko Hilpinen
 * @since 23.04.2024, v2.4
 */
class OpenEnumeration[V <: OpenEnumerationValue[A], A](initialValues: Seq[V] = Empty,
                                                       identifiersMatch: EqualsFunction[A] = EqualsFunction.default)
{
	// ATTRIBUTES   --------------------------
	
	private var _values = initialValues
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return All registered enumeration values
	 */
	def values = _values
	
	
	// OTHER    ------------------------------
	
	/**
	  * Introduces a new value to this enumeration
	  * @param value A new value to introduce
	  */
	def introduce(value: V): Unit = introduce(Single(value))
	def introduce(first: V, second: V, more: V*): Unit = introduce(Pair(first, second) ++ more)
	/**
	 * Introduces new values to this enumeration
	 * @param values Values to introduce
	 */
	def introduce(values: IterableOnce[V]) = _values = _values.appendAllIfDistinct(values)
	
	/**
	 * @param identifier An enumeration identifier
	 * @return A value which matches the specified identifier.
	 *         None if no matching value was found.
	 */
	def findFor(identifier: A) = values.find { v => identifiersMatch(identifier, v.identifier) }
	/**
	  * @param identifier An enumeration identifier
	  * @return A value which matches the specified identifier.
	  *         Failure if no match was found.
	  */
	def tryFindFor(identifier: A) =
		findFor(identifier)
			.toTry { new NoSuchElementException(s"No ${ getClass.getSimpleName } matches \"$identifier\"") }
}
