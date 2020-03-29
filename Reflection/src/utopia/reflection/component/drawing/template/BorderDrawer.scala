package utopia.reflection.component.drawing.template

import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.shape2D.{Bounds, Size}
import utopia.genesis.util.Drawer
import utopia.reflection.shape.{Border, Insets}

import scala.collection.immutable.VectorBuilder

/**
  * This custom drawer draws a set of borders inside the component without affecting component layout
  * @author Mikko Hilpinen
  * @since 5.5.2019, v1+
  */
trait BorderDrawer extends CustomDrawer
{
	// ABSTRACT	----------------------
	
	/**
	  * @return The border being drawn
	  */
	def border: Border
	
	
	// IMPLEMENTED	------------------
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		// Draws the border (recursively)
		drawBorder(drawer, bounds, border)
	}
	
	
	// OTHER	----------------------
	
	private def drawBorder(drawer: Drawer, bounds: Bounds, border: Border): Unit =
	{
		if (bounds.width > 0 && bounds.height > 0)
		{
			// Sets the color & draws the borders
			if (border.color.isDefined)
			{
				val boundsToDraw = boundsFromInsets(bounds, border.insets)
				if (boundsToDraw.nonEmpty)
					drawer.withColor(border.color.get, border.color.get).disposeAfter { d => boundsToDraw.foreach(d.draw) }
			}
			
			// Moves to the inner border
			border.inner.foreach { b2 => drawBorder(drawer, boundsInsideInsets(bounds, border.insets), b2) }
		}
	}
	
	private def boundsFromInsets(bounds: Bounds, insets: Insets) =
	{
		val buffer = new VectorBuilder[Bounds]
		
		// Top is limited by left
		if (insets.top > 0)
			buffer += Bounds(bounds.topLeft.plusX(insets.left), Size(bounds.width - insets.left, insets.top))
		// Right is limited by top
		if (insets.right > 0)
			buffer += Bounds(bounds.topRight + (-insets.right, insets.top), Size(insets.right, bounds.height - insets.top))
		// Bottom is limited by right
		if (insets.bottom > 0)
			buffer += Bounds(bounds.bottomLeft - (insets.bottom, Y), Size(bounds.width - insets.right, insets.bottom))
		// Left is limited by bottom
		if (insets.left > 0)
			buffer += Bounds(bounds.topLeft, Size(insets.left, bounds.height - insets.bottom))
		
		buffer.result()
	}
	
	private def boundsInsideInsets(original: Bounds, insets: Insets) =
		Bounds(original.position + (insets.left, insets.top), original.size - insets.total)
}
