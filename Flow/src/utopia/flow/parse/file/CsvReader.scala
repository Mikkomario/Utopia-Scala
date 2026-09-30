package utopia.flow.parse.file

import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser

import java.nio.file.Path
import scala.io.Codec

/**
  * Used for reading .csv file contents
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.8
  */
@deprecated("Please use CsvRows instead, but notice the different default column-separator", "v2.9")
object CsvReader
{
	// ATTRIBUTES	---------------------
	
	private val defaultSeparator = ';'
	
	
	// OTHER    -------------------------
	
	/**
	  * Iterates over the lines in a csv document. Doesn't search for or use headers.
	  * @param path      Path to the target document
	  * @param separator Separator between columns (default = ";")
	  * @param f         Function that consumes the lines iterator.
	  * @param codec     Implicit encoding context
	  * @tparam A Type of function result
	  * @return Failure if file handling failed. Function result otherwise.
	  */
	@deprecated("Please use CsvRows.iterateRaw.path(Path)(...) instead, but notice the different default column-separator", "v2.9")
	def iterateRawRowsIn[A](path: Path, separator: Char = defaultSeparator)(f: Iterator[IndexedSeq[String]] => A)
	                       (implicit codec: Codec) =
		CsvRows.separatedBy(separator).iterateRaw.path(path)(f)
	
	/**
	  * Iterates over the lines in a csv document
	  * @param path                    Path to the target document
	  * @param separator               Separator between columns (default = ";")
	  * @param ignoreEmptyStringValues Whether empty string values should not be applied to the resulting models
	  *                                (default = false = apply all values)
	  * @param f                       Function that consumes the parsed lines iterator. Each line is a model that combines headers with line
	  *                                values. The passed iterator must not be used outside this function.
	  * @param codec                   Implicit encoding context
	  * @return Failure if file handling failed. function result otherwise.
	  */
	@deprecated("Please use CsvRows.iterate.path(Path)(...) instead, but notice the different default column-separator", "v2.9")
	def iterateLinesIn[A](path: Path, separator: Char = defaultSeparator, ignoreEmptyStringValues: Boolean = false)
	                     (f: Iterator[Model] => A)(implicit codec: Codec, jsonParser: JsonParser) =
		CsvRows.separatedBy(separator).withoutEmptyValuesIf(ignoreEmptyStringValues).iterate.path(path)(f)
	
	/**
	  * Calls the specified function for each line in the target document
	  * @param path      Path to the target document
	  * @param separator Separator between columns (default = ";")
	  * @param f         Function called for each line in the document. Takes a model parsed from the line and merged
	  *                  with headers.
	  * @param codec     Implicit encoding context
	  * @return Failure if file handling failed. Success otherwise.
	  */
	def foreachLine(path: Path, separator: Char = defaultSeparator, ignoreEmptyStringValues: Boolean = false)
	               (f: Model => Unit)(implicit codec: Codec, jsonParser: JsonParser) =
		iterateLinesIn(path, separator, ignoreEmptyStringValues) { _.foreach(f) }
}
