package utopia.flow.parse

import java.io.{File, InputStream}
import scala.io.Codec
import scala.util.Try

/**
 * Common trait for [[OpenInput]] implementations that do so by wrapping another such instance.
 * @tparam I Type of the input presented to the user/caller
 * @author Mikko Hilpinen
 * @since 01.10.2026, v2.9
 */
trait OpenInputWrapper[+I] extends OpenInput[I]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The wrapped [[OpenInput]] implementation.
	 */
	protected def wrapped: OpenInput[I]
	
	
	// IMPLEMENTED  --------------------
	
	override def stream[A](stream: InputStream)(f: I => A)(implicit codec: Codec): Try[A] =
		wrapped.stream(stream)(f)(codec)
	override def file[A](file: File)(f: I => A)(implicit codec: Codec): Try[A] =
		wrapped.file(file)(f)(codec)
	
	override def string[A](string: String)(f: I => A): Try[A] = wrapped.string(string)(f)
	override def lines[A](lines: IterableOnce[String])(f: I => A): Try[A] = wrapped.lines(lines)(f)
}
