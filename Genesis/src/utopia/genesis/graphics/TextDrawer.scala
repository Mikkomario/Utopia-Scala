package utopia.genesis.graphics

import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.TopLeft
import utopia.paradigm.shape.shape2d.{Bounds, Point}

import java.awt.Font

/**
  * Used for drawing text
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
class TextDrawer(protected override val graphics: LazyGraphics, color: Color) extends GraphicsContextLike[TextDrawer]
{
	// IMPLEMENTED  -----------------------------
	
	override protected def withGraphics(newGraphics: LazyGraphics) = new TextDrawer(newGraphics, color)
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param font New font to use
	  * @return A copy of this drawer instance with that font used
	  */
	def withFont(font: Font) = withMutatedGraphics { _.setFont(font) }
	/**
	  * @param color New color to use
	  * @return A copy of this drawer instance with that color used
	  */
	def withColor(color: Color) = if (color == this.color) this else new TextDrawer(graphics, color)
	
	/**
	  * Draws a string without any additional positioning
	  * @param text Text to draw
	  * @param baseLineStart Location of the leftmost text baseline point
	  */
	def draw(text: String, baseLineStart: Point) = {
		graphics.value.setColor(color.toAwt)
		graphics.value.drawString(text, baseLineStart.x.toFloat, baseLineStart.y.toFloat)
	}
	
	/**
	  * Draws a single line of text around a specific position
	  * @param text Text to draw (should span only one line)
	  * @param anchor Anchor position, around which the text is drawn (default = (0,0))
	  * @param anchorPosition Alignment used for interpreting the anchor. E.g. for BottomRight alignment,
	  *                       the anchor position will be located at the bottom left corner of the drawn text area.
	  *                       Default is TopLeft.
	  * @param heightSettings Settings used for determining, how text height is calculated
	  *                       (whether margins are included and so on)
	  */
	def drawAround(text: String, anchor: Point = Point.origin, anchorPosition: Alignment = TopLeft)
	        (implicit heightSettings: TextDrawHeight) =
	{
		// Calculates text bounds for positioning
		val bounds = heightSettings.drawBounds(text, fontMetrics)
		// Positions the text using the anchor
		val topLeft = anchorPosition.positionAround(bounds.size, anchor)
		val textStart = topLeft - bounds.position
		graphics.value.setColor(color.toAwt)
		graphics.value.drawString(text, textStart.x.toFloat, textStart.y.toFloat)
	}
	
	/**
	  * Draws a pre-measured piece of text
	  * @param text Text to draw, including alignment settings
	  * @param anchor Location around which the text is drawn. The meaning of this location is dependent from the
	  *               specified alignment.
	  */
	def drawMeasured(text: MeasuredText, anchor: Point) = {
		graphics.value.setColor(color.toAwt)
		text.defaultDrawTargets.foreach { case (text, relativePosition) =>
			val actualPosition = anchor + relativePosition
			graphics.value.drawString(text, actualPosition.x.toFloat, actualPosition.y.toFloat)
		}
	}
	
	/**
	  * Draws a pre-measured text within a specific set of bounds. Respects the text's alignment
	  * @param text Text to draw
	  * @param within Area within which the text would be positioned using the text's own alignment
	  */
	def drawMeasured(text: MeasuredText, within: Bounds) = {
		graphics.value.setColor(color.toAwt)
		val areaPosition = text.alignment.position(text.size, within)
		val areaOrigin = areaPosition - text.bounds.position
		text.defaultDrawTargets.foreach { case (text, relativePosition) =>
			val actualPosition = areaOrigin + relativePosition
			graphics.value.drawString(text, actualPosition.x.toFloat, actualPosition.y.toFloat)
		}
	}
	
	// TODO: Add versions which draw highlighting (and possibly caret) as well
}
