package utopia.flow.parse

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.template
import utopia.flow.datastructure.immutable.{Constant, Model, TreeLike, Value}
import utopia.flow.generic.StringType
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.FromModelFactory
import utopia.flow.datastructure.template.Property

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
     * @param model the model that contains the element attributes. The following attribute names 
     * are used:<br>
     * - value / text: Element value<br>
     * - children: Element children (array of models)<br>
     * - attributes: Element attributes (model)<br>
     * Unused attributes are converted into children or attributes if some of the primary attributes 
     * were missing.
     */
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
}