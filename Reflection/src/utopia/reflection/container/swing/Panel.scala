package utopia.reflection.container.swing

import java.awt.{Container, Graphics}

import javax.swing.{JComponent, JPanel, SwingUtilities}
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.ComponentLike
import utopia.reflection.component.drawing.mutable.{CustomDrawable, CustomDrawableWrapper}
import utopia.reflection.component.swing.{AwtComponentRelated, CustomDrawComponent, JWrapper}
import utopia.reflection.container.MultiContainer

/**
* Panel is the standard container that holds other components in it (based on JPanel)
* @author Mikko Hilpinen
* @since 25.2.2019
**/
class Panel[C <: ComponentLike with AwtComponentRelated] extends MultiContainer[C] with JWrapper with
	AwtContainerRelated with CustomDrawableWrapper
{
    // ATTRIBUTES    -------------------
    
	private val panel = new CustomPanel()
	private var _components = Vector[C]()
	
	
	// IMPLEMENTED    ------------------
	
	override def drawable: CustomDrawable = panel
	
	override def component: JComponent with Container = panel
	
	override def components = _components
	
	override protected def add(component: C) =
	{
	    _components :+= component
		// Adds the component to the underlying panel in GUI thread
		SwingUtilities.invokeLater(() => panel.add(component.component))
	}
	
	override protected def remove(component: C) =
	{
	    _components = components filterNot { _.equals(component) }
		// Panel action is done in the GUI thread
	    SwingUtilities.invokeLater(() => panel.remove(component.component))
	}
}

private class CustomPanel extends JPanel with CustomDrawComponent
{
	// INITIAL CODE	-----------------
	
	setLayout(null)
	setOpaque(false)
	
	
	// IMPLEMENTED	-----------------
	
	override def drawBounds = Bounds(Point.origin, Size.of(getSize()) - (1, 1))
	
	override def paintComponent(g: Graphics) = customPaintComponent(g, super.paintComponent)
	
	override def paintChildren(g: Graphics) = customPaintChildren(g, super.paintChildren)
	
	override def isPaintingOrigin = shouldPaintOrigin()
}