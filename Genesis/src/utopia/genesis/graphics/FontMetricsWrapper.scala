package utopia.genesis.graphics

import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.awt.FontMetrics
import scala.collection.immutable.VectorBuilder
import scala.language.implicitConversions

object FontMetricsWrapper
{
	implicit def wrap(metrics: FontMetrics): FontMetricsWrapper = apply(metrics)
}

/**
  * Used for wrapping a FontMetrics instance, which is considered to be immutable afterwards
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
case class FontMetricsWrapper(private val wrapped: FontMetrics)
{
	/**
	  * @return The space between lines of text in pixels. Please note that this value is included in lineHeight.
	  */
	def leading = wrapped.getLeading
	
	/**
	  * @return The standard character height above the baseline in pixels.
	  */
	def ascent = wrapped.getAscent
	/**
	  * @return The maximum character height above the baseline in pixels.
	  */
	def maxAscent = wrapped.getMaxAscent
	
	/**
	  * @return The standard character height below the baseline in pixels.
	  */
	def descent = wrapped.getDescent
	/**
	  * @return The maximum character height below the baseline in pixels.
	  */
	def maxDescent = wrapped.getMaxDescent
	
	/**
	  * @return The standard line height in this context in pixels. Includes leading.
	  * @see standardCharacterHeight
	  */
	def lineHeight = wrapped.getHeight
	/**
	  * @return The standard height of individual characters in this context (ascent + descent)
	  */
	def standardCharacterHeight = ascent + descent
	/**
	  * @return The largest possible height of individual characters in this context (max ascent + max descent)
	  */
	def maxCharacterHeight = maxAscent + maxDescent
	/**
	  * @return The largest possible width of a character in this context, if such may be calculated.
	  */
	def maxCharacterWidth = Some(wrapped.getMaxAdvance).filterNot { _ < 0 }
	
	/**
	  * @param text Text
	  * @return Width of that text in pixels
	  */
	def widthOf(text: String) = wrapped.stringWidth(text)
	/**
	  * @param char A character
	  * @return Width of that character in pixels
	  */
	def widthOf(char: Char) = wrapped.charWidth(char)
	/**
	  * @param codePoint A character code
	  * @return Width of that character in pixels
	  */
	def widthOf(codePoint: Int) = wrapped.charWidth(codePoint)
	
	/**
	  * @param text A string
	  * @return Caret x-coordinates applicable to that string, where 0 is located at the start of the first character.
	  *         Includes the caret at the end of the string as well, making the resulting collection's length 1 more
	  *         than the string's
	  */
	def caretCoordinatesFrom(text: String) = {
		if (text.isEmpty)
			Vector(0)
		else {
			val totalWidth = widthOf(text)
			val charWidths = text.map(widthOf)
			val totalCharsWidth = charWidths.sum
			
			// <= 10% estimated error range => uses raw character widths
			if ((totalCharsWidth - totalWidth).abs <= totalWidth * 0.1) {
				val builder = new VectorBuilder[Int]()
				var sum = 0
				builder += sum
				charWidths.foreach { w =>
					sum += w
					builder += sum
				}
				builder.result()
			}
			// > 10% estimated error range => uses exact string widths
			else
				0 +: text.indices.tail.map { index => widthOf(text.take(index)) } :+ totalWidth
		}
	}
	
	/**
	  * @param text Text
	  * @return Boundaries of that text, where (0,0) is the leftmost point of the baseline
	  */
	def boundsOf(text: String) = {
		if (text.isEmpty)
			Bounds(Point(0, -ascent), Size(0, ascent + descent))
		else {
			val context = wrapped.getFontRenderContext
			val vector = wrapped.getFont.createGlyphVector(context, text)
			Bounds.fromAwt(vector.getPixelBounds(null, 0, 0))
		}
	}
}
