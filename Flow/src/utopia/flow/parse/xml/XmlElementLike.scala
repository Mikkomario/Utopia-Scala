package utopia.flow.parse.xml

import utopia.flow.collection.template.TreeLike
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.parse.xml.XmlElementLike.isAllowedInContent
import utopia.flow.util.StringExtensions._

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

object XmlElementLike
{
    // ATTRIBUTES   -----------------------
    
    // Characters not accepted in the xml text (http://validchar.com/d/xml10/xml10_namestart [17.1.2018])
    // Ordered
    /**
      * Ranges of characters that are not allowed within an XML element body / content. Ordered.
      */
    val invalidContentCharacterRanges = Vector(/*(0 to 64), */91 to 94, 123 to 191, 768 to 879,
        8192 to 8203, 8206 to 8303, 8592 to 11263, 12272 to 12288, 55296 to 63743,
        64976 to 65007, 65534 to 1114111)
    /**
      * Individual characters (in addition to 'invalidContentCharacterRanges') that are not allowed within
      * an XML element body / content. Ordered.
      */
    val invalidExtraCharacters = Vector(34, 38, 39, 60, 62, 96, 215, 247, 894)
    
    
    // OTHER    ----------------------------
    
    /**
      * @param string A string that would appear in an xml element body
      * @return Whether the specified string may appear as is (true).
      *         If false, the string must be wrapped in CDATA if present in an xml element body.
      */
    def isAllowedInContent(string: String): Boolean = string.forall(isAllowedInContent)
    /**
      * @param c A character that would appear in an xml element body
      * @return Whether the specified character may appear in an xml element's body as is (true).
      *         If false, the xml element content must be wrapped in CDATA.
      */
    def isAllowedInContent(c: Char) = {
        val i = c.toInt
        // Character is not allowed if it lies in an invalid char range or is specifically invalid
        invalidContentCharacterRanges.find { _.end >= i }.forall { _.start > i } &&
            !invalidExtraCharacters.contains(i)
    }
}

/**
 * XML Elements are used for representing XML data
 * @author Mikko Hilpinen
 * @since 13.1.2017 (v1.3)
 */
trait XmlElementLike[+Repr <: XmlElementLike[Repr]]
    extends TreeLike[NamespacedString, Repr] with ModelConvertible
{
    // ABSTRACT --------------------------------
    
    /**
      * @return "This" instance
      */
    def self: Repr
    /**
      * @return Name of this xml element
      */
    def name: NamespacedString
    /**
      * @return Value within this xml element
      */
    def value: Value
    /**
      * @return Namespaced attributes assigned to this xml element.
      *         Keys are namespaces, values are models that represent attribute sets.
      */
    def attributeMap: Map[Namespace, Model]
    
    
    // COMPUTED PROPERTIES    ------------------
    
    /**
      * @return The local name of this xml element
      */
    def localName = name.local
    
    /**
      * @return Attributes assigned to this xml element, as a model
      */
    def attributes: Model = attributeMap.valuesIterator.reduceLeftOption { _ ++ _ }.getOrElse(Model.empty)
    
    /**
     * The text inside this xml element. Empty if this element doesn't contain any text
     */
    def text = value.getString
    
    /**
      * @return Whether this is an empty xml element (e.g. <test/>). Empty elements may still contain attributes.
      */
    def isEmptyElement = children.isEmpty && value.isEmpty
    
    /**
     * Prints an xml string from this element.
     */
    def toXml: String = {
        val builder = new mutable.StringBuilder()
        appendToXml(builder)
        builder.result()
    }
    
    /**
      * @return A simplified model representation of this xml element
      */
    def toSimpleModel = {
        val nameProperty = Constant("name", localName)
        toConstants match {
            // Case: This element consists of a single property
            case Left(constant) =>
                // Doesn't use element name as a property key. Uses 'value' instead.
                if (constant.name == localName)
                    Model.withConstants(Vector(nameProperty, constant.withName("value")))
                // Specifies element name if possible
                else if (constant.name == "name")
                    Model.withConstants(Single(constant))
                else
                    Model.withConstants(Pair(nameProperty, constant))
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
    
    private def toConstants: Either[Constant, Seq[Constant]] = {
        // Case: No children
        if (children.isEmpty) {
            // Case: Empty element with no attributes => Converts to name value pair
            if (attributes.isEmpty)
                Left(Constant(localName, value))
            // Case: Empty element with attributes => returns those
            else if (text.isEmpty)
                Right(attributes.properties)
            // Case: Attributes and value are defined => wraps them into a model, possibly overwriting 'value' attribute
            else
                Right((attributes +Constant("value", value)).properties)
        }
        // Case: Wraps a single child => Attempts to convert it into a single property
        else if (children hasSize 1) {
            val childName = children.head.localName
            children.head.toConstants match {
                // Case: The wrapped element consists of a single property
                case Left(childProperty) =>
                    val actualProperty = {
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
        else if (children.map { _.localName }.toSet.size == 1) {
            // val childName = children.head.name
            val childrenProperty =Constant(localName, groupChildren(children.toVector))
            propertyWithAttributes(childrenProperty)
        }
        else {
            val childConstants = children.map { _.localName }.distinct.map { childName =>
                val children = childrenWithName(childName)
                if (children.size > 1)
                   Constant(childName, groupChildren(children.toVector))
                else
                    children.head.toConstants match {
                        case Left(constant) =>
                            if (constant.name == childName)
                                constant
                            else
                                constant.mapName { n => s"$childName.$n" }
                        case Right(constants) => Constant(childName, Model.withConstants(constants))
                    }
            }
            if (attributes.isEmpty)
                Right(childConstants.toVector)
            else if (childConstants.exists { c => attributes.contains(c.name) })
                Right(Vector(
                   Constant("attributes", attributes), Constant("children", Model.withConstants(childConstants))))
            else
                Right(attributes.properties ++ childConstants)
        }
    }
    
    // Eg. 'att1="abc" att2="3"'
    private def attributesString =
        attributeMap.flatMap { case (namespace, model) => model.properties.map { att =>
            val attName = if (namespace.isEmpty) att.name else s"${namespace.name}:${att.name}"
            s"$attName=${att.value.getString.quoted}"
        } }.mkString(" ")
    
    
    // IMPLEMENTED  ----------------------------
    
    override implicit def navEquals: EqualsFunction[NamespacedString] = NamespacedString.approxEquals
    
    /**
      * @return The "content" of this xml element, as it appears in a tree. I.e. the name of this xml element.
      */
    override def nav = name
    
    override def toString = toXml
    override def toModel: Model = {
        val atts = new VectorBuilder[(String, Value)]()
        atts += ("name" -> name.toString)
        
        if (!value.isEmpty)
            atts += ("value" -> value)
        
        // Children are only included if necessary
        if (children.nonEmpty)
            atts += ("children" -> children.map(_.toModel).toVector)
        
        // Attributes are also only included if necessary
        val attributesModel = attributeMap
            .map { case (namespace, model) =>
                if (namespace.isEmpty)
                    model
                else
                    model.mapKeys { n => namespace(n).toString }
            }
            .reduceOption { _ ++ _ }
            .getOrElse(Model.empty)
        if (attributesModel.nonEmpty)
            atts += ("attributes" -> attributesModel)
        
        Model(atts.result())
    }
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * Finds the first child with the provided name
     */
    def childWithName(name: String) = children.find { _.name ~== name }
    /**
     * Finds the children with the provided name
     */
    def childrenWithName(name: String) = children.filter { _.name ~== name }
    
    /**
      * Finds the value of the specified attribute within this element. If a non-namespaced name is given, namespaced
      * attributes are searched, also (non-namespace-specific use-case).
      * @param attName Name of the targeted attribute
      * @return Value for that attribute, or an empty value
      */
    def valueOfAttribute(attName: NamespacedString) = attName.namespaceOption match {
        case Some(namespace) =>
            attributeMap.get(namespace) match {
                case Some(model) => model(attName.local)
                case None => Value.empty
            }
        case None =>
            // Prefers the no namespace -attribute set
            attributeMap.get(Namespace.empty).map { _(attName.local) }.filter { _.isDefined }
                .orElse {
                    attributeMap.iterator.filterNot { _._1.isEmpty }.map { _._2(attName.local) }.find { _.isDefined }
                }
                .getOrElse(Value.empty)
    }
    /**
      * Finds the value under a child element
      * @param childName Name of the targeted (direct) child element
      * @return Value of that child element. Empty value if no such child existed
      */
    def valueOfChild(childName: String) = childWithName(childName) match {
        case Some(c) => c.value
        case None => Value.empty
    }
    /**
      * Finds the value under a child element
      * @param firstChildName Name of the direct child element
      * @param secondChildName Name of the indirect child element
      * @param more Names of additional targeted child elements (i.e. path)
      * @return Value of the child element at the end of the specified path. Empty value if no such child was found.
      */
    def valueOfChild(firstChildName: String, secondChildName: String, more: String*) = {
        val path = Pair(firstChildName, secondChildName) ++ more
        path.foldLeftIterator(Some(self): Option[Repr]) { (elem, next) => elem.flatMap { _.childWithName(next) } }
            .takeTo { _.isEmpty }
            .last
        match {
            case Some(elem) => elem.value
            case None => Value.empty
        }
    }
    /**
      * @param key Targeted attribute or child name
      * @return Value under that attribute or child. Empty value if neither is found.
      */
    def valueOf(key: NamespacedString) = {
        valueOfAttribute(key).orElse { valueOfChild(key.local) }
    }
    
    /**
      * Prints an xml string from this element. Character data is represented as is.
      * @param xmlBuilder Builder for building the final xml string
      */
    def appendToXml(xmlBuilder: mutable.StringBuilder): Unit = {
        val namePart = name.toString
        val attsPart = attributesString.mapIfNotEmpty { " " + _ }
        
        // Case: Empty element
        // Eg. <foo att1="2"/>
        if (isEmptyElement)
            xmlBuilder ++= s"<$namePart$attsPart/>"
        // Case: Contains content
        // Eg. <foo att1="2">Test value</foo>
        // Or <foo><bar/></foo>
        else {
            val cDataRequired = !isAllowedInContent(text)
            
            xmlBuilder ++= s"<$namePart$attsPart>"
            if (cDataRequired)
                xmlBuilder ++= "<![CDATA["
            xmlBuilder ++= text
            if (cDataRequired)
                xmlBuilder ++= "]]>"
            children.foreach { _.appendToXml(xmlBuilder) }
            xmlBuilder ++= s"</$namePart>"
        }
    }
    
    private def groupChildren(children: Vector[XmlElementLike[_]]): Value = {
        val childResults = children.map { _.toConstants }
        if (childResults.forall { _.isLeft })
            childResults.flatMap { _.leftOption }.map { _.value }
        else
            childResults.map {
                case Left(constant) => Model.withConstants(Single(constant))
                case Right(constants) => Model.withConstants(constants)
            }
    }
    
    private def propertyWithAttributes(property: Constant) = {
        if (attributes.isEmpty)
            Left(property)
        else if (attributes.contains(property.name) && property.name.toLowerCase != "attributes")
            Right(Vector(immutable.Constant("attributes", attributes), property))
        else
            Right((attributes + property).properties)
    }
}