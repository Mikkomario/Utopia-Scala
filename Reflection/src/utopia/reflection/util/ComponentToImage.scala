package utopia.reflection.util

import java.awt.image.BufferedImage

import javax.swing.CellRendererPane
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.layout.Area

/**
  * Used for drawing components as images
  * @author Mikko Hilpinen
  * @since 18.4.2020, v1.1.1
  */
object ComponentToImage
{
	/**
	  * Draws an image of the specified component. Please note that this draws the image with this component's
	  * current size. If this component doesn't have a size, no image is drawn.
	  * @param component Component to draw
	  * @return An image of this component's paint result.
	  */
	def apply(component: AwtComponentRelated with Area): Image =
	{
		val imageSize = component.size
		
		// For visible displayed components, may simply draw them to an image
		if (component.isInVisibleHierarchy)
		{
			if (imageSize.isPositive)
			{
				val image = new BufferedImage(imageSize.width.toInt, imageSize.height.toInt, BufferedImage.TYPE_INT_ARGB)
				val graphics = image.getGraphics
				component.component.paint(graphics)
				graphics.dispose()
				Image.from(image)
			}
			else
				Image.empty
		}
		else
			apply(component, imageSize)
	}
	
	/**
	  * Draws an image of the specified component.
	  * @param component Component to draw
	  * @param imageSize Size of the resulting image
	  * @return An image of this component's paint result
	  */
	def apply(component: AwtComponentRelated with Area, imageSize: Size) =
	{
		// Prepares the image
		if (imageSize.isPositive)
		{
			val image = new BufferedImage(imageSize.width.toInt, imageSize.height.toInt, BufferedImage.TYPE_INT_ARGB)
			
			// Draws the image using cell renderer panel
			val cellRenderedPanel = new CellRendererPane
			cellRenderedPanel.add(component.component)
			val boundsBefore = component.bounds
			cellRenderedPanel.paintComponent(image.createGraphics(), component.component, cellRenderedPanel,
				Bounds(Point.origin, imageSize).toAwt)
			cellRenderedPanel.remove(component.component)
			component.component.setBounds(boundsBefore.toAwt)
			
			// Returns wrapped image
			Image.from(image)
		}
		else
			Image.empty
	}
}
