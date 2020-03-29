package utopia.reflection.container.swing

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.stack.{CachingStackable, Stackable}
import utopia.reflection.component.swing.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.SingleStackContainer
import utopia.reflection.container.swing.Stack.AwtStackable
import utopia.reflection.shape.StackSize

object SwitchPanel
{
	/**
	  * Creates a new switch panel with initial content
	  * @param initialContent Initial panel content
	  * @tparam C Type of switch panel content
	  * @return A new panel
	  */
	def apply[C <: AwtStackable](initialContent: C) =
	{
		val container = new SwitchPanel[C]
		container.set(initialContent)
		container
	}
}

/**
  * Switch panels may switch the component they contain
  * @author Mikko Hilpinen
  * @since 27.4.2019, v1+
  */
class SwitchPanel[C <: Stackable with AwtComponentRelated] extends SingleStackContainer[C]
	with AwtComponentWrapperWrapper with SwingComponentRelated with AwtContainerRelated with CachingStackable
	with CustomDrawableWrapper
{
	// ATTRIBUTES	-------------------
	
	private val panel = new Panel[C]()
	
	
	// INITIAL CODE	-------------------
	
	addResizeListener(updateLayout())
	
	
	// IMPLEMENTED	-------------------
	
	override def children = super[SingleStackContainer].children
	
	override def drawable = panel
	
	override protected def wrapped = panel
	
	override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].isVisible_=(visible)
	
	override def component = panel.component
	
	// Content size matches that of this panel
	override def updateLayout() =
	{
		content.foreach { _.size = this.size }
		repaint()
	}
	
	override def calculatedStackSize = content.map { _.stackSize } getOrElse StackSize.any
	
	override def components = panel.components
	
	override protected def add(component: C) = panel += component
	
	override protected def remove(component: C) = panel -= component
	
	override def background_=(color: Color) = super[AwtComponentWrapperWrapper].background_=(color)
}
