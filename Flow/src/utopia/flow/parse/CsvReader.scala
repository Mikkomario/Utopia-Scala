package utopia.flow.parse

import java.nio.file.Path

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.util.IterateLines
import utopia.flow.generic.ValueConversions._

import scala.io.Codec

/**
  * Used for reading .csv file contents
  * @author Mikko Hilpinen
  * @since 3.8.2020, v1.8
  */
object CsvReader
{
	/**
	  * Calls the specified function for each line in the target document
	  * @param path Path to the target document
	  * @param separator Separator between columns (default = ";")
	  * @param f Function called for each line in the document. Takes a model parsed from the line and merged
	  *          with headers.
	  * @param codec Implicit encoding context
	  * @return Failure if file handling failed. Success otherwise.
	  */
	def foreachLine(path: Path, separator: String = ";")(f: Model[Constant] => Unit)(implicit codec: Codec) =
	{
		// Iterates all lines from the target path
		IterateLines.fromPath(path) { linesIter =>
			// The first line is interpreted as the headers list
			val iter = linesIter.filterNot { _.isEmpty }.map { _.split(separator).toVector.map { _.trim } }
			if (iter.hasNext)
			{
				val headers = iter.next()
				// Calls the specified function for each parsed line, wrapped in a model
				iter.foreach { line =>
					f(Model.withConstants(headers.zip(line).map { case (header, value) => Constant(header, value) }))
				}
			}
		}(codec)
	}
}
