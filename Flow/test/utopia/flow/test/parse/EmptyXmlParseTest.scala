package utopia.flow.test.parse

import utopia.flow.parse.xml.XmlReader

import java.io.InputStream

/**
  * Attempts parsing XML from an empty stream
  * @author Mikko Hilpinen
  * @since 31.10.2024, v
  */
object EmptyXmlParseTest extends App
{
	val emptyStream = new InputStream {
		override def read() = -1
	}
	
	println(XmlReader.parseStream(emptyStream).get)
}
