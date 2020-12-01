package utopia.flow.parse

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.template
import utopia.flow.datastructure.immutable.{Constant, Model, TreeLike, Value}
import utopia.flow.generic.StringType
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.FromModelFactory
import utopia.flow.datastructure.template.Property
import utopia.flow.util.CollectionExtensions._

import scala.collection.immutable.VectorBuilder
import scala.util.{Failure, Success, Try}

object XmlElement extends FromModelFactory[XmlElement]
{
    def apply(model: template.Model[Property]): Try[XmlElement] =
    {
        // If the name is not provided by user, it is read from the model
        model("name").string.map { name => Success(apply(name, model)) }.getOrElse(
            Failure(new NoSuchElementException(s"Cannot parse XmlElement from $model without 'name' property")))
    }
    
    /**
     * Parses an xml element from a model
     * @param name the name for the xml element
     * @param model the model that contains the element attributes. The following attribute names are used:<br>
     * - value / text: Element value<br>
     * - children: Element children (array of models)<br>
     * - attributes: Element attributes (model)<br>
     * Unused attributes are converted into children or attributes if some of the primary attributes 
     * were missing.
     */
    // TODO: Handle vector value types
    def apply(name: String, model: template.Model[Property]): XmlElement = 
    {
        // Value is either in 'value' or 'text' attribute
        val valueAttribute = model.findExisting("value")
        val value = valueAttribute.map(_.value).orElse(
                model.findExisting("text").map(_.value)).getOrElse(Value.emptyWithType(StringType))
        
        // There may be some unused / non-standard attributes in the model
        val unspecifiedAttributes = model.attributes.filter(att => 
            att.name != valueAttribute.map(_.name).getOrElse("text") && att.name != "attributes" && 
            att.name != "children" && att.name != "name")
        
        // Children are either read from 'children' attribute or from the unused attributes
        val specifiedChildren = model.findExisting("children").map { _.value.getVector.flatMap(
                _.model).flatMap { apply(_).toOption } }
        val children = specifiedChildren.getOrElse
        {
            // Expects model type but parses other types as well
            val modelChildren = unspecifiedAttributes.flatMap(att => att.value.model.map { (att.name, _) }).map {
                case (attName, attValue) => XmlElement(attName, attValue) }
            
            // Other types are parsed into simple xml elements
            val nonModelChildren = unspecifiedAttributes.filterNot { att => modelChildren.exists {
                _.name == att.name } }.map { att => new XmlElement(att.name, att.value) }
            
            modelChildren ++ nonModelChildren
        }
        
        // Attributes are either read from 'attributes' attribute or from the unused attributes
        val specifiedAttributes = model.findExisting("attributes").map { _.value.getModel }
        val attributes = specifiedAttributes.getOrElse
        {
            if (specifiedChildren.isDefined)
                Model.withConstants(unspecifiedAttributes.map { att => Constant(att.name, att.value) })
            else
            {
                // If unused attributes were parsed into children, doesn't parse them into attributes
                Model.empty
            }
        }
        
        new XmlElement(name, value, attributes, children)
    }
    
    /**
      * Builds an xml element using a separate function
      * @param name Name of this element
      * @param attributes Attributes assigned to this element (default = empty)
      * @param fill A function that adds child elements to the provided buffer
      * @return A new xml element
      */
    def build(name: String, attributes: Model[Constant] = Model(Vector()))(fill: VectorBuilder[XmlElement] => Unit) =
    {
        val buffer = new VectorBuilder[XmlElement]()
        fill(buffer)
        apply(name, attributes = attributes, children = buffer.result())
    }
}

/**
 * XML Elements are used for representing XML data
 * @author Mikko Hilpinen
 * @since 13.1.2017 (v1.3)
 */
case class XmlElement(name: String, value: Value = Value.emptyWithType(StringType), attributes: Model[Constant] = Model(Vector()),
                      override val children: Vector[XmlElement] = Vector())
    extends TreeLike[String, XmlElement] with ModelConvertible
{
    // COMPUTED PROPERTIES    ------------------
    
    /**
     * The text inside this xml element. None if the element doesn't contain any text
     */
    def text = value.string
    override def toModel: Model[Constant] = 
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
    
    /**
     * Prints an xml string from this element. Character data is represented as is.
     */
    def toXml: String = 
    {
        // Case: Empty element
        if (text.forall(_.isEmpty) && children.isEmpty)
        {
            // Eg. <foo att1="2"/>
            s"<$name${ attributesString.map(" " + _).getOrElse("") }/>"
        }
        else
        {
            // Eg. <foo att1="2">Test value</foo>
            // Or <foo><bar/></foo>
            s"<$name${ attributesString.map(" " + _).getOrElse("") }>${ text.getOrElse("") }${ 
                    children.map(_.toXml).reduceLeftOption(_ + _).getOrElse("") }</$name>"
        }
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
                    base + Constant("name", name)
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
    private def attributesString = attributes.attributes.map(a => 
            s"${a.name}=${"\""}${a.value.stringOr()}${"\""}").reduceOption(_ + " " + _)
    
    
    // IMPLEMENTED  ----------------------------
    
    override protected def makeNode(content: String, children: Vector[XmlElement]) = XmlElement(name = content,
        children = children)
    
    override def content = name
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * Finds the first child with the provided name
     */
    def childWithName(name: String) = children.find(_.name.equalsIgnoreCase(name))
    
    /**
     * Finds the children with the provided name
     */
    def childrenWithName(name: String) = children.filter(_.name.equalsIgnoreCase(name))
    
    /**
     * Finds the value for an attribute with the specified name
     */
    def valueForAttribute(attName: String) = attributes(attName)
    
    /**
      * @param value New value for this element
      * @return A copy of this element with specified value
      */
    def withValue(value: Value) = copy(value = value)
    
    /**
      * @param text New text for this element
      * @return A copy of this element with new text
      */
    def withText(text: String) = withValue(if (text.isEmpty) Value.emptyWithType(StringType) else text)
    
    /**
      * @param attributes New set of attributes for this element
      * @return A copy of this element with those attributes
      */
    def withAttributes(attributes: Model[Constant]) = copy(attributes = attributes)
    
    /**
      * @param newAttributes Additional attributes for this element
      * @return A copy of this element with those attributes added
      */
    def withAttributesAdded(newAttributes: Model[Constant]) = withAttributes(attributes ++ newAttributes)
    
    /**
      * @param attribute A new attribute for this element
      * @return A copy of this element with specified attribute added
      */
    def withAttribute(attribute: Constant) = withAttributes(attributes + attribute)
    
    /**
      * @param attName Attribute name
      * @param value Attribute value
      * @return A copy of this element with specified attribute added
      */
    def withAttribute(attName: String, value: Value): XmlElement = withAttribute(Constant(attName, value))
    
    /**
      * @param children New set of children
      * @return A copy of this element with exactly those children
      */
    def withChildren(children: Vector[XmlElement]) = copy(children = children)
    
    /**
      * @param newChildren New children to add
      * @return A copy of this element with those children added
      */
    def withChildrenAdded(newChildren: IterableOnce[XmlElement]) = withChildren(children ++ newChildren)
    
    /**
      * @param child A new child to add
      * @return A copy of this element with specified child added
      */
    def withChildAdded(child: XmlElement) = withChildren(children :+ child)
    
    /**
      * @param child A child element
      * @return A copy of this element with only that child
      */
    def withChild(child: XmlElement) = withChildren(Vector(child))
    
    private def groupChildren(children: Vector[XmlElement]): Value =
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