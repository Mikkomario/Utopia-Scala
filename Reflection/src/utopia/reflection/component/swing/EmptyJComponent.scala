package utopia.reflection.component.swing

import java.awt.Graphics

import javax.swing.JLabel
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}

/**
  * This is an attempt for a light-weight swing component that can be used with custom drawing and/or spacing
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class EmptyJComponent extends JLabel with CustomDrawComponent
{
	// IMPLEMENTED	-----------------
	
	override def drawBounds = Bounds(Point.origin, Size.of(getSize()) - (1, 1))
	
	override def paintComponent(g: Graphics) = customPaintComponent(g, super.paintComponent)
	
	override def paintChildren(g: Graphics) = customPaintChildren(g, super.paintChildren)
	
	override def isPaintingOrigin = shouldPaintOrigin()
}
