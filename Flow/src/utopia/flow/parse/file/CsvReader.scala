package utopia.flow.parse.file

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.flow.parse.string.{IterateLines, Regex}
import utopia.flow.util.StringExtensions._

import java.nio.file.Path
import scala.io.Codec

/**
  * Used for reading .csv file contents
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.8
  */
object CsvReader
{
	private lazy val defaultSeparator = Regex(";").ignoringQuotations
	
	/**
	  * Iterates over the lines in a csv document. Doesn't search for or use headers.
	  * @param path      Path to the target document
	  * @param separator Separator between columns (default = ";")
	  * @param f         Function that consumes the lines iterator.
	  * @param codec     Implicit encoding context
	  * @tparam A Type of function result
	  * @return Failure if file handling failed. Function result otherwise.
	  */
	def iterateRawRowsIn[A](path: Path, separator: Regex = defaultSeparator)(f: Iterator[Vector[String]] => A)
	                       (implicit codec: Codec) =
	{
		IterateLines.fromPath(path) { linesIter =>
			f(linesIter.filterNot { _.isEmpty }
				.map { _.split(separator).toVector.map(processValue) })
		}
	}
	
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
	def iterateLinesIn[A](path: Path, separator: Regex = defaultSeparator, ignoreEmptyStringValues: Boolean = false)
	                     (f: Iterator[Model] => A)(implicit codec: Codec) =
	{
		// Iterates all lines from the target path
		IterateLines.fromPath(path) { linesIter =>
			// The first line is interpreted as the headers list
			val iter = linesIter.filterNot { _.isEmpty }.map { _.split(separator).toVector.map(processValue) }
			if (iter.hasNext) {
				val headers = iter.next()
				// Parses each line to models (on call) and passes this mapped iterator to the specified function
				f(iter.map { line =>
					val constants = {
						if (ignoreEmptyStringValues)
							headers.zip(line).filter { _._2.nonEmpty }
								.map { case (header, value) => Constant(header, value) }
						else
							headers.zip(line).map { case (header, value) => Constant(header, value) }
					}
					Model.withConstants(constants)
				})
			}
			else
				f(Iterator.empty)
		}(codec)
	}
	
	/**
	  * Calls the specified function for each line in the target document
	  * @param path      Path to the target document
	  * @param separator Separator between columns (default = ";")
	  * @param f         Function called for each line in the document. Takes a model parsed from the line and merged
	  *                  with headers.
	  * @param codec     Implicit encoding context
	  * @return Failure if file handling failed. Success otherwise.
	  */
	def foreachLine(path: Path, separator: Regex = defaultSeparator, ignoreEmptyStringValues: Boolean = false)
	               (f: Model => Unit)(implicit codec: Codec) =
		iterateLinesIn(path, separator, ignoreEmptyStringValues) { _.foreach(f) }
	
	private def processValue(original: String) =
	{
		val trimmed = original.trim
		if (trimmed.startsWith("'"))
			trimmed.drop(1)
		else if (trimmed.startsWith("\"") && trimmed.endsWith("\""))
			trimmed.drop(1).dropRight(1)
		else
			trimmed
	}
}
