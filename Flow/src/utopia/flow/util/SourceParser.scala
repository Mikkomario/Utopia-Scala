package utopia.flow.util

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.file.Path

import scala.io.Codec

/**
 * Common trait for access points that simply parse source content into some form
 * @author Mikko Hilpinen
 * @since 1.11.2019, v1.8
 */
trait SourceParser[+A]
{
	// ABSTRACT	--------------------------
	
	/**
	  * Processes the lines read from a source
	  * @param linesIterator Lines iterator being processed. This iterator only functions inside this function call.
	  * @return Processed and buffered contents
	  */
	protected def process(linesIterator: Iterator[String]): A
	
	
	// OTHER	--------------------------
	
	/**
	 * Reads stream contents
	 * @param stream An input stream
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return Processed result
	 */
	def stream(stream: InputStream, encoding: String) = IterateLines.fromStream(stream, encoding)(process)
	
	/**
	  * Reads stream contents
	  * @param stream An input stream
	  * @param codec Character encoding used (implicit)
	  * @return Processed result
	  */
	def stream(stream: InputStream)(implicit codec: Codec) = IterateLines.fromStream(stream)(process)(codec)
	
	/**
	  * Reads stream contents
	  * @param stream An input stream
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @return Processed result
	  */
	def stream(stream: InputStream, encoding: Charset) = IterateLines.fromStream(stream, encoding)(process)
	
	/**
	  * Reads file contents
	  * @param file An input file
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @return Processed result
	  */
	def file(file: File, encoding: String) = IterateLines.fromFile(file, encoding)(process)
	
	/**
	  * Reads file contents
	  * @param file An input file
	  * @param codec Character encoding used (implicit)
	  * @return Processed result
	  */
	def file(file: File)(implicit codec: Codec) = IterateLines.fromFile(file)(process)(codec)
	
	/**
	  * Reads file contents
	  * @param file An input file
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @return Processed result
	  */
	def file(file: File, encoding: Charset) = IterateLines.fromFile(file, encoding)(process)
	
	/**
	  * Reads file contents
	  * @param path Path to target file
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @return Processed result
	  */
	def path(path: Path, encoding: String) = IterateLines.fromPath(path, encoding)(process)
	
	/**
	  * Reads file contents
	  * @param path Path to target file
	  * @param codec Character encoding used (implicit)
	  * @return Processed result
	  */
	def path(path: Path)(implicit codec: Codec) = IterateLines.fromPath(path)(process)(codec)
	
	/**
	  * Reads file contents
	  * @param path Path to target file
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @return Processed result
	  */
	def path(path: Path, encoding: Charset) = IterateLines.fromPath(path, encoding)(process)
}
