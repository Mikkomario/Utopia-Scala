package utopia.genesis.graphics

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Alignment.TopLeft
import utopia.genesis.shape.shape2D.{Alignment, Point}

import java.awt.Font

/**
  * Used for drawing text
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
class TextDrawer(protected override val graphics: LazyGraphics) extends GraphicsContextLike[TextDrawer]
{
	// IMPLEMENTED  -----------------------------
	
	override protected def withGraphics(newGraphics: LazyGraphics) = new TextDrawer(newGraphics)
	
	
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
	def withColor(color: Color) = withMutatedGraphics { _.setColor(color.toAwt) }
	
	def draw(text: String, anchor: Point, anchorPosition: Alignment = TopLeft)
	        (implicit heightSettings: TextDrawHeight) =
	{
		// Calculates text bounds for positioning
		val bounds = heightSettings.drawBounds(text, fontMetrics)
		// Positions the text using the anchor
		val topLeft = anchorPosition.positionAround(bounds.size, anchor)
		val textStart = topLeft - bounds.position
		graphics.value.drawString(text, textStart.x.toFloat, textStart.y.toFloat)
	}
}
