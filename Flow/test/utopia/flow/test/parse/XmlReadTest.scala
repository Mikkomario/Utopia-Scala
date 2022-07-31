package utopia.flow.test.parse

import utopia.flow.generic.DataType
import utopia.flow.parse.XmlReader
import utopia.flow.util.FileExtensions._

/**
  * Tests xml reading / parsing
  * @author Mikko Hilpinen
  * @since 31.7.2022, v1.16
  */
object XmlReadTest extends App
{
	DataType.setup()
	
	val xml = XmlReader.parseFile("Flow/data/test-data/example.xml").get
	println(xml.toXml)
}
