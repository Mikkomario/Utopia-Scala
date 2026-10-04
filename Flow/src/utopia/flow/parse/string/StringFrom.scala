package utopia.flow.parse.string

import utopia.flow.parse.{BufferInput, ReadInput}
import utopia.flow.parse.StreamExtensions._

import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.file.{Files, Path}
import scala.io.{Codec, Source}
import scala.util.{Success, Try}

/**
  * This object contains some utility methods for producing / reading strings
  * @author Mikko Hilpinen
  * @since 1.11.2019, v1.6.1+
  */
object StringFrom extends ReadInput[String]
{
	// IMPLEMENTED  ------------------------
	
	override def stream(stream: InputStream)(implicit codec: Codec): Try[String] = {
		Try {
			// Buffers the input into a byte array and uses it to form the string
			val buffer = new ByteArrayOutputStream(8192)
			stream.writeTo(buffer, 8192)
			new String(buffer.toByteArray, codec.charSet)
		}
	}
	
	override def file(file: File)(implicit codec: Codec): Try[String] = _path(file.toPath)
	override def path(path: Path)(implicit codec: Codec) = _path(path)
	
	override def string(string: String): Try[String] = Success(string)
	override def lines(lines: IterableOnce[String]): Try[String] = Success(lines.iterator.mkString("\n"))
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param maxCharacters Maximum characters to read
	 * @return A copy of this interface only reading strings up to 'maxCharacters' length
	 */
	def take(maxCharacters: Int) = source { _.take(maxCharacters) }
	
	/**
	 * @param f A function used for extracting a string from a [[Source]]
	 * @return An interface for reading extracted strings from various sources
	 */
	def source(f: Source => Iterator[Char]) =
		BufferInput(OpenSource) { source => f(source).mkString }
	
	private def _path(path: Path)(implicit codec: Codec) =
		Try { new String(Files.readAllBytes(path), codec.charSet) }
}
