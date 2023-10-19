package utopia.courier.controller.read

import java.io.InputStream
import scala.util.Try

object FromEmailBuilder
{
	// OTHER    ------------------------
	
	/**
	 * Wraps another email builder, mapping its result type
	 * @param other Another email builder
	 * @param f A mapping function to apply
	 * @tparam A Type of original email type
	 * @tparam B Type of mapped type
	 * @return A builder that produces mapped values
	 */
	def mapping[A, B](other: FromEmailBuilder[A])(f: A => B): FromEmailBuilder[B] = new MappingBuilder[A, B](other, f)
	
	
	// NESTED   ------------------------
	
	private class MappingBuilder[-A, +B](wrapped: FromEmailBuilder[A], f: A => B) extends FromEmailBuilder[B]
	{
		override def append(message: String): Try[Unit] = wrapped.append(message)
		override def appendFrom(stream: InputStream): Try[Unit] = wrapped.appendFrom(stream)
		override def attachFrom(attachmentName: String, stream: InputStream): Try[Unit] =
			wrapped.attachFrom(attachmentName, stream)
		
		override def result(): Try[B] = wrapped.result().map(f)
	}
}

/**
  * Used for building a new email or other object from read message data
  * @author Mikko Hilpinen
  * @since 11.9.2021, v0.1
  * @tparam A Type of content produced by this builder
  */
trait FromEmailBuilder[+A]
{
	// ABSTRACT -------------------------
	
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
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param f A mapping function applied to the results of this builder
	 * @tparam B Type of mapping results
	 * @return A new email builder that wraps this one and maps the results using the specified function
	 */
	def mapResult[B](f: A => B) = FromEmailBuilder.mapping(this)(f)
}
