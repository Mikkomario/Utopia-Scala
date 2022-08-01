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
    // IMPLEMENTED  -----------------------
    
    def apply(model: template.Model[Property]): Try[XmlElement] = {
        model("name").string.map { name =>
            val namespacedName = {
                if (name.contains(':')) {
                    val (nsPart, namePart) = name.splitAtFirst(":")
                    Namespace(nsPart)(namePart)
                }
                else
                    Namespace.empty(name)
            }
            Success(apply(namespacedName, model))
        }.getOrElse(
            Failure(new NoSuchElementException(s"Cannot parse XmlElement from $model without 'name' property")))
    }
    
    
    // OTHER    --------------------------
    
    /**
     * Parses an xml element from a model
     * @param name the name for the xml element (namespaced)
     * @param model the model that contains the element attributes. The following attribute names are used:<br>
     * - value / text: Element value<br>
     * - children: Element children (array of models)<br>
     * - attributes: Element attributes (model)<br>
     * Unused attributes are converted into children or attributes if some of the primary attributes 
     * were missing.
     */
    // TODO: Handle vector value types and instead of model, accept value
    def apply(name: NamespacedString, model: template.Model[Property]): XmlElement =
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
        val children = specifiedChildren.getOrElse {
            // Expects model type but parses other types as well
            val modelChildren = unspecifiedAttributes
                .flatMap(att => att.value.model.map { (att.name, _) })
                .map { case (attName, attValue) =>
                    // Attribute name may specify namespace
                    val childName = {
                        if (attName.contains(':')) {
                            val (namespacePart, namePart) = attName.splitAtFirst(":")
                            Namespace(namespacePart)(namePart)
                        }
                        else
                            name.namespace(attName)
                    }
                    XmlElement(childName, attValue)
                }
            
            // Other types are parsed into simple xml elements
            val nonModelChildren = unspecifiedAttributes.filterNot { att => modelChildren.exists {
                _.name == att.name } }.map { att => new XmlElement(att.name, att.value) }
            
            modelChildren ++ nonModelChildren
        }
        
        // Attributes are either read from 'attributes' attribute or from the unused attributes
        val specifiedAttributes = model.findExisting("attributes").map { _.value.getModel }
        val attributes = specifiedAttributes.getOrElse {
            if (specifiedChildren.isDefined)
                Model.withConstants(unspecifiedAttributes.map { att => Constant(att.name, att.value) })
            // If unused attributes were parsed into children, doesn't parse them into attributes
            else
                Model.empty
        }
        // Processes attribute namespaces, also
        val namespacedAttributes = attributes.attributes.map { c =>
            if (c.name.contains(':')) {
                val (namespacePart, namePart) = c.name.splitAtFirst(":")
                Namespace(namespacePart) -> c.withName(namePart)
            }
            else
                Namespace.empty -> c
        }.asMultiMap.view.mapValues { Model.withConstants(_) }.toMap
        
        new XmlElement(name, value, namespacedAttributes, children)
    }
    
    /**
      * Creates a new xml element where name and attributes are tied to a single namespace
      * @param name Name of this element (namespaced)
      * @param value Value directly within this element (default = empty)
      * @param attributes Attributes within this element (namespaced) (default = empty)
      * @param children Children under this element (default = empty)
      * @param namespace Implicit namespace to use for the name and attributes of this element
      * @return A new xml element
      */
    def namespaced(name: String, value: Value = Value.emptyWithType(StringType), attributes: Model = Model.empty,
                   children: Vector[XmlElement] = Vector())
                  (implicit namespace: Namespace) =
        apply(namespace(name), value, Map(namespace -> attributes), children)
    /**
      * Creates a new xml element without namespacing
      * @param name Name of this element
      * @param value Value directly within this element (default = empty)
      * @param attributes Attributes within this element (default = empty)
      * @param children Children under this element (default = empty)
      * @return A new xml element
      */
    def local(name: String, value: Value = Value.emptyWithType(StringType), attributes: Model = Model.empty,
              children: Vector[XmlElement] = Vector()) =
        namespaced(name, value, attributes, children)(Namespace.empty)
    
    /**
      * Builds an xml element using a separate function
      * @param name Name of this element (namespaced)
      * @param attributes Attributes assigned to this element (default = empty)
      * @param fill A function that adds child elements to the provided buffer
      * @return A new xml element
      */
    def build(name: NamespacedString, attributes: Map[Namespace, Model] = Map.empty)
             (fill: VectorBuilder[XmlElement] => Unit) =
    {
        val buffer = new VectorBuilder[XmlElement]()
        fill(buffer)
        apply(name, attributeMap = attributes, children = buffer.result())
    }
    /**
      * Builds an xml element using a separate function
      * @param name Name of this element
      * @param attributes Attributes assigned to this element (default = empty)
      * @param fill A function that adds child elements to the provided buffer
      * @param namespace Namespace to apply to element name and attributes
      * @return A new xml element
      */
    def buildNamespaced(name: String, attributes: Model = Model.empty)
                       (fill: VectorBuilder[XmlElement] => Unit)
                       (implicit namespace: Namespace) =
        build(namespace(name), Map(namespace -> attributes))(fill)
    /**
      * Builds an xml element using a separate function
      * @param name Name of this element
      * @param attributes Attributes assigned to this element (default = empty)
      * @param fill A function that adds child elements to the provided buffer
      * @return A new xml element
      */
    def buildLocal(name: String, attributes: Model = Model.empty)(fill: VectorBuilder[XmlElement] => Unit) =
        buildNamespaced(name, attributes)(fill)(Namespace.empty)
}

/**
 * XML Elements are used for representing XML data
 * @author Mikko Hilpinen
 * @since 13.1.2017 (v1.3)
  * @param name Name of this element (namespaced)
  * @param value A simple value wrapped directly within this element
  * @param attributeMap A map that contains attribute models for different namespaces (default = empty)
  * @param children Elements appearing within this element
 */
case class XmlElement(name: NamespacedString, value: Value = Value.emptyWithType(StringType),
                      attributeMap: Map[Namespace, Model] = Map.empty,
                      override val children: Vector[XmlElement] = Vector())
    extends XmlElementLike[XmlElement] with TreeLike[NamespacedString, XmlElement]
{
    // ATTRIBUTES   ----------------------------
    
    // Caches the attributes model
    override lazy val attributes = super.attributes
    
    
    // IMPLEMENTED  ----------------------------
    
    override def repr = this
    
    override protected def newNode(content: NamespacedString) = XmlElement(content)
    
    override protected def createCopy(content: NamespacedString, children: Vector[XmlElement]) =
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
      * @param attributeMap New set of attributes for this element
      * @return A copy of this element with those attributes
      */
    def withAttributes(attributeMap: Map[Namespace, Model]) = copy(attributeMap = attributeMap)
    /**
      * @param attributes New set of attributes for this element
      * @return A copy of this element with those attributes
      */
    def withAttributes(attributes: Model)(implicit namespace: Namespace): XmlElement =
        withAttributes(Map(namespace -> attributes))
    /**
      * @param newAttributes Additional attributes for this element
      * @return A copy of this element with those attributes added
      */
    def withAttributesAdded(newAttributes: Model)(implicit namespace: Namespace) =
        withAttributes(attributeMap.appendOrMerge(namespace, newAttributes) { _ ++ _ })
    /**
      * @param attribute A new attribute for this element
      * @return A copy of this element with specified attribute added
      */
    def withAttribute(attribute: Constant)(implicit namespace: Namespace) = {
        val newModel = attributeMap.get(namespace) match {
            case Some(model) => model + attribute
            case None => Model.withConstants(Vector(attribute))
        }
        withAttributes(attributeMap + (namespace -> newModel))
    }
    /**
      * @param attName Attribute name
      * @param value Attribute value
      * @return A copy of this element with specified attribute added
      */
    def withAttribute(attName: NamespacedString, value: Value): XmlElement =
        withAttribute(Constant(attName.local, value))(attName.namespace)
    
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
    def editPath[U](path: Seq[NamespacedString])(f: XmlElementBuilder => U): XmlElement = mapPath(path) { e =>
        val builder = e.mutableCopy()
        f(builder)
        builder.result()
    }
}