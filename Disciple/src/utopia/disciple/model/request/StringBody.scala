package utopia.disciple.model.request

import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.WWWFormCodec
import utopia.access.model.ContentType
import utopia.access.model.enumeration.ContentCategory._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

import java.io.OutputStream
import java.nio.charset.{Charset, StandardCharsets}
import scala.jdk.CollectionConverters._
import scala.util.Try

object StringBody
{
    /**
     * Creates a plain text string body
     */
    def plainText(text: String, charset: Charset) = apply(text, Text.plain, charset)
    /**
     * Creates a JSON string body
     */
    def json(json: String) = apply(json, Application.json, StandardCharsets.UTF_8)
    /**
     * Creates an XML string body
     */
    def xml(xml: String) = apply(xml, Application.xml, StandardCharsets.UTF_8)
	
	/**
	  * Creates a urlencoded www form based on specified parameters
	  * @param content Model that contains the form fields. Parameter values are read as strings.
	  * @param charset Charset to use (default = http client default = ISO-8859-1)
	  * @return A string body wrapping the content as a url-encoded form
	  */
	def urlEncodedForm(content: HasProperties, charset: Charset = StandardCharsets.ISO_8859_1) = {
		// Wraps the string in a body
		StringBody(
			WWWFormCodec.format(content.propertiesIterator
				.map { prop => new BasicNameValuePair(prop.name, prop.value.getString) }.toOptimizedSeq.asJava, charset),
			Application/"x-www-form-urlencoded", charset)
	}
	
	/**
	 * @param string String to write
	 * @param contentType Applicable content type, without the character set specified
	 * @param charset Character set to use
	 * @return A new string request body
	 */
	def apply(string: String, contentType: ContentType, charset: Charset) =
		new StringBody(string, contentType.withCharset(charset), charset)
}

/**
* A 100% buffered, String-backed request body implementation
* @author Mikko Hilpinen
* @since 1.5.2018
**/
class StringBody private(string: String, override val contentType: ContentType, charset: Charset) extends RequestBody
{
	// ATTRIBUTES   -----------------------
	
    private val bytes = string.getBytes(charset)
	private val len = bytes.length
	override val contentEncoding = None
	
    override val repeatable = true
	override val streaming: Boolean = false
	
	
	// IMPLEMENTED  ---------------------
	
	override def contentLength = Some(len)
	
	override def toString = s"$string ($contentType)"
	
	override def writeTo(output: OutputStream): Try[Unit] = Try { output.write(bytes, 0, len) }
	override def close(): Unit = ()
}