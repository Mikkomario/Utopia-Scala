package utopia.flow.util

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
			val values = columns.map(items.map)
			val xRange = values.indices
			val actualHeaders = headers.padTo(values.size, "")
			val columnWidths = xRange.map { x => values(x).view.map { _.length }.max max actualHeaders(x).length }
			
			// Generates the actual table
			val separatorLine = s"\n+${ columnWidths.map { w => "-" * (w + 2) }.mkString("+") }+\n"
			val builder = new StringBuilder(separatorLine)
			
			// Writes the headers
			rowContents(xRange, actualHeaders, columnWidths, builder)
			builder += '|'
			
			// Writes the table contents
			items.indices.foreach { y =>
				builder ++= separatorLine
				rowContents(xRange, values.map { _(y) }, columnWidths, builder)
				builder += '|'
			}
			builder ++= separatorLine
			
			builder.result()
		}
	}
	
	private def rowContents(xRange: Iterable[Int], row: Seq[String], widths: Seq[Int], builder: StringBuilder) =
		xRange.foreach { x =>
			builder ++= "| "
			builder ++= row(x).padTo(widths(x) + 1, ' ')
		}
}
