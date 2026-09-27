package utopia.nexus.controller.write

import utopia.access.model.ContentType
import utopia.access.model.enumeration.ContentCategory.{Application, Text}
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.caching.iterable.CachingSeq
import utopia.flow.operator.MaybeEmpty
import utopia.flow.parse.BufferedPrintWriter
import utopia.flow.parse.StreamExtensions._
import utopia.flow.parse.json.JsonConvertible
import utopia.flow.parse.xml.XmlElement
import utopia.flow.util.StringExtensions._

import java.io.{OutputStream, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}
import scala.collection.View
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
	/**
	 * @param content JSON content to write
	 * @param minBufferSize Minimum number of 'content' entries that should be buffered before switching
	 *                      into streamed content.
	 *                      Default = 1 = Streaming may be started after the first entry.
	 *                      Use a negative number to force buffering.
	 * @param minBufferLength Minimum number of characters that
	 *                        should be buffered before switching into streamed content.
	 *                        Default = 1024.
	 *                        Use a negative number to force buffering.
	 * @param exc Implicit execution context
	 * @return The specified JSON content as a JSON array.
	 *         May be either streamed or buffered, based on the content collection type & size.
	 */
	def jsonArray(content: IterableOnce[JsonConvertible], minBufferSize: Int = 1, minBufferLength: Int = 1024)
	             (implicit exc: ExecutionContext) =
		jsonFlow(content, Application.json, ",", "[", "]", minBufferSize, minBufferLength)
	/**
	 * @param content JSON content to write
	 * @param minBufferSize Minimum number of 'content' entries that should be buffered before switching
	 *                      into streamed content.
	 *                      Default = 1 = Streaming may be started after the first entry.
	 *                      Use a negative number to force buffering.
	 * @param minBufferLength Minimum number of characters that
	 *                        should be buffered before switching into streamed content.
	 *                        Default = 1024.
	 *                        Use a negative number to force buffering.
	 * @param exc Implicit execution context
	 * @return The specified JSON content as newline-delimited JSON.
	 *         May be either streamed or buffered, based on the content collection type & size.
	 */
	def ndJson(content: IterableOnce[JsonConvertible], minBufferSize: Int = 1, minBufferLength: Int = 1024)
	          (implicit exc: ExecutionContext) =
		jsonFlow(content, Application.ndJson, "\n", minBufferSize = minBufferSize, minBufferLength = minBufferLength)
	
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
	
	/**
	 * Yields streamed or buffered JSON content
	 * @param content The entries to convert to JSON
	 * @param contentType Applied content type
	 * @param separator Separator to place between the JSON entries
	 * @param prefix Content prefix (default = empty)
	 * @param suffix Content suffix (default = empty)
	 * @param minBufferSize Minimum number of 'content' entries that should be buffered before switching
	 *                      into streamed content.
	 *                      Default = 0 = Streaming may be started from 0 elements.
	 * @param minBufferLength Minimum number of characters that
	 *                        should be buffered before switching into streamed content.
	 *                        Default = 1024.
	 * @param exc Implicit execution context
	 * @return Content-writing implementation for the specified content
	 */
	private def jsonFlow(content: IterableOnce[JsonConvertible], contentType: ContentType, separator: String,
	                     prefix: String = "", suffix: String = "", minBufferSize: Int = 0, minBufferLength: Int = 1024)
	                    (implicit exc: ExecutionContext) =
	{
		// Case: Streaming is not allowed => Buffers all content
		if (minBufferSize < 0 || minBufferLength < 0)
			string(s"$prefix${ content.iterator.map { _.toJson }.mkString(separator) }$suffix", contentType)
		// Case: Streaming may be applicable => Checks input collection type, whether it's cached or lazy
		else {
			val (buffered, remainderIter)  = content match {
				case c: CachingSeq[JsonConvertible] => c.current -> c.cacheIterator
				case v: View[JsonConvertible] => Empty -> v.iterator
				case i: Iterable[JsonConvertible] => i -> Iterator.empty
				case i => Empty -> i.iterator
			}
			// Buffers the cached portion as JSON
			// Also includes up to 'minStreamCount' number of additional elements
			val bufferedJson = {
				val bufferIter = {
					if (minBufferSize > 0)
						buffered.iterator ++ remainderIter.take(minBufferSize)
					else
						buffered.iterator
				}
				bufferIter.map { _.toJson }.mkString(separator)
			}
			// Case: There's also a lazy remainder => Checks whether that should be buffered, also
			if (remainderIter.hasNext) {
				// Case: The buffered content is already quite long => Streams the remainder
				if (minBufferLength == 0 || bufferedJson.length >= minBufferLength)
					stream.usingWriter(contentType) { writer =>
						writer.write(s"$prefix$bufferedJson")
						remainderIter.foreach { item =>
							writer.write(separator)
							writer.write(item.toJson)
							writer.flush()
						}
						writer.write(suffix)
					}
				// Case: Streaming is conditional => Buffers up to a certain character count
				else {
					val buffer = new StringBuilder()
					buffer ++= prefix
					buffer ++= bufferedJson
					var bufferLen = bufferedJson.length
					val separatorLen = separator.length
					
					// Ensures that there's content, so that the separators are placed correctly
					if (bufferedJson.isEmpty) {
						val firstAddition = remainderIter.next().toJson
						buffer ++= firstAddition
						bufferLen += firstAddition.length
					}
					
					while (remainderIter.hasNext && bufferLen < minBufferLength) {
						val nextAddition = remainderIter.next().toJson
						buffer ++= separator
						buffer ++= nextAddition
						bufferLen += separatorLen + nextAddition.length
					}
					
					// Case: There's still more content => Streams the remainder
					if (remainderIter.hasNext)
						stream.usingWriter(contentType) { writer =>
							// Writes the buffered portion & flushes
							writer.print(buffer.toString())
							writer.flush()
							
							// Writes the remainder one item at a time, flushing between each
							remainderIter.foreach { nextItem =>
								writer.print(separator)
								writer.print(nextItem.toJson)
								writer.flush()
							}
							
							writer.print(suffix)
						}
					// Case: The whole content fit into the character limit => Yields the buffered content
					else
						string(s"$buffer$suffix", contentType)
				}
			}
			// Case: The input was fully buffered => Yields the buffered content
			else
				string(s"$prefix$bufferedJson$suffix", contentType)
		}
	}
	
	
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
		def usingWriter(contentType: ContentType, autoFlush: Boolean = false)(write: BufferedPrintWriter => Unit)
		               (implicit exc: ExecutionContext) =
			apply(contentType.withCharsetSpecified) { stream =>
				Future { Try {
					stream.writeUsing(contentType.charset.getOrElse(codec.charSet), autoFlush = autoFlush)(write)
				} }
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
	 * Populates a response body's stream. This function may block during the writing IF [[contentLength]] is specified.
	 * Otherwise, the writing should be completed asynchronously.
	 *
	 * Note: When streaming the content in chunks, call 'stream.flush()' in order to start sending out data.
	 *
	 * @param stream Stream to populate
	 * @return A future that resolves once all content has been written into the stream.
	 *         May yield a failure. For buffered responses, this future may resolve immediately.
	 */
	def writeTo(stream: OutputStream): Future[Try[Unit]]
}
