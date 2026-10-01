package utopia.flow.parse

import utopia.flow.parse.ReadInput.ReadMappedInput

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.file.Path
import scala.io.Codec
import scala.util.Try

object ReadInput
{
	// NESTED   --------------------------
	
	private class ReadMappedInput[-A, +B](delegate: ReadInput[A], f: A => B) extends ReadInput[B]
	{
		override def stream(stream: InputStream)(implicit codec: Codec): Try[B] = delegate.stream(stream).map(f)
		override def file(file: File)(implicit codec: Codec): Try[B] = delegate.file(file).map(f)
	}
}

/**
 * Common trait for interfaces that read content from streamed sources.
 * @tparam A Type of content read
 * @author Mikko Hilpinen
 * @since 01.10.2026, v2.9
 */
trait ReadInput[+A]
{
	// ABSTRACT --------------------------
	
	/**
	 * Reads stream contents
	 * @param stream An input stream
	 * @param codec  Character encoding used (implicit)
	 * @return Processed result. Failure if stream-reading failed.
	 */
	def stream(stream: InputStream)(implicit codec: Codec): Try[A]
	/**
	 * Reads file contents
	 * @param file  An input file
	 * @param codec Character encoding used (implicit)
	 * @return Processed result. Failure if file-reading failed.
	 */
	def file(file: File)(implicit codec: Codec): Try[A]
	
	
	// OTHER	--------------------------
	
	/**
	 * Reads stream contents
	 * @param stream An input stream
	 * @param codec  Character encoding used (implicit)
	 * @return Processed result. Failure if stream-reading failed.
	 */
	def apply(stream: InputStream)(implicit codec: Codec): Try[A] = this.stream(stream)
	/**
	 * Reads file contents
	 * @param file  An input file
	 * @param codec Character encoding used (implicit)
	 * @return Processed result. Failure if file-reading failed.
	 */
	def apply(file: File)(implicit codec: Codec): Try[A] = this.file(file)
	
	/**
	 * Reads stream contents
	 * @param stream   An input stream
	 * @param encoding Character encoding used (E.g. "UTF-8")
	 * @return Processed result. Failure if stream-reading failed.
	 */
	def stream(stream: InputStream, encoding: String): Try[A] = this.stream(stream)(Codec(encoding))
	/**
	 * Reads stream contents
	 * @param stream   An input stream
	 * @param encoding Character encoding used (E.g. UTF-8)
	 * @return Processed result. Failure if stream-reading failed.
	 */
	def stream(stream: InputStream, encoding: Charset): Try[A] = this.stream(stream)(Codec(encoding))
	
	/**
	 * Reads file contents
	 * @param file     An input file
	 * @param encoding Character encoding used (E.g. "UTF-8")
	 * @return Processed result. Failure if file-reading failed.
	 */
	def file(file: File, encoding: String): Try[A] = this.file(file)(Codec(encoding))
	/**
	 * Reads file contents
	 * @param file     An input file
	 * @param encoding Character encoding used (E.g. UTF-8)
	 * @return Processed result. Failure if file-reading failed.
	 */
	def file(file: File, encoding: Charset): Try[A] = this.file(file)(Codec(encoding))
	
	/**
	 * Reads file contents
	 * @param path  Path to the target file
	 * @param codec Character encoding used (implicit)
	 * @return Processed result. Failure if file-reading failed.
	 */
	def path(path: Path)(implicit codec: Codec) = file(path.toFile)
	/**
	 * Reads file contents
	 * @param path     Path to the target file
	 * @param encoding Character encoding used (E.g. "UTF-8")
	 * @return Processed result. Failure if file-reading failed.
	 */
	def path(path: Path, encoding: String): Try[A] = this.path(path)(Codec(encoding))
	/**
	 * Reads file contents
	 * @param path     Path to the target file
	 * @param encoding Character encoding used (E.g. UTF-8)
	 * @return Processed result. Failure if file-reading failed.
	 */
	def path(path: Path, encoding: Charset): Try[A] = this.file(path.toFile)(Codec(encoding))
	
	/**
	 * @param f A mapping function to apply to this interface's output
	 * @tparam B Type of mapping results
	 * @return A copy of this interface that also applies the specified mapping function
	 */
	def map[B](f: A => B): ReadInput[B] = new ReadMappedInput(this, f)
}
