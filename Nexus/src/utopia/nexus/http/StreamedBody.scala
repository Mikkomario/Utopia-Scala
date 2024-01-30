package utopia.nexus.http

import utopia.access.http.ContentCategory._
import utopia.access.http.{ContentType, Headers}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.error.DataTypeException
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.parse.xml.XmlReader

import java.io.{BufferedReader, OutputStream}
import scala.language.postfixOps
import scala.util.{Success, Try}

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
    def buffered[T](f: BufferedReader => T) =
        BufferedBody(f(reader), contentType, contentLength, headers, name)
    
    /**
      * @return A buffered version of this body where the stream is read into a string
      */
    def bufferedToString = buffered { reader =>
        Try { Iterator.continually(reader.readLine()).takeWhile(_ != null).mkString("\n") } }
    
    /**
      * @return A buffered version of this body where contents are parsed from a JSON into a value
      */
    def bufferedJson(implicit parser: JsonParser) =
        bufferedToString.map { _.flatMap { parser(_) } }
    
    /**
      * @return A buffered version of this body where contents are parsed from a JSON into a value
      */
    def bufferedJsonObject(implicit parser: JsonParser) = bufferedToString.map {
        _.flatMap { parser(_).flatMap { v =>
            if (v.isEmpty)
                Success(Model.empty)
            else
                v.model.toTry { DataTypeException(s"${v.description} can't be converted to model") }
        } } }
    
    /**
      * @return A buffered version of this body where contents are parsed from a JSON array into a vector of values
      */
    def bufferedJsonArray =
        bufferedToString.map { _.flatMap { JsonReader(_) }.flatMap { v =>
            if (v.isEmpty)
                Success(Vector())
            else
                v.vector.toTry { DataTypeException(s"${v.description} can't be converted to a vector") }
        } }
    
    /**
      * @return A buffered version of this body where contents are parsed into an xml element
      */
    def bufferedXml = buffered { XmlReader.parseWith(_) }
    
    /**
     * Writes the contents of this body into an output stream.
      * Best performance is achieved when the output stream is buffered.
     */
    def writeTo(output: OutputStream) = {
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
    def writeTo(path: java.nio.file.Path): Try[java.nio.file.Path] =
        path.writeWith { stream => writeTo(stream) }.map { _ => path }
}