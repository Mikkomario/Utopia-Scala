package utopia.flow.parse.string

import utopia.flow.parse.AutoClose._

import java.io.{File, InputStream}
import java.nio.charset.Charset
import java.nio.file.Path
import scala.io.{Codec, Source}
import scala.util.Try

/**
  * Used for iterating over text lines from various sources
  * @author Mikko Hilpinen
  * @since 1.11.2019, v1.8
  */
object IterateLines
{
	/**
	  * Iterates over lines read from a stream.
	  * @param stream   An input stream
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def fromStream[A](stream: InputStream, encoding: String)(f: Iterator[String] => A) =
		withSource(Source.fromInputStream(stream, encoding))(f)
	
	/**
	  * Iterates over lines read from a stream.
	  * @param stream An input stream
	  * @param f      Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @param codec  Character encoding used (implicit)
	  * @return Parse function result
	  */
	def fromStream[A](stream: InputStream)(f: Iterator[String] => A)(implicit codec: Codec) =
		withSource(Source.fromInputStream(stream)(codec))(f)
	
	/**
	  * Iterates over lines read from a stream.
	  * @param stream   An input stream
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def fromStream[A](stream: InputStream, encoding: Charset)(f: Iterator[String] => A): Try[A] =
		fromStream(stream)(f)(Codec(encoding))
	
	/**
	  * Iterates over lines read from a file.
	  * @param file     An input file
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def fromFile[A](file: File, encoding: String)(f: Iterator[String] => A) =
		withSource(Source.fromFile(file, encoding))(f)
	
	/**
	  * Iterates over lines read from a file.
	  * @param file  An input file
	  * @param f     Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @param codec Character encoding used (implicit)
	  * @return Parse function result
	  */
	def fromFile[A](file: File)(f: Iterator[String] => A)(implicit codec: Codec) =
		withSource(Source.fromFile(file)(codec))(f)
	
	/**
	  * Iterates over lines read from a file.
	  * @param file     An input file
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def fromFile[A](file: File, encoding: Charset)(f: Iterator[String] => A): Try[A] = fromFile(file)(f)(Codec(encoding))
	
	/**
	  * Iterates over lines read from a file.
	  * @param path     Path to target file
	  * @param encoding Character encoding used (Eg. "UTF-8")
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def fromPath[A](path: Path, encoding: String)(f: Iterator[String] => A) = fromFile(path.toFile, encoding)(f)
	
	/**
	  * Iterates over lines read from a file.
	  * @param path  Path to target file
	  * @param f     Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @param codec Character encoding used (implicit)
	  * @return Parse function result
	  */
	def fromPath[A](path: Path)(f: Iterator[String] => A)(implicit codec: Codec) =
		fromFile(path.toFile)(f)(codec)
	
	/**
	  * Iterates over lines read from a file.
	  * @param path     Path to target file
	  * @param encoding Character encoding used (Eg. UTF-8)
	  * @param f        Parsing function. Please note that the accepted iterator won't work outside this function.
	  * @return Parse function result
	  */
	def fromPath[A](path: Path, encoding: Charset)(f: Iterator[String] => A): Try[A] =
		fromFile(path.toFile)(f)(Codec(encoding))
	
	private def withSource[A](openSource: => Source)(f: Iterator[String] => A) =
		Try { openSource.consume { s => f(s.getLines()) } }
}
