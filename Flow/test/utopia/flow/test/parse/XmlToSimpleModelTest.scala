package utopia.flow.test.parse

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.xml.XmlElement

/**
 * Tests xml to simple model conversion
 * @author Mikko Hilpinen
 * @since 1.12.2020, v1.9
 */
object XmlToSimpleModelTest extends App
{
	DataType.setup()
	
	def printElement(elem: XmlElement) =
	{
		println()
		println(elem.toXml)
		println(elem.toSimpleModel.toJson)
	}
	
	val empty = XmlElement("Empty")
	printElement(empty)
	
	val value = XmlElement("ValueWrapper", "test")
	printElement(value)
	
	val testAtts = Model(Vector("a" -> 1, "b" -> 2))
	val attributes = XmlElement.local("Attributes", attributes = testAtts)
	printElement(attributes)
	
	val wrapper = XmlElement("Wrapper", children = Vector(value))
	printElement(wrapper)
	
	val wrapperWithAttributes = wrapper.withAttributes(testAtts)
	printElement(wrapperWithAttributes)
	
	val list = XmlElement("List", children = Vector(value, value, value))
	printElement(list)
	
	val listWrapper = XmlElement("Wrapper", children = Vector(list))
	printElement(listWrapper)
	
	val listWrapperWithAttributes = listWrapper.withAttributes(testAtts)
	printElement(listWrapperWithAttributes)
	
	val differentChildren = XmlElement("Parent", children = Vector(empty, value))
	printElement(differentChildren)
	
	val list2 = XmlElement("List", children = Vector(differentChildren, differentChildren, differentChildren))
	printElement(list2)
	
	val list2Wrapper = XmlElement("Wrapper", children = Vector(list2))
	printElement(list2Wrapper)
	
	val wrapperWrapper = XmlElement("Wrapper2", children = Vector(wrapper))
	printElement(wrapperWrapper)
}
