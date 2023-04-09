package utopia.genesis.graphics

import utopia.flow.operator.ScopeUsable
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.{Bounds, Point, ShapeConvertible}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import java.awt.{Font, Graphics2D, RenderingHints, Shape}

object Drawer
{
	/**
	  * @param graphics A (root level) graphics instance to wrap (lazily called)
	  * @return A new drawer instance based on that graphics instance
	  */
	def apply(graphics: => Graphics2D) = new Drawer(LazyGraphics.wrap(graphics))
}

/**
  * A graphics object wrapper used for performing drawing operations
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
class Drawer(protected override val graphics: LazyGraphics)
	extends GraphicsContextLike[Drawer] with AutoCloseable with ScopeUsable[Drawer]
{
	// COMPUTED ------------------------------
	
	// IMPLEMENTED  --------------------------
	
	override def self = this
	
	override protected def withGraphics(newGraphics: LazyGraphics) = new Drawer(newGraphics)
	
	override def close() = graphics.close()
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param font Font to use
	  * @param color Color to use when drawing text
	  * @return A copy of this drawer for text drawing
	  */
	def forTextDrawing(font: Font, color: Color = Color.textBlack) =
		new TextDrawer(graphics.forTextDrawing(font, color), color)
	
	/**
	  * Clears a visible area from previous drawings
	  * @param area Area to clear
	  */
	def clear(area: Bounds) = {
		val a = area.round
		graphics.value.clearRect(a.position.x.toInt, a.position.y.toInt, a.width.toInt, a.height.toInt)
	}
	
	/**
	  * Draws a shape
	  * @param shape Shape to draw
	  * @param settings Settings to use when drawing that shape (implicit)
	  */
	def draw(shape: Shape)(implicit settings: DrawSettings) = {
		// Fills the shape (optional)
		settings.visibleFillPaint.foreach { p =>
			graphics.value.setPaint(p)
			graphics.value.fill(shape)
		}
		// Draws the edges (optional)
		settings.visibleStrokeSettings.foreach { s =>
			s.modify(graphics.value)
			graphics.value.draw(shape)
		}
	}
	/**
	  * Draws a shape
	  * @param shape Shape to draw
	  * @param settings Settings to use when drawing that shape (implicit)
	  */
	def draw(shape: ShapeConvertible)(implicit settings: DrawSettings): Unit = draw(shape.toShape)
	
	/**
	  * Draws an image
	  * @param image Image to draw
	  * @param position Position of the top left corner of that image
	  * @return True if the drawing has already completed
	  */
	def drawAwtImage(image: java.awt.Image, position: Point = Point.origin) = {
		graphics.value.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
		graphics.value.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
		graphics.value.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
		
		graphics.value.drawImage(image, position.x.round.toInt, position.y.round.toInt, null)
	}
	
	/**
	  * Copies a region of the drawn area to another location
	  * @param area Area that is copied
	  * @param translation The amount of translation applied to the area
	  */
	def copyArea(area: Bounds, translation: HasDoubleDimensions) = {
		if (translation.xyPair.exists { _ != 0 })
			graphics.value.copyArea(
				area.position.x.round.toInt, area.position.y.round.toInt, area.width.round.toInt, area.height.round.toInt,
				translation.x.round.toInt, translation.y.round.toInt)
	}
}
