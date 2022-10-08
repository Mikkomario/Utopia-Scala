package utopia.flow.test.parse

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.parse.xml.{XmlElement, XmlReader, XmlWriter}
import utopia.flow.parse.file.FileExtensions._

import java.nio.file.Paths
import scala.util.Try

/**
 * This app tests xml writing and reading functions
 * @author Mikko Hilpinen
 * @since 17.1.2018 (v1.3)
 */
object XmlTest extends App
{
	
	
	// Creates the xml elements
	val grandChild1 = XmlElement.local("c", "Test & Values", Model(Vector("att1" -> 1, "att2" -> "b")))
	val grandChild2 = XmlElement.local("d", 123456)
	val grandChild3 = XmlElement.local("e")
	
	val child = XmlElement.local(name = "b", children = Vector(grandChild1, grandChild2, grandChild3))
	val root = XmlElement.local(name = "a", attributes = Model(Vector("id" -> 34)), children = Vector(child))
	
	// Tests some basic XmlElement methods
	assert(root.childWithName("b").contains(child))
	assert(root / "b" / "d" == grandChild2)
	
	// Test prints
	println(root.toXml)
	println(root.toJson)
	
	val parsed = XmlElement(root.toModel)
	
	println(parsed.get.toXml)
	println(parsed.get.toJson)
	
	// Makes sure model parsing works for xml elements
	assert(parsed.get == root)
	
	// Tries to write the xml data to a file
	val testFile = Paths.get("test/XmlTest.xml")
	testFile.createParentDirectories()
	assert(XmlWriter.writeElementToFile(testFile, root).isSuccess)
	
	// Parses the contents of the xml file (dom)
	val parsed2 = XmlReader.parseFile(testFile)
	
	println(parsed2.get.toXml)
	println(parsed2.get.toJson)
	
	assert(parsed2.get == root)
	
	// Parses an element from the xml file (sax)
	val parsed3 = XmlReader.readFile(file = testFile) { reader =>
		Try {
			reader.toNextChildWithName("c")
			reader.readElement().get
		}
	}
	
	println(parsed3.get.toXml)
	println(parsed3.get.toJson)
	
	assert(parsed3.get == grandChild1)
	
	println("Success!")
}
