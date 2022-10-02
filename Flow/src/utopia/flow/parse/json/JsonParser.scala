package utopia.flow.parse.json

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value

import java.io.{File, InputStream}
import java.nio.file.Path
import scala.io.Codec
import scala.util.Try

/**
  * Common trait for json parser implementations
  * @author Mikko Hilpinen
  * @since 12.5.2020, v1.8
  */
trait JsonParser
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return The encoding used by this parser by default
	  */
	def defaultEncoding: Codec
	
	/**
	  * @param json A json string
	  * @return value parsed from the json. Failure if json was malformed.
	  */
	def apply(json: String): Try[Value]
	
	/**
	  * Reads a file using default encoding
	  * @param file a json file
	  * @return Value parsed from the file. Failure if the file couldn't be read or parsed
	  */
	def apply(file: File): Try[Value]
	
	/**
	  * Reads a stream using default encoding
	  * @param inputStream A json stream
	  * @return Value parsed from the stream. Failure if the stream couldn't be parsed
	  */
	def apply(inputStream: InputStream): Try[Value]
	
	
	// OTHER	-------------------------
	
	/**
	  * @param string A string that may be json
	  * @return Parsed json value from string or just the string as a value
	  */
	def valueOf(string: String) = apply(string).getOrElse(string: Value)
	
	/**
	  * Reads a file using default encoding
	  * @param path A json file path
	  * @return Value parsed from the file. Failure if the file couldn't be read or parsed.
	  */
	def apply(path: Path): Try[Value] = apply(path.toFile)
}
