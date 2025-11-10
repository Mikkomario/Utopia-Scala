package utopia.nexus.controller.write

import utopia.access.model.ContentType
import utopia.access.model.enumeration.ContentCategory.{Application, Text}
import utopia.flow.async.TryFuture
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.StreamExtensions._
import utopia.flow.parse.json.JsonConvertible
import utopia.flow.parse.xml.XmlElement

import java.io.{OutputStream, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.util.Try

object WriteResponseBody
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * Character encoding applied by default
	 */
	private implicit val codec: Codec = Codec.UTF8
	private lazy val utf8Text = Text.plain.withCharset(StandardCharsets.UTF_8)
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Access to streaming response-writing constructors
	 */
	def stream = Stream
	
	
	// OTHER    -------------------------
	
	/**
	 * @param json JSON content to write
	 * @return The specified content as a buffered JSON response body
	 */
	def json(json: JsonConvertible): WriteResponseBody = this.json(json.toJson)
	/**
	 * @param json JSON to write
	 * @return The specified JSON as a buffered response body
	 */
	def json(json: String) = string(json, Application.json)
	// TODO: Add support for NDJSON
	/**
	 * @param content JSON content to write
	 * @param exc Implicit execution context
	 * @return The specified JSON content as a JSON array. May be a streamed / chunked response body.
	 */
	def jsonArray(content: IterableOnce[JsonConvertible])(implicit exc: ExecutionContext) = {
		val kn = content.knownSize
		// Case: Empty content => Writes a fixed string
		if (kn == 0)
			json("[]")
		// Case: One item only => Buffers it regardless of collection type
		else if (kn == 1)
			json(s"[${ content.iterator.next().toJson }]")
		else
			content match {
				// Case: Lazily cached collection that's partially uncached
				//       => Writes the cached portion at once, and the others one item at a time
				case c: CachingSeq[JsonConvertible] if !c.isFullyCached =>
					stream.usingWriter(Application.json) { writer =>
						writer.print('[')
						val prebuffered = c.current
						var nextIsFirst = true
						
						def write(value: JsonConvertible) = {
							if (nextIsFirst)
								nextIsFirst = false
							else
								writer.print(", ")
							writer.print(value.toJson)
						}
						
						prebuffered.foreach(write)
						writer.flush()
						c.iterator.drop(prebuffered.size).foreach(write)
						writer.print(']')
					}
				// Case: Cached collection => Buffers to JSON
				case i: Iterable[JsonConvertible] => json(s"[${ i.iterator.map { _.toJson }.mkString(", ") }]")
				// Case: Iterator => Streams one item at a time
				case i =>
					stream.usingWriter(Application.json) { writer =>
						writer.print('[')
						val iter = i.iterator
						iter.nextOption().foreach { value =>
							writer.print(value.toJson)
							writer.flush()
						}
						iter.foreach { value =>
							writer.print(", ")
							writer.print(value.toJson)
							writer.flush()
						}
						writer.print(']')
					}
			}
	}
	
	/**
	 * @param xml The XML element to write to the response body
	 * @return Buffered response body containing the specified XML
	 */
	def xml(xml: XmlElement) = string(xml.toXml, Application.xml)
	
	/**
	 * @param text Text to write to the response body
	 * @param codec Character-encoding to use (implicit)
	 * @return A buffered response body containing the specified text
	 */
	def plainText(text: String)(implicit codec: Codec): WriteResponseBody = plainText(text, codec.charSet)
	/**
	 * @param text Text to write to the response body
	 * @param charset Character-set to use
	 * @return A buffered response body containing the specified text
	 */
	def plainText(text: String, charset: Charset) = string(text, Text.plain.withCharset(charset))
	/**
	 * @param string A string to write into the response body
	 * @param contentType Assigned content type
	 * @return A buffered response body containing the specified string
	 */
	def string(string: String, contentType: ContentType) =
		if (string.isEmpty) NoBody else WriteString(string, contentType)
	
	/**
	 * @param bytes Bytes to write into the response body
	 * @param contentType Content type to assign
	 * @return A new buffered response body
	 */
	def bytes(bytes: Array[Byte], contentType: ContentType): WriteResponseBody =
		if (bytes.isEmpty) NoBody else WriteBytes(bytes, Some(contentType))
	/**
	 * @param bytes Bytes to write into the response body
	 * @return A new buffered response body with no content type specified
	 */
	def bytes(bytes: Array[Byte]): WriteResponseBody =
		if (bytes.isEmpty) NoBody else WriteBytes(bytes)
	
	
	// NESTED   -------------------------
	
	/**
	 * A [[WriteResponseBody]] implementation that never writes a response body
	 */
	case object NoBody extends WriteResponseBody
	{
		// ATTRIBUTES   ----------------
		
		override val isEmpty: Boolean = true
		override val contentType: Option[ContentType] = None
		override val contentLength: Option[Long] = Some(0)
		
		
		// IMPLEMENTED  ---------------
		
		override def self: WriteResponseBody = this
		
		override def writeTo(stream: OutputStream): Future[Try[Unit]] = TryFuture.successCompletion
	}
	
	object Stream
	{
		/**
		 * Writes a response body using a [[PrintWriter]]
		 * @param contentType Type of the content written
		 * @param autoFlush Whether to automatically flush the stream whenever a newline is printed. Default = false.
		 * @param write A function that receives a print writer and performs the writing.
		 * @param exc Implicit execution context
		 * @return A new streamed response body -writer
		 */
		def usingWriter(contentType: ContentType, autoFlush: Boolean = false)(write: PrintWriter => Unit)
		               (implicit exc: ExecutionContext) =
			apply(contentType.withCharsetSpecified) { stream =>
				Future { Try { stream.writeUsing(contentType.charset.getOrElse(codec.charSet), autoFlush)(write) } }
			}
		
		/**
		 * @param contentType Type of the content written
		 * @param write A function that initiates the stream-writing.
		 *              Receives the [[OutputStream]] to write to.
		 *              Yields a future that resolves once the streaming has completed.
		 *              May yield a failure.
		 *              This function is not expected to block.
		 * @return A new streamed response body -writer
		 */
		def apply(contentType: ContentType)(write: OutputStream => Future[Try[Unit]]) =
			new Stream(Some(contentType))(write)
		/**
		 * @param write A function that initiates the stream-writing.
		 *              Receives the [[OutputStream]] to write to.
		 *              Yields a future that resolves once the streaming has completed.
		 *              May yield a failure.
		 *              This function is not expected to block.
		 * @return A new streamed response body -writer, which specifies no content type
		 */
		def withoutContentType(write: OutputStream => Future[Try[Unit]]) = new Stream(None)(write)
	}
	/**
	 * An interface for writing streamed content into a response body
	 * @param contentType Type of content written. None if unspecified.
	 * @param f A function that writes the response body contents into a stream.
	 *          Yields a future that may yield a failure.
	 *          The future is not expected to resolve immediately, and this function is not expected to block.
	 */
	class Stream(override val contentType: Option[ContentType])(f: OutputStream => Future[Try[Unit]])
		extends WriteResponseBody
	{
		// ATTRIBUTES   -------------------------
		
		override val contentLength: Option[Long] = None
		override val isEmpty: Boolean = false
		
		
		// IMPLEMENTED  -------------------------
		
		override def self: WriteResponseBody = this
		
		override def writeTo(stream: OutputStream): Future[Try[Unit]] = f(stream)
	}
	
	private object WriteString
	{
		def apply(string: String, contentType: ContentType = utf8Text) =
			new WriteString(string, contentType.withCharsetSpecified)
	}
	private class WriteString(string: String, cType: ContentType = utf8Text) extends WriteResponseBody
	{
		// ATTRIBUTES   ---------------
		
		private lazy val bytes = Try { string.getBytes(cType.charset.getOrElse(StandardCharsets.UTF_8)) }
		override lazy val contentLength: Option[Long] = bytes.toOption.map { _.length }
		
		
		// IMPLEMENTED  --------------
		
		override def self: WriteResponseBody = this
		override def isEmpty: Boolean = string.isEmpty
		
		override def contentType: Option[ContentType] = Some(cType)
		
		override def writeTo(stream: OutputStream): Future[Try[Unit]] =
			Future.successful(bytes.flatMap { bytes => Try { stream.write(bytes) } })
	}
	
	private case class WriteBytes(bytes: Array[Byte], contentType: Option[ContentType] = None) extends WriteResponseBody
	{
		override lazy val contentLength: Option[Long] = Some(bytes.length)
		
		override def self: WriteResponseBody = this
		override def isEmpty: Boolean = bytes.isEmpty
		
		override def writeTo(stream: OutputStream): Future[Try[Unit]] = Future.successful(Try { stream.write(bytes) })
	}
}

/**
 * A common trait for interfaces which populate response body streams.
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
trait WriteResponseBody extends MaybeEmpty[WriteResponseBody]
{
	/**
	 * @return Type of this content. None if unspecified.
	 */
	def contentType: Option[ContentType]
	/**
	 * @return The length of the written content, in bytes, if known.
	 *         None if unknown, which is typically the case for more streamed content.
	 */
	def contentLength: Option[Long]
	
	/**
	 * Populates a response body's stream. This function may block during the writing,
	 * if [[contentLength]] is specified. Otherwise, the writing should be completed asynchronously.
	 *
	 * Note: When streaming the content in chunks, call 'stream.flush()' in order to start sending out data.
	 *
	 * @param stream Stream to populate
	 * @return A future that resolves once all content has been written into the stream.
	 *         May yield a failure. For buffered responses, this future may resolve immediately.
	 */
	def writeTo(stream: OutputStream): Future[Try[Unit]]
}
