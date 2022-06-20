package utopia.flow.parse

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.template
import utopia.flow.datastructure.immutable.{Constant, Model, Value}
import utopia.flow.generic.ModelConvertible
import utopia.flow.util.CollectionExtensions._

import scala.collection.immutable.VectorBuilder


/**
 * XML Elements are used for representing XML data
 * @author Mikko Hilpinen
 * @since 13.1.2017 (v1.3)
 */
trait XmlElementLike[+Repr <: XmlElementLike[Repr]]
    extends template.TreeLike[String, Repr] with ModelConvertible
{
    // ABSTRACT --------------------------------
    
    /**
      * @return Name of this xml element
      */
    def name: String
    /**
      * @return Value within this xml element
      */
    def value: Value
    /**
      * @return Attributes assigned to this xml element, as a model
      */
    def attributes: Model
    /**
      * @return Namespace of this element (which may be empty)
      */
    def namespace: Namespace
    
    
    // COMPUTED PROPERTIES    ------------------
    
    /**
      * @return Name of this element, including the possible namespace
      */
    def nameWithNamespace = if (namespace.isEmpty) name else s"$namespace:$name"
    
    /**
     * The text inside this xml element. None if the element doesn't contain any text
     */
    def text = value.string
    
    /**
      * @return Whether this is an empty xml element (e.g. <test/>). Empty elements may still contain attributes.
      */
    def isEmptyElement = children.isEmpty && text.forall { _.isEmpty }
    
    /**
     * Prints an xml string from this element. Character data is represented as is.
     */
    def toXml: String = 
    {
        val namePart = nameWithNamespace
        val attsPart = attributesString match {
            case Some(str) => " " + str
            case None => ""
        }
        
        // Case: Empty element
        // Eg. <foo att1="2"/>
        if (isEmptyElement)
            s"<$namePart$attsPart/>"
        // Case: Contains content
        // Eg. <foo att1="2">Test value</foo>
        // Or <foo><bar/></foo>
        else
            s"<$namePart$attsPart>${ text.getOrElse("") }${ children.map { _.toXml }.mkString }</$namePart>"
    }
    
    /**
      * @return A simplified model representation of this xml element
      */
    def toSimpleModel =
    {
        val nameProperty = Constant("name", name)
        toConstants match
        {
            // Case: This element consists of a single property
            case Left(constant) =>
                // Doesn't use element name as a property key. Uses 'value' instead.
                if (constant.name == name)
                    Model.withConstants(Vector(nameProperty, constant.withName("value")))
                // Specifies element name if possible
                else if (constant.name == "name")
                    Model.withConstants(Vector(constant))
                else
                    Model.withConstants(Vector(nameProperty, constant))
            // Case: This element consists of multiple properties => wraps those properties into a model
            case Right(constants) =>
                val base = Model.withConstants(constants)
                // Includes the name property if possible
                if (base.contains("name"))
                    base
                else
                    base + nameProperty
        }
    }
    
    private def toConstants: Either[Constant, Vector[Constant]] =
    {
        // Case: No children
        if (children.isEmpty)
        {
            // Case: Empty element with no attributes => Converts to name value pair
            if (attributes.isEmpty)
                Left(Constant(name, value))
            // Case: Empty element with attributes => returns those
            else if (text.isEmpty)
                Right(attributes.attributes)
            // Case: Attributes and value are defined => wraps them into a model, possibly overwriting 'value' attribute
            else
                Right((attributes + Constant("value", value)).attributes)
        }
        // Case: Wraps a single child => Attempts to convert it into a single property
        else if (children.size == 1)
        {
            val childName = children.head.name
            children.head.toConstants match
            {
                // Case: The wrapped element consists of a single property
                case Left(childProperty) =>
                    val actualProperty =
                    {
                        if (childProperty.name == childName)
                            childProperty
                        else
                            childProperty.mapName { name => s"$childName.$name" }
                    }
                    propertyWithAttributes(actualProperty)
                // Case: The wrapped element needs to be expressed as a model
                case Right(constants) => propertyWithAttributes(Constant(childName, Model.withConstants(constants)))
            }
        }
        // Case: All children have the same name and can therefore be expressed as a single array
        else if (children.map { _.name }.toSet.size == 1)
        {
            // val childName = children.head.name
            val childrenProperty = Constant(name, groupChildren(children))
            propertyWithAttributes(childrenProperty)
        }
        else
        {
            val childConstants = children.map { _.name }.distinct.map { childName =>
                val children = childrenWithName(childName)
                if (children.size > 1)
                    Constant(childName, groupChildren(children))
                else
                    children.head.toConstants match
                    {
                        case Left(constant) =>
                            if (constant.name == childName)
                                constant
                            else
                                constant.mapName { n => s"$childName.$n" }
                        case Right(constants) => Constant(childName, Model.withConstants(constants))
                    }
            }
            if (attributes.isEmpty)
                Right(childConstants)
            else if (childConstants.exists { c => attributes.contains(c.name) })
                Right(Vector(
                    Constant("attributes", attributes), Constant("children", Model.withConstants(childConstants))))
            else
                Right(attributes.attributes ++ childConstants)
        }
    }
    
    // Eg. 'att1="abc" att2="3"'. None if empty
    private def attributesString = attributes.attributes.map { a =>
            s"${a.name}=${"\""}${a.value.getString}${"\""}" }.reduceOption { _ + " " + _ }
    
    
    // IMPLEMENTED  ----------------------------
    
    /**
      * @return The "content" of this xml element, as it appears in a tree. I.e. the name of this xml element.
      */
    override def content = name
    
    override def toModel: Model =
    {
        val atts = new VectorBuilder[(String, Value)]
        atts += ("name" -> name)
        
        if (!value.isEmpty)
            atts += ("value" -> value)
        
        // Children are only included if necessary
        if (children.nonEmpty)
            atts += ("children" -> children.map(_.toModel).toVector)
        
        // Attributes are also only included if necessary
        if (!attributes.isEmpty)
            atts += ("attributes" -> attributes)
        
        Model(atts.result())
    }
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * Finds the first child with the provided name
     */
    def childWithName(name: String) = children.find { _.name.equalsIgnoreCase(name) }
    /**
     * Finds the children with the provided name
     */
    def childrenWithName(name: String) = children.filter { _.name.equalsIgnoreCase(name) }
    
    /**
     * Finds the value for an attribute with the specified name
     */
    def valueForAttribute(attName: String) = attributes(attName)
    
    private def groupChildren(children: Vector[XmlElementLike[_]]): Value =
    {
        val childResults = children.map { _.toConstants }
        if (childResults.forall { _.isLeft })
            childResults.flatMap { _.leftOption }.map { _.value }
        else
            childResults.map
            {
                case Left(constant) => Model.withConstants(Vector(constant))
                case Right(constants) => Model.withConstants(constants)
            }
    }
    
    private def propertyWithAttributes(property: Constant) =
    {
        if (attributes.isEmpty)
            Left(property)
        else if (attributes.contains(property.name) && property.name.toLowerCase != "attributes")
            Right(Vector(Constant("attributes", attributes), property))
        else
            Right((attributes + property).attributes)
    }
}