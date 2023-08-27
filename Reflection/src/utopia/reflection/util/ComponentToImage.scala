package utopia.reflection.util

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.HasMutableBounds
import utopia.firmament.component.stack.Stackable
import utopia.genesis.image.Image
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.template.AwtComponentRelated

import java.awt.Frame
import java.awt.image.BufferedImage
import javax.swing.CellRendererPane

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
	def apply(component: AwtComponentRelated with HasMutableBounds): Image = {
		// Tries to avoid using 0x0 size by backing up with component preferred size, if available
		val componentSize = component.size
		val imageSize = {
			if (componentSize.sign.isPositive)
				componentSize
			else
				component match {
					case c: Stackable => c.stackSize.optimal
					case _ => componentSize
				}
		}
		
		// For visible displayed components, may simply draw them to an image
		if (component.isInVisibleHierarchy) {
			if (imageSize.sign.isPositive)
				AwtEventThread.blocking {
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
	def apply(component: AwtComponentRelated with HasMutableBounds, imageSize: Size) = {
		// Prepares the image
		if (imageSize.sign.isPositive) {
			AwtEventThread.blocking {
				val image = new BufferedImage(imageSize.width.toInt, imageSize.height.toInt, BufferedImage.TYPE_INT_ARGB)
				
				// Draws the image using cell renderer panel. The panel is added to a temporary invisible frame
				// because child components can't be properly painted otherwise
				val testFrame = new Frame()
				testFrame.setUndecorated(true)
				testFrame.setBackground(Color.white.withAlpha(0.0).toAwt)
				val cellRenderedPanel = new CellRendererPane
				val boundsBefore = component.bounds
				// Resizes the component and updates its contents if necessary
				component match
				{
					case c: Stackable =>
						c.size = imageSize
						c.updateLayout()
					case _ => ()
				}
				cellRenderedPanel.add(component.component)
				testFrame.add(cellRenderedPanel)
				cellRenderedPanel.paintComponent(image.createGraphics(), component.component, cellRenderedPanel,
					Bounds(Point.origin, imageSize).toAwt)
				cellRenderedPanel.remove(component.component)
				component match
				{
					case c: Stackable => c.bounds = boundsBefore
					case _ => component.component.setBounds(boundsBefore.toAwt)
				}
				testFrame.dispose()
				
				// Returns wrapped image
				Image.from(image)
			}
		}
		else
			Image.empty
	}
}
