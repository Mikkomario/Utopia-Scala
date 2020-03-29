package utopia.flow.parse

import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.xml.stream.XMLOutputFactory
import java.io.OutputStreamWriter
import scala.util.Try
import javax.xml.stream.XMLStreamException
import java.io.IOException
import scala.collection.immutable.HashMap
import java.io.File
import java.io.FileOutputStream
import scala.util.Success
import scala.util.Failure
import scala.collection.immutable.HashSet

object XmlWriter
{
    // ATTRIBUTES    -------------------
    
    // Characters not accepted in the xml text (http://validchar.com/d/xml10/xml10_namestart [17.1.2018])
    // Ordered
    private val invalidCharRanges = Vector(/*(0 to 64), */(91 to 94), (123 to 191), (768 to 879), 
            (8192 to 8203), (8206 to 8303), (8592 to 11263), (12272 to 12288), (55296 to 63743), 
            (64976 to 65007), (65534 to 1114111));
    private val invalidExtraChars = Vector(34, 38, 39, 60, 62, 96, 215, 247, 894)
    
    
    // OTHER METHODS    ----------------
    
    /**
     * Writes an xml document to the target stream
     * @param stream the targeted stream
     * @param charset the used charset (default = UTF-8)
     * @param contentWriter the function that writes the document contents
     * @return The results of the operation
     */
    def writeToStream(stream: OutputStream, charset: Charset = StandardCharsets.UTF_8, 
            contentWriter: XmlWriter => Unit) = 
    {
        def write = 
        {
            val writer = new XmlWriter(stream, charset)
            try
            {
                writer.writeDocument(() => contentWriter(writer))
            }
            finally
            {
                writer.close()
            }
        }
        
        Try(write)
    }
    
    /**
     * Writes an xml document to the target stream
     * @param stream the targeted stream
     * @param element The root element that is written to the document
     * @param charset the charset to use (default = UTF-8)
     * @return The results of the operation
     */
    def writeElementToStream(stream: OutputStream, element: XmlElement, 
            charset: Charset = StandardCharsets.UTF_8) = writeToStream(stream, charset, 
            w => w.write(element));
    
    /**
     * Writes an xml document to the target file
     * @param file the targeted file
     * @param charset the used charset (default = UTF-8)
     * @param contentWriter the function that writes the document contents
     * @return The results of the operation
     */
    def writeFile(file: File, charset: Charset = StandardCharsets.UTF_8, contentWriter: XmlWriter => Unit) = 
    {
        val stream = Try(new FileOutputStream(file, false))
        stream match 
        {
            case Success(stream) => try { 
                    writeToStream(stream, charset, contentWriter) } finally { stream.close() }
            case Failure(ex) => Failure(ex)
        }
    }
    
    /**
     * Writes an xml document to the target file
     * @param file the targeted file
     * @param element The root element that is written to the document
     * @param charset the used charset (default = UTF-8)
     * @return The results of the operation
     */
    def writeElementToFile(file: File, element: XmlElement, 
            charset: Charset = StandardCharsets.UTF_8) = writeFile(file, charset, w => w.write(element));
}

/**
 * This writer is used for writing xml documents
 * @author Mikko Hilpinen
 * @since 17.1.2018 (v1.3)
 */
@throws(classOf[XMLStreamException])
class XmlWriter(stream: OutputStream, val charset: Charset = StandardCharsets.UTF_8) extends AutoCloseable
{
    // ATTRIBUTES    --------------------------
    
    private val writer = XMLOutputFactory.newInstance().createXMLStreamWriter(
            new OutputStreamWriter(stream, charset));
    
    
    // IMPLEMENTED METHODS    -----------------
    
    @throws(classOf[XMLStreamException])
    override def close() = 
    {
        writer.flush()
        writer.close()
    }
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * Writes a complete xml document
     * @param contentWriter a function that is used for writing the contents of the document
     */
    def writeDocument(contentWriter: () => Unit) = 
    {
        writer.writeStartDocument(charset.name(), "1.0")
        contentWriter()
        writer.writeEndDocument()
    }
    
    /**
     * Writes an element with content. Closes the element afterwards
     * @param elementName the name of the element
     * @param attributes the attributes written to element (optional)
     * @param text the text written to element (optional)
     * @param contentWriter the function that is used for writing the element contents
     */
    def writeElement(elementName: String, attributes: Map[String, String] = HashMap(), 
            text: Option[String] = None, contentWriter: () => Unit = () => Unit) = 
    {
        // Writes element start, attributes & text
        writer.writeStartElement(elementName)
        attributes.foreach{ case (key, value) => writer.writeAttribute(key, value) }
        text.foreach(writeCharacters)
        // Writes other content
        contentWriter()
        // finally closes the element
        writer.writeEndElement()
    }
    
    /**
     * Writes a simple element with only text data
     * @param elementName the name of the element
     * @param text the text written inside the element
     */
    def writeTextElement(elementName: String, text: String) = 
    {
        writer.writeStartElement(elementName)
        writer.writeCharacters(text)
        writer.writeEndElement()
    }
    
    /**
     * Writes an element with only attribute data
     * @param elementName the name of the element
     * @param attributes the attributes written to the element (optional)
     */
    def writeEmptyElement(elementName: String, attributes: Map[String, String] = HashMap()) = 
    {
        writer.writeStartElement(elementName)
        attributes.foreach{ case (key, value) => writer.writeAttribute(key, value) }
        writer.writeEndElement()
    }
    
    /**
     * Writes a complete xml element tree to the document
     * @param element the element tree that is written
     */
    def write(element: XmlElement): Unit = writeElement(element.name, 
            element.attributes.attributeMap.mapValues(a => a.value.stringOr()), element.text, 
            () => element.children.foreach(write));
    
    private def writeCharacters(text: String) = 
    {
        // CDATA is used if necessary
        if (text.exists(charIsIllegal))
            writer.writeCData(text) 
        else 
            writer.writeCharacters(text)
    }
    
    private def charIsIllegal(c: Char) = 
    {
        val i = c.toInt
        // Character is not allowed if it lies in an invalid char range or is specifically invalid
        XmlWriter.invalidCharRanges.find(_.end >= i).exists(_.start <= i) || 
                XmlWriter.invalidExtraChars.contains(i)
    }
}