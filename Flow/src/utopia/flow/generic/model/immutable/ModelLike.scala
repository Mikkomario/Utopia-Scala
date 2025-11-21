package utopia.flow.generic.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair, Single}
import utopia.flow.generic.factory.PropertyFactory
import utopia.flow.generic.model.mutable
import utopia.flow.generic.model.mutable.DataType.ModelType
import utopia.flow.generic.model.mutable.{MutableModel, Variable}
import utopia.flow.generic.model.template.{HasPropertiesLike, Property, ValueConvertible}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.{Mutate, UncertainBoolean}

/**
 * Common trait for immutable models which contain constant properties
 * @author Mikko Hilpinen
 * @since 23.10.2025, v2.7, from Model written 29.11.2016
 */
trait ModelLike[+Repr] extends HasPropertiesLike[Constant] with EqualsBy with ValueConvertible with MaybeEmpty[Repr]
{
	// ABSTRACT ---------------------
	
	/**
	 * @param properties New properties to assign
	 * @return A copy of this model containing the specified properties
	 */
	def withProperties(properties: IterableOnce[Constant]): Repr
	
	/**
	 * @param renames Property renamings to apply
	 * @return A copy of this model with the specified renamings applied
	 */
	def +(renames: PropertyRenames): Repr
	
	/**
	 * Tests containment without iterating or caching new properties
	 * @param propName Name of the searched property
	 * @return Whether this model contains that property.
	 *         Uncertain if the result would require further iteration or processing.
	 */
	def knownContains(propName: String): UncertainBoolean
	
	
	// COMP. PROPERTIES    ----------
	
	/**
	 * A version of this model where all empty values have been filtered out
	 */
	def withoutEmptyValues = withProperties(properties.filter {_.nonEmpty})
	
	/**
	 * @return Copy of this model where the properties appear in alphabetical order
	 */
	def sorted = sortBy { _.name }
	
	/**
	 * @return A mutable copy of this model
	 */
	def mutableCopy: MutableModel[Variable] = mutableCopyUsing(PropertyFactory.forVariables)
	
	
	// IMPLEMENTED METHODS    ----
	
	override def nonEmpty = !isEmpty
	
	override def toValue = new Value(Some(this), ModelType)
	
	protected override def equalsProperties: IterableOnce[Any] = propertiesIterator
	
	override def property(propName: String): Constant =
		existingProperty(propName).getOrElse {Constant(propName, simulateValueFor(propName))}
	
	override protected def simulateValueFor(propName: String): Value = Value.empty
	
	
	// OTHER    --------------
	
	/**
	 * Creates a new model with the provided property added
	 */
	def +(prop: Constant) = {
		if (isEmpty)
			withProperties(Single(prop))
		else if (knownContains(prop.name).isCertainlyFalse)
			withProperties(properties :+ prop)
		else
			withProperties(propertiesIterator.filterNot { _.name ~== prop.name } ++ Iterator.single(prop))
	}
	/**
	 * @param prop A new property as a key value -pair
	 * @return A copy of this model with that property added
	 */
	def +[A](prop: (String, A))(implicit convert: A => ValueConvertible): Repr =
		this + Constant(prop._1, convert(prop._2).toValue)
	/**
	 * @param prop A property
	 * @return A copy of this model with that property prepended
	 */
	def +:(prop: Constant) = {
		if (isEmpty)
			withProperties(Single(prop))
		else if (knownContains(prop.name).isCertainlyFalse)
			withProperties(prop +: properties)
		else
			withProperties(Iterator.single(prop) ++ propertiesIterator.filterNot { _.name ~== prop.name })
	}
	/**
	 * @param prop A property as a key value -pair
	 * @return A copy of this model with that property prepended
	 */
	def +:[A](prop: (String, A))(implicit convert: A => ValueConvertible): Repr =
		Constant(prop._1, convert(prop._2).toValue) +: this
	
	/**
	 * Creates a new model with the specified properties added
	 */
	def ++(props: IterableOnce[Constant]) = {
		// Case: This model is empty => Just constructs a new model from the specified properties
		if (isEmpty)
			withProperties(props)
		// Case: This model is not empty => There may be overlap between the existing and the introduced properties
		//       => Since only the latest value must be preserved, has to iterate through the new properties
		else {
			val (newProps, newPropNames) = props match {
				case v: scala.collection.View[Constant] =>
					val propsBuilder = OptimizedIndexedSeq.newBuilder[Constant]
					val namesBuilder = scala.collection.mutable.Set[String]()
					
					v.foreach { p =>
						propsBuilder += p
						namesBuilder += p.name.toLowerCase
					}
					propsBuilder.result() -> namesBuilder.result()
				
				case i: Iterable[Constant] => i -> i.iterator.map { _.name.toLowerCase }.toSet
				case i =>
					// WET WET
					val propsBuilder = OptimizedIndexedSeq.newBuilder[Constant]
					val namesBuilder = scala.collection.mutable.Set[String]()
					
					i.iterator.foreach { p =>
						propsBuilder += p
						namesBuilder += p.name.toLowerCase
					}
					propsBuilder.result() -> namesBuilder.result()
			}
			
			// Case: No new properties => No change
			if (newProps.isEmpty)
				self
			// Case: Possible overlap => Filters out duplicates
			else if (newPropNames.exists { knownContains(_).mayBeTrue })
				withProperties(propertiesIterator.filterNot { p => newPropNames.contains(p.name.toLowerCase) } ++
					newProps)
			// Case: No overlap => Simply combines the properties
			else
				withProperties(properties ++ newProps)
		}
	}
	/**
	 * Creates a new model that contains properties from both of these models.
	 * The resulting model will still use this model's property factory.
	 */
	def ++(other: HasPropertiesLike[Constant]): Repr = if (other.isEmpty) self else this ++ other.propertiesIterator
	
	/**
	 * Creates a new model without the exact specified property
	 */
	def -(prop: Property) = {
		if (knownContains(prop.name).isCertainlyFalse)
			self
		else
			withProperties(properties.filterNot { _ == prop })
	}
	/**
	 * @return A copy of this model without a property with the specified name (case-insensitive)
	 */
	def -(propName: String): Repr = {
		if (knownContains(propName).isCertainlyFalse)
			self
		else
			withProperties(properties.filterNot { _.name ~== propName })
	}
	/**
	 * @param propName Name of the property to remove
	 * @return A copy of this model without the specified property
	 */
	def without(propName: String) = this - propName
	
	/**
	 * Creates a copy of this model without any property listed in the specified collection (case-insensitive)
	 */
	def --(propNames: IterableOnce[String]) = without(propNames)
	/**
	 * @param firstKey Name of the first key to remove (case-insensitive)
	 * @param moreKeys Names of the other keys to remove (case-insensitive)
	 * @return A copy of this model with the specified keys removed
	 */
	def without(firstKey: String, secondKey: String, moreKeys: String*): Repr =
		without(Pair(firstKey, secondKey).iterator ++ moreKeys)
	/**
	 * @param keys Names of the keys to remove (case-insensitive)
	 * @return A copy of this model with the specified keys removed
	 */
	def without(keys: IterableOnce[String]) = {
		val lowerKeys = keys.iterator.map { _.toLowerCase }.toSet
		if (lowerKeys.forall { knownContains(_).isCertainlyFalse })
			self
		else
			withProperties(properties.filterNot { p => lowerKeys.contains(p.name.toLowerCase) })
	}
	
	/**
	 * Creates a new model without any properties listed in the specified model (with the exact same values)
	 */
	def --(other: HasPropertiesLike[Constant]): Repr = {
		if (other.isEmpty)
			self
		else
			withoutProperties(other.propertiesIterator.toSet)
	}
	/**
	 * @param properties Properties to remove from this model
	 * @return A copy of this model with none of the specified properties
	 */
	def withoutProperties(properties: Set[Constant]) = {
		if (properties.forall { p => knownContains(p.name).isCertainlyFalse })
			self
		else
			withProperties(this.properties.filterNot(properties.contains))
	}
	
	/**
	 * Creates a copy of this model with filtered properties
	 */
	def filter(f: Constant => Boolean) = withProperties(properties.filter(f))
	/**
	 * Creates a copy of this model with filtered properties.
	 * The result model only contains properties not included by the filter
	 */
	def filterNot(f: Constant => Boolean) = withProperties(properties.filterNot(f))
	
	/**
	 * Renames multiple properties in this model
	 * @param renames The property name changes (old -> new)
	 * @return A copy of this model with renamed properties
	 */
	def renamed(renames: IterableOnce[Pair[String]]): Repr = this + PropertyRenames(renames)
	/**
	 * @param oldName Old property name
	 * @param newName New property name
	 * @return A copy of this model with that one property renamed
	 */
	def renamed(oldName: String, newName: String): Repr = this + PropertyRenames(oldName -> newName)
	
	/**
	 * Creates a copy of this model with sorted property order
	 * @param f A mapping function that accepts a property and returns the value to sort by
	 * @param ord Implicit ordering to use
	 * @tparam A Type of sorting key
	 * @return A sorted copy of this model
	 */
	def sortBy[A](f: Constant => A)(implicit ord: Ordering[A]) = {
		val props = properties
		if (props.hasSize < 2)
			self
		else
			withProperties(props.sortBy(f))
	}
	@deprecated("Renamed to sortBy", "v2.7")
	def sortPropertiesBy[A](f: Constant => A)(implicit ord: Ordering[A]) = sortBy(f)
	
	/**
	 * Maps the properties within this model
	 * @param f A mapping function for properties
	 * @return A mapped copy of this model
	 */
	def map(f: Mutate[Constant]) = withProperties(propertiesIterator.map(f).distinctBy { _.name.toLowerCase })
	/**
	 * Maps the attribute names within this model
	 * @param f A mapping function for attribute names
	 * @return A mapped copy of this model
	 */
	def mapKeys(f: Mutate[String]) = map { _.mapName(f) }
	/**
	 * Maps attribute values within this model
	 * @param f A mapping function for attribute values
	 * @return A mapped copy of this model
	 */
	def mapValues(f: Mutate[Value]) = withProperties(properties.map { _.mapValue(f) })
	
	/**
	 * Modifies and replaces a single property in this model
	 * @param propName Name of the targeted property
	 * @param requireExisting Whether, if there didn't exist a property with name 'propName',
	 *                        this mapping should be skipped.
	 *                        Default = false = mapping will be performed using a simulated property instead.
	 * @param f A mapping function to apply
	 * @return A copy of this model with a modified version of that property.
	 */
	def map(propName: String, requireExisting: Boolean = false)(f: Mutate[Constant]) = existingProperty(propName) match {
		case Some(prop) =>
			val modified = f(prop)
			// Case: Only value was modified => Replaces the original property
			if (modified.name ~== propName)
				withProperties(propertiesIterator.filterNot { _ == prop } :+ modified)
			// Case: Name was also modified => Replaces the original, as well as a potential duplicate property
			else {
				val oldNames = Set(propName.toLowerCase, modified.name.toLowerCase)
				withProperties(propertiesIterator.filterNot { p => oldNames.contains(p.name.toLowerCase) } :+ modified)
			}
		// Case: No such property => May still add a new propert
		case None =>
			if (requireExisting)
				self
			else
				this + f(Constant(propName, simulateValueFor(propName)))
	}
	/**
	 * Modifies and replaces a single property in this model
	 * @param propName Name of the targeted property
	 * @param f A mapping function to apply
	 * @return A copy of this model with a modified version of that property.
	 *         Note: If this model didn't contain the specified property, no changes are made.
	 */
	def mapExisting(propName: String)(f: Mutate[Constant]) = map(propName, requireExisting = true)(f)
	/**
	 * Modifies the value of a single property in this model
	 * @param propName Name of the targeted property
	 * @param requireExisting Whether, if there didn't exist a property with name 'propName',
	 *                        this mapping should be skipped.
	 *                        Default = false = mapping will be performed using a simulated property value instead.
	 * @param f A mapping function applied to the targeted property's value
	 * @return A copy of this model where the specified property's value was modified.
	 */
	def mapValue(propName: String, requireExisting: Boolean = false)(f: Mutate[Value]) =
		map(propName, requireExisting) { _.mapValue(f) }
	/**
	 * Modifies the value of a single property in this model
	 * @param propName Name of the targeted property
	 * @param f A mapping function applied to the targeted property's value
	 * @return A copy of this model where the specified property's value was modified.
	 *         Note: If this model didn't contain the specified property, no changes are made.
	 */
	def mapExistingValue(propName: String)(f: Mutate[Value]) = mapValue(propName, requireExisting = true)(f)
	/**
	 * Adds a "computed property" to this model
	 * @param propName Name of the property from which a new value is computed
	 * @param newName Name of the new "computed" property
	 * @param replace Whether to replace the original property with the computed property.
	 *                Default = false = Both properties will be present in the new model
	 *                (except if they have the same name)
	 * @param requireExisting Whether, if there doesn't exist a property with name 'propName',
	 *                        no computed property should be generated either.
	 *                        Default = false = a computed property will be generated using a simulated property value.
	 * @param mapValue A function which produces the value for the computed property.
	 *                 Accepts the value of the original property (or possibly a simulated property value)
	 * @return A copy of this model with the computed property added.
	 *         Returns an unmodified copy of this model,
	 *         if 'requireExisting' was set to true and no 'propName' existed on this model.
	 */
	def addComputed(propName: String, newName: String, replace: Boolean = false, requireExisting: Boolean = false)
	               (mapValue: Mutate[Value]) =
	{
		// Case: Replacing the original property with another using the same name => Simply maps the property value
		if (newName ~== propName)
			this.mapValue(propName, requireExisting)(mapValue)
		// Case: Replacing the original property => Maps it
		else if (replace)
			map(propName, requireExisting) { p => Constant(newName, mapValue(p.value)) }
		// Case: Both properties should be kept
		else
			existingProperty(propName) match {
				// Case: Existing prop found => Generates the computed property and adds it to this model
				case Some(prop) => this + Constant(newName, mapValue(prop.value))
				// Case: No existing property => May skip the computed property, also
				case None =>
					if (requireExisting)
						self
					else
						this + Constant(newName, mapValue(simulateValueFor(propName)))
			}
	}
	
	/**
	 * Creates a mutable copy of this model
	 * @param factory The property factory used for creating the properties of the new model
	 * @return A mutable copy of this model using the specified property generator
	 */
	def mutableCopyUsing[P <: Variable](factory: PropertyFactory[P]) =
		mutable.MutableModel.withVariables(properties.map { prop => factory(prop.name, prop.value) }, factory)
}