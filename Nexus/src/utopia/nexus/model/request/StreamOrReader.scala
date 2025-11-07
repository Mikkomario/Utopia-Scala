package utopia.nexus.model.request

import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.EmptyInputStream
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.StringFrom
import utopia.flow.parse.xml.{XmlElement, XmlReader}
import utopia.flow.util.UncertainBoolean

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.nio.charset.{Charset, StandardCharsets}
import scala.util.{Failure, Success, Try}

object StreamOrReader
{
	// ATTRIBUTES   -------------------
	
	private val defaultBufferSize = 8192
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return An empty input stream wrapper
	 */
	def empty: StreamOrReader = EmptyStream
	
	
	// OTHER    -----------------------
	
	/**
	 * @param charset Character set to use, when/if constructing a buffered reader (call-by-name)
	 * @param bufferSize Size of the buffer used, when/if constructing a buffered reader (call-by-name)
	 * @param preferReader Whether to rather prepare a buffered reader than a mere input-stream, when calling .either.
	 *                     Default = false = if neither is initialized when calling .either,
	 *                     an input-stream will be opened.
	 *                     Call-by-name: Only called if necessary.
	 * @param stream A function that yields the input stream. Call-by-name.
	 * @param reader A function that yields the buffered reader. Call-by-name.
	 *               Only called if 'stream' is not called.
	 * @return A new interface for accessing either the 'stream' or the 'reader'.
	 */
	def apply(charset: => Charset, bufferSize: => Int = defaultBufferSize, preferReader: => Boolean = false)
	         (stream: => InputStream)(reader: => BufferedReader): StreamOrReader =
		new _StreamOrReader(stream, reader, charset, bufferSize, preferReader)
	
	
	// NESTED   -----------------------
	
	private object EmptyStream extends StreamOrReader
	{
		// ATTRIBUTES   ---------------
		
		override val isEmpty: UncertainBoolean = true
		
		override val stream: InputStream = EmptyInputStream
		override val charset: Charset = StandardCharsets.UTF_8
		
		
		// IMPLEMENTED  ---------------
		
		override def closed: Boolean = false
		
		// Constructs a placeholder BufferedReader with no input and buffer of 1 byte
		override def reader: BufferedReader = new BufferedReader(new InputStreamReader(stream, charset), 1)
		
		override def either: Either[InputStream, BufferedReader] = Left(stream)
		
		override def close(): Unit = ()
		
		override def bufferToString: Try[String] = Success("")
		override def bufferToXml: Try[XmlElement] = Failure(new UnsupportedOperationException("This stream is empty"))
		override def bufferAsJson(implicit jsonParser: JsonParser): Try[Value] = Success(Value.empty)
	}
	
	private class _StreamOrReader(openStream: => InputStream, getReader: => BufferedReader, getCharset: => Charset,
	                              bufferSize: => Int, preferReader: => Boolean)
		extends StreamOrReader
	{
		// ATTRIBUTES   ---------------
		
		override val isEmpty: UncertainBoolean = UncertainBoolean
		override lazy val charset: Charset = getCharset
		
		private var _stream: Option[InputStream] = None
		private var _reader: Option[BufferedReader] = None
		private var _closed = false
		
		
		// IMPLEMENTED  ---------------
		
		override def closed: Boolean = _closed
		
		override def stream: InputStream = {
			if (_reader.isDefined)
				throw new IllegalStateException("A reader has already been prepared")
			else
				_stream.getOrElse {
					testIfClosed()
					val stream = openStream
					_stream = Some(stream)
					stream
				}
		}
		override def reader: BufferedReader = _reader.getOrElse {
			testIfClosed()
			_stream match {
				case Some(stream) =>
					val reader = new BufferedReader(new InputStreamReader(stream, charset), bufferSize)
					_reader = Some(reader)
					reader
				case None =>
					val reader = getReader
					_reader = Some(reader)
					reader
			}
		}
		
		override def either: Either[InputStream, BufferedReader] = {
			testIfClosed()
			_reader match {
				case Some(reader) => Right(reader)
				case None =>
					_stream match {
						case Some(stream) => Left(stream)
						case None =>
							if (preferReader)
								Right(reader)
							else
								Left(stream)
					}
			}
		}
		
		override def close(): Unit = {
			if (!_closed) {
				_closed = true
				val readerCloseResult = _reader.map { reader => Try { reader.close() } }
				_reader = None
				val streamCloseResult = _stream.map { stream => Try { stream.close() } }
				_stream = None
				readerCloseResult.foreach { _.get }
				streamCloseResult.foreach { _.get }
			}
		}
		
		
		// OTHER    ----------------------
		
		private def testIfClosed() = {
			if (_closed)
				throw new IllegalStateException("Already closed")
		}
	}
}

/**
 * An interface for acquiring either an InputStream, or a BufferedReader
 * @author Mikko Hilpinen
 * @since 05.11.2025, v2.0
 */
// TODO: When buffering, close the stream afterwards
trait StreamOrReader extends AutoCloseable
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Whether this represents an empty stream. May be uncertain.
	 */
	def isEmpty: UncertainBoolean
	
	/**
	 * @return Whether this interface has already closed
	 */
	def closed: Boolean
	
	/**
	 * @return Character set used in the buffered reader
	 */
	def charset: Charset
	
	/**
	 * @throws IllegalStateException If a buffered reader has already been acquired using this interface,
	 *                               or if [[close]] has already been called.
	 * @return The accessible input stream
	 */
	@throws[IllegalStateException]("If a BufferedReader has already been acquired")
	def stream: InputStream
	/**
	 * @throws IllegalStateException If already closed
	 * @return A buffered reader for reading the streamed content
	 */
	@throws[IllegalStateException]("If already closed")
	def reader: BufferedReader
	
	/**
	 * @throws IllegalStateException If already closed
	 * @return If a buffered reader has been initialized, yields that as a Right.
	 *         Otherwise, if the stream (only) has been acquired, yields that as a Left.
	 *         Otherwise, may yield either format, depending on the implementation.
	 */
	@throws[IllegalStateException]("If already closed")
	def either: Either[InputStream, BufferedReader]
	
	
	// COMPUTED ---------------------
	
	/**
	 * @return Whether this represents a non-empty stream. May be uncertain.
	 */
	def nonEmpty = !isEmpty
	
	/**
	 * Buffers this stream's contents into a string
	 * @return The string read from the underlying stream.
	 *         Failure if an exception was encountered, or if this interface had already been closed.
	 */
	def bufferToString = {
		if (closed)
			Failure(new IllegalStateException("Already closed"))
		else
			either match {
				case Left(stream) => StringFrom.stream(stream, charset)
				case Right(reader) => readerToString(reader)
			}
	}
	/**
	 * Buffers this stream's contents into XML (assumes that the content is XML)
	 * @return The XML element parsed from the stream.
	 *         Failure if parsing failed or if this interface had already closed.
	 */
	def bufferToXml = {
		if (closed)
			Failure(new IllegalStateException("Already closed"))
		else
			either match {
				case Left(stream) => XmlReader.parseStream(stream, charset)
				case Right(reader) => readerToString(reader).flatMap(XmlReader.parseString)
			}
	}
	
	/**
	 * Buffers this stream's contents into a [[utopia.flow.generic.model.immutable.Value]],
	 * assuming that the streamed content is JSON.
	 * @param jsonParser JSON parser used for processing the streamed JSON
	 * @return Parsed value. Failure if parsing failed, or if this interface had already closed.
	 */
	def bufferAsJson(implicit jsonParser: JsonParser) = {
		if (closed)
			Failure(new IllegalStateException("Already closed"))
		else
			either match {
				case Left(stream) => jsonParser(stream)
				case Right(reader) => readerToString(reader).flatMap(jsonParser.apply)
			}
	}
	
	
	// OTHER    ---------------------
	
	private def readerToString(reader: BufferedReader) =
		Try { OptionsIterator.continually { Option(reader.readLine()) }.mkString("\n") }
}
