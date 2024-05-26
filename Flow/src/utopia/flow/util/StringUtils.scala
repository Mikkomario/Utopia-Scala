package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._

import scala.annotation.tailrec

/**
  * Contains some utility functions related to Strings
  * @author Mikko Hilpinen
  * @since 20.05.2024, v2.4
  */
object StringUtils
{
	/**
	  * Converts a set of items into a table represented with a string
	  * @param items Items to represent as rows
	  * @param headers Headers to use
	  * @param columns Functions that extract column values from the specified items
	  * @tparam A Type of the items represented
	  * @return A string representing a table which consists of all item column & row values
	  */
	def asciiTableFrom[A](items: Seq[A], headers: Seq[String], columns: (A => String)*) = {
		if (items.isEmpty)
			""
		else {
			/*
			+---+
			| A |
			+---+
			 */
			// Determines column values in order to know column widths
			// 1. keys are column indices, 2. keys are item indices
			val values = columns.map { c => items.map { c(_).linesIterator.toVector } }
			val xRange = values.indices
			val actualHeaders = headers.padTo(values.size, "")
			val columnWidths = xRange.map { x =>
				values(x).view.map { _.view.map { _.length }.maxOption.getOrElse(0) }.max max actualHeaders(x).length
			}
			
			// Generates the actual table
			val separatorLine = s"\n+${ columnWidths.map { w => "-" * (w + 2) }.mkString("+") }+\n"
			val builder = new StringBuilder(separatorLine)
			
			// Writes the headers
			rowContents(xRange, actualHeaders.map { Vector(_) }, columnWidths, builder)
			
			// Writes the table contents
			items.indices.foreach { y =>
				builder ++= separatorLine
				rowContents(xRange, values.map { _(y) }, columnWidths, builder)
			}
			builder ++= separatorLine
			
			builder.result()
		}
	}
	
	@tailrec
	private def rowContents(xRange: Iterable[Int], row: Seq[Seq[String]], widths: Seq[Int], builder: StringBuilder,
	                        index: Int = 0): Unit =
	{
		if (row.exists { _.hasSize > index }) {
			if (index > 0)
				builder += '\n'
			xRange.foreach { x =>
				builder ++= "| "
				builder ++= row(x).getOrElse(index, "").padTo(widths(x) + 1, ' ')
			}
			builder += '|'
			rowContents(xRange, row, widths, builder, index + 1)
		}
	}
}
