package utopia.firmament.drawing.immutable

import utopia.firmament.drawing.template.BorderDrawerLike
import utopia.firmament.model.Border
import utopia.genesis.graphics.DrawLevel
import utopia.genesis.graphics.DrawLevel.{Background, Foreground, Normal}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.shape.shape2d.insets.{Insets, SidesFactory}

object BorderDrawer
{
	// OTHER    ---------------------------
	
	/**
	 * @param color Color for drawing the borders
	 * @return A factory for finalizing this drawer
	 */
	def apply(color: Color): BorderDrawerFactory = BorderDrawerFactory(Some(color), Normal)
	/**
	 * @param border Border to draw
	 * @param level Drawing depth (default = normal)
	 * @return A new border drawer
	 */
	def apply(border: Border, level: DrawLevel = Normal) = new BorderDrawer(border, level)
	
	
	// NESTED   ---------------------------
	
	case class BorderDrawerFactory(color: Option[Color], level: DrawLevel) extends SidesFactory[Double, BorderDrawer]
	{
		// COMPUTED -----------------------
		
		/**
		 * @return Copy of this drawer drawing behind the component contents
		 */
		def background = withDrawLevel(Background)
		/**
		 * @return Copy of this drawer drawing above the component and its children
		 */
		def foreground = withDrawLevel(Foreground)
		
		
		// IMPLEMENTED  -------------------
		
		override def withSides(sides: Map[Direction2D, Double]): BorderDrawer =
			new BorderDrawer(Border(color).withSides(sides), level)
			
		
		// OTHER    -----------------------
		
		/**
		 * @param sides Lengths of the border sides
		 * @return A new drawer drawing borders of the specified widths
		 */
		def apply(sides: Insets) = new BorderDrawer(Border(color)(sides), level)
		
		/**
		 * @param level New draw level to assign to this factory
		 * @return Copy of this factory with the specified draw level
		 */
		def withDrawLevel(level: DrawLevel) = copy(level = level)
	}
}

/**
  * This custom drawer draws a set of borders inside the component without affecting component layout
  * @author Mikko Hilpinen
  * @since 5.5.2019, Reflection v1+
  */
case class BorderDrawer(override val border: Border, drawLevel: DrawLevel) extends BorderDrawerLike
