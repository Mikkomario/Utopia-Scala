package utopia.flow.parse.string

import utopia.flow.parse.AutoClose._
import utopia.flow.parse.OpenInput
import utopia.flow.util.StringExtensions._

import java.io.{File, InputStream}
import scala.collection.View
import scala.io.{Codec, Source}
import scala.util.Try

object OpenSource extends OpenSource[Source]
{
	override protected def presentSource[A](source: Source, processor: Source => A): A = processor(source)
	
	override def string[A](string: String)(f: Source => A): Try[A] = Try { f(Source.fromString(string)) }
	override def lines[A](lines: IterableOnce[String])(f: Source => A): Try[A] = {
		val chars = lines match {
			case i: Iterable[String] => View.fromIteratorProvider { () => i.toCharsIterator }
			case i => lines.toCharsIterator.toVector
		}
		Try { f(Source.fromIterable(chars)) }
	}
}

/**
  * Common trait for interfaces that read data from a [[Source]] instance.
  * @author Mikko Hilpinen
  * @since 1.11.2019, v2.7
  */
trait OpenSource[+I] extends OpenInput[I]
{
	// ABSTRACT ----------------------------
	
	/**
	 * Converts a [[Source]] instance into the intermediary, processed type
	 * @param source Source to convert into the intermediary type, and to present to the specified processor function
	 * @param processor A function that's to receive the pre-processed source
	 */
	protected def presentSource[A](source: Source, processor: I => A): A
	
	
	// IMPLEMENTED  ------------------------
	
	override def stream[A](stream: InputStream)(f: I => A)(implicit codec: Codec) =
		_apply(Source.fromInputStream(stream)(codec))(f)
		
	override def file[A](file: File)(f: I => A)(implicit codec: Codec) = _apply(Source.fromFile(file)(codec))(f)
	
	
	// OTHER    ----------------------------
	
	private def _apply[A](open: => Source)(process: I => A) = Try { open.consume { presentSource(_, process) } }
}
