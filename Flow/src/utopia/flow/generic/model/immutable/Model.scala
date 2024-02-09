package utopia.flow.generic.model.immutable

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.factory.PropertyFactory
import utopia.flow.generic.model.mutable
import utopia.flow.generic.model.mutable.DataType.ModelType
import utopia.flow.generic.model.mutable.{MutableModel, Variable}
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.generic.model.template.{ModelLike, Property, ValueConvertible}
import utopia.flow.operator.equality.{ApproxSelfEquals, EqualsBy, EqualsFunction}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsExtensions._

object Model
{
    // ATTRIBUTES    -------------------
    
    /**
     * An empty model with a basic constant generator
     */
    val empty = new Model(Map(), Vector(), PropertyFactory.forConstants)
    
    /**
      * Checks whether the two models have equal non-empty properties.
      * The values are considered equal as long as they convert to the same value (i.e. data types may differ).
      * Value ordering is not considered.
      */
    implicit val similarProperties: EqualsFunction[Model] = (a, b) => {
        // Only compares defined (i.e. non-empty) properties
        val props = Pair(a, b).map { _.propertyMap.filter { _._2.nonEmpty } }
        if (props.isSymmetricBy { _.keySet })
            props.first.keys.forall { n =>
                props.map { _(n).value }.isSymmetricWith(Value.convertToEqual)
            }
        else
            false
    }
    
    
    // OTHER    --------------------
    
    /**
      * Creates a new model that contains the specified constants
      * @param constants Model properties
      * @param propFactory Property factory to use when generating new properties (default = basic constant generator)
      * @return A new model containing the specified properties
      */
    def withConstants(constants: Iterable[Constant],
                      propFactory: PropertyFactory[Constant] = PropertyFactory.forConstants) =
    {
        // Filters out duplicates (case-insensitive) (if there are duplicates, last instance is used)
        val attributeMap = constants.groupBy { _.name.toLowerCase }.map { case (name, props) => name -> props.last }
        val attributeOrder = constants.map { _.name.toLowerCase }.toVector.distinct
        new Model(attributeMap, attributeOrder, propFactory)
    }
    
    /**
     * Creates a new model from key value pairs
     * @param content The name value -pairs used for generating the model's properties
     * @param propFactory The property factory that is used to build the properties in this model
     * @return A new model
     */
    def apply(content: Iterable[(String, Value)], propFactory: PropertyFactory[Constant]) =
            withConstants(content.map { case (name, value) => propFactory(name, value) }, propFactory)
    /**
      * Creates a new model from key value pairs
      * @param content The name value -pairs used for generating the model's properties
      * @return A new model
      */
    def apply(content: Iterable[(String, Value)]): Model = apply(content, PropertyFactory.forConstants)
    
    /**
      * Converts a model into an immutable format
      * @param model A model
      * @return An immutable version of that model (specified model if already immutable)
      */
    def from(model: AnyModel): Model = model match {
        case m: Model => m
        case m: MutableModel[_] => m.immutableCopy
        case m =>
            withConstants(m.properties.map {
                case c: Constant => c
                case p => Constant(p.name, p.value)
            })
    }
    /**
      * @param first First name-value pair
      * @param second Second name-value pair
      * @param more More name-value pairs
      * @return A new model with all specified name-value pairs as attributes
      */
    def from(first: (String, Value), second: (String, Value), more: (String, Value)*): Model =
        apply(Vector(first, second) ++ more)
    /**
      * @param pair A name value -pair
      * @return A new model that contains the specified name-value pair
      */
    def from(pair: (String, Value)): Model = apply(Vector(pair))
    /**
      * @param pair A name-values -pair
      * @param convert Implicit value conversion for the specified value
      * @tparam A Type of the specified value
      * @return A model that contains the specified name value pair
      */
    def from[A](pair: (String, A))(implicit convert: A => Value): Model = apply(Vector(pair._1 -> convert(pair._2)))
    
    /**
     * Converts a map of valueConvertible elements into a model format. The generator the model 
     * uses can be specified as well.
     * @param content The map that is converted to model attributes
     * @param propFactory the attirbute generator that will generate the attributes
     * (simple constant generator used by default)
     * @return The newly generated model
     */
    def fromMap[C1](content: Map[String, C1], propFactory: PropertyFactory[Constant] = PropertyFactory.forConstants)
                   (implicit f: C1 => ValueConvertible) =
            withConstants(content.map { case (name, value) => propFactory(name, value.toValue) }, propFactory)
}

/**
 * This is the immutable model implementation
 * The model will only accept constant properties
 * @author Mikko Hilpinen
 * @since 29.11.2016
 */
class Model private(override val propertyMap: Map[String, Constant],
                    override protected val propertyOrder: Vector[String],
                    propFactory: PropertyFactory[Constant])
    extends ModelLike[Constant] with EqualsBy with ValueConvertible with MaybeEmpty[Model] with ApproxSelfEquals[Model]
{
    // COMP. PROPERTIES    -------
    
    /**
     * A version of this model where all empty values have been filtered out
     */
    def withoutEmptyValues = {
        val emptyKeys = propertyMap.view.filter { _._2.isEmpty }.keySet
        if (emptyKeys.isEmpty)
            this
        else
            new Model(propertyMap -- emptyKeys, propertyOrder.filterNot(emptyKeys.contains), propFactory)
    }
    
    /**
      * @return Copy of this model where the properties appear in alphabetical order
      */
    def sorted = new Model(propertyMap, propertyOrder.sorted, propFactory)
    
    /**
      * @return A mutable copy of this model
      */
    def mutableCopy: mutable.MutableModel[Variable] = mutableCopyUsing(PropertyFactory.forVariables)
    
    
    // IMPLEMENTED METHODS    ----
    
    override def self: Model = this
    
    override def nonEmpty = !isEmpty
    
    protected override def equalsProperties: Seq[Any] = Vector(propertyMap)
    override implicit def equalsFunction: EqualsFunction[Model] = Model.similarProperties
    
    override def toValue = new Value(Some(this), ModelType)
    
    override def newProperty(attName: String) = propFactory(attName)
    
    
    // OPERATORS    --------------
    
    /**
     * Creates a new model with the provided property added
     */
    def +(prop: Constant) = withProperties(properties :+ prop)
    /**
      * @param prop A new property as a key value -pair
      * @return A copy of this model with that property added
      */
    def +(prop: (String, Value)): Model = this + propFactory(prop._1, prop._2)
    /**
     * @param prop A property
     * @return A copy of this model with that property prepended
     */
    def +:(prop: Constant) = withProperties(prop +: properties)
    /**
      * @param prop A property as a key value -pair
      * @return A copy of this model with that property prepended
      */
    def +:(prop: (String, Value)): Model = propFactory(prop._1, prop._2) +: this
    
    /**
     * Creates a new model with the specified properties added
     */
    def ++(props: IterableOnce[Constant]) = withProperties(this.properties ++ props)
    /**
     * Creates a new model that contains properties from both of these models.
      * The resulting model will still use this model's property factory.
     */
    def ++(other: ModelLike[Constant]): Model = {
        if (other.isEmpty)
            this
        else
            this ++ other.properties
    }
    
    /**
     * Creates a new model without the exact specified property
     */
    def -(prop: Property) = withProperties(properties.filterNot { _ == prop })
    /**
     * @return A copy of this model without a property with the specified name (case-insensitive)
     */
    def -(propName: String) = {
        val lowerName = propName.toLowerCase
        if (propertyMap.contains(lowerName))
            new Model(propertyMap - lowerName, propertyOrder.filterNot { _ == lowerName }, propFactory)
        else
            this
    }
    
    /**
     * Creates a copy of this model without any property listed in the specified collection (case-insensitive)
     */
    def --(propNames: IterableOnce[String]) = without(propNames)
    /**
     * Creates a new model without any properties listed in the specified model (with the exact same values)
     */
    def --(other: ModelLike[Constant]): Model = {
        if (other.isEmpty)
            this
        else
            withoutProperties(other.properties.toSet)
    }
    
    
    // OTHER METHODS    ------
    
    /**
     * Creates a copy of this model with the same property factory,
      * but different properties
     */
    def withProperties(props: Iterable[Constant]) = Model.withConstants(props, propFactory)
    /**
      * @param attributes Attributes to remove from this model
      * @return A copy of this model with none of the specified attributes
      */
    def withoutProperties(attributes: Set[Constant]) =
        withProperties(this.properties.filterNot(attributes.contains))
    /**
      * @param keys Names of the keys to remove (case-insensitive)
      * @return A copy of this model with the specified keys removed
      */
    def without(keys: IterableOnce[String]) = {
        val lowerKeys = keys.iterator.map { _.toLowerCase }.toSet
        new Model(propertyMap -- lowerKeys, propertyOrder.filterNot(lowerKeys.contains), propFactory)
    }
    /**
      * @param firstKey Name of the first key to remove (case-insensitive)
      * @param moreKeys Names of the other keys to remove (case-insensitive)
      * @return A copy of this model with the specified keys removed
      */
    def without(firstKey: String, moreKeys: String*): Model = without(firstKey +: moreKeys)
    
    /**
      * @param propFactory A new property factory to use
     * @return A copy of this model with the specified property factory being used
     */
    def withFactory(propFactory: PropertyFactory[Constant]) = new Model(propertyMap, propertyOrder, propFactory)
    
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
    def renamed(renames: Iterable[(String, String)]) = {
        val renamedProps = properties.map { prop =>
            renames.find { _._1 ~== prop.name } match {
                case Some((_, newName)) => prop.withName(newName)
                case None => prop
            }
        }
        withProperties(renamedProps)
    }
    /**
      * @param oldName Old property name
      * @param newName New property name
      * @return A copy of this model with that one property renamed
      */
    def renamed(oldName: String, newName: String): Model = renamed(Vector(oldName -> newName))
    
    /**
      * Creates a copy of this model with sorted property order
      * @param f A mapping function that accepts a property and returns the value to sort by
      * @param ord Implicit ordering to use
      * @tparam A Type of sorting key
      * @return A sorted copy of this model
      */
    def sortPropertiesBy[A](f: Constant => A)(implicit ord: Ordering[A]) =
        new Model(propertyMap, propertyOrder.sortBy { propName => f(propertyMap(propName.toLowerCase)) }, propFactory)
    
    /**
      * Maps the properties within this model
      * @param f A mapping function for properties
      * @return A mapped copy of this model
      */
    def map(f: Constant => Constant) = withProperties(properties.map(f))
    /**
      * Maps the attribute names within this model
      * @param f A mapping function for attribute names
      * @return A mapped copy of this model
      */
    def mapKeys(f: String => String) = map { _.mapName(f) }
    /**
      * Maps attribute values within this model
      * @param f A mapping function for attribute values
      * @return A mapped copy of this model
      */
    def mapValues(f: Value => Value) =
        new Model(propertyMap.view.mapValues { _.mapValue(f) }.toMap, propertyOrder, propFactory)
    
    /**
     * Creates a mutable copy of this model
     * @param factory The property factory used for creating the properties of the new model
     * @return A mutable copy of this model using the specified property generator
     */
    def mutableCopyUsing[P <: Variable](factory: PropertyFactory[P]) =
        mutable.MutableModel.withVariables(properties.map { prop => factory(prop.name, prop.value) }, factory)
}