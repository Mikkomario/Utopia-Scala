package utopia.flow.generic.model.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.generic.model.mutable.Variable
import utopia.flow.generic.model.template.HasPropertiesLike.haveSimilarProperties
import utopia.flow.operator.equality.{ApproxEquals, EqualsFunction}
import utopia.flow.parse.json.JsonConvertible

import scala.collection.mutable

object HasPropertiesLike
{
	// TYPES    ----------------------------
	
	/**
	 * Type alias for generic [[HasPropertiesLike]] of type [[Property]]
	 */
	type HasProperties = HasPropertiesLike[Property]
	/**
	 * Type alias for instances that contain Constants
	 */
	type HasConstants = HasPropertiesLike[Constant]
	/**
	 * Type alias for instances that contain mutable Variables
	 */
	type ContainsVariables = HasPropertiesLike[Variable]
	
	
	// ATTRIBUTES   ------------------------
	
	/**
	 * Checks whether the two models have equal non-empty properties.
	 * The values are considered equal as long as they convert to the same value (i.e. data types may differ).
	 * If a value is defined in one model and not in the other, the models may still be considered equal.
	 * Value ordering is not considered.
	 */
	implicit val haveSimilarProperties: EqualsFunction[HasPropertiesLike[Property]] = (a, b) => {
		// Only compares defined (i.e. non-empty) properties
		a.propertiesIterator.forall { ap =>
			ap.value.isEmpty || b.existingProperty(ap.name).forall { bp => bp.value.isEmpty || (ap.value ~== bp.value) }
		}
	}
}

/**
  * Common trait for interfaces that contain (and possibly generate) named values, and store them as properties
  * @author Mikko Hilpinen
  * @since 23.10.2025, v2.7, based on ModelLike written 26.11.2016
  * @tparam P The type of the properties stored within this model
  */
trait HasPropertiesLike[+P <: Property]
	extends HasValues with JsonConvertible with ApproxEquals[HasPropertiesLike[Property]]
{
	// ABSTRACT    --------------
	
	/**
	 * @return An ordered iterator that returns the properties within this model.
	 */
	def propertiesIterator: Iterator[P]
	/**
	 * @return The properties within this model. Ordered.
	 */
	def properties: Seq[P]
	
	/**
	 * Finds an existing property from this model.
	 * No new properties will be generated.
	 * @param propName The name of the targeted property (case-insensitive)
	 * @return a property in this model with the specified name.
	 *         None if no such property exists.
	 */
	def existingProperty(propName: String): Option[P]
	/**
	 * Finds a property from this model.
	 * Generates one if necessary.
	 * @param propName The name of the targeted property (case-insensitive)
	 * @return The property from this model (possibly generated)
	 */
	def property(propName: String): P
	
	/**
	 * Called when acquiring values for properties that don't (yet) exist in this model
	 * @param propName Name of a property that doesn't exist in this model
	 * @return A value simulated for that non-existing property
	 */
	protected def simulateValueFor(propName: String): Value
	
	
	// COMP. PROPERTIES    --------
	
	/**
	 * The names of the properties stored in this model. Ordered.
	 */
	def propertyNamesIterator = propertiesIterator.map { _.name }
	/**
	  * The names of the properties stored in this model. Ordered.
	  */
	def propertyNames = propertyNamesIterator.toOptimizedSeq
	
	/**
	  * The properties within this model that have a defined (non-empty) value. Ordered.
	  */
	def nonEmptyPropertiesIterator = propertiesIterator.filter { _.nonEmpty }
	/**
	  * The properties within this model that have a defined (non-empty) value. Ordered.
	  */
	def nonEmptyProperties = nonEmptyPropertiesIterator.toOptimizedSeq
	
	/**
	  * Converts this model into a map of a specific type.
	  * The keys in the resulting map are lower-case property names
	  * and values are conversion results.
	  * @param conversion A conversion function that accepts a property value
	  * @tparam A Type of conversion result
	  * @return A map containing converted values matched to lower-case property names
	  */
	def toMap[A](implicit conversion: Value => A) =
		propertiesIterator.map { p => p.name.toLowerCase -> conversion(p.value) }.toMap
	/**
	  * Converts (unwraps) this model's properties into a map.
	  * Doesn't include cases where conversion failed (yielded None)
	  * @param conversion a function that converts a property value into the desired instance type.
	  *          Returns None if the value can't be converted.
	  * @return a map from the converted properties of this model.
	  *         The keys of the resulting map are all lower-case property names.
	  */
	def toPartialMap[A](implicit conversion: Value => Option[A]) =
		propertiesIterator.flatMap { p => conversion(p.value).map { (p.name.toLowerCase, _) } }.toMap
	
	
	// IMPLEMENTED    -------------
	
	override def isEmpty = propertiesIterator.isEmpty
	override def hasNonEmptyValues = nonEmptyPropertiesIterator.hasNext
	
	override def apply(propName: String): Value = existingProperty(propName) match {
		case Some(p) => p.value
		case None => simulateValueFor(propName)
	}
	override def apply(propNames: IterableOnce[String]): Value = {
		if (propNames.knownSize == 1)
			apply(propNames.iterator.next())
		else
			_apply(propNames.iterator)
	}
	
	override def ~==(other: HasPropertiesLike[Property]): Boolean = haveSimilarProperties(this, other)
	
	override def appendToJson(jsonBuilder: mutable.StringBuilder) = {
		if (isEmpty)
			jsonBuilder ++= "{}"
		else {
			jsonBuilder += '{'
			val propsIter = propertiesIterator
			propsIter.nextOption().foreach { _.appendToJson(jsonBuilder) }
			propsIter.foreach { prop =>
				jsonBuilder ++= ", "
				prop.appendToJson(jsonBuilder)
			}
			jsonBuilder += '}'
		}
	}
	
	
	// OTHER    -----------
	
	/**
	 * @param firstName Name of the primarily targeted property
	 * @param alternativeName Name of the alternatively targeted properties
	 * @param moreNames More alternative property names
	 * @return An existing property that matches one of the specified names.
	 *         None if no existing property matched the specified name.
	 */
	def existingProperty(firstName: String, alternativeName: String, moreNames: String*): Option[P] =
		existingProperty(Pair(firstName, alternativeName).iterator ++ moreNames)
	/**
	  * Finds an existing property from this model.
	  * No new properties will be generated.
	  * @param propNames The name of the targeted properties (ordered by priority, case-insensitive)
	  * @return The first property in this model that matched a specified name.
	  *         None if none of the specified keys matched an existing property.
	  */
	def existingProperty(propNames: IterableOnce[String]): Option[P] = propNames.findMap(existingProperty)
	
	@deprecated("Renamed to property(String)", "v2.7")
	def get(propName: String) = property(propName)
	@deprecated("Renamed to existingProperty(String)", "v2.7")
	def existing(propName: String) = existingProperty(propName)
	@deprecated("Renamed to existingProperty(String)", "v2.7")
	def existing(propNames: IterableOnce[String]) = existingProperty(propNames)
	
	private def _apply(propNamesIter: Iterator[String]): Value = {
		// Case: At least one property name is targeted
		if (propNamesIter.hasNext) {
			// Returns the value of the first targeted property by default
			val defaultTarget = propNamesIter.next()
			val defaultExisting = existingProperty(defaultTarget)
			// Looks for an existing property with a non-empty value
			// Returns the first such property
			(defaultExisting.iterator ++ propNamesIter.flatMap(existingProperty)).map { _.value }.find { _.isDefined }
				.getOrElse {
					// If no existing non-empty property was found, returns the value of the initially targeted property,
					// which is generated if it didn't exist
					defaultExisting match {
						case Some(p) => p.value
						case None => simulateValueFor(defaultTarget)
					}
				}
		}
		// Case: No properties are targeted => yields an empty value
		else
			Value.empty
	}
}
