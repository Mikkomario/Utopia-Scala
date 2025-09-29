package utopia.flow.parse.string

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.file.Path
import scala.io.Codec

/**
 * Common trait for interfaces that read and buffer from streamed sources.
 * @tparam I The type of accepted preprocessed input
 * @tparam A The type of buffered output
 * @author Mikko Hilpinen
 * @since 28.09.2025, v2.7
 */
trait FromSource[I, +A]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Interface used for opening source data
	 */
	protected def open: OpenSource[I]
	/**
	 * Buffers and processes the input. May throw.
	 * @param input Preprocessed source data
	 * @return Buffered / processed data
	 */
	protected def buffer(input: I): A
	
	
	// OTHER	--------------------------
	
	/**
	 * Reads stream contents
	 * @param stream   An input stream
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return Processed result
	 */
	def stream(stream: InputStream, encoding: String) = open.stream(stream, encoding)(buffer)
	/**
	 * Reads stream contents
	 * @param stream An input stream
	 * @param codec  Character encoding used (implicit)
	 * @return Processed result
	 */
	def stream(stream: InputStream)(implicit codec: Codec) = open.stream(stream)(buffer)
	/**
	 * Reads stream contents
	 * @param stream   An input stream
	 * @param encoding Character encoding used (Eg. UTF-8)
	 * @return Processed result
	 */
	def stream(stream: InputStream, encoding: Charset) = open.stream(stream, encoding)(buffer)
	
	/**
	 * Reads file contents
	 * @param file     An input file
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return Processed result
	 */
	def file(file: File, encoding: String) = open.file(file, encoding)(buffer)
	/**
	 * Reads file contents
	 * @param file  An input file
	 * @param codec Character encoding used (implicit)
	 * @return Processed result
	 */
	def file(file: File)(implicit codec: Codec) = open.file(file)(buffer)
	/**
	 * Reads file contents
	 * @param file     An input file
	 * @param encoding Character encoding used (Eg. UTF-8)
	 * @return Processed result
	 */
	def file(file: File, encoding: Charset) = open.file(file, encoding)(buffer)
	
	/**
	 * Reads file contents
	 * @param path     Path to target file
	 * @param encoding Character encoding used (Eg. "UTF-8")
	 * @return Processed result
	 */
	def path(path: Path, encoding: String) = open.path(path, encoding)(buffer)
	/**
	 * Reads file contents
	 * @param path  Path to target file
	 * @param codec Character encoding used (implicit)
	 * @return Processed result
	 */
	def path(path: Path)(implicit codec: Codec) = open.path(path)(buffer)
	/**
	 * Reads file contents
	 * @param path     Path to target file
	 * @param encoding Character encoding used (Eg. UTF-8)
	 * @return Processed result
	 */
	def path(path: Path, encoding: Charset) = open.path(path, encoding)(buffer)
}
