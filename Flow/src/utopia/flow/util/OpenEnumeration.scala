package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
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
class OpenEnumeration[V <: OpenEnumerationValue[A], A](initialValues: Vector[V] = Vector.empty,
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
}
