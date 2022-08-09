package utopia.reflection.component.drawing.template
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Bounds
import utopia.genesis.util.Drawer

/**
  * A template for background custom drawer implementations
  * @author Mikko Hilpinen
  * @since 30.11.2020, v2
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
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		val targetBounds = drawer.clipBounds match
		{
			case Some(clipArea) => bounds.within(clipArea).filter { _.size.isPositive }
			case None => Some(bounds).filter { _.size.isPositive }
		}
		targetBounds.foreach { drawer.onlyFill(color).draw(_) }
	}
}
