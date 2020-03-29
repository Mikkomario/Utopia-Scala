package utopia.reflection.container.swing

import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.stack.Stackable
import utopia.reflection.component.swing.{AwtComponentRelated, SwingComponentRelated}
import utopia.reflection.container.stack.FramingLike
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.{StackInsets, StackSize}

object Framing
{
	/**
	  * Creates a symmetric framing
	  * @param component Component being framed
	  * @param margins Symmetric margins placed around the component on each side (equal margins on both sides).
	  *                The total amount of margins around the component will be twice the amount of these margins
	  * @tparam C Type of wrapped component
	  * @return A framing
	  */
	def symmetric[C <: AwtStackable](component: C, margins: StackSize) =
		new Framing(component, StackInsets.symmetric(margins * 2))
}

/**
  * Framings are containers that present a component with scaling 'frames', like a painting
  * @author Mikko Hilpinen
  * @since 26.4.2019, v1+
  */
class Framing[C <: Stackable with AwtComponentRelated](initialComponent: C, val insets: StackInsets) extends
	FramingLike[C] with SwingComponentRelated with AwtContainerRelated with CustomDrawableWrapper
{
	// ATTRIBUTES	--------------------
	
	private val panel = new Panel[C]()
	
	
	// INITIAL CODE	--------------------
	
	set(initialComponent)
	// Each time Framing size changes, changes content size too
	addResizeListener(updateLayout())
	
	
	// IMPLEMENTED	--------------------
	
	override def drawable = panel
	
	override protected def container = panel
	
	override def component = panel.component
	
	override protected def add(component: C) = panel += component
	
	override protected def remove(component: C) = panel -= component
}
