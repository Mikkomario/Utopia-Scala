package utopia.courier.controller.read

import java.io.InputStream
import scala.util.Try

/**
  * Used for building a new email or other object from read message data
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  * @tparam A Type of content produced by this builder
  */
trait FromEmailBuilder[+A]
{
	/**
	  * Appends (html) text to the message body
	  * @param message Message read from the mail
	  * @return Success or failure
	  */
	def append(message: String): Try[Unit]
	
	/**
	  * Appends stream content to the message body
	  * @param stream Stream to read
	  * @return Success or failure
	  */
	def appendFrom(stream: InputStream): Try[Unit]
	
	/**
	  * Includes an attachment
	  * @param attachmentName Name of the attachment (may be empty)
	  * @param stream         Attachment contents as a stream
	  * @return Success or failure
	  */
	def attachFrom(attachmentName: String, stream: InputStream): Try[Unit]
	
	/**
	  * Builds a new item, clearing this builder.
	  * @return The built content. May be a failure.
	  */
	def result(): Try[A]
}
