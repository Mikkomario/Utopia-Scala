package utopia.flow.parse.json

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.string.Lines
import utopia.flow.util.result.TryExtensions._

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
	  * @param json A JSON string
	  * @return value parsed from the JSON. Failure if JSON was malformed.
	  */
	def apply(json: String): Try[Value]
	/**
	  * Reads a file using default encoding
	  * @param file a JSON file
	  * @return Value parsed from the file. Failure if the file couldn't be read or parsed
	  */
	def apply(file: File): Try[Value]
	/**
	  * Reads a stream using default encoding
	  * @param inputStream A JSON stream
	  * @return Value parsed from the stream. Failure if the stream couldn't be parsed
	  */
	def apply(inputStream: InputStream): Try[Value]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return An interface for reading NDJSON input
	 */
	def ndJson = ParseNdJson
	
	
	// OTHER	-------------------------
	
	/**
	  * @param string A string that may be JSON
	  * @return Parsed JSON value from the input string or just the string as a Value
	  */
	def valueOf(string: String) = apply(string).getOrElse(string: Value)
	
	/**
	  * Reads a file using default encoding
	  * @param path A JSON file path
	  * @return Value parsed from the file. Failure if the file couldn't be read or parsed.
	  */
	def apply(path: Path): Try[Value] = apply(path.toFile)
	
	
	// NESTED   -------------------------
	
	object ParseNdJson
	{
		/**
		 * @param ndJson A newline-delimited string of JSON content
		 * @return Parsed JSON contents or a failure
		 */
		def apply(ndJson: String): Try[IndexedSeq[Value]] = ndJson.linesIterator.map(JsonParser.this.apply).toTry
		/**
		 * @param path Path to a NDJON file
		 * @return Parsed JSON contents or a failure
		 */
		def apply(path: Path): Try[IndexedSeq[Value]] = apply(path.toFile)
		/**
		 * @param file an NDJSON file
		 * @return Parsed JSON contents or a failure
		 */
		def apply(file: File): Try[IndexedSeq[Value]] =
			Lines.iterate.file(file) { _.map(JsonParser.this.apply).toTry }.flatten
		/**
		 * @param inputStream An input stream of newline-delimited JSON content
		 * @return Parsed JSON contents or a failure
		 */
		def apply(inputStream: InputStream): Try[IndexedSeq[Value]] =
			Lines.iterate.stream(inputStream) { _.map(JsonParser.this.apply).toTry }.flatten
	}
}
