package utopia.flow.test.parse

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.DataType
import utopia.flow.generic.ValueConversions._
import utopia.flow.parse.{Namespace, XmlElement}

/**
 * Tests Xml to string conversion
 * @author Mikko Hilpinen
 * @since 15.12.2020, v1.9
 */
object XmlToStringTest extends App
{
	DataType.setup()
	
	implicit val ns: Namespace = Namespace("test")
	
	val elem1 = XmlElement("CamelCaseName", "CamelCaseValue")
	val elem2 = XmlElement.namespaced("ALLCAPSNAME",
		attributes = Model(Vector("CamelCaseAttribute" -> "CamelCaseAttValue")))
	val elem3 = XmlElement("Parent", children = Vector(elem1, elem2))
	
	val xml1 = elem1.toXml
	val xml2 = elem2.toXml
	val xml3 = elem3.toXml
	
	println(xml1)
	assert(xml1.contains("<test:CamelCaseName>"))
	assert(xml1.contains("CamelCaseValue"))
	
	println(xml2)
	assert(xml2.contains("test:ALLCAPSNAME"))
	assert(xml2.contains("CamelCaseAttribute"))
	assert(xml2.contains("CamelCaseAttValue"))
	
	println(xml3)
	assert(xml3.contains("<test:CamelCaseName>"))
	assert(xml3.contains("CamelCaseValue"))
	assert(xml3.contains("ALLCAPSNAME"))
	assert(xml3.contains("CamelCaseAttribute"))
	assert(xml3.contains("CamelCaseAttValue"))
	
	println("Success!")
}
