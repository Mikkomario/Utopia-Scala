package utopia.flow.parse.xml

import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._

import java.io.{File, FileOutputStream, OutputStream, OutputStreamWriter}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Path
import javax.xml.stream.{XMLOutputFactory, XMLStreamException}
import scala.util.Try

object XmlWriter
{
    // ATTRIBUTES    -------------------
    
    // Characters not accepted in the xml text (http://validchar.com/d/xml10/xml10_namestart [17.1.2018])
    // Ordered
    private val invalidCharRanges = Vector(/*(0 to 64), */91 to 94, 123 to 191, 768 to 879,
            8192 to 8203, 8206 to 8303, 8592 to 11263, 12272 to 12288, 55296 to 63743,
            64976 to 65007, 65534 to 1114111)
    private val invalidExtraChars = Vector(34, 38, 39, 60, 62, 96, 215, 247, 894)
    
    
    // OTHER METHODS    ----------------
    
    /**
      * Writes an xml document to the target stream
      * @param stream the targeted stream
      * @param charset the used charset (default = UTF-8)
      * @param contentWriter the function that writes the document contents
      * @return The results of the operation
      */
    def writeToStream[U](stream: OutputStream, charset: Charset = StandardCharsets.UTF_8)(contentWriter: XmlWriter => U) =
        new XmlWriter(stream, charset).tryConsume { writer => writer.writeDocument { contentWriter(writer) } }
    
    /**
     * Writes an xml document to the target stream
     * @param stream the targeted stream
     * @param element The root element that is written to the document
     * @param charset the charset to use (default = UTF-8)
     * @return The results of the operation
     */
    def writeElementToStream(stream: OutputStream, element: XmlElement, charset: Charset = StandardCharsets.UTF_8) =
        writeToStream(stream, charset) { _.write(element) }
    
    /**
      * Writes an xml document to the target file
      * @param file the targeted file
      * @param charset the used charset (default = UTF-8)
      * @param contentWriter the function that writes the document contents
      * @return The results of the operation
      */
    def writeFile(file: Path, charset: Charset = StandardCharsets.UTF_8)(contentWriter: XmlWriter => Unit) =
    {
        // Makes sure the target directory exists
        file.createParentDirectories().flatMap { file =>
            Try {
                new FileOutputStream(file.toFile, false).consume { writeToStream(_, charset)(contentWriter) }
            }.flatten
        }
    }
    
    /**
      * Writes an xml document to the target file
      * @param file the targeted file
      * @param element The root element that is written to the document
      * @param charset the used charset (default = UTF-8)
      * @return The results of the operation
      */
    def writeElementToFile(file: Path, element: XmlElement, charset: Charset = StandardCharsets.UTF_8) =
        writeFile(file, charset) { _.write(element) }
}

/**
 * This writer is used for writing xml documents
 * @author Mikko Hilpinen
 * @since 17.1.2018 (v1.3)
 */
class XmlWriter(stream: OutputStream, val charset: Charset = StandardCharsets.UTF_8) extends AutoCloseable
{
    // ATTRIBUTES    --------------------------
    
    private val writer = XMLOutputFactory.newInstance()
        .createXMLStreamWriter(new OutputStreamWriter(stream, charset))
    
    
    // IMPLEMENTED METHODS    -----------------
    
    @throws(classOf[XMLStreamException])
    override def close() = {
        writer.flush()
        writer.close()
    }
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * Writes a complete xml document
     * @param contentWriter a function that is used for writing the contents of the document
     */
    def writeDocument[U](contentWriter: => U) = {
        writer.writeStartDocument(charset.name(), "1.0")
        contentWriter
        writer.writeEndDocument()
    }
    
    /**
     * Writes an element with content. Closes the element afterwards
     * @param elementName the name of the element
     * @param attributes the attributes written to element (optional)
     * @param text the text written to element (optional)
     * @param contentWriter the function that is used for writing the element contents
     */
    def writeElement(elementName: NamespacedString, attributes: IterableOnce[(NamespacedString, String)] = Map(),
            text: String = "")(contentWriter: => Unit = ()) =
    {
        // Writes element start, attributes & text
        writer.writeStartElement(elementName.toString)
        attributes.iterator.foreach { case (key, value) => writer.writeAttribute(key.toString, value) }
        writeCharacters(text)
        // Writes other content
        contentWriter
        // finally closes the element
        writer.writeEndElement()
    }
    
    /**
     * Writes a simple element with only text data
     * @param elementName the name of the element
     * @param text the text written inside the element
     */
    def writeTextElement(elementName: NamespacedString, text: String) = {
        writer.writeStartElement(elementName.toString)
        writer.writeCharacters(text)
        writer.writeEndElement()
    }
    
    /**
     * Writes an element with only attribute data
     * @param elementName the name of the element
     * @param attributes the attributes written to the element (optional)
     */
    def writeEmptyElement(elementName: NamespacedString, attributes: Map[NamespacedString, String] = Map()) = {
        writer.writeStartElement(elementName.toString)
        attributes.foreach{ case (key, value) => writer.writeAttribute(key.toString, value) }
        writer.writeEndElement()
    }
    
    /**
     * Writes a complete xml element tree to the document
     * @param element the element tree that is written
     */
    def write(element: XmlElement): Unit = {
        val attributes = element.attributeMap.flatMap { case (namespace, model) =>
            model.properties.map { c => namespace(c.name) -> c.value.getString }
        }
        writeElement(element.name, attributes, element.text) { element.children.foreach(write) }
    }
    
    private def writeCharacters(text: String) = {
        if (text.nonEmpty) {
            // CDATA is used if necessary
            if (text.exists(charIsIllegal))
                writer.writeCData(text)
            else
                writer.writeCharacters(text)
        }
    }
    
    private def charIsIllegal(c: Char) = {
        val i = c.toInt
        // Character is not allowed if it lies in an invalid char range or is specifically invalid
        XmlWriter.invalidCharRanges.find { _.end >= i }.exists { _.start <= i } ||
                XmlWriter.invalidExtraChars.contains(i)
    }
}