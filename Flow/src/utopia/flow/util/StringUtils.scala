package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import StringExtensions._
import utopia.flow.collection.immutable.Single

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
	  * @param columns Functions that extract column values from the specified items.
	 *                Each function is preceded by the matching header name.
	 * @param header Main header of this table. Not applied if empty. Default = empty.
	  * @param skipLineSeparators Whether separators between lines should be omitted. Default = false.
	 * @tparam A Type of the items represented
	  * @return A string representing a table which consists of all item column & row values
	  */
	def asciiTableFrom[A](items: Seq[A], columns: Seq[(String, A => String)], header: String = "",
	                      skipLineSeparators: Boolean = false) =
	{
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
			val values = columns.map { case (_, toValue) => items.map { toValue(_).linesIterator.toOptimizedSeq } }
			val xRange = values.indices
			val headers = columns.map { _._1 }
			val columnWidths = xRange.map { x =>
				values(x).view.map { _.view.map { _.length }.maxOption.getOrElse(0) }.max max headers(x).length
			}
			
			// Generates the actual table
			val separatorLine = s"+${ columnWidths.map { w => "-" * (w + 2) }.mkString("+") }+"
			val separatorRow = s"\n$separatorLine\n"
			val builder = new StringBuilder(separatorLine)
			builder += '\n'
			
			// Writes the main header, if applicable
			header.ifNotEmpty.foreach { header =>
				val headerWidth = columnWidths.sum + (columns.size - 1) * 3
				// Splits the header in case its very long
				builder ++= header.splitToLinesIterator(headerWidth)
					.map { headerPart => s"| ${ headerPart.padTo(headerWidth, ' ') } |" }.mkString("\n")
				builder ++= separatorRow
			}
			
			// Writes the headers
			rowContents(xRange, headers.map { Single(_) }, columnWidths, builder)
			
			// Writes the table contents
			items.indices.foreach { y =>
				if (y > 0 && skipLineSeparators)
					builder += '\n'
				else
					builder ++= separatorRow
				rowContents(xRange, values.map { _(y) }, columnWidths, builder)
			}
			builder += '\n'
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
