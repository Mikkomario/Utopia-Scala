package utopia.firmament.drawing.template

import utopia.firmament.model.Border
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape2d.insets.Insets

import scala.collection.immutable.VectorBuilder

/**
  * This custom drawer draws a set of borders inside the component without affecting component layout
  * @author Mikko Hilpinen
  * @since 5.5.2019, Reflection v1+
  */
trait BorderDrawerLike extends CustomDrawer
{
	// ABSTRACT	----------------------
	
	/**
	  * @return The border being drawn
	  */
	def border: Border
	
	
	// IMPLEMENTED	------------------
	
	override def opaque = false
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		// Draws the border (recursively)
		// Uses anti-aliasing
		drawBorder(drawer.antialiasing, bounds, border)
	}
	
	
	// OTHER	----------------------
	
	private def drawBorder(drawer: Drawer, bounds: Bounds, border: Border): Unit = {
		val roundedBounds = bounds.floor
		if (roundedBounds.width > 0 && roundedBounds.height > 0) {
			// Sets the color & draws the borders
			border.color.foreach { color =>
				// TODO: Rounding here helps with many repainting issues.
				//  We will likely need better rounding tools going forward, however.
				val boundsToDraw = boundsFromInsets(roundedBounds, border.insets.round)
				if (boundsToDraw.nonEmpty) {
					implicit val ds: DrawSettings = DrawSettings.onlyFill(color)
					boundsToDraw.foreach { drawer.draw(_) }
				}
			}
			
			// Moves to the inner border
			border.inner.foreach { b2 => drawBorder(drawer, boundsInsideInsets(roundedBounds, border.insets), b2) }
		}
	}
	
	private def boundsFromInsets(bounds: Bounds, insets: Insets) = {
		val buffer = new VectorBuilder[Bounds]
		
		// FIXME: Probably rounding errors here
		
		// Top is limited by left
		if (insets.top > 0)
			buffer += Bounds(bounds.topLeft + X(insets.left), Size(bounds.width - insets.left, insets.top))
		// Right is limited by top
		if (insets.right > 0)
			buffer += Bounds(bounds.topRight + Vector2D(-insets.right, insets.top), Size(insets.right, bounds.height - insets.top))
		// Bottom is limited by right
		if (insets.bottom > 0)
			buffer += Bounds(bounds.bottomLeft - Y(insets.bottom), Size(bounds.width - insets.right, insets.bottom))
		// Left is limited by bottom
		if (insets.left > 0)
			buffer += Bounds(bounds.topLeft, Size(insets.left, bounds.height - insets.bottom))
		
		buffer.result()
	}
	
	private def boundsInsideInsets(original: Bounds, insets: Insets) =
		Bounds(original.position + Vector2D(insets.left, insets.top), original.size - insets.total)
}
