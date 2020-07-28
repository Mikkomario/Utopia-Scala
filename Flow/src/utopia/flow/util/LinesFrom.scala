package utopia.flow.util

import java.io.{File, InputStream}
import java.nio.charset.Charset

import scala.io.{Codec, Source}
import scala.util.Try

import utopia.flow.util.AutoClose._

/**
 * Used for reading text lines from various sources
 * @author Mikko Hilpinen
 * @since 1.11.2019, v1.6.1+
 */
object LinesFrom
{
	// TODO: Contains some amounts of repeated code inside this file and between StringFrom (latter separate for performance reasons)
	
	/**
	 * Reads a stream into a string
	 * @param stream An input stream
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return Lines read from stream
	 */
	def stream(stream: InputStream, encoding: String) =
		Try { Source.fromInputStream(stream, encoding).consume { _.getLines.toVector } }
	
	/**
	 * Reads a stream into a string using default encoding
	 * @param stream An input stream
	 * @param codec Character encoding used (implicit)
	 * @return Lines read from stream
	 */
	def stream(stream: InputStream)(implicit codec: Codec) =
		Try { Source.fromInputStream(stream)(codec).consume { _.getLines.toVector } }
	
	/**
	 * Reads a stream into a string
	 * @param stream An input stream
	 * @param encoding Character encoding used
	 * @return Lines read from stream
	 */
	def stream(stream: InputStream, encoding: Charset): Try[String] = StringFrom.stream(stream)(Codec(encoding))
	
	/**
	 * Reads a file into a string
	 * @param file File to read
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return Lines read from file
	 */
	def file(file: File, encoding: String) =
		Try { Source.fromFile(file, encoding).consume { _.getLines.toVector } }
	
	/**
	 * Reads a file into a string using default encoding
	 * @param file File to read
	 * @param codec Character encoding used (implicit)
	 * @return Lines read from file
	 */
	def file(file: File)(implicit codec: Codec) =
		Try { Source.fromFile(file)(codec).consume { _.getLines.toVector } }
	
	/**
	 * Reads a file into a string
	 * @param file File to read
	 * @param encoding Character encoding used
	 * @return Lines read from file
	 */
	def file(file: File, encoding: Charset): Try[String] = StringFrom.file(file)(Codec(encoding))
}
