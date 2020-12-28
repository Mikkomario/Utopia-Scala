package utopia.flow.test

import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.DataType
import utopia.flow.parse.XmlElement
import utopia.flow.datastructure.immutable.Model

import utopia.flow.parse.XmlWriter
import utopia.flow.parse.XmlReader
import utopia.flow.util.FileExtensions._

import java.nio.file.Paths
import scala.util.Try

/**
 * This app tests xml writing and reading functions
 * @author Mikko Hilpinen
 * @since 17.1.2018 (v1.3)
 */
object XmlTest extends App
{
    DataType.setup()
    
    // Creates the xml elements
    val grandChild1 = new XmlElement("c", "Test & Values", Model(Vector("att1" -> 1, "att2" -> "b")))
    val grandChild2 = new XmlElement("d", 123456)
    val grandChild3 = new XmlElement("e")
    
    val child = new XmlElement(name = "b", children = Vector(grandChild1, grandChild2, grandChild3))
    val root = new XmlElement(name = "a", attributes = Model(Vector("id" -> 34)), children = Vector(child))
    
    // Tests some basic XmlElement methods
    assert(root.childWithName("b").contains(child))
    assert(root/"b"/"d" == grandChild2)
    
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
        Try
        {
            reader.toNextChildWithName("c")
            reader.readElement().get
        }
    }
    
    println(parsed3.get.toXml)
    println(parsed3.get.toJson)
    
    assert(parsed3.get == grandChild1)
    
    println("Success!")
}