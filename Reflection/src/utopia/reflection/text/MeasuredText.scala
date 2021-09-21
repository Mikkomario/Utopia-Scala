package utopia.reflection.text

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.shape2D.{Bounds, Direction2D, Line, Point}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment

import scala.collection.immutable.VectorBuilder

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
	lazy val lines =
	{
		if (allowLineBreaks)
		{
			// Makes sure the line breaks at the end of the text are also included
			val lineBreaksAtEnd = text.string.reverseIterator.takeWhile { _ == '\n' }.size
			text.lines.map { _.stripControlCharacters } ++ Vector.fill(lineBreaksAtEnd) { LocalizedString.empty }
		}
		else
			Vector(text.stripControlCharacters)
	}
	private lazy val _lines = lines.map { _.string }
	
	lazy val (firstLineCaretIndices, lastLineCaretIndices) =
	{
		val startBuilder = new VectorBuilder[Int]()
		val endBuilder = new VectorBuilder[Int]()
		startBuilder.sizeHint(lines.size)
		endBuilder.sizeHint(lines.size)
		
		var lastCaretIndex = -1
		_lines.foreach { line =>
			val start = lastCaretIndex + 1
			lastCaretIndex = start + line.length
			
			startBuilder += start
			endBuilder += lastCaretIndex
		}
		
		startBuilder.result() -> endBuilder.result()
	}
	lazy val (size, lineBounds) = context.boundsOf(_lines, alignment)
	/**
	  * Caret positions based on line and string indices
	  */
	lazy val carets = _lines.zip(lineBounds).map { case (line, bounds) => context.caretsFromLine(line, bounds) }
	
	/**
	  * The default text draw targets (texts to draw and the positions where they should be drawn)
	  */
	lazy val defaultDrawTargets = lines.indices.iterator.map { lineIndex =>
		_lines(lineIndex) -> lineBounds(lineIndex).position
	}.toVector
	
	
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
	
	/**
	  * @return The largest allowed caret index
	  */
	def maxCaretIndex = text.string.length
	
	
	// OTHER	---------------------------------------
	
	/**
	  * @param index A proposed string index
	  * @return Whether that string index is a possible caret index
	  */
	def isValidCaretIndex(index: Int) = index >= 0 && index <= maxCaretIndex
	
	/**
	  * @param index Target string index
	  * @return A caret line at the specified index
	  */
	def caretAt(index: Int): Line =
	{
		val (lineIndex, indexOnLine) = mapCaretIndex(index)
		caretAt(lineIndex, indexOnLine)
	}
	
	def caretAt(lineIndex: Int, indexOnLine: Int) =
	{
		if (isEmpty)
			Line(Point.origin, Point(0, context.lineHeight))
		else
		{
			if (lineIndex < 0)
				carets.head.head
			else if (lineIndex >= carets.size)
				carets.last.last
			else
			{
				val line = carets(lineIndex)
				if (indexOnLine < 0)
					line.head
				else if (indexOnLine >= line.size)
					line.last
				else
					line(indexOnLine)
			}
		}
	}
	
	/**
	  * @param position A position relative to the text top left position
	  * @return The index of the closest text line and the index of the closest caret on that line
	  */
	def caretIndexClosestTo(position: Point) =
	{
		if (isEmpty)
			0 -> 0
		else
		{
			// Finds the line that is closest to the specified position
			val lineIndex =
			{
				if (lines.size > 1)
					lineBounds.minIndexBy { b =>
						if (b.y > position.y)
							b.y - position.y
						else if (b.bottomY < position.y)
							position.y - b.bottomY
						else
							0.0
					}
				else
					0
			}
			// Finds the caret index on the line that is closes to the specified position
			val indexOnLine = carets(lineIndex).minIndexBy { c => (c.start.x - position.x).abs }
			lineIndex -> indexOnLine
		}
	}
	
	/**
	  * @param index Target caret index
	  * @return A caret index above the specified index. None if there are no lines above the specified index.
	  */
	def caretIndexAbove(index: Int) = caretIndexParallelTo(index, Negative)
	
	/**
	  * @param index Target caret index
	  * @return A caret index below the specified index. None if there are no lines below the specified index.
	  */
	def caretIndexBelow(index: Int) = caretIndexParallelTo(index, Positive)
	/**
	  * Finds the caret index that is above or below a specified caret index
	  * @param caretIndex A caret index
	  * @param direction Vertical direction sign towards which the caret is moved
	  * @return The next caret index. None if there are no lines in that direction
	  */
	def caretIndexParallelTo(caretIndex: Int, direction: Sign): Option[Int] =
	{
		val (lineIndex, indexOnLine) = mapCaretIndex(caretIndex)
		caretIndexParallelTo(lineIndex, indexOnLine, direction).map { case (lineIndex, indexOnLine) =>
			mapCaretIndex(lineIndex, indexOnLine)
		}
	}
	
	/**
	  * Finds the caret index that is above or below a specified caret index
	  * @param lineIndex Index of the targeted line
	  * @param indexOnLine Relative caret index on that line
	  * @param direction Vertical direction sign towards which the caret is moved
	  * @return The next line and relative caret index. None if there are no lines in that direction
	  */
	def caretIndexParallelTo(lineIndex: Int, indexOnLine: Int, direction: Sign) =
	{
		val nextLineIndex = lineIndex + 1 * direction.modifier
		if (nextLineIndex < 0 || nextLineIndex > lines.size - 1)
			None
		else
		{
			val x = caretX(lineIndex, indexOnLine)
			carets(nextLineIndex).minOptionIndexBy { c => (c.start.x - x).abs }.map { nextLineIndex -> _ }
		}
	}
	
	/**
	  * Finds the caret index that is next in line to a specific direction
	  * @param index Starting caret index
	  * @param direction Direction of movement
	  * @return The next caret index. None if there are no available indices to that direction.
	  */
	def caretIndexNextTo(index: Int, direction: Direction2D) = direction.axis match
	{
		case X => Some(index + 1 * direction.sign.modifier).filter(isValidCaretIndex)
		case Y => caretIndexParallelTo(index, direction.sign)
	}
	
	private def caretX(lineIndex: Int, caretIndexOnLine: Int) =
	{
		if (caretIndexOnLine < 0)
			lineBounds(lineIndex).x
		else if (caretIndexOnLine >= carets(lineIndex).size)
			lineBounds(lineIndex).rightX
		else
			carets(lineIndex)(caretIndexOnLine).start.x
	}
	
	/*
	  * Finds information about a sub-string within this text
	  * @param start The first <b>included</b> string index
	  * @param end The first <b>excluded</b> string index (default = end of string)
	  * @return A substring based on the specified indices and <b>either</b><br>
	  *         Right (if the resulting sub-string falls into a single line): Relative sub-string bounds <b>or</b><br>
	  *         Left (if the resulting sub-string spans multiple lines): Line-specific sub-strings in order,
	  *         with their relative bounds included.
	  * @throws IndexOutOfBoundsException If specified start or end indices are not valid sub-string indices
	  */
	/*
		// Handle caret vs. string index problem
	@throws[IndexOutOfBoundsException]("If specified start or end indices are not valid sub-string indices")
	def subString(start: Int, end: Int = text.string.length) =
	{
		// Calculates the targeted line and caret positions, as well as the resulting string
		val string = text.local.subString(start, end)
		val (startLineIndex, startIndexOnLine) = mapCaretIndex(start)
		val (endLineIndex, endIndexOnLine) = mapCaretIndex(end)
		val startCaret = caretAt(startLineIndex, startIndexOnLine)
		val endCaret = caretAt(endLineIndex, endIndexOnLine)
		
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
	}*/
	
	/**
	  * Converts an index in the string to a caret index
	  * @param stringIndex Index of a character in the string
	  * @param targetEndOfCharacter Whether the caret at the end of that character should be returned. False if the
	  *                             caret at the beginning of that character should be returned (default)
	  * @return A line index and a relative caret index
	  */
	def stringIndexToCaretIndex(stringIndex: Int, targetEndOfCharacter: Boolean = false) =
	{
		if (stringIndex < 0)
			0 -> 0
		else
		{
			// Iterates through the lines, advancing a simulated cursor
			_lines.indices.foldLeft[Either[Int, (Int, Int)]](Left(stringIndex)) { (previous, lineIndex) =>
				previous match
				{
					case result: Right[Int, (Int, Int)] => result
					case Left(remaining) =>
						val line = _lines(lineIndex)
						val lineLength = line.length
						if (lineLength > remaining)
						{
							val startCaret = firstLineCaretIndices(lineIndex)
							if (targetEndOfCharacter)
								Right(lineIndex -> (startCaret + remaining + 1))
							else
								Right(lineIndex -> (startCaret + remaining))
						}
						else
							Left(remaining - lineLength - 1) // Also skips the line break character
				}
			} match
			{
				case Right(result) => result
				// Case: Out of bounds
				case Left(_) =>
					if (isEmpty)
						0 -> 0
					else
						(lines.size - 1) -> _lines.last.length
			}
		}
	}
	
	/**
	  * Converts a caret index to an index in the string
	  * @param caretIndex A caret index
	  * @return An index in the string (inside string bounds)
	  */
	def caretIndexToCharacterIndex(caretIndex: Int) =
	{
		if (caretIndex < 0)
			0
		else
			caretIndex min (text.string.length - 1)
	}
	
	/**
	  * Converts a caret index to an index in the string
	  * @param lineIndex Index of the targeted line
	  * @param indexOnLine A relative caret index on the line
	  * @return An index in the string
	  */
	def caretIndexToCharacterIndex(lineIndex: Int, indexOnLine: Int): Int =
		caretIndexToCharacterIndex(mapCaretIndex(lineIndex, indexOnLine))
	
	/**
	  * @param startCaretIndex The starting point caret index
	  * @param endCaretIndex Then ending point caret index
	  * @return A string between the two caret points
	  */
	def subString(startCaretIndex: Int, endCaretIndex: Int) =
	{
		if (isEmpty)
			text.string
		else
		{
			val startStringIndex = caretIndexToCharacterIndex(startCaretIndex) // Inclusive
			val endStringIndex = (endCaretIndex min text.string.length) max startStringIndex // Exclusive
			text.string.substring(startStringIndex, endStringIndex)
		}
	}
	
	/**
	  * Maps a caret index to a line index + caret index on that line
	  * @param index A string index
	  * @return Index of the targeted line + caret index on that line
	  */
	def mapCaretIndex(index: Int) =
	{
		if (index < 0 || lines.isEmpty)
			0 -> index
		else
		{
			// Finds the correct line first
			val lineIndex = lastLineCaretIndices.indexWhereOption { _ >= index }.getOrElse(lines.size - 1)
			lineIndex -> (index - firstLineCaretIndices(lineIndex))
		}
	}
	
	/**
	  * @param lineIndex Index of the targeted line
	  * @param indexOnLine Relative caret index on the specified line
	  * @return A caret index
	  */
	def mapCaretIndex(lineIndex: Int, indexOnLine: Int) =
	{
		if (isEmpty || lineIndex < 0)
			0
		else if (lineIndex >= lines.size)
			lastLineCaretIndices.last
		else
			(firstLineCaretIndices(lineIndex) + indexOnLine) min lastLineCaretIndices(lineIndex)
	}
	
	/**
	  * @param highlightedCaretRanges Areas within this text to highlight
	  * @return Standard draw targets + highlight draw targets (which include bounds)
	  */
	def drawTargets(highlightedCaretRanges: Iterable[Range] = Vector()) =
	{
		// If there aren't any highlighted ranges, uses the cached values
		if (highlightedCaretRanges.isEmpty || isEmpty)
			defaultDrawTargets -> Vector()
		else
		{
			// Converts the ranges to { line index: Character ranges } -map
			val ranges = highlightedCaretRanges.iterator.flatMap { range =>
				val (startLineIndex, startIndexOnLine) = mapCaretIndex(range.head)
				val (endLineIndex, endIndexOnLine) = mapCaretIndex(range.last)
				// Case: Range is contained within a single line
				if (startLineIndex == endLineIndex)
					Vector(startLineIndex -> (startIndexOnLine -> Some(endIndexOnLine)))
				// Case: Range spans multiple lines
				else
				{
					// Adds the end of the starting line
					val startLine = startLineIndex -> (startIndexOnLine -> None)
					// Adds the beginning of the ending line
					val endLine = endLineIndex -> (0, Some(endIndexOnLine))
					// Adds lines in between
					if (endLineIndex > startLineIndex + 1)
						startLine +: (startLineIndex + 1).until(endLineIndex)
							.map { lineIndex => lineIndex -> (0 -> None) } :+ endLine
					else
						Vector(startLine, endLine)
				}
			}.toVector.asMultiMap
			
			// Collects draw targets, one line at a time
			val normalStringsBuffer = new VectorBuilder[(String, Point)]
			val highlightedStringsBuffer = new VectorBuilder[(String, Bounds)]
			lines.indices.foreach { lineIndex =>
				// Case: Highlights concern this line
				if (ranges.contains(lineIndex))
				{
					val (string, position) = defaultDrawTargets(lineIndex)
					val affectingHighlights = ranges(lineIndex).sortBy { _._1 }
					
					// Goes through the highlights, adding normal and highlighted areas in sequence
					var lastHighlightEnd: Option[(Int, Point)] = Some(0 -> position)
					affectingHighlights.foreach { case (startIndex, endIndex) =>
						// Adds space between the two highlights, if applicable
						lastHighlightEnd.foreach { case (lastEndIndex, lastEndPosition) =>
							if (lastEndIndex < startIndex)
								normalStringsBuffer += (string.substring(lastEndIndex, startIndex) -> lastEndPosition)
						}
						val startCaret = caretAt(lineIndex, startIndex)
						endIndex match
						{
							// Case: Highlight doesn't span the whole line
							case Some(endIndex) =>
								val endCaret = caretAt(lineIndex, endIndex)
								highlightedStringsBuffer += (string.substring(startIndex, endIndex min string.length) ->
									Bounds.between(startCaret.start, endCaret.end))
								lastHighlightEnd = Some(endIndex -> endCaret.start)
							// Case: Highlight takes the rest of the line
							case None =>
								highlightedStringsBuffer += (string.substring(startIndex) ->
									Bounds.between(startCaret.start, lineBounds(lineIndex).bottomRight))
								lastHighlightEnd = None
						}
					}
					// Adds the space after the last highlight, if applicable
					lastHighlightEnd.foreach { case (lastEndIndex, lastEndPosition) =>
						if (string.length > lastEndIndex)
							normalStringsBuffer += (string.substring(lastEndIndex) -> lastEndPosition)
					}
				}
				// Case: No highlights for this line => returns default values
				else
					normalStringsBuffer += defaultDrawTargets(lineIndex)
			}
			
			// Returns both "normal" draw targets and highlight targets
			normalStringsBuffer.result() -> highlightedStringsBuffer.result()
		}
	}
}
