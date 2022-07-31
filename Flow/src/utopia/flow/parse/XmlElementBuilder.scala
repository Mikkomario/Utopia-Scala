package utopia.flow.parse

import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.datastructure.mutable
import utopia.flow.generic.StringType
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._

object XmlElementBuilder
{
	/**
	  * Creates a new xml element builder from an existing xml element
	  * @param element An xml element
	  * @tparam X Type of the xml element
	  * @return A builder based on that xml element
	  */
	def apply[X <: XmlElementLike[X]](element: X): XmlElementBuilder = {
		val builder = new XmlElementBuilder(element.name, element.value, element.attributeMap)
		builder.children = element.children.map { apply(_) }
		builder
	}
}

/**
  * A mutable XmlElement version which may be used to build and edit (immutable) xml element structures
  * @author Mikko Hilpinen
  * @since 10.4.2022, v1.15
  */
class XmlElementBuilder(initialName: NamespacedString, initialValue: Value = Value.emptyWithType(StringType),
                        initialAttributeMap: Map[Namespace, Model] = Map())
	extends XmlElementLike[XmlElementBuilder] with mutable.TreeLike[String, XmlElementBuilder]
{
	// ATTRIBUTES   --------------------------------
	
	var name = initialName
	var value = initialValue
	var attributeMap = initialAttributeMap
	var children = Vector[XmlElementBuilder]()
	
	
	// IMPLEMENTED  --------------------------------
	
	override def repr = this
	
	def text_=(newText: String) = value = newText
	
	override protected def newNode(content: String) = {
		// Adds the node as a new child
		val node = new XmlElementBuilder(name.namespace(content))
		children :+= node
		node
	}
	
	override protected def setChildren(newChildren: Vector[XmlElementBuilder]) = children = newChildren
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @return An immutable xml element based on this builder's current state
	  */
	def result(): XmlElement = XmlElement(name, value, attributeMap, children.map { _.result() })
	
	/**
	  * Adds a new child node under this node
	  * @param child The (pre-built) child xml element to add
	  */
	def +=(child: XmlElement): Unit = this += XmlElementBuilder(child)
	/**
	  * Removes children with the specified name from under this node. Only targets direct children.
	  * @param childName Name of the child or children to remove.
	  */
	def -=(childName: String) = children = children.filterNot { _.name ~== childName }
	
	/**
	  * Updates the value of a single attribute (alias for .setAttribute(String, Value))
	  * @param attName Name of the specified / updated attribute
	  * @param newValue New value to assign to this attribute
	  */
	def update(attName: NamespacedString, newValue: Value) = setAttribute(attName, newValue)
	/**
	  * Specifies an attribute value
	  * @param attName Name of the specified attribute
	  * @param newValue New value assigned for this attribute
	  */
	def setAttribute(attName: NamespacedString, newValue: Value) = {
		if (attributeMap.contains(attName.namespace))
			attributeMap = attributeMap.mapValue(attName.namespace) { _ + (attName.local -> newValue) }
		else
			attributeMap += (attName.namespace -> Model.from(attName.local -> newValue))
	}
	
	/**
	  * Removes an attribute value
	  * @param attName Name of the attribute to remove
	  */
	def clearAttribute(attName: NamespacedString) = {
		if (attName.hasNamespace)
			attributeMap = attributeMap.mapValue(attName.namespace) { _ - attName.local }
		else
			attributeMap = attributeMap.view.mapValues { _ - attName.local }.toMap
	}
	
	/**
	  * Adds a new child to this element builder
	  * @param childName Name of the new child
	  * @param childValue Value to assign to this new child (default = empty)
	  * @param f A function that modifies the new child
	  * @tparam A Function result type
	  * @return Result value of the specified function
	  */
	def buildNewChild[A](childName: NamespacedString, childValue: Value = Value.emptyWithType(StringType))
	                    (f: XmlElementBuilder => A) =
	{
		val builder = new XmlElementBuilder(childName, childValue)
		val result = f(builder)
		children :+= builder
		result
	}
}
