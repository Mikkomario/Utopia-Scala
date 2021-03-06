package utopia.disciple.http.request

import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HTTP

import java.io.ByteArrayInputStream
import java.nio.charset.{Charset, StandardCharsets}
import utopia.access.http.ContentCategory._
import utopia.access.http.ContentType
import utopia.flow.datastructure.template.{Model, Property}

import scala.jdk.CollectionConverters._
import scala.util.Success

object StringBody
{
    /**
     * Creates a plain text string body
     */
    def plainText(s: String, charset: Charset) = new StringBody(s, charset, Text.plain)
    /**
     * Creates a json string body
     */
    def json(s: String) = new StringBody(s, StandardCharsets.UTF_8, Application.json)
    /**
     * Creates an xml string body
     */
    def xml(s: String) = new StringBody(s, StandardCharsets.UTF_8, Application.xml)
	
	/**
	  * Creates a urlencoded www form based on specified parameters
	  * @param content Model that contains the form fields. Parameter values are read as strings.
	  * @param charset Charset to use (default = http client default = ISO-8859-1)
	  * @return A string body wrapping the content as a url-encoded form
	  */
	def urlEncodedForm(content: Model[Property], charset: Charset = HTTP.DEF_CONTENT_CHARSET) =
	{
		// Produces the url-encoded string
		val parameters = content.attributes.map { c => new BasicNameValuePair(c.name, c.value.getString) }
		// Wraps the string in a body
		new StringBody(URLEncodedUtils.format(parameters.asJava, charset), charset, Application/"x-www-form-urlencoded")
	}
}

/**
* This request body is formed of a string
* @author Mikko Hilpinen
* @since 1.5.2018
**/
class StringBody(s: String, cset: Charset, val contentType: ContentType) extends Body
{
    val charset = Some(cset)
	
    private val bytes = s.getBytes(cset)
	
    def contentLength = Some(bytes.length)
    
    def repeatable = true
	def chunked = false
	def contentEncoding = None
	
	def stream = Success(new ByteArrayInputStream(bytes))
	
	override def toString = s"$s ($contentType)"
}