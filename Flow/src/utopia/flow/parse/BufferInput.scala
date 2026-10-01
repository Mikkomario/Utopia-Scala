package utopia.flow.parse

import utopia.flow.collection.immutable.OptimizedIndexedSeq

import java.io.{File, InputStream}
import scala.io.Codec

object BufferInput
{
	// OTHER    -------------------------
	
	/**
	 * @param open An interface which opens streamed data in iterable format
	 * @tparam A Type of iterated entries
	 * @return An interface for reading buffered stream/file contents
	 */
	def from[A](open: OpenInput[IterableOnce[A]]): BufferInput[IterableOnce[A], IndexedSeq[A]] =
		apply(open)(OptimizedIndexedSeq.from)
	
	/**
	 * @param open An interface used for opening streamed data
	 * @param f A buffering function applied
	 * @tparam I Type of processed stream input
	 * @tparam A Type of buffered output
	 * @return An interface for reading the buffered output
	 */
	def apply[I, A](open: OpenInput[I])(f: I => A): BufferInput[I, A] = new _BufferInput(open, f)
	
	
	// NESTED   -------------------------
	
	private class _BufferInput[I, A](override protected val open: OpenInput[I], f: I => A) extends BufferInput[I, A]
	{
		override protected def buffer(input: I): A = f(input)
	}
}

/**
 * Common trait for interfaces that read and buffer content from streamed sources.
 * @tparam I The type of accepted preprocessed input
 * @tparam A The type of buffered output
 * @author Mikko Hilpinen
 * @since 01.10.2026, v2.9
 */
trait BufferInput[I, +A] extends ReadInput[A]
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Interface used for opening input data
	 */
	protected def open: OpenInput[I]
	/**
	 * Buffers and processes the input. May throw.
	 * @param input Accessible input data
	 * @return Buffered / processed data
	 */
	protected def buffer(input: I): A
	
	
	// IMPLEMENTED  ----------------------
	
	override def stream(stream: InputStream)(implicit codec: Codec) = open.stream(stream)(buffer)
	override def file(file: File)(implicit codec: Codec) = open.file(file)(buffer)
}
