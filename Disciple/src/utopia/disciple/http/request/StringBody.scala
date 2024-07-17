package utopia.disciple.http.request

import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.URLEncodedUtils
import utopia.access.http.ContentCategory._
import utopia.access.http.ContentType
import utopia.flow.generic.model.template.{ModelLike, Property}

import java.io.ByteArrayInputStream
import java.nio.charset.{Charset, StandardCharsets}
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
	def urlEncodedForm(content: ModelLike[Property], charset: Charset = StandardCharsets.ISO_8859_1) = {
		// Produces the url-encoded string
		val parameters = content.properties.map { c => new BasicNameValuePair(c.name, c.value.getString) }
		// Wraps the string in a body
		/*
		val builder = new URIBuilder()
		content.attributes.foreach { c => builder.addParameter(c.name, c.value.getString) }
		builder.setCharset(charset)
		 */
		// TODO: Fix this (above implementation, although it didn't work)
		new StringBody(URLEncodedUtils.format(parameters.asJava, charset), charset, Application/"x-www-form-urlencoded")
	}
}

/**
* This request body is formed of a string
* @author Mikko Hilpinen
* @since 1.5.2018
**/
class StringBody(s: String, charset: Charset, cType: ContentType) extends Body
{
    private val bytes = s.getBytes(charset)
	override val contentType: ContentType = cType.withCharset(charset)
	
	def contentLength = Some(bytes.length)
    
    def repeatable = true
	def chunked = false
	def contentEncoding = None
	
	def stream = Success(new ByteArrayInputStream(bytes))
	
	override def toString = s"$s ($contentType)"
}