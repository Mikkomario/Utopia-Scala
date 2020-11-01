package utopia.reflection.text

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.shape2D.{Bounds, Line, Point, Size}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment

/**
  * A text class that contains (context-dependent) measurement information as well. Most calculations are performed
  * lazily.
  * @author Mikko Hilpinen
  * @since 1.11.2020, v2
  * @param text Wrapped text
  * @param context Text measurement context used
  * @param alignment Alignment used when placing text on multiple lines (default = left)
  * @param allowLineBreaks Whether line breaks should be applied (default = true)
  */
case class MeasuredText(text: LocalizedString, context: TextMeasurementContext, alignment: Alignment = Alignment.Left,
						allowLineBreaks: Boolean = true)
{
	// ATTRIBUTES	-----------------------------------
	
	/**
	  * Individual lines of text
	  */
	lazy val lines = if (allowLineBreaks) text.lines else Vector(text)
	private lazy val _lines = lines.map { _.string }
	/**
	  * The first string index on each line
	  */
	lazy val lineStartIndices = _lines.dropRight(1).foldMapLeft(0) { (lastIndex, line) => lastIndex + line.length }
	/**
	  * The string end index (exclusive) on each line
	  */
	lazy val lineEndIndices = _lines.zip(lineStartIndices).map { case (line, start) => start + line.length }
	
	lazy val (size, lineBounds) = context.boundsOf(_lines, alignment)
	
	/**
	  * Caret positions based on line and string indices
	  */
	lazy val carets = _lines.zip(lineBounds).map { case (line, bounds) => context.caretsFromLine(line, bounds) }
	
	
	// COMPUTED	---------------------------------------
	
	/**
	  * @return Whether this text is empty
	  */
	def isEmpty = text.isEmpty
	/**
	  * @return Whether this text is not empty
	  */
	def nonEmpty = !isEmpty
	
	/**
	  * @return The width of this text in pixels
	  */
	def width = size.width
	/**
	  * @return The height of this text in pixels
	  */
	def height = size.height
	
	
	// OTHER	---------------------------------------
	
	/**
	  * @param index A proposed string index
	  * @return Whether that string index is a possible caret index
	  */
	def isValidCaretIndex(index: Int) = index >= 0 && index <= text.string.length
	
	/**
	  * @param index Target string index
	  * @return A caret line at the specified index
	  */
	def caretAt(index: Int) =
	{
		if (isEmpty)
			Line(Point.origin, Point(0, context.lineHeight))
		else if (index >= text.string.length)
			carets.last.last
		else
		{
			val (lineIndex, indexOnLine) = mapIndex(index)
			carets(lineIndex)(indexOnLine)
		}
	}
	
	/**
	  * Finds information about a sub-string within this text
	  * @param start The first <b>included</b> string index
	  * @param end The first <b>excluded</b> string index (default = end of string)
	  * @return A substring based on the specified indices and <b>either</b><br>
	  *         Right (if the resulting sub-string falls into a single line): Relative sub-string bounds <b>or</b><br>
	  *         Left (if the resulting sub-string spans multiple lines): Line-specific sub-strings in order,
	  *         with their relative bounds included.
	  * @throws IndexOutOfBoundsException If specified start or end indices are not valid sub-string indices
	  */
	@throws[IndexOutOfBoundsException]("If specified start or end indices are not valid sub-string indices")
	def subString(start: Int, end: Int = text.string.length) =
	{
		// Calculates the targeted line and caret positions, as well as the resulting string
		val string = text.local.subString(start, end)
		val (startLineIndex, startIndexOnLine) = mapIndex(start)
		val (endLineIndex, endIndexOnLine) = mapIndex(end)
		val startCaret = carets(startLineIndex)(startIndexOnLine)
		val endCaret = carets(endLineIndex)(endIndexOnLine)
		
		val boundsInfo =
		{
			// Case: Start and end are on the same line
			if (startLineIndex == endLineIndex)
				Right(Bounds.between(startCaret.start, endCaret.end))
			// Case: Start and end are on different lines
			else
			{
				// Calculates start line part
				val startEndText = _lines(startLineIndex).drop(startIndexOnLine)
				val startEndBounds = Bounds(startCaret.start, Size(width - startCaret.start.x,
					startCaret.end.y - startCaret.start.y))
				// Calculates end line part
				val endStartText = _lines(endLineIndex).take(endIndexOnLine)
				val endLineBounds = lineBounds(endLineIndex)
				val endStartBounds = Bounds.between(endLineBounds.topLeft, endCaret.end)
				// Takes in the lines in between
				val midLineInfo =
				{
					if (endLineIndex > startLineIndex + 1)
					{
						val midLineTexts = _lines.slice(startLineIndex + 1, endLineIndex)
						val midLineBounds = lineBounds.slice(startLineIndex + 1, endLineIndex)
						midLineTexts zip midLineBounds
					}
					else
						Vector()
				}
				Left((startEndText -> startEndBounds) +: midLineInfo :+ (endStartText -> endStartBounds))
			}
		}
		string -> boundsInfo
	}
	
	/**
	  * Maps a string index to a line index + string index on that line
	  * @param index A string index
	  * @return Index of the targeted line + string index on that line
	  */
	def mapIndex(index: Int) =
	{
		if (index < 0)
			0 -> index
		else
		{
			// Finds the correct line first
			val lineIndex = lineEndIndices.indexWhereOption { _ > index }.getOrElse(lines.size - 1)
			lineIndex -> (index - lineStartIndices(lineIndex))
		}
	}
}
