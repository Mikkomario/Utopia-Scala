package utopia.disciple.http.request

import java.io.{File, FileInputStream}
import java.nio.charset.Charset

import utopia.access.http.ContentType

import scala.util.Try

/**
* This request body contains file contents
* @author Mikko Hilpinen
* @since 1.5.2018
**/
class FileBody(val file: File, val contentType: ContentType, val charset: Option[Charset] = None, 
        val contentEncoding: Option[String] = None) extends Body
{
	override def repeatable = true
	override def chunked = false
	
    override def contentLength = Some(file.length())
    override def stream = Try(new FileInputStream(file))
	
	override def toString = s"${file.getName} ($contentType)"
}