package utopia.reflection.container.stack

import utopia.genesis.color.Color
import utopia.genesis.shape.Axis2D
import utopia.genesis.util.Drawer
import utopia.reflection.shape.ScrollBarBounds

object BoxScrollBarDrawer
{
	/**
	  * Creates a new drawer
	  * @param barColor The color used when drawing the scroll bar
	  * @param backgroundColor The color used for drawing scroll bar area background
	  * @return A new drawer
	  */
	def apply(barColor: Color, backgroundColor: Color): BoxScrollBarDrawer = new BoxScrollBarDrawer(barColor, Some(backgroundColor))
	
	/**
	  * Creates a rounded scroll bar drawer
	  * @param barColor Color used when drawing the bar
	  * @return A new drawer
	  */
	def roundedBarOnly(barColor: Color) = new BoxScrollBarDrawer(barColor, None, true)
	
	/**
	  * Creates a rounded scroll bar drawer
	  * @param barColor Color used when drawing the bar
	  * @param backgroundColor Color used when drawing the background
	  * @return A new drawer
	  */
	def rounded(barColor: Color, backgroundColor: Color) = new BoxScrollBarDrawer(barColor, Some(backgroundColor), true)
}

/**
  * This drawer draws a scroll bar with simple rectangles
  * @author Mikko Hilpinen
  * @since 30.4.2019, v1+
  */
class BoxScrollBarDrawer(val barColor: Color, val backgroundColor: Option[Color] = None, val rounded: Boolean = false)
	extends ScrollBarDrawer
{
	override def draw(drawer: Drawer, bounds: ScrollBarBounds, barDirection: Axis2D) =
	{
		// Fills background and bar
		drawer.noEdges.disposeAfter
		{
			d =>
				backgroundColor.foreach { d.withFillColor(_).draw(bounds.area) }
				val bar = if (rounded) bounds.bar.toRoundedRectangle(1) else bounds.bar.toShape
				d.withFillColor(barColor).draw(bar)
		}
	}
}
