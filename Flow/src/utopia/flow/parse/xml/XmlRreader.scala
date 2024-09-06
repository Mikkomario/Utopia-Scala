package utopia.flow.parse.xml

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.json.JsonReader

import java.io._
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.Path
import javax.xml.stream.XMLInputFactory
import scala.collection.immutable.VectorBuilder
import scala.util.{Failure, Success, Try}

object XmlReader
{
    // OPERATORS    ----------------
    
    /**
      * Creates a new open xml reader
      * @param stream Xml input stream
      * @param charset Encoding used in source (default = UTF-8)
      */
    def apply(stream: InputStream, charset: Charset = StandardCharsets.UTF_8) =
        new XmlReader(new InputStreamReader(stream, charset))
    
    
    // OTHER    --------------------
    
    /**
      * Reads the contents of a stream using a reader and a function. Please note that the reader will be closed after
      * this method completes
      * @param reader The reader used for reading xml data (will be closed)
      * @param contentReader the function that uses the reader to parse the stream contents
      * @return The data parsed from the stream (may fail)
      */
    def readWith[A](reader: Reader)(contentReader: XmlReader => Try[A]) =
        new XmlReader(reader).tryConsume(contentReader).flatten
    
    /**
     * Reads the contents of a stream using the specified reader function
     * @param stream the target stream (will be closed afterwards)
     * @param charset the charset of the stream contents (default = UTF 8)
     * @param contentReader the function that uses the reader to parse the stream contents
     * @return The data parsed from the stream (may fail)
     */
    def readStream[A](stream: InputStream, charset: Charset = StandardCharsets.UTF_8)(contentReader: XmlReader => Try[A]) =
        readWith(new InputStreamReader(stream, charset))(contentReader)
    
    /**
      * Reads the contents of an xml file using the specified reader function
      * @param file the target file
      * @param charset the charset of the file contents
      * @param contentReader the function that uses the reader to parse the file contents
      * @return The data parsed from the file (may fail)
      */
    def readFile[A](file: Path, charset: Charset = StandardCharsets.UTF_8)(contentReader: XmlReader => Try[A]) =
        Try(new FileInputStream(file.toFile).consume { readStream(_, charset)(contentReader) }).flatten
    
    /**
      * Parses reader contents into an xml element
      * @param reader A reader that reads xml data
      * @return The read xml element
      */
    def parseWith(reader: Reader) = readWith(reader) { xml =>
        val element = xml.readElement()
        if (element.isDefined) Success(element.get) else Failure(new NoSuchElementException())
    }
    
    /**
     * Parses the contents of a stream into an xml element
     * @param stream the target stream
     * @param charset the charset of the stream contents
     * @return The element parsed from the stream (may fail)
     */
    def parseStream(stream: InputStream, charset: Charset = StandardCharsets.UTF_8) =
        parseWith(new InputStreamReader(stream, charset))
    
    /**
      * Parses the contents of an xml file into an xml element
      * @param file the target file
      * @param charset the target charset (default = UTF-8)
      * @return The element parsed from the file (may fail)
      */
    def parseFile(file: Path, charset: Charset = StandardCharsets.UTF_8) =
        Try(new FileInputStream(file.toFile).consume { parseStream(_, charset) }).flatten
    
    /**
     * Parses an xml element from a string
     * @param xml A string representing an xml element
     * @return Xml element read from the string. May contain a failure.
     */
    def parseString(xml: String) = {
        val charset = StandardCharsets.UTF_8
        Try { new BufferedInputStream(new ByteArrayInputStream(xml.getBytes(charset))) }
            .flatMap { _.consume { parseStream(_, charset) } }
    }
    
    /**
      * Parses a value from a string
      */
    private[parse] def valueFromString(s: String) = JsonReader(s).toOption.getOrElse(s.toValue)
}

/**
 * XMLReaders can be used for parsing and traversing through an xml document. The reader supports 
 * both SAX and DOM approaches to xml parsing
 * @author Mikko Hilpinen
 * @since 24.1.2018
 */
// TODO: Consider adding a version of this class which reads a pre-parsed xml DOM element.
//  The benefits of sax parsing are then lost, but that would offer the same interface to both approaches
// TODO: This class needs a lot more utility functions anyway
class XmlReader(streamReader: Reader) extends AutoCloseable
{
    // ATTRIBUTES    ------------------------
    
    private val reader = XMLInputFactory.newInstance().createXMLStreamReader(streamReader)
    
    
    // INITIAL CODE    ----------------------
    
    // Moves the reader to the first element start
    _toNextElementStart()
    
    
    // COMPUTED PROPERTIES    ---------------
    
    /**
     * Whether the reader has reached the end of the document
     */
    def isAtDocumentEnd = !reader.hasNext
    
    /**
     * The name of the current element. None if at the end of the document
     */
    def currentElementName = if (isAtDocumentEnd) None else Some(reader.getLocalName)
    
    /**
     * The attributes in the current element in model format. An empty model if at the end of 
     * the document. Doesn't include namespace information.
     */
    def currentElementAttributes = {
        if (isAtDocumentEnd) 
            Model.empty 
        else 
            Model.withConstants(parseAttributes().map { case (attName, attValue) =>
                Constant(attName.local, XmlReader.valueFromString(attValue))
            })
    }
    
    private def currentEvent = 
    {
        if (reader.isStartElement)
            ElementStart
        else if (reader.isEndElement)
            ElementEnd
        else if (reader.isCharacters)
            Text
        else
            nextEvent()
    }
    
    
    // IMPLEMENTED METHODS    ---------------
    
    override def close() = reader.close()
    
    
    // OTHER METHODS    ---------------------
    
    /**
     * Parses the contents of a single xml element, including all its children. this reader is then 
     * moved to the next sibling element or higher
     * @return the parsed element
     */
    // TODO: Should expose _readElement() -kind of function, also
    def readElement() = if (isAtDocumentEnd) None else Some(_readElement()._1.toXmlElement)
    
    /**
     * Parses the contents of all remaining elements under the current parent element. this reader 
     * is then moved to the parent's next sibling or higher
     * @return the parsed elements
     */
    def readSiblings() = 
    {
        val elementsBuffer = new VectorBuilder[XmlElement]()
        var depth = 0
        
        while (depth >= 0 && !isAtDocumentEnd)
        {
            val result = _readElement()
            elementsBuffer += result._1.toXmlElement
            depth += result._2
        }
        
        elementsBuffer.result()
    }
    
    /**
     * Moves this reader to the next element (child, sibling, etc.)
     * @return how much the 'depth' of this reader changed in the process (1 for child, 
     * 0 for sibling, -1 for parent level and so on)
     */
    //noinspection AccessorLikeMethodIsEmptyParen
    def toNextElement() = _toNextElementStart()
    
    /**
     * Moves this reader to the next element (child, sibling, etc.) with a name that is accepted 
     * by the provided filter
     * @param nameFilter a filter that determines whether the name is accepted or not
     * @return how much the 'depth' of this reader changed in the process (1 for child, 
     * 0 for sibling, -1 for parent level and so on)
     */
    def toNextElementWithName(nameFilter: String => Boolean) = {
        var depthChange = toNextElement()
        while (currentElementName.exists(!nameFilter(_))) {
            depthChange += toNextElement()
        }
        depthChange
    }
    
    /**
     * Moves this reader to the next element (child, sibling, etc.) with the specified name
     * @param searchedName the name the targeted element must have (case-insensitive)
     * @return how much the 'depth' of this reader changed in the process (1 for child, 
     * 0 for sibling, -1 for parent level and so on)
     */
    //noinspection ConvertibleToMethodValue
    def toNextElementWithName(searchedName: String): Int =
        toNextElementWithName { searchedName.equalsIgnoreCase(_) }
    
    /**
     * Moves this reader to the next element with a name accepted by the provided filter. Limits the 
     * search to elements under the current element (children, grand children, etc.). If no such 
     * element is found, stops at the next sibling, parent or higher.
     * @param nameFilter the filter that defines whether an element name is accepted
     * @return Whether such a child element was found (if true, this reader is now at the searched element)
     */
    def toNextChildWithName(nameFilter: String => Boolean) = 
    {
        var depthChange = toNextElement()
        while (depthChange > 0 && currentElementName.exists(!nameFilter(_)))
        {
            depthChange += toNextElement()
        }
        depthChange > 0
    }
    
    /**
     * Moves this reader to the next element with the specified name. Limits the 
     * search to elements under the current element (children, grand children, etc.). If no such 
     * element is found, stops at the next sibling, parent or higher.
     * @param searchedName the name of the searched element (case-insensitive)
     * @return Whether such a child element was found (if true, this reader is now at the searched element)
     */
    //noinspection ConvertibleToMethodValue
    def toNextChildWithName(searchedName: String): Boolean = toNextChildWithName { 
            searchedName.equalsIgnoreCase(_) }
    
    /**
     * Moves this reader to the next sibling element that has a name that is accepted by the provided 
     * filter. If no such sibling is found, stops at the next parent level element or higher.
     * @param nameFilter the filter that determines whether an element name is accepted
     * @return Whether such a sibling was found (if true, this reader is now at the searched element)
     */
    def toNextSiblingWithName(nameFilter: String => Boolean) = 
    {
        var depthChange = skipElement()
        while (depthChange == 0 && currentElementName.exists(!nameFilter(_)))
        {
            depthChange += skipElement()
        }
        depthChange == 0
    }
    
    /**
     * Moves this reader to the next sibling element that has the specified name. If no such 
     * sibling is found, stops at the next parent level element or higher.
     * @param searchedName the name of the searched element (case-insensitive)
     * @return Whether such a sibling was found (if true, this reader is now at the searched element)
     */
    //noinspection ConvertibleToMethodValue
    def toNextSiblingWithName(searchedName: String): Boolean = toNextSiblingWithName { 
            searchedName.equalsIgnoreCase(_) }
    
    /**
     * Skips this element and moves to the next sibling, parent or higher
     * @return how much the 'depth' of this reader changed in the process 
     * (0 for sibling, -1 for parent level and so on)
     */
    def skipElement() = skip(0)
    
    /**
     * Skips this element as well as any siblings this element may have and moves to the parent's 
     * next sibling or higher
     * @return how much the 'depth' of this reader changed in the process (-1 for parent level, 
     * -2 for grandparent level and so on)
     */
    def skipParent() = skip(-1)
    
    private def skip(depthChangeRequirement: Int) = {
        var depth = _toNextElementStart()
        while (depth > depthChangeRequirement && !isAtDocumentEnd) {
            depth += _toNextElementStart()
        }
        depth
    }
    
    private def _readElement(): (UnfinishedElement, Int) = {
        val name = reader.getName
        val element = new UnfinishedElement(Namespace(name.getPrefix)(name.getLocalPart), parseAttributes())
        var depthChange = _toNextElementStart(Some(element))
        
        while (depthChange > 0) {
            val nextResult = _readElement()
            element.children :+= nextResult._1
            depthChange += nextResult._2
        }
        
        element -> depthChange
    }
    
    private def parseAttributes() = {
        // Reads attributes
        val attCount = reader.getAttributeCount
        val attributes = (0 until attCount).map { i =>
            val name = reader.getAttributeName(i)
            Namespace(name.getPrefix)(name.getLocalPart) -> reader.getAttributeValue(i)
        }.toMap
        // Also reads namespace declarations
        val namespaceCount = reader.getNamespaceCount
        val namespaces = (0 until namespaceCount).map { i =>
            Namespace.namespaceDeclaration(reader.getNamespacePrefix(i)) -> reader.getNamespaceURI(i)
        }.toMap
        attributes ++ namespaces
    }
    
    // Updates openElement text, returns the depth change
    private def _toNextElementStart(openElement: Option[UnfinishedElement] = None): Int = {
        nextEvent() match {
            case ElementStart =>
                // Text before element start is discarded
                openElement.filter { !_.completed }.foreach { _.text = "" }
                1
            case ElementEnd =>
                openElement.foreach { _.completed = true }
                _toNextElementStart(openElement) - 1
            case Text =>
                // Text after element end is discarded
                openElement.filter { !_.completed }.foreach { _.text += reader.getText }
                _toNextElementStart(openElement)
            case DocumentEnd => 0
        }
    }
    
    private def nextEvent(): XmlReadEvent = {
        if (reader.hasNext) {
            reader.next()
            currentEvent
        }
        else
            DocumentEnd
    }
}

private sealed trait XmlReadEvent
private case object ElementStart extends XmlReadEvent
private case object ElementEnd extends XmlReadEvent
private case object Text extends XmlReadEvent
private case object DocumentEnd extends XmlReadEvent

private class UnfinishedElement(name: NamespacedString, attributes: Map[NamespacedString, String])
{
    // ATTRIBUTES    -----------------------
    
    var children: Seq[UnfinishedElement] = Empty
    var text = ""
    var completed = false
    
    
    // COMPUTED PROPERTIES    --------------
    
    def toXmlElement: XmlElement = {
        val attMap = attributes.groupMap { _._1.namespace } { case (name, str) =>
            Constant(name.local, XmlReader.valueFromString(str))
        }.view.mapValues { Model.withConstants(_) }.toMap
        new XmlElement(name, if (text.isEmpty) Value.emptyWithType(StringType) else
                XmlReader.valueFromString(text), attMap, children.map(_.toXmlElement))
    }
}