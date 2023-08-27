package utopia.reflection.component.swing

import utopia.firmament.awt.AwtEventThread
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import java.awt.Graphics

import javax.swing.JLabel
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.template.CustomDrawComponent

object EmptyJComponent
{
	/**
	  * Creates a new component (in the awt event thread)
	  * @return A new EmptyJComponent
	  */
	def apply() = AwtEventThread.blocking { new EmptyJComponent() }
}

/**
  * This is an attempt for a light-weight swing component that can be used with custom drawing and/or spacing
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
class EmptyJComponent private() extends JLabel with CustomDrawComponent
{
	// ATTRIBUTES   -----------------
	
	private var _isWaitingRepaint = false
	
	
	// IMPLEMENTED	-----------------
	
	override def drawBounds = Bounds(Point.origin, Size(getSize()))
	
	override def paintComponent(g: Graphics) = {
		_isWaitingRepaint = false
		customPaintComponent(g, super.paintComponent)
	}
	
	override def paintChildren(g: Graphics) = customPaintChildren(g, super.paintChildren)
	
	override def isPaintingOrigin = shouldPaintOrigin()
	
	override def repaint() = {
		// This component won't request repaint while the previous request is still in effect
		if (!_isWaitingRepaint) {
			_isWaitingRepaint = true
			super.repaint()
		}
	}
}
