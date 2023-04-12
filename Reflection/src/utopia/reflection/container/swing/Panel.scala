package utopia.reflection.container.swing

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.container.many.MutableMultiContainer
import utopia.firmament.drawing.mutable.{MutableCustomDrawable, MutableCustomDrawableWrapper}
import utopia.flow.util.NotEmpty
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.reflection.component.swing.template.{AwtComponentRelated, CustomDrawComponent, JWrapper}
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.container.template.Container

import java.awt.Graphics
import javax.swing.{JComponent, JPanel}

/**
* Panel is the standard container that holds other components in it (based on JPanel)
* @author Mikko Hilpinen
* @since 25.2.2019
**/
class Panel[C <: ReflectionComponentLike with AwtComponentRelated] extends MutableMultiContainer[C, C] with JWrapper with
	AwtContainerRelated with MutableCustomDrawableWrapper with Container[C]
{
    // ATTRIBUTES    -------------------
    
	private val panel = AwtEventThread.blocking { new CustomPanel() }
	private var _components = Vector[C]()
	
	
	// IMPLEMENTED    ------------------
	
	override def drawable: MutableCustomDrawable = panel
	
	override def component: JComponent with java.awt.Container = panel
	
	override def components = _components
	
	override protected def add(component: C, index: Int): Unit = add(Vector(component), index)
	override protected def add(components: IterableOnce[C], index: Int): Unit = {
		NotEmpty(Vector.from(components)).foreach { newComps =>
			_components = _components.take(index) ++ newComps ++ _components.drop(index)
			// Adds the component to the underlying panel in GUI thread
			AwtEventThread.async { newComps.foreach { c => panel.add(c.component) } }
		}
	}
	override def addBack(component: C, index: Int): Unit = add(component, index)
	override def addBack(components: IterableOnce[C], index: Int): Unit = add(components, index)
	
	override protected def remove(component: C) = {
	    _components = components filterNot { _.equals(component) }
		// Panel action is done in the GUI thread
		AwtEventThread.async { panel.remove(component.component) }
	}
	override protected def remove(components: IterableOnce[C]): Unit = components.iterator.foreach(remove)
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