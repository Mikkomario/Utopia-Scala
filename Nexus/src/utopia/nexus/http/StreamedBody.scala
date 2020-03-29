package utopia.nexus.http

import scala.language.postfixOps
import utopia.access.http.ContentCategory._
import utopia.flow.util.AutoClose._
import utopia.flow.util.FileExtensions._
import java.io.{BufferedReader, File, FileOutputStream, OutputStream}

import utopia.access.http.ContentType
import utopia.access.http.Headers

import scala.util.Try
import utopia.flow.parse.{JSONReader, XmlReader}

/**
* This class represents a body send along with a request. These bodies can only be read once.
* @author Mikko Hilpinen
* @since 12.5.2018
**/
class StreamedBody(val reader: BufferedReader, val contentType: ContentType = Text.plain, 
        val contentLength: Option[Long] = None, val headers: Headers = Headers.currentDateHeaders,
        val name: Option[String] = None) extends Body
{
    // OTHER METHODS    --------------------
    
    /**
      * @param f A function for handling stream contents
      * @tparam T The type of buffered result
      * @return A buffered body from the parsing results
      */
    def buffered[T](f: BufferedReader => T) = BufferedBody(f(reader), contentType, contentLength, headers, name)
    
    /**
      * @return A buffered version of this body where the stream is read into a string
      */
    def bufferedToString = buffered { reader =>
        Try(Stream.continually(reader.readLine()).takeWhile(_ != null).mkString("\n")) }
    
    /**
      * @return A buffered version of this body where contents are parsed from a JSON into a value
      */
    def bufferedJSON = bufferedToString.map { _.flatMap { JSONReader(_) } }
    
    /**
     * @return A buffered version of this body where contents are parsed from a JSON into a value
     */
    def bufferedJSONModel = bufferedToString.map { _.flatMap { JSONReader(_).map { _.getModel } } }
    
    /**
      * @return A buffered version of this body where contents are parsed from a JSON array into a vector of values
      */
    def bufferedJSONArray = bufferedToString.map { _.flatMap { JSONReader(_) }.map { _.getVector } }
    
    /**
      * @return A buffered version of this body where contents are parsed into an xml element
      */
    def bufferedXml = buffered { XmlReader.parseWith(_) }
    
    /**
     * Writes the contents of this body into an output stream. Best performance is
     * achieved if the output stream is buffered.
     */
    def writeTo(output: OutputStream) =
    {
        // See: https://stackoverflow.com/questions/6927873/
        // how-can-i-read-a-file-to-an-inputstream-then-write-it-into-an-outputstream-in-sc
          reader.tryConsume(r => Iterator
                  .continually (r.read)
                  .takeWhile (-1 !=)
                  .foreach (output.write))
    }
    
    /**
     * Writes the contents of this body to a file
     * @param path Path where to write the stream contents
     * @return Provided path. Failure if writing failed.
     */
    def writeTo(path: java.nio.file.Path): Try[java.nio.file.Path] = path.writeWith { stream => writeTo(stream) }
    
    /**
     * Writes the contents of this body into a file
     */
    @deprecated("Please use writeTo(Path) instead", "v1.3")
    def writeToFile(file: File) = new FileOutputStream(file).consume(writeTo)
}