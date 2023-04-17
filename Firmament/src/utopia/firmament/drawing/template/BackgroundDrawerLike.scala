package utopia.firmament.drawing.template
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Bounds

/**
  * A template for background custom drawer implementations
  * @author Mikko Hilpinen
  * @since 30.11.2020, Reflection v2
  */
trait BackgroundDrawerLike extends CustomDrawer
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @return The background color used when drawing
	  */
	def color: Color
	
	
	// IMPLEMENTED	-------------------------
	
	override def opaque = color.opaque
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		val targetBounds = drawer.clippingBounds match {
			case Some(clipArea) => bounds.overlapWith(clipArea).filter { _.size.isPositive }
			case None => Some(bounds).filter { _.size.isPositive }
		}
		targetBounds.foreach { drawer.draw(_)(DrawSettings.onlyFill(color)) }
	}
}
