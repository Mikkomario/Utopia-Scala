package utopia.flow.datastructure.template

import utopia.flow.datastructure.immutable.Value
import utopia.flow.parse.JsonConvertible
import utopia.flow.util.CollectionExtensions._

/**
 * Models are used for storing named values
 * @author Mikko Hilpinen
 * @since 26.11.2016
 * @tparam Attribute The type of the properties stored within this model
 */
trait Model[+Attribute <: Property] extends MapLike[String, Value] with JsonConvertible
{
    // ABSTRACT    --------------
    
    /**
     * The attributes stored in this model. The key is the attribute's name (lower case), 
     * the value is the attribute itself
     */
    def attributeMap: Map[String, Attribute]
    
    /**
     * @return Attribute names in an order (lower case). Should contain all of this model's attributes and no attribute
     *         names which are not contained within this model.
     */
    protected def attributeOrder: Vector[String]
    
    /**
      * Generates a new attribute with the provided name
      * @param attName The name of the new attribute
      * @return The new attribute
      */
    protected def generateAttribute(attName: String): Attribute
    
    
    // COMP. PROPERTIES    --------
    
    /**
      * @return The attributes of this model
      */
    def attributes = {
        val allAttributes = attributeMap
        attributeOrder.flatMap(allAttributes.get)
    }
    
    /**
     * The names of the attributes stored in this model
     */
    def attributeNames = attributeMap.keySet
    
    /**
     * The attributes which have a defined value
     */
    def attributesWithValue = attributes.filter { _.value.isDefined }
    
    /**
     * A model is empty when no attributes have been assigned to it. It doesn't matter whether the
     * defined attributes actually contain a value or not.
     * @see hasOnlyEmptyValues
     */
    def isEmpty = attributeMap.isEmpty
    
    /**
      * @return Whether this model contains attributes. Notice that it doesn't matter whether the attributes
     *         have a value or not.
     * @see hasNonEmptyValues
      */
    def nonEmpty = !isEmpty
    
    /**
     * @return Whether this model specifies any non-empty values
     */
    def hasNonEmptyValues = attributeMap.values.exists { _.value.isDefined }
    
    /**
     * @return Whether all values in this model are empty
     */
    def hasOnlyEmptyValues = !hasNonEmptyValues
    
    
    // IMPLEMENTED    -------------
    
    /**
     * Gets the value of a single attribute in this model
     * @param attName The name of the attribute from which the value is taken
     * @return The value of the attribute with the provided name
     */
    override def apply(attName: String): Value = get(attName).value
    
    override def appendToJson(jsonBuilder: StringBuilder) = {
        if (isEmpty)
            jsonBuilder ++= "{}"
        else {
            jsonBuilder ++= "{"
            val atts = attributes
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
      * Finds an attribute value in this model, possibly searching from alternative properties.
      * May generate one or more new attributes.
      * @param attNames Names of the targeted attributes (ordered)
      * @return The first attribute value which was non-empty. Empty value if all searches resulted in empty values.
      */
    def apply(attNames: IterableOnce[String]): Value =
        attNames.iterator.map(apply).find { _.isDefined }.getOrElse(Value.empty)
    
    /**
      * Finds an attribute value in this model, possibly searching from alternative properties.
      * May generate one or more new attributes.
      * @param attName Name of the primary target attribute
      * @param secondaryAttName Name of the secondary target attribute
      * @param moreAttNames Name of the additional backup attributes
      * @return The first attribute value which was non-empty. Empty value if all searches resulted in empty values.
      */
    def apply(attName: String, secondaryAttName: String, moreAttNames: String*): Value =
        apply(Vector(attName, secondaryAttName) ++ moreAttNames)
    
    /**
     * Finds an existing attribute from this model. No new attributes will be generated
     * @param attName The name of the attribute
     * @return an attribute in this model with the provided name or None if no such attribute 
     * exists
     */
    def findExisting(attName: String) = attributeMap.get(attName.toLowerCase())
    def findExisting(attNames: IterableOnce[String]): Option[Attribute] = attNames.findMap(findExisting)
    
    /**
     * Finds an attribute from this model. Generating one if necessary.
     * @param attName The name of the attribute
     * @return The attribute from this model (possibly generated)
     */
    def get(attName: String) = findExisting(attName).getOrElse(generateAttribute(attName))
    
    /**
     * Whether this model contains an existing attribute for the specified attribute name
     * @param attName the name of the attribute
     */
    def contains(attName: String) = attributeMap.contains(attName.toLowerCase)
    
    /**
      * @param attName Name of searched attribute
      * @return Whether this model contains a non-empty attribute with the specified name
      */
    def containsNonEmpty(attName: String) = attributeMap.get(attName.toLowerCase).exists { _.value.isDefined }
    
    /**
     * Converts and unwraps this model's attributes into a map format
     * @param f a function that converts an attribute value into the desired instance type
     * @return a map from the converted attributes of this model
     */
    def toMap[T](f: Value => Option[T]) = attributeMap.flatMap { case (name, attribute) => 
            f(attribute.value).map { (name, _) } }
}