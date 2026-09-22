package utopia.flow.parse.xml

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.mutable.tree.MutableTreeLike
import utopia.flow.collection.template.PathNavigator
import utopia.flow.view.mutable.Pointer

object XmlElementBuilder
{
	/**
	  * Creates a new xml element builder from an existing xml element
	  * @param element An xml element
	  * @tparam X Type of the xml element
	  * @return A builder based on that xml element
	  */
	@deprecated("Deprecated for removal. Please use .from(XmlElementLike) instead", "v2.9")
	def apply[X <: XmlElementLike[X]](element: X): XmlElementBuilder = {
		val builder = new XmlElementBuilder(element.name, element.value, element.attributeMap)
		builder.children = element.children.map { apply(_) }.toVector
		builder
	}
	
	def from[X <: XmlElementLike[X]](element: X): XmlElementBuilder = element match {
		case builder: XmlElementBuilder => builder
		case elem =>
			new XmlElementBuilder(elem.name, elem.value, elem.attributeMap, elem.children.view.map(from[X]).toOptimizedSeq)
	}
}

/**
  * A mutable [[XmlElementLike]] implementation that may be used to build and edit (immutable) XML element structures
  * @author Mikko Hilpinen
  * @since 10.4.2022, v1.15
  */
class XmlElementBuilder(initialName: NamespacedString, initialValue: Value = Value.emptyWithType(StringType),
                        initialAttributeMap: Map[Namespace, Model] = Map(),
                        initialChildren: Seq[XmlElementBuilder] = Empty)
	extends XmlElementLike[XmlElementBuilder] with MutableTreeLike[XmlElement, XmlElementBuilder] with Pointer[Value]
		with PathNavigator[NamespacedString, XmlElementBuilder]
{
	// ATTRIBUTES   --------------------------------
	
	var name = initialName
	var value = initialValue
	var attributeMap = initialAttributeMap
	var children: Seq[XmlElementBuilder] = initialChildren
	
	
	// COMPUTED ------------------------------------
	
	def text_=(newText: String) = value = newText
	
	
	// IMPLEMENTED  --------------------------------
	
	override def self = this
	override protected def current: XmlElementBuilder = this
	
	override def +=(child: XmlElement): Unit = children :+= XmlElementBuilder.from(child)
	override def ++=(children: IterableOnce[XmlElement]): Unit =
		this.children ++= children.iterator.map(XmlElementBuilder.from[XmlElement])
	
	override def filterDirect(f: XmlElementBuilder => Boolean): Unit = children = children.filter(f)
	override def filter(f: XmlElementBuilder => Boolean): Unit = {
		filterDirect(f)
		children.foreach { _.filter(f) }
	}
	
	override def clear(): Unit = children = Empty
	
	override protected def findUnder(parent: XmlElementBuilder, nav: NamespacedString): Option[XmlElementBuilder] =
		children.find { _.name ~== nav }
	
	override protected def nodeFor(nav: NamespacedString): XmlElementBuilder = {
		val newChild = new XmlElementBuilder(nav)
		children :+= newChild
		newChild
	}
	override protected def nodeForPath(parents: Seq[XmlElementBuilder], path: Iterator[NamespacedString]): XmlElementBuilder = {
		var lastParent = parents.last
		while (path.hasNext) {
			lastParent = lastParent.nodeFor(path.next())
		}
		lastParent
	}
	
	
	// OTHER    -----------------------------------
	
	/**
	  * @return An immutable xml element based on this builder's current state
	  */
	def result(): XmlElement = XmlElement(name, value, attributeMap, children.map { _.result() })
	
	/**
	  * Removes children with the specified name from under this node. Only targets direct children.
	  * @param childName Name of the child or children to remove.
	  */
	def -=(childName: String) = filterDirect { _.name !~== childName }
	
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
			attributeMap = attributeMap.mapValue(attName.namespace) { _ + Constant(attName.local -> newValue) }
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
