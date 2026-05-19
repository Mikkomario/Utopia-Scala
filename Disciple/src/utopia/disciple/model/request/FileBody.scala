package utopia.disciple.model.request

import utopia.access.model.ContentType
import utopia.flow.parse.AutoClose._
import utopia.flow.parse.StreamExtensions._

import java.io.{File, FileInputStream, OutputStream}
import scala.util.Try

/**
* This request body contains file contents
* @author Mikko Hilpinen
* @since 1.5.2018
**/
class FileBody(file: File, override val contentType: ContentType, override val contentEncoding: Option[String] = None)
	extends RequestBody
{
	// ATTRIBUTES   ---------------------
	
	private val len = file.length()
	
	override val repeatable = true
	override val streaming: Boolean = true
	
	
	// IMPLEMENTED  --------------------
	
	override def contentLength = Some(len)
	
	override def toString = s"${file.getName} ($contentType)"
	
	override def writeTo(output: OutputStream): Try[Unit] = Try {
		new FileInputStream(file).consume { _.writeTo(output) }
	}
	
	override def close(): Unit = ()
}