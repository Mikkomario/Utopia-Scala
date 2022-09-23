package utopia.flow.collection.value.typeless

import utopia.flow.collection
import utopia.flow.collection.mutable.typeless.Variable
import utopia.flow.collection.template.typeless
import utopia.flow.datastructure.mutable
import utopia.flow.generic.{ModelType, PropertyGenerator, SimpleConstantGenerator, SimpleVariableGenerator, ValueConvertible}
import utopia.flow.operator.Equatable

object Model
{
    // ATTRIBUTES    -------------------
    
    /**
     * An empty model with a simple constant generator
     */
    val empty = Model.withConstants(Vector())
    
    
    // OPERATORS    --------------------
    
    /**
      * Creates a new model that uses constants and a basic attribute generator
      * @param constants The constants for the model
      * @param attributeGenerator Attribute generator to use (default = simple constant generator)
      * @return A new model
      */
    def withConstants(constants: Iterable[Constant],
                      attributeGenerator: PropertyGenerator[Constant] = SimpleConstantGenerator) =
    {
        // Filters out duplicates (case-insensitive) (if there are duplicates, last instance is used)
        val attributeMap = constants.groupBy { _.name.toLowerCase() }.map { case (name, atts) => name -> atts.last }
        val attributeOrder = constants.map { _.name.toLowerCase }.toVector.distinct
        new Model(attributeMap, attributeOrder, attributeGenerator)
    }
    
    /**
     * Creates a new model with input format that is more friendly to literals
     * @param content The attribute name value pairs used for generating the model's attributes
     * @param generator The attribute generator that will generate the attributes
     * @return The newly generated model
     */
    def apply(content: Iterable[(String, Value)], generator: PropertyGenerator[Constant]) =
            withConstants(content.map { case (name, value) => generator(name, Some(value)) }, generator)
    /**
      * Creates a new model
      * @param content A list of name-value pairs that will be used as attributes
      * @return A new model with specified attributes
      */
    def apply(content: Iterable[(String, Value)]): Model = apply(content, SimpleConstantGenerator)
    
    /**
      * @param first First name-value pair
      * @param more More name-value pairs
      * @return A new model with all specified name-value pairs as attributes
      */
    def from(first: (String, Value), more: (String, Value)*): Model = apply(first +: more)
    
    /**
     * Converts a map of valueConvertible elements into a model format. The generator the model 
     * uses can be specified as well.
     * @param content The map that is converted to model attributes
     * @param generator the attirbute generator that will generate the attributes 
     * (simple constant generator used by default)
     * @return The newly generated model
     */
    def fromMap[C1](content: Map[String, C1], generator: PropertyGenerator[Constant] = SimpleConstantGenerator)
                   (implicit f: C1 => ValueConvertible) =
            withConstants(content.map { case (name, value) => generator(name, Some(value.toValue)) }, generator)
}

/**
 * This is the immutable model implementation
 * The model will only accept constant properties
 * @author Mikko Hilpinen
 * @since 29.11.2016
 */
class Model private(val attributeMap: Map[String, Constant], protected val attributeOrder: Vector[String],
                    val attributeGenerator: PropertyGenerator[Constant])
    extends typeless.Model[Constant] with Equatable with ValueConvertible
{
    // COMP. PROPERTIES    -------
    
    /**
     * A version of this model where all empty values have been filtered out
     */
    def withoutEmptyValues = {
        val emptyKeys = attributeMap.view.filter { _._2.isEmpty }.keySet
        if (emptyKeys.isEmpty)
            this
        else
            new Model(attributeMap -- emptyKeys, attributeOrder.filterNot(emptyKeys.contains), attributeGenerator)
    }
    
    
    // IMPLEMENTED METHODS    ----
    
    override def properties = Vector(attributeMap, attributeGenerator)
    
    override def toValue = new Value(Some(this), ModelType)
    
    override def generateAttribute(attName: String) = attributeGenerator(attName, None)
    
    
    // OPERATORS    --------------
    
    /**
     * Creates a new model with the provided attribute added
     */
    def +(attribute: Constant) = withAttributes(attributes :+ attribute)
    /**
      * @param attribute A new attribute (key + value pair)
      * @return A copy of this model with that attribute
      */
    def +(attribute: (String, Value)): Model = this + attributeGenerator(attribute._1, Some(attribute._2))
    /**
     * @param attribute An attribute
     * @return A copy of this model with that attribute added (prepended)
     */
    def +:(attribute: Constant) = withAttributes(attribute +: attributes)
    /**
      * @param attribute An attribute (key + value)
      * @return A copy of this model with that attribute added (prepended)
      */
    def +:(attribute: (String, Value)): Model = attributeGenerator(attribute._1, Some(attribute._2)) +: this
    
    /**
     * Creates a new model with the provided attributes added
     */
    def ++(attributes: IterableOnce[Constant]) = withAttributes(this.attributes ++ attributes)
    /**
     * Creates a new model that contains the attributes from both of the models. The new model 
     * will still use this model's attribute generator
     */
    def ++(other: typeless.Model[Constant]): Model = this ++ other.attributes
    
    /**
     * Creates a new model without the provided attribute
     */
    def -(attribute: Constant) = withAttributes(attributes.filterNot { _ == attribute })
    /**
     * Creates a new model without an attribute with the provided name (case-insensitive)
     */
    def -(attributeName: String) = {
        val lowerName = attributeName.toLowerCase
        new Model(attributeMap - lowerName, attributeOrder.filterNot { _ == lowerName }, attributeGenerator)
    }
    
    /**
     * Creates a new model without the provided attributes
     */
    def --(attributeNames: Iterable[String]) = without(attributeNames)
    /**
     * Creates a new model without any attributes within the provided model
     */
    def --(other: typeless.Model[Constant]): Model = withoutAttributes(other.attributes.toSet)
    
    
    // OTHER METHODS    ------
    
    /**
     * Creates a new model with the same generator but different attributes
     */
    def withAttributes(attributes: Iterable[Constant]) = Model.withConstants(attributes, attributeGenerator)
    /**
      * @param attributes Attributes to remove from this model
      * @return A copy of this model with none of the specified attributes
      */
    def withoutAttributes(attributes: Set[Constant]) =
        withAttributes(this.attributes.filterNot(attributes.contains))
    /**
      * @param keys Names of the keys to remove (case-insensitive)
      * @return A copy of this model with the specified keys removed
      */
    def without(keys: Iterable[String]) = {
        val lowerKeys = keys.map { _.toLowerCase }.toSet
        new Model(attributeMap -- lowerKeys, attributeOrder.filterNot(lowerKeys.contains), attributeGenerator)
    }
    /**
      * @param firstKey Name of the first key to remove (case-insensitive)
      * @param moreKeys Names of the other keys to remove (case-insensitive)
      * @return A copy of this model with the specified keys removed
      */
    def without(firstKey: String, moreKeys: String*): Model = without(firstKey +: moreKeys)
    
    /**
     * Creates a new model with the same attributes but a different attribute generator
     */
    def withGenerator(generator: PropertyGenerator[Constant]) = new Model(attributeMap, attributeOrder, generator)
    
    /**
     * Creates a copy of this model with filtered attributes
     */
    def filter(f: Constant => Boolean) = withAttributes(attributes.filter(f))
    /**
     * Creates a copy of this model with filtered attributes. The result model only contains 
     * attributes not included by the filter
     */
    def filterNot(f: Constant => Boolean) = withAttributes(attributes.filterNot(f))
    
    /**
      * Renames multiple attiributes in this model
      * @param renames The attribute name changes (old -> new)
      * @return A copy of this model with renamed attributes
      */
    def renamed(renames: Iterable[(String, String)]) = withAttributes(attributes.map {
        a => renames.find { _._1 == a.name }.map { n => a.withName(n._2) } getOrElse a })
    /**
      * @param oldName Old attribute name
      * @param newName New attribute name
      * @return A copy of this model with that one attribute renamed
      */
    def renamed(oldName: String, newName: String): Model = renamed(Vector(oldName -> newName))
    
    /**
      * Maps the attributes within this model
      * @param f A mapping function for attributes
      * @return A mapped copy of this model
      */
    def map(f: Constant => Constant) = withAttributes(attributes.map(f))
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
        new Model(attributeMap.view.mapValues { _.mapValue(f) }.toMap, attributeOrder, attributeGenerator)
    
    /**
     * Creates a mutable copy of this model
     * @param generator The property generator used for creating the properties of the new model
     * @return A mutable copy of this model using the provided property generator
     */
    def mutableCopy[T <: Variable](generator: PropertyGenerator[T]) =
    {
        val copy = new collection.mutable.typeless.Model(generator)
        attributes.foreach { att => copy(att.name) = att.value }
        
        copy
    }
    /**
      * @return A mutable copy of this model
      */
    def mutableCopy(): collection.mutable.typeless.Model[Variable] = mutableCopy(new SimpleVariableGenerator())
}