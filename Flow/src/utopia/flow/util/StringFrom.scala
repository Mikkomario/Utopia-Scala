package utopia.flow.util

import java.io.{File, InputStream}
import java.nio.charset.Charset

import utopia.flow.util.AutoClose._

import scala.io.{Codec, Source}
import scala.util.Try

/**
 * This object contains some utility methods for producing / reading strings
 * @author Mikko Hilpinen
 * @since 1.11.2019, v1.6.1+
 */
object StringFrom
{
	/**
	 * Reads a stream into a string
	 * @param stream An input stream
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return A string from specified stream (ignores newlines)
	 */
	def stream(stream: InputStream, encoding: String) =
		Try(Source.fromInputStream(stream, encoding).consume { _.getLines.mkString })
	
	/**
	 * Reads a stream into a string using default encoding
	 * @param stream An input stream
	 * @param codec Character encoding used (implicit)
	 * @return A string from specified stream (ignores newlines)
	 */
	def stream(stream: InputStream)(implicit codec: Codec) =
		Try { Source.fromInputStream(stream)(codec).consume { _.getLines.mkString } }
	
	/**
	 * Reads a stream into a string
	 * @param stream An input stream
	 * @param encoding Character encoding used
	 * @return A string from specified stream (ignores newlines)
	 */
	def stream(stream: InputStream, encoding: Charset): Try[String] = StringFrom.stream(stream)(Codec(encoding))
	
	/**
	 * Reads a file into a string
	 * @param file File to read
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return The file's contents as a single string (ignores newlines)
	 */
	def file(file: File, encoding: String) =
		Try(Source.fromFile(file, encoding).consume { _.getLines.mkString })
	
	/**
	 * Reads a file into a string using default encoding
	 * @param file File to read
	 * @param codec Character encoding used (implicit)
	 * @return The file's contents as a single string (ignores newlines)
	 */
	def file(file: File)(implicit codec: Codec) =
		Try(Source.fromFile(file)(codec).consume { _.getLines.mkString })
	
	/**
	 * Reads a file into a string
	 * @param file File to read
	 * @param encoding Character encoding used
	 * @return The file's contents as a single string (ignores newlines)
	 */
	def file(file: File, encoding: Charset): Try[String] = StringFrom.file(file)(Codec(encoding))
}
