package utopia.disciple.http

import utopia.access.http.ContentCategory._

import utopia.access.http.ContentType
import java.nio.charset.Charset
import java.io.ByteArrayInputStream
import scala.util.Success
import java.nio.charset.StandardCharsets

object StringBody
{
    /**
     * Creates a plain text string body
     */
    def plainText(s: String, charset: Charset) = new StringBody(s, charset, Text/"plain")
    /**
     * Creates a json string body
     */
    def json(s: String) = new StringBody(s, StandardCharsets.UTF_8, Application/"json")
    /**
     * Creates an xml string body
     */
    def xml(s: String) = new StringBody(s, StandardCharsets.UTF_8, Application/"xml")
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