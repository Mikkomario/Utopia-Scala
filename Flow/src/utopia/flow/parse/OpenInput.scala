package utopia.flow.parse

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.parse.OpenInput.{MappedInput, TryMappedInput}

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.file.Path
import scala.io.Codec
import scala.util.Try

object OpenInput
{
	// NESTED   ----------------------------
	
	private class MappedInput[I, O](delegate: OpenInput[I], map: I => O) extends OpenInput[O]
	{
		override def stream[C](stream: InputStream)(f: O => C)(implicit codec: Codec): Try[C] =
			delegate.stream(stream)(map andThen f)
		
		override def file[A](file: File)(f: O => A)(implicit codec: Codec): Try[A] = delegate.file(file)(map andThen f)
	}
	
	private class TryMappedInput[I, O](delegate: OpenInput[I], map: I => Try[O]) extends OpenInput[O]
	{
		override def stream[C](stream: InputStream)(f: O => C)(implicit codec: Codec): Try[C] =
			delegate.stream(stream) { map(_).map(f) }.flatten

		override def file[A](file: File)(f: O => A)(implicit codec: Codec): Try[A] =
			delegate.file(file) { map(_).map(f) }.flatten
	}
}

/**
  * Common trait for interfaces that provide (temporary) access to input data (streams, paths, etc.).
  * @tparam I Type of the input presented to the user/caller
 * @author Mikko Hilpinen
  * @since 1.10.2026, v2.9
  */
trait OpenInput[+I]
{
	// ABSTRACT   --------------------------
	
	/**
	 * Provides access to stream contents
	 * @param stream An input stream
	 * @param f      Parsing function that receives the prepared stream contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @param codec  Character encoding used (implicit)
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if stream-reading failed or if 'f' threw an exception.
	 */
	def stream[A](stream: InputStream)(f: I => A)(implicit codec: Codec): Try[A]
	/**
	 * Provides access to file contents
	 * @param file  An input file
	 * @param f      Parsing function that receives the prepared file contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @param codec Character encoding used (implicit)
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if file-reading failed or if 'f' threw an exception.
	 */
	def file[A](file: File)(f: I => A)(implicit codec: Codec): Try[A]
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @param ev Implicit evidence that this interface presents iterable content
	 * @tparam A Type of iterated elements
	 * @return Interface for reading buffered contents from this input
	 */
	def buffered[A](implicit ev: I <:< IterableOnce[A]): ReadInput[IndexedSeq[A]] =
		bufferUsing {OptimizedIndexedSeq.from(_)}
	
	
	// OTHER    ----------------------------
	
	/**
	 * Provides access to stream contents
	 * @param stream An input stream
	 * @param f      Parsing function that receives the prepared stream contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @param codec  Character encoding used (implicit)
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if stream-reading failed or if 'f' threw an exception.
	 */
	def apply[A](stream: InputStream)(f: I => A)(implicit codec: Codec): Try[A] = this.stream[A](stream)(f)
	/**
	 * Provides access to file contents
	 * @param path  Path to the file to read
	 * @param f      Parsing function that receives the prepared file contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @param codec Character encoding used (implicit)
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if file-reading failed or if 'f' threw an exception.
	 */
	def apply[A](path: Path)(f: I => A)(implicit codec: Codec) = this.path[A](path)(f)
	
	/**
	 * Provides access to stream contents
	 * @param stream An input stream
	 * @param encoding Name of the stream's character encoding. E.g. "UTF-8".
	 * @param f      Parsing function that receives the prepared stream contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if stream-reading failed or if 'f' threw an exception.
	 */
	def stream[A](stream: InputStream, encoding: String)(f: I => A): Try[A] = this.stream[A](stream)(f)(Codec(encoding))
	/**
	 * Provides access to stream contents
	 * @param stream An input stream
	 * @param encoding Stream's character encoding. E.g. UTF-8.
	 * @param f      Parsing function that receives the prepared stream contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if stream-reading failed or if 'f' threw an exception.
	 */
	def stream[A](stream: InputStream, encoding: Charset)(f: I => A): Try[A] = this.stream(stream)(f)(Codec(encoding))
	
	/**
	 * Provides access to file contents
	 * @param file  An input file
	 * @param encoding Name of the file's character encoding. E.g. "UTF-8".
	 * @param f      Parsing function that receives the prepared file contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if file-reading failed or if 'f' threw an exception.
	 */
	def file[A](file: File, encoding: String)(f: I => A): Try[A] = this.file(file)(f)(Codec(encoding))
	/**
	 * Provides access to file contents
	 * @param file  An input file
	 * @param encoding File's character encoding. E.g. UTF-8.
	 * @param f      Parsing function that receives the prepared file contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if file-reading failed or if 'f' threw an exception.
	 */
	def file[A](file: File, encoding: Charset)(f: I => A): Try[A] = this.file(file)(f)(Codec(encoding))
	
	/**
	 * Provides access to file contents
	 * @param path  Path to the file to read
	 * @param f      Parsing function that receives the prepared file contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @param codec Character encoding used (implicit)
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if file-reading failed or if 'f' threw an exception.
	 */
	def path[A](path: Path)(f: I => A)(implicit codec: Codec) = file(path.toFile)(f)(codec)
	/**
	 * Provides access to file contents
	 * @param path  Path to the file to read
	 * @param encoding Name of the file's character encoding. E.g. "UTF-8".
	 * @param f      Parsing function that receives the prepared file contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if file-reading failed or if 'f' threw an exception.
	 */
	def path[A](path: Path, encoding: String)(f: I => A) = file(path.toFile, encoding)(f)
	/**
	 * Provides access to file contents
	 * @param path  Path to the file to read
	 * @param encoding File's character encoding. E.g. UTF-8.
	 * @param f      Parsing function that receives the prepared file contents.
	 *               Note: The received contents should only be used during this function's execution.
	 * @tparam A Type of 'f' results
	 * @return Result of 'f'. Failure if file-reading failed or if 'f' threw an exception.
	 */
	def path[A](path: Path, encoding: Charset)(f: I => A): Try[A] = file(path.toFile)(f)(Codec(encoding))
	
	/**
	 * @param f A mapping function applied to this interface's input
	 * @tparam I2 Type of mapped input
	 * @return A copy of this interface that presents mapped input
	 */
	def map[I2](f: I => I2): OpenInput[I2] = new MappedInput(this, f)
	/**
	 * @param f A mapping function applied to this interface's input.
	 *          May yield a failure.
	 * @tparam I2 Type of mapped input
	 * @return A copy of this interface that presents mapped input, when successful
	 */
	def tryMap[I2](f: I => Try[I2]): OpenInput[I2] = new TryMappedInput(this, f)
	
	/**
	 * @param f A function that receives opened input data and buffers it
	 * @tparam A Type of buffered contents
	 * @return An interface for reading the buffered contents
	 */
	def bufferUsing[A](f: I => A): ReadInput[A] = BufferInput(this)(f)
}
