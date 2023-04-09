package utopia.reflection.component.drawing.immutable

import utopia.genesis.graphics.{DrawSettings, Drawer3}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.component.drawing.template.ScrollBarDrawerLike
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
case class BoxScrollBarDrawer(barColor: Color, backgroundColor: Option[Color] = None, rounded: Boolean = false)
	extends ScrollBarDrawerLike
{
	private val barDs = DrawSettings.onlyFill(barColor)
	
	override def draw(drawer: Drawer3, bounds: ScrollBarBounds, barDirection: Axis2D) =
	{
		val clipArea = drawer.clippingBounds
		if (clipArea.forall { _.overlapsWith(bounds.area) }) {
			// Fills background and bar
			backgroundColor.foreach { c => drawer.draw(bounds.area)(DrawSettings.onlyFill(c)) }
			if (clipArea.forall { _.overlapsWith(bounds.bar) }) {
				val bar = if (rounded) bounds.bar.toRoundedRectangle(1) else bounds.bar.toShape
				drawer.draw(bar)(barDs)
			}
		}
	}
}
