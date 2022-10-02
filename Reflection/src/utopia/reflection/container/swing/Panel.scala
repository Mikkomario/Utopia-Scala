package utopia.reflection.container.swing

import java.awt.{Container, Graphics}

import javax.swing.{JComponent, JPanel}
import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.reflection.component.drawing.mutable.{CustomDrawable, CustomDrawableWrapper}
import utopia.reflection.component.swing.template.{AwtComponentRelated, CustomDrawComponent, JWrapper}
import utopia.reflection.component.template.ComponentLike
import utopia.reflection.container.template.MultiContainer
import utopia.reflection.util.AwtEventThread

/**
* Panel is the standard container that holds other components in it (based on JPanel)
* @author Mikko Hilpinen
* @since 25.2.2019
**/
class Panel[C <: ComponentLike with AwtComponentRelated] extends MultiContainer[C] with JWrapper with
	AwtContainerRelated with CustomDrawableWrapper
{
    // ATTRIBUTES    -------------------
    
	private val panel = AwtEventThread.blocking { new CustomPanel() }
	private var _components = Vector[C]()
	
	
	// IMPLEMENTED    ------------------
	
	override def drawable: CustomDrawable = panel
	
	override def component: JComponent with Container = panel
	
	override def components = _components
	
	override protected def add(component: C, index: Int) =
	{
	    _components = _components.inserted(component, index)
		// Adds the component to the underlying panel in GUI thread
		AwtEventThread.async { panel.add(component.component) }
	}
	
	override protected def remove(component: C) =
	{
	    _components = components filterNot { _.equals(component) }
		// Panel action is done in the GUI thread
		AwtEventThread.async { panel.remove(component.component) }
	}
}

private class CustomPanel extends JPanel with CustomDrawComponent
{
	// INITIAL CODE	-----------------
	
	setLayout(null)
	setOpaque(false)
	
	
	// IMPLEMENTED	-----------------
	
	override def drawBounds = Bounds(Point.origin, Size.of(getSize()))
	
	override def paintComponent(g: Graphics) = customPaintComponent(g, super.paintComponent)
	
	override def paintChildren(g: Graphics) = customPaintChildren(g, super.paintChildren)
	
	override def isPaintingOrigin = shouldPaintOrigin()
	
	/*
	override def repaint() =
	{
		// Only repaints this component if part of a window hierarchy
		if (this.isInWindow)
			super.repaint()
	}*/
}