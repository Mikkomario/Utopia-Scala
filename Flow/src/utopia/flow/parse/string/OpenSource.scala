package utopia.flow.parse.string

import utopia.flow.parse.AutoClose._

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.file.Path
import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Common trait for interfaces that read data from a [[Source]] instance.
  * @author Mikko Hilpinen
  * @since 1.11.2019, v2.7
  */
trait OpenSource[+I]
{
	// ABSTRACT ----------------------------
	
	/**
	 * Converts a [[Source]] instance into the intermediary, processed type
	 * @param source Source to convert into the intermediary type, and to present to the specified processor function
	 * @param processor A function that's to receive the pre-processed source
	 */
	protected def presentSource[A](source: Source, processor: I => A): A
	
	
	// OTHER    ----------------------------
	
	/**
	  * Iterates over lines read from a stream.
	  * @param stream   An input stream
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def stream[A](stream: InputStream, encoding: String)(f: I => A) =
		_apply(Source.fromInputStream(stream, encoding))(f)
	/**
	  * Iterates over lines read from a stream.
	  * @param stream An input stream
	  * @param f      Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @param codec  Character encoding used (implicit)
	  * @return Parse function result
	  */
	def stream[A](stream: InputStream)(f: I => A)(implicit codec: Codec) =
		_apply(Source.fromInputStream(stream)(codec))(f)
	/**
	  * Iterates over lines read from a stream.
	  * @param stream   An input stream
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def stream[A](stream: InputStream, encoding: Charset)(f: I => A): Try[A] = this.stream(stream)(f)(Codec(encoding))
	
	/**
	  * Iterates over lines read from a file.
	  * @param file     An input file
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def file[A](file: File, encoding: String)(f: I => A) = _apply(Source.fromFile(file, encoding))(f)
	/**
	  * Iterates over lines read from a file.
	  * @param file  An input file
	  * @param f     Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @param codec Character encoding used (implicit)
	  * @return Parse function result
	  */
	def file[A](file: File)(f: I => A)(implicit codec: Codec) = _apply(Source.fromFile(file)(codec))(f)
	/**
	  * Iterates over lines read from a file.
	  * @param file     An input file
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def file[A](file: File, encoding: Charset)(f: I => A): Try[A] = this.file(file)(f)(Codec(encoding))
	/**
	  * Iterates over lines read from a file.
	  * @param path     Path to target file
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def path[A](path: Path, encoding: String)(f: I => A) = file(path.toFile, encoding)(f)
	/**
	  * Iterates over lines read from a file.
	  * @param path  Path to target file
	  * @param f     Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @param codec Character encoding used (implicit)
	  * @return Parse function result
	  */
	def path[A](path: Path)(f: I => A)(implicit codec: Codec) = file(path.toFile)(f)(codec)
	/**
	  * Iterates over lines read from a file.
	  * @param path     Path to target file
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def path[A](path: Path, encoding: Charset)(f: I => A): Try[A] = file(path.toFile)(f)(Codec(encoding))
	
	private def _apply[A](open: => Source)(process: I => A) = Try { open.consume { presentSource(_, process) } }
}
