package utopia.flow.parse

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.template
import utopia.flow.datastructure.immutable.{Constant, Model, TreeLike, Value}
import utopia.flow.generic.StringType
import utopia.flow.generic.FromModelFactory
import utopia.flow.datastructure.template.Property
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._

import scala.collection.immutable.VectorBuilder
import scala.util.{Failure, Success, Try}

object XmlElement extends FromModelFactory[XmlElement]
{
    def apply(model: template.Model[Property]): Try[XmlElement] =
    {
        // If the name is not provided by user, it is read from the model
        model("name").string
            .toTry { new NoSuchElementException(s"Cannot parse XmlElement from $model without a 'name' property") }
            .map { name =>
                // Checks namespace specifications
                val (actualName, namespace) = {
                    model("namespace").string match {
                        case Some(explicitNamespace) => name -> Namespace(explicitNamespace)
                        case None =>
                            if (name.contains(":")) {
                                val (namespacePart, namePart) = name.splitAtFirst(":")
                                namePart -> Namespace(namespacePart)
                            }
                            else
                                name -> Namespace.empty
                    }
                }
                apply(actualName, model)(namespace)
            }
        
        model("name").string.map { name =>
            Success(apply(name, model))
        }.getOrElse(
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
      * @param namespace Namespace of this element
     */
    // TODO: Handle vector value types and instead of model, accept value
    def apply(name: String, model: template.Model[Property])(implicit namespace: Namespace): XmlElement =
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
            val modelChildren = unspecifiedAttributes
                .flatMap(att => att.value.model.map { (att.name, _) })
                .map { case (attName, attValue) =>
                    // Attribute name may specify namespace
                    val (name, childNamespace) = {
                        if (attName.contains(':')) {
                            val (namespacePart, namePart) = attName.splitAtFirst(":")
                            namePart -> Namespace(namespacePart)
                        }
                        else
                            attName -> namespace
                    }
                    XmlElement(name, attValue)(childNamespace)
                }
            
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
      * @param namespace Namespace of this element (implicit)
      * @return A new xml element
      */
    def build(name: String, attributes: Model = Model.empty)
             (fill: VectorBuilder[XmlElement] => Unit)(implicit namespace: Namespace) =
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
  * @param name Name of this element
  * @param value A simple value wrapped directly within this element
  * @param attributes Attributes assigned to this element
  * @param children Elements appearing within this element
  * @param namespace Namespace of this element
 */
case class XmlElement(name: String, value: Value = Value.emptyWithType(StringType), attributes: Model = Model.empty,
                      override val children: Vector[XmlElement] = Vector())(implicit override val namespace: Namespace)
    extends XmlElementLike[XmlElement] with TreeLike[String, XmlElement]
{
    // IMPLEMENTED  ----------------------------
    
    override def repr = this
    
    override protected def newNode(content: String) = XmlElement(content)
    
    override protected def createCopy(content: String, children: Vector[XmlElement]) =
        copy(name = content, children = children)
    
    
    // OTHER METHODS    ------------------------
    
    /**
      * @return Creates a new mutable copy of this xml element
      */
    def mutableCopy() = XmlElementBuilder(this)
    
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
    def withAttributes(attributes: Model) = copy(attributes = attributes)
    /**
      * @param newAttributes Additional attributes for this element
      * @return A copy of this element with those attributes added
      */
    def withAttributesAdded(newAttributes: Model) = withAttributes(attributes ++ newAttributes)
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
    def withChildren(children: Vector[XmlElement]) = createCopy(children = children)
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
    
    /**
      * Performs a mapping operation for all direct children that have the specified name
      * @param childName Name of the targeted child / children
      * @param f A mapping function
      * @return A modified copy of this element
      */
    def mapChildrenWithName(childName: String)(f: XmlElement => XmlElement) =
        copy(children = children.map { c => if (c.name ~== childName) f(c) else c })
    /**
      * Performs a flat-map operation for all direct children that have the specified name
      * @param childName Name of the targeted child / children
      * @param f A mapping function
      * @return A modified copy of this element
      */
    def flatMapChildrenWithName(childName: String)(f: XmlElement => IterableOnce[XmlElement]) =
        copy(children = children.flatMap { c => if (c.name ~== childName) Some(c) else f(c) })
    
    /**
      * Edits the targeted child/children using the specified function. The targeted element is converted to a mutable
      * copy during the edit
      * @param childName Name of the targeted child or children
      * @param f A function called for mutable copies of the targeted element(s)
      * @tparam U Arbitrary function result
      * @return A copy of this element where the changes made to the mutable element copies have been applied
      */
    def editChildrenWithName[U](childName: String)(f: XmlElementBuilder => U): XmlElement =
        mapChildrenWithName(childName) { e =>
            val builder = e.mutableCopy()
            f(builder)
            builder.result()
        }
    /**
      * Edits the targeted child/children using the specified function. The targeted element is converted to a mutable
      * copy during the edit.
      * @param path Path to the targeted child or children, where each item is an xml element name.
      *             Empty path points to this node.
      * @param f A function called for mutable copies of the targeted element(s)
      * @tparam U Arbitrary function result
      * @return A copy of this element where the changes made to the mutable element copies have been applied
      */
    def editPath[U](path: Seq[String])(f: XmlElementBuilder => U): XmlElement = mapPath(path) { e =>
        val builder = e.mutableCopy()
        f(builder)
        builder.result()
    }
}