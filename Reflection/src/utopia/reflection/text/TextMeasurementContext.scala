package utopia.reflection.text

import utopia.paradigm.enumeration.LinearAlignment.Close
import utopia.paradigm.shape.shape2d.{Bounds, Line, Point, Size}
import utopia.paradigm.enumeration.LinearAlignment

/**
  * A common trait for context information that allows deduction of string widths and heights
  * @author Mikko Hilpinen
  * @since 1.11.2020, v2
  */
trait TextMeasurementContext
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return Height of each line of text in this context
	  */
	def lineHeight: Double
	
	/**
	  * @param string A string
	  * @return Width of that string in this context
	  */
	def lineWidthOf(string: String): Double
	
	/**
	  * @return Vertical margin placed between horizontal lines of text in this context
	  */
	def marginBetweenLines: Double
	
	
	// OTHER	------------------------------
	
	/**
	  * @param line A single line of text
	  * @return Size of that line of text
	  */
	def sizeOfLine(line: String) = Size(lineWidthOf(line), lineHeight)
	
	/**
	  * @param lines A set of lines of text
	  * @return Total size of those lines of text in this context
	  */
	def sizeOf(lines: Seq[String]) =
	{
		val numberOfLines = lines.size
		
		if (numberOfLines == 0)
			Size.zero
		else if (numberOfLines == 1)
			sizeOfLine(lines.head)
		else
		{
			val maxWidth = lines.map(lineWidthOf).max
			val totalMargin = if (numberOfLines > 1) (numberOfLines - 1) * marginBetweenLines else 0.0
			val totalHeight = numberOfLines * lineHeight + totalMargin
			Size(maxWidth, totalHeight)
		}
	}
	
	/**
	  * @param text A piece of text that may contain one or more lines
	  * @return Total size of that text in this context
	  */
	def sizeOf(text: String): Size = sizeOf(text.linesIterator.toVector)
	
	/**
	  * @param lines Lines of text
	  * @param alignment Alignment used for placing the lines horizontally
	  * @return Total size of the text area + relative bounds of each line of text
	  */
	def boundsOf(lines: Seq[String], alignment: LinearAlignment = Close) =
	{
		val numberOfLines = lines.size
		
		// In case there is only 0-1 line(s) of text, skips the more complex calculations
		if (numberOfLines == 0)
			Size.zero -> Vector()
		else if (numberOfLines == 1)
		{
			val size = sizeOfLine(lines.head)
			size -> Vector(Bounds(Point.origin, size))
		}
		else
		{
			// Calculates size information about the whole set of lines
			val lineHeight = this.lineHeight
			val lineWidths = lines.map(lineWidthOf)
			val totalWidth = lineWidths.max
			val totalMargin = if (numberOfLines > 1) (numberOfLines - 1) * marginBetweenLines else 0.0
			val totalHeight = numberOfLines * lineHeight + totalMargin
			
			// Determines relative bounds for individual lines
			val lineBounds = lines.indices.iterator.map { index =>
				val width = lineWidths(index)
				val marginsBefore = if (index > 0) (index - 1) * marginBetweenLines else 0.0
				Bounds(Point(alignment.position(width, totalWidth), lineHeight * index + marginsBefore),
					Size(width, lineHeight))
			}.toVector
			Size(totalWidth, totalHeight) -> lineBounds
		}
	}
	
	/**
	  * @param text A string that may contain one or more lines
	  * @param alignment Alignment used for placing the lines horizontally
	  * @return Total size of the text area + relative bounds of each line of text
	  */
	def boundsOf(text: String, alignment: LinearAlignment): (Size, Vector[Bounds]) =
		boundsOf(text.linesIterator.toVector, alignment)
	
	/**
	  * @param line A single line of text
	  * @param lineBounds Bounds that contain that text line
	  * @return Possible caret lines within that text
	  */
	def caretsFromLine(line: String, lineBounds: Bounds) = {
		val startX = lineBounds.leftX
		val startY = lineBounds.topY
		val endY = lineBounds.bottomY
		// Calculates the middle point carets
		val midPoints = line.indices.iterator.drop(1).map { index =>
			val x = startX + lineWidthOf(line.take(index))
			Line(Point(x, startY), Point(x, endY))
		}.toVector
		// Places one caret at each end of bounds
		(Line(Point(startX, startY), Point(startX, endY)) +: midPoints) :+ lineBounds.rightSide
	}
}
