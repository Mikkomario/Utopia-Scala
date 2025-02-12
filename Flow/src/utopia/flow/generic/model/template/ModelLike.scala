package utopia.flow.generic.model.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.template.MapAccess
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonConvertible
import utopia.flow.parse.string.Regex
import utopia.flow.util.TryExtensions._

import scala.collection.mutable
import scala.util.Try

object ModelLike
{
	// TYPES    ---------------------
	
	/**
	  * The most generic model type
	  */
	type AnyModel = ModelLike[Property]
	
	
	// ATTRIBUTES   -----------------
	
	private val nestingRegex = Regex.escape('/')
}

/**
  * Models are used for storing named values
  * @author Mikko Hilpinen
  * @since 26.11.2016
  * @tparam P The type of the properties stored within this model
  */
trait ModelLike[+P <: Property] extends MapAccess[String, Value] with JsonConvertible
{
	// ABSTRACT    --------------
	
	/**
	  * @return Properties contained within this model as a map.
	  *         The map keys are lower case property names.
	  */
	def propertyMap: Map[String, P]
	/**
	  * @return Property names (lower case) in order.
	  *         Contains the names of all of this model's properties.
	  */
	protected def propertyOrder: Seq[String]
	
	/**
	  * Generates a new property with the specified name
	  * @param attName The name given to the new property
	  * @return A new property
	  */
	protected def newProperty(attName: String): P
	/**
	  * @param propName A property name
	  * @return Whether [[newProperty]](propName) would generate a non-empty value for that property name.
	  */
	protected def generatesNonEmptyFor(propName: String): Boolean
	
	
	// COMP. PROPERTIES    --------
	
	/**
	  * @return The properties within this model. Ordered.
	  */
	def properties = {
		val allProps = propertyMap
		propertyOrder.flatMap(allProps.get)
	}
	/**
	  * @return An ordered iterator that returns the properties within this model.
	  */
	def propertiesIterator = {
		val allProps = propertyMap
		propertyOrder.iterator.flatMap(allProps.get)
	}
	
	/**
	  * The names of the properties stored in this model. Ordered.
	  */
	def propertyNames = propertiesIterator.map { _.name }.toVector
	
	/**
	  * The properties within this model that have a defined (non-empty) value. Ordered.
	  */
	def nonEmptyPropertiesIterator = propertiesIterator.filter { _.nonEmpty }
	/**
	  * The properties within this model that have a defined (non-empty) value. Ordered.
	  */
	def nonEmptyProperties = nonEmptyPropertiesIterator.toVector
	
	/**
	  * @return Whether this model contains no properties.
	  *         Returns true even when this model contains only empty properties.
	  */
	def isEmpty = propertyMap.isEmpty
	/**
	  * @return Whether this model contains at least one property.
	  *         Notice that it doesn't matter whether the properties have a value or not.
	  */
	def nonEmpty = !isEmpty
	
	/**
	  * @return Whether this model specifies any non-empty values
	  */
	def hasNonEmptyValues = propertyMap.valuesIterator.exists { _.value.isDefined }
	/**
	  * @return Whether all values in this model are empty
	  */
	def hasOnlyEmptyValues = !hasNonEmptyValues
	
	/**
	  * @return A map based on this model where the keys are lower-case property names
	  *         and the values are property values
	  */
	def toValueMap = propertyMap.view.mapValues { _.value }.toMap
	
	/**
	  * Converts this model into a map of a specific type.
	  * The keys in the resulting map are lower-case property names
	  * and values are conversion results.
	  * @param conversion A conversion function that accepts a property value
	  * @tparam A Type of conversion result
	  * @return A map containing converted values matched to lower-case property names
	  */
	def toMap[A](implicit conversion: Value => A) =
		propertyMap.view.mapValues { p => conversion(p.value) }.toMap
	/**
	  * Converts (unwraps) this model's properties into a map.
	  * Doesn't include cases where conversion failed (yielded None)
	  * @param conversion a function that converts a property value into the desired instance type.
	  *          Returns None if the value can't be converted.
	  * @return a map from the converted properties of this model.
	  *         The keys of the resulting map are all lower-case property names.
	  */
	def toPartialMap[A](implicit conversion: Value => Option[A]) =
		propertyMap.flatMap { case (name, attribute) => conversion(attribute.value).map { (name, _) } }
	
	
	// IMPLEMENTED    -------------
	
	/**
	  * Gets the value of a single property in this model.
	  * If no property with such name exists, one is generated.
	  * @param propName The name of the targeted property.
	  *                 I.e. name of the property from which the value is taken.
	  *                 Case-insensitive.
	  * @return The value of the property with the specified name
	  */
	override def apply(propName: String): Value = get(propName).value
	
	override def appendToJson(jsonBuilder: mutable.StringBuilder) = {
		if (isEmpty)
			jsonBuilder ++= "{}"
		else {
			jsonBuilder ++= "{"
			val atts = properties
			atts.head.appendToJson(jsonBuilder)
			atts.tail.foreach { att =>
				jsonBuilder ++= ", "
				att.appendToJson(jsonBuilder)
			}
			jsonBuilder ++= "}"
		}
	}
	
	
	// OTHER METHODS    -----------
	
	/**
	  * Finds a property value,
	  * potentially searching from alternative (non-empty) properties.
	  * May generate a new property for the first targeted key.
	  * @param propNames Names of the targeted properties (ordered by priority)
	  * @return The first property value which was non-empty.
	  *         If all searches resulted in empty values, a new property is generated and its value returned.
	  */
	def apply(propNames: IterableOnce[String]): Value = {
		propNames match {
			case i: Iterable[String] =>
				if (i.knownSize == 1)
					apply(i.head)
				else
					_apply(i.iterator)
			case i => _apply(i.iterator)
		}
	}
	private def _apply(propNamesIter: Iterator[String]): Value = {
		// Case: At least one property name is targeted
		if (propNamesIter.hasNext) {
			// Returns the value of the first targeted property by default
			val defaultTarget = propNamesIter.next()
			val defaultExisting = existing(defaultTarget)
			// Looks for an existing property with a non-empty value
			// Returns the first such property
			(defaultExisting ++ propNamesIter.flatMap(existing)).map { _.value }.find { _.isDefined }.getOrElse {
				// If no existing non-empty property was found, returns the value of the initially targeted property,
				// which is generated if it didn't exist
				defaultExisting.getOrElse(newProperty(defaultTarget)).value
			}
		}
		// Case: No properties are targeted => yields an empty value
		else
			Value.empty
	}
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
	  * Finds an existing property from this model.
	  * No new properties will be generated.
	  * @param propName The name of the targeted property (case-insensitive)
	  * @return a property in this model with the specified name.
	  *         None if no such property exists.
	  */
	def existing(propName: String) = propertyMap.get(propName.toLowerCase)
	/**
	  * Finds an existing property from this model.
	  * No new properties will be generated.
	  * @param propNames The name of the targeted properties (ordered by priority, case-insensitive)
	  * @return The first property in this model that matched a specified name.
	  *         None if none of the specified keys matched an existing property.
	  */
	def existing(propNames: IterableOnce[String]): Option[P] = propNames.findMap(existing)
	
	/**
	  * Finds a property from this model.
	  * Generates one if necessary.
	  * @param propName The name of the targeted property (case-insensitive)
	  * @return The property from this model (possibly generated)
	  */
	def get(propName: String) = existing(propName).getOrElse { newProperty(propName) }
	
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
	def tryGet[A](propName: String, altPropNames: String*)(f: Value => Try[A]) = {
		val value = apply(propName +: altPropNames)
		f(value).mapFailure { cause =>
			if (value.isEmpty)
				new IllegalArgumentException(s"\"$propName\" was not specified", cause)
			else
				new IllegalArgumentException(s"Value of \"$propName\" could not be accepted", cause)
		}
	}
	
	/**
	  * Whether this model contains an existing property with the specified name
	  * @param propName the name of the targeted property (case-insensitive)
	  */
	def contains(propName: String) =
		propertyMap.contains(propName.toLowerCase) || generatesNonEmptyFor(propName)
	/**
	  * @param propName Name of the targeted property (case-insensitive)
	  * @return Whether this model contains a non-empty property with the specified name
	  */
	def containsNonEmpty(propName: String) = existing(propName) match {
		case Some(p) => p.nonEmpty
		case None => generatesNonEmptyFor(propName)
	}
}
