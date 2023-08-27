package utopia.genesis.graphics

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{MaybeEmpty, Sign}
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.genesis.graphics.TextDrawHeight.LineHeight
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Alignment, Direction2D}
import utopia.paradigm.shape.shape2d.Line
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import scala.collection.immutable.VectorBuilder

/**
  * A text class that contains (context-dependent) measurement information as well.
  * Most calculations are performed lazily.
  * @author Mikko Hilpinen
  * @since 1.11.2020
  * @param text Wrapped text
  * @param context Text measurement context used
  * @param alignment Alignment used when placing text on multiple lines and when drawing text (default = top left)
  * @param heightSettings Settings used when interpreting text height (default = use standard line height)
  * @param lineSplitThreshold A width threshold after which new lines are formed.
  *                           No line within this text will be longer than the specified limit, unless it consists
  *                           of a single word only.
  *
  *                           None if there should not occur any automatic line-splitting (default)
  *
  * @param betweenLinesAdjustment Adjustment applied to the between lines margins, in pixels (default = 0)
  * @param allowLineBreaks Whether line breaks should be applied (default = true)
  */
case class MeasuredText(text: String, context: FontMetricsWrapper, alignment: Alignment = Alignment.TopLeft,
                        heightSettings: TextDrawHeight = LineHeight, lineSplitThreshold: Option[Double] = None,
                        betweenLinesAdjustment: Double = 0.0, allowLineBreaks: Boolean = true)
	extends MaybeEmpty[MeasuredText]
{
	// ATTRIBUTES	-----------------------------------
	
	/**
	  * Individual lines of text
	  */
	lazy val lines = {
		val default = {
			if (allowLineBreaks) {
				// Makes sure the line breaks at the end of the text are also included
				val lineBreaksAtEnd = text.reverseIterator.takeWhile { c => Regex.newLine(c.toString) }.size
				(text.linesIterator.map { _.stripControlCharacters } ++ Vector.fill(lineBreaksAtEnd) { "" }).toVector
			}
			else
				Vector(text.stripControlCharacters)
		}
		// Also applies automatic line-breaks, if applicable
		lineSplitThreshold match {
			case Some(t) =>
				default.flatMap { line =>
					if (line.isEmpty)
						Some(line)
					else {
						// Measures the width of all parts of this line
						val parts = line.split(Regex.whiteSpace).map { s => s -> context.widthOf(s) }
						val whiteSpaceWidth = context.widthOf(' ')
						
						val resultBuilder = new VectorBuilder[String]()
						var nextStartIndex = 0
						
						// Assigns each sequence of parts into a line
						while (nextStartIndex < parts.size) {
							// Takes the maximum number of words until the threshold is met
							val takeCount = parts.drop(nextStartIndex + 1)
								// 1: Total width, 2: Number of parts included
								.foldLeftIterator(parts(nextStartIndex)._2 -> 1) { case ((width, takeCount), (_, partWidth)) =>
									// Includes a whitespace between consecutive parts
									(width + whiteSpaceWidth + partWidth) -> (takeCount + 1)
								}
								.takeWhile { _._1 <= t }
								.last._2
							resultBuilder += parts.slice(nextStartIndex, nextStartIndex + takeCount)
								.iterator.map { _._1 }.mkString(" ")
							// Moves to the next sequence
							nextStartIndex += takeCount
						}
						
						resultBuilder.result()
					}
				}
			case None => default
		}
	}
	
	lazy val (firstLineCaretIndices, lastLineCaretIndices) = {
		val startBuilder = new VectorBuilder[Int]()
		val endBuilder = new VectorBuilder[Int]()
		startBuilder.sizeHint(lines.size)
		endBuilder.sizeHint(lines.size)
		
		var lastCaretIndex = -1
		lines.foreach { line =>
			val start = lastCaretIndex + 1
			lastCaretIndex = start + line.length
			
			startBuilder += start
			endBuilder += lastCaretIndex
		}
		
		startBuilder.result() -> endBuilder.result()
	}
	lazy val (bounds, lineBounds) = boundsOf(lines)
	/**
	  * Caret positions based on line and string indices
	  */
	lazy val carets = lines.zip(lineBounds).map { case (line, (bounds, _)) =>
		context.caretCoordinatesFrom(line).map { relativeX =>
			Line.ofVector(bounds.topLeft + X(relativeX), Vector2D(0, bounds.height))
		}
	}
	
	/**
	  * The default text draw targets (texts to draw and the positions where they should be drawn)
	  */
	lazy val defaultDrawTargets = lines.indices.toVector.map { lineIndex => lines(lineIndex) -> lineBounds(lineIndex)._2 }
	
	
	// COMPUTED	---------------------------------------
	
	/**
	  * @return Size of this text area
	  */
	def size = bounds.size
	/**
	  * @return The width of this text in pixels
	  */
	def width = bounds.width
	/**
	  * @return The height of this text in pixels
	  */
	def height = bounds.height
	
	/**
	  * @return The largest allowed caret index
	  */
	def maxCaretIndex = text.length
	
	
	// IMPLEMENTED  -----------------------------------
	
	override def self = this
	
	/**
	  * @return Whether this text is empty
	  */
	override def isEmpty = text.isEmpty
	
	
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
	/**
	  * @param lineIndex Index of targeted line
	  * @param indexOnLine Index of targeted character on that line
	  * @return Caret line at the specified location
	  */
	def caretAt(lineIndex: Int, indexOnLine: Int) = {
		// TODO: lineHeight might not be the correct function to call (see heightSettings)
		if (isEmpty)
			Line(Point.origin, Point(0, context.lineHeight))
		else {
			if (lineIndex < 0)
				carets.head.head
			else if (lineIndex >= carets.size)
				carets.last.last
			else {
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
	def caretIndexClosestTo(position: Point) = {
		if (isEmpty)
			0 -> 0
		else {
			// Finds the line that is closest to the specified position
			val lineIndex = {
				if (lines.size > 1)
					lineBounds.minIndexBy { case (b, _) =>
						if (b.topY > position.y)
							b.topY - position.y
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
	def caretIndexParallelTo(caretIndex: Int, direction: Sign): Option[Int] = {
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
	def caretIndexParallelTo(lineIndex: Int, indexOnLine: Int, direction: Sign) = {
		val nextLineIndex = lineIndex + 1 * direction.modifier
		if (nextLineIndex < 0 || nextLineIndex > lines.size - 1)
			None
		else {
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
	def caretIndexNextTo(index: Int, direction: Direction2D) = direction.axis match {
		case X => Some(index + 1 * direction.sign.modifier).filter(isValidCaretIndex)
		case Y => caretIndexParallelTo(index, direction.sign)
	}
	
	private def caretX(lineIndex: Int, caretIndexOnLine: Int) = {
		if (caretIndexOnLine < 0)
			lineBounds(lineIndex)._1.leftX
		else if (caretIndexOnLine >= carets(lineIndex).size)
			lineBounds(lineIndex)._1.rightX
		else
			carets(lineIndex)(caretIndexOnLine).start.x
	}
	
	/**
	  * Converts an index in the string to a caret index
	  * @param stringIndex Index of a character in the string
	  * @param targetEndOfCharacter Whether the caret at the end of that character should be returned. False if the
	  *                             caret at the beginning of that character should be returned (default)
	  * @return A line index and a relative caret index
	  */
	def stringIndexToCaretIndex(stringIndex: Int, targetEndOfCharacter: Boolean = false) = {
		if (stringIndex < 0)
			0 -> 0
		else {
			// Iterates through the lines, advancing a simulated cursor
			lines.indices.foldLeft[Either[Int, (Int, Int)]](Left(stringIndex)) { (previous, lineIndex) =>
				previous match {
					case result: Right[Int, (Int, Int)] => result
					case Left(remaining) =>
						val line = lines(lineIndex)
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
			} match {
				case Right(result) => result
				// Case: Out of bounds
				case Left(_) =>
					if (isEmpty)
						0 -> 0
					else
						(lines.size - 1) -> lines.last.length
			}
		}
	}
	
	/**
	  * Converts a caret index to an index in the string
	  * @param caretIndex A caret index
	  * @return An index in the string.
	 *         The resulting index is inside the string bounds, or equal to the length of this string
	  */
	def caretIndexToCharacterIndex(caretIndex: Int) =
		if (caretIndex < 0) 0 else caretIndex min text.length
	
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
	def subString(startCaretIndex: Int, endCaretIndex: Int) = {
		if (isEmpty)
			text
		else {
			val startStringIndex = caretIndexToCharacterIndex(startCaretIndex) // Inclusive
			val endStringIndex = (endCaretIndex min text.length) max startStringIndex // Exclusive
			text.substring(startStringIndex, endStringIndex)
		}
	}
	
	/**
	  * Maps a caret index to a line index + caret index on that line
	  * @param index A string index
	  * @return Index of the targeted line + caret index on that line
	  */
	def mapCaretIndex(index: Int) = {
		if (index < 0 || lines.isEmpty)
			0 -> index
		else {
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
	def mapCaretIndex(lineIndex: Int, indexOnLine: Int) = {
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
	def drawTargets(highlightedCaretRanges: Iterable[Range] = Vector()): (Vector[(String, Point)], Vector[(String, Point, Bounds)]) = {
		// If there aren't any highlighted ranges, uses the cached values
		if (highlightedCaretRanges.isEmpty || isEmpty)
			defaultDrawTargets -> Vector()
		else {
			// Converts the ranges to { line index: Character ranges } -map
			val ranges = highlightedCaretRanges.iterator.flatMap { range =>
				val (startLineIndex, startIndexOnLine) = mapCaretIndex(range.head)
				val (endLineIndex, endIndexOnLine) = mapCaretIndex(range.last)
				// Case: Range is contained within a single line
				if (startLineIndex == endLineIndex)
					Vector(startLineIndex -> (startIndexOnLine -> Some(endIndexOnLine)))
				// Case: Range spans multiple lines
				else {
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
			val highlightedStringsBuffer = new VectorBuilder[(String, Point, Bounds)]
			lines.indices.foreach { lineIndex =>
				// Case: Highlights concern this line
				if (ranges.contains(lineIndex)) {
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
						endIndex match {
							// Case: Highlight doesn't span the whole line
							case Some(endIndex) =>
								val endCaret = caretAt(lineIndex, endIndex)
								highlightedStringsBuffer += ((string.substring(startIndex, endIndex min string.length),
									startCaret.start.withY(position.y),
									Bounds.between(startCaret.start, endCaret.end)))
								lastHighlightEnd = Some(endIndex -> endCaret.start.withY(position.y))
							// Case: Highlight takes the rest of the line
							case None =>
								highlightedStringsBuffer += ((string.substring(startIndex),
									startCaret.start.withY(position.y),
									Bounds.between(startCaret.start, lineBounds(lineIndex)._1.bottomRight)))
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
	
	/**
	  * @param lines Lines of text
	  * @return Total text area bounds + relative bounds of each line of text, including their text draw origins.
	  *         All coordinates are relative to a (0,0) anchor position,
	  *         which is determined by text area size and alignment.
	  */
	private def boundsOf(lines: Seq[String]): (Bounds, Vector[(Bounds, Point)]) = {
		val numberOfLines = lines.size
		
		// In case there is only 0-1 line(s) of text, skips the more complex calculations
		if (numberOfLines <= 1) {
			// Bounds of the line, where (0,0) is at text draw origin
			val lineBounds = heightSettings.drawBounds(lines.headOption.getOrElse(""), context)
			// Top left coordinate of this text area, relative to (0,0) anchor position
			val topLeft = alignment.positionAround(lineBounds.size)
			// Line draw position relative to (0,0) anchor position
			val textStart = topLeft - lineBounds.position
			// Bounds of this text area
			val areaBounds = Bounds(topLeft, lineBounds.size)
			
			(areaBounds, Vector(areaBounds -> textStart))
		}
		else {
			// Calculates size information about the whole set of lines
			val rawLineBounds = lines.map { heightSettings.drawBounds(_, context) }
			val totalWidth = rawLineBounds.map { _.width }.max
			val extraLineMargin = {
				if (heightSettings.includesLeading)
					betweenLinesAdjustment
				else
					context.leading + betweenLinesAdjustment
			}
			val totalMargin = (numberOfLines - 1) * extraLineMargin
			val totalHeight = rawLineBounds.map { _.height }.sum + totalMargin
			val areaSize = Size(totalWidth, totalHeight)
			
			// Top left coordinate of this text area, relative to (0,0) anchor position
			val topLeft = alignment.positionAround(areaSize)
			
			// Determines drawing bounds for individual lines
			val lineBoundsBuilder = new VectorBuilder[(Bounds, Point)]()
			// The next top y-coordinate, with appropriate margins included
			var yCursor = topLeft.y
			rawLineBounds.foreach { rawBounds =>
				// Aligns the line start horizontally
				val xMod = alignment.horizontal.position(rawBounds.width, totalWidth)
				val lineTopLeft = Point(topLeft.x + xMod, yCursor)
				// Stores visible line bounds and text draw origin, both relative to (0,0) anchor
				lineBoundsBuilder += ((Bounds(lineTopLeft, rawBounds.size), lineTopLeft - rawBounds.position))
				
				yCursor += rawBounds.height + extraLineMargin
			}
			
			Bounds(topLeft, areaSize) -> lineBoundsBuilder.result()
		}
	}
}
