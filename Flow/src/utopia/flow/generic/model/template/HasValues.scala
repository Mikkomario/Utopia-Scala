package utopia.flow.generic.model.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.template.MapAccess
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.result.TryExtensions._

import scala.util.{Failure, Try}

/**
  * Common trait for interfaces that contain (and possibly generate) named values
  * @author Mikko Hilpinen
  * @since 23.10.2025, v2.7, based on ModelLike written 26.11.2016
  */
trait HasValues extends MapAccess[String, Value]
{
	// ABSTRACT    --------------
	
	/**
	 * @return Whether this model contains no properties.
	 *         Returns true even when this model contains only empty properties.
	 */
	def isEmpty: Boolean
	/**
	 * @return Whether this model specifies any non-empty values
	 */
	def hasNonEmptyValues: Boolean
	
	/**
	 * Whether this model contains an existing property with the specified name
	 * @param propName the name of the targeted property (case-insensitive)
	 */
	def contains(propName: String): Boolean
	/**
	 * @param propName Name of the targeted property (case-insensitive)
	 * @return Whether this model contains a non-empty property with the specified name
	 */
	def containsNonEmpty(propName: String): Boolean
	
	/**
	 * Finds a property value,
	 * potentially searching from alternative (non-empty) properties.
	 * May generate a new property for the first targeted key.
	 * @param propNames Names of the targeted properties (ordered by priority)
	 * @return The first property value which was non-empty.
	 *         If all searches resulted in empty values, a new property is generated and its value returned.
	 */
	def apply(propNames: IterableOnce[String]): Value
	
	
	// COMP. PROPERTIES    --------
	
	/**
	  * @return Whether this model contains at least one property.
	  *         Notice that it doesn't matter whether the properties have a value or not.
	  */
	def nonEmpty = !isEmpty
	/**
	  * @return Whether all values in this model are empty
	  */
	def hasOnlyEmptyValues = !hasNonEmptyValues
	
	
	// OTHER    -----------
	
	/**
	  * Finds a property value,
	  * potentially searching from alternative (non-empty) properties.
	  * May generate a new property for the first targeted key.
	  * @param attName          Name of the primary target attribute
	  * @param secondaryAttName Name of the secondary target attribute
	  * @param moreAttNames     Name of the additional backup attributes
	  * @return The first property value which was non-empty.
	  *         If all searches resulted in empty values, a new property is generated and its value returned.
	  */
	def apply(attName: String, secondaryAttName: String, moreAttNames: String*): Value =
		apply(Pair(attName, secondaryAttName) ++ moreAttNames)
	
	/**
	 * @param propName Name of the targeted property
	 * @return Value of the targeted property, but only if it is non-empty.
	 */
	def nonEmpty(propName: String) = apply(propName).notEmpty
	/**
	 * @param propNames Names of the targeted properties
	 * @return Value of the first targeted property that contains a non-empty value.
	 *         None if no such property was found.
	 */
	def nonEmpty(propNames: IterableOnce[String]): Option[Value] = propNames.findMap(nonEmpty)
	/**
	 * @param propName Name of the primary targeted property
	 * @param altName Name of the secondary targeted property
	 * @param moreNames Names of alternative properties
	 * @return Value of the first targeted property that contains a non-empty value.
	 *              None if no such property was found.
	 */
	def nonEmpty(propName: String, altName: String, moreNames: String*): Option[Value] =
		nonEmpty(Pair(propName, altName) ++ moreNames)
	
	/**
	  * Attempts to retrieve a value from this model.
	  * In case of a failure, provides an error message which indicates, which property was being accessed.
	  * @param propName Name of the accessed property
	  * @param altPropNames Alternative names for the accessed property
	  * @param f A function which converts the value to the desired type,
	  *          yielding a failure if the value could not be parsed.
	  * @tparam A type of expected parse results
	  * @return Failure if 'f' yielded a failure. Otherwise, returns the parsed value.
	  */
	def tryGet[A](propName: String, altPropNames: String*)(f: Value => Try[A]) =
		_tryGet(apply(propName +: altPropNames), propName)(f)
	/**
	 * Attempts to retrieve a value from this model.
	 * In case of a failure, provides an error message which indicates, which property was being accessed.
	 * @param propNames Names of the targeted properties, in order of descending priority
	 * @param f A function which converts the value to the desired type,
	 *          yielding a failure if the value could not be parsed.
	 * @tparam A type of expected parse results
	 * @return Failure if 'f' yielded a failure. Otherwise, returns the parsed value.
	 */
	def tryGet[A](propNames: Iterable[String])(f: Value => Try[A]) = {
		if (propNames.isEmpty)
			Failure(new IllegalArgumentException("No property was targeted"))
		else
			_tryGet(apply(propNames), propNames.head)(f)
	}
	private def _tryGet[A](value: Value, propName: => String)(f: Value => Try[A]) =
		f(value).mapFailure { cause =>
			if (value.isEmpty)
				new IllegalArgumentException(s"\"$propName\" was not specified", cause)
			else
				new IllegalArgumentException(s"Value of \"$propName\" could not be accepted", cause)
		}
}
