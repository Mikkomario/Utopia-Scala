package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.ShapeConvertible

import java.awt.Shape

/**
  * A graphics object wrapper used for performing drawing operations
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
class Drawer3(protected override val graphics: LazyGraphics) extends GraphicsContextLike[Drawer3] with AutoCloseable
{
	// IMPLEMENTED  --------------------------
	
	override protected def withGraphics(newGraphics: LazyGraphics) = new Drawer3(newGraphics)
	
	override def close() = graphics.close()
	
	
	// OTHER    ------------------------------
	
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
}
