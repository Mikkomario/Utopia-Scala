package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template
import utopia.flow.util.Equatable
import utopia.flow.generic.SimpleConstantGenerator
import utopia.flow.generic.PropertyGenerator
import utopia.flow.datastructure.mutable
import utopia.flow.datastructure.mutable.Variable
import utopia.flow.generic.SimpleVariableGenerator
import utopia.flow.generic.ValueConvertible
import utopia.flow.generic.ModelType

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
      * @return A new model
      */
    def withConstants(constants: Iterable[Constant]) = new Model(constants, new SimpleConstantGenerator())
    
    /**
     * Creates a new model with input format that is more friendly to literals
     * @param content The attribute name value pairs used for generating the model's attributes
     * @param generator The attribute generator that will generate the attributes
     * @return The newly generated model
     */
    def apply[Attribute <: Constant](content: Iterable[(String, Value)], generator: PropertyGenerator[Attribute]) =
            new Model(content.map { case (name, value) => generator(name, Some(value)) }, generator)
    
    /**
      * Creates a new model
      * @param content A list of name-value pairs that will be used as attributes
      * @return A new model with specified attributes
      */
    def apply(content: Iterable[(String, Value)]): Model[Constant] = apply(content, new SimpleConstantGenerator())
    
    /**
      * @param attName Attribute name
      * @param value Attribute value
      * @return Creates a new model with only single attribute
      */
    def apply(attName: String, value: Value): Model[Constant] = apply(Vector(attName -> value))
    
    /**
      * @param attName Attribute name
      * @param value Attribute value (will be converted)
      * @return Creates a new model with only single attribute
      */
    def apply(attName: String, value: ValueConvertible): Model[Constant] = apply(attName, value.toValue)
    
    /**
      * @param first First name-value pair
      * @param second Second name-value pair
      * @param more More name-value pairs
      * @return A new model with all specified name-value pairs as attributes
      */
    def apply(first: (String, Value), second: (String, Value), more: (String, Value)*): Model[Constant] = apply(Vector(first, second) ++ more)
    
    /**
     * Converts a map of valueConvertible elements into a model format. The generator the model 
     * uses can be specified as well.
     * @param content The map that is converted to model attributes
     * @param generator the attirbute generator that will generate the attributes 
     * (simple constant generator used by default)
     * @return The newly generated model
     */
    def fromMap[Attribute <: Constant, C1](content: Map[String, C1], 
            generator: PropertyGenerator[Attribute])(implicit f: C1 => ValueConvertible) =
            new Model(content.map { case (name, value) => generator(name, Some(value.toValue)) }, generator)
    
    /**
      * Converts a map of valueConvertible elements into a model format.
      * @param content The map that is converted to model attributes
      * @return The newly generated model
      */
    def fromMap[C1](content: Map[String, C1])(implicit f: C1 => ValueConvertible): Model[Constant] =
        fromMap(content, new SimpleConstantGenerator())
}

/**
 * This is the immutable model implementation
 * The model will only accept constant properties
 * @author Mikko Hilpinen
 * @since 29.11.2016
 */
class Model[+Attribute <: Constant](content: Iterable[Attribute], val attributeGenerator: PropertyGenerator[Attribute])
    extends template.Model[Attribute] with Equatable with ValueConvertible
{
    // ATTRIBUTES    --------------
    
    // Filters out duplicates (case-insensitive) (if there are duplicates, last instance is used)
    val attributeMap = content.groupBy { _.name.toLowerCase() }.map { case (name, atts) => name -> atts.last }
    
    protected val attributeOrder = content.map { _.name.toLowerCase }.toVector.distinct
    
    
    // COMP. PROPERTIES    -------
    
    override def properties = Vector(attributeMap, attributeGenerator)
    
    override def toValue = new Value(Some(this), ModelType)
    
    /**
     * A version of this model where all empty values have been filtered out
     */
    def withoutEmptyValues = new Model(attributesWithValue, attributeGenerator)
    
    
    // IMPLEMENTED METHODS    ----
    
    override def generateAttribute(attName: String) = attributeGenerator(attName, None)
    
    
    // OPERATORS    --------------
    
    /**
     * Creates a new model with the provided attribute added
     */
    def +[B >: Attribute <: Constant](attribute: B) = withAttributes(attributes :+ attribute)
    
    /**
     * Creates a new model with the provided attributes added
     */
    def ++[B >: Attribute <: Constant](attributes: IterableOnce[B]) = withAttributes(this.attributes ++ attributes)
    
    /**
     * Creates a new model that contains the attributes from both of the models. The new model 
     * will still use this model's attribute generator
     */
    def ++[B >: Attribute <: Constant](other: template.Model[B]): Model[B] = this ++ other.attributes
    
    /**
     * Creates a new model without the provided attribute
     */
    def -[B >: Attribute <: Constant](attribute: B) = new Model(attributes.filterNot { _ == attribute }, attributeGenerator)
    
    /**
     * Creates a new model without an attribute with the provided name (case-insensitive)
     */
    def -(attributeName: String) = new Model(attributes.filterNot { 
            _.name.toLowerCase == attributeName.toLowerCase }, attributeGenerator)
    
    /**
     * Creates a new model without the provided attributes
     */
    def --[B >: Attribute <: Constant](attributes: Seq[B]): Model[Attribute] = new Model(
            this.attributes.filterNot { attributes.contains(_) }, attributeGenerator)
    
    /**
     * Creates a new model without any attributes within the provided model
     */
    def --[B >: Attribute <: Constant](other: template.Model[B]): Model[B] = this -- other.attributes
    
    
    // OTHER METHODS    ------
    
    /**
     * Creates a new model with the same generator but different attributes
     */
    def withAttributes[B >: Attribute <: Constant](attributes: Iterable[B]) =
            new Model[B](attributes, attributeGenerator)
    
    /**
     * Creates a new model with the same attributes but a different attribute generator
     */
    def withGenerator[B >: Attribute <: Constant](generator: PropertyGenerator[B]) = 
            new Model[B](attributes, generator)
    
    /**
     * Creates a copy of this model with filtered attributes
     */
    def filter(f: Attribute => Boolean) = withAttributes(attributes.filter(f))
    
    /**
     * Creates a copy of this model with filtered attributes. The result model only contains 
     * attributes not included by the filter
     */
    def filterNot(f: Attribute => Boolean) = withAttributes(attributes.filterNot(f))
    
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
    def renamed(oldName: String, newName: String): Model[Constant] = renamed(Vector(oldName -> newName))
    
    /**
     * Creates a mutable copy of this model
     * @param generator The property generator used for creating the properties of the new model
     * @return A mutable copy of this model using the provided property generator
     */
    def mutableCopy[T <: mutable.Variable](generator: PropertyGenerator[T]) =
    {
        val copy = new mutable.Model(generator)
        attributes.foreach { att => copy(att.name) = att.value }
        
        copy
    }
    
    /**
      * @return A mutable copy of this model
      */
    def mutableCopy(): mutable.Model[Variable] = mutableCopy(new SimpleVariableGenerator())
}