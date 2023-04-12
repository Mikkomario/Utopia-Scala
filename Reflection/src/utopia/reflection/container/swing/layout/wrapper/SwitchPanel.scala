package utopia.reflection.container.swing.layout.wrapper

import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.paradigm.color.Color
import utopia.reflection.component.swing.template.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackable}
import utopia.reflection.container.stack.template.SingleStackContainer
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}

object SwitchPanel
{
	/**
	  * Creates a new switch panel with initial content
	  * @param initialContent Initial panel content
	  * @tparam C Type of switch panel content
	  * @return A new panel
	  */
	def apply[C <: AwtStackable](initialContent: C) = new SwitchPanel[C](initialContent)
}

/**
  * Switch panels may switch the component they contain
  * @author Mikko Hilpinen
  * @since 27.4.2019, v1+
  */
class SwitchPanel[C <: ReflectionStackable with AwtComponentRelated](initialContent: C)
	extends SingleStackContainer[C] with AwtComponentWrapperWrapper with SwingComponentRelated
		with AwtContainerRelated with CachingReflectionStackable with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	-------------------
	
	private val panel = new Panel[C]()
	private var _content = initialContent
	
	
	// INITIAL CODE	-------------------
	
	panel += initialContent
	addResizeListener(updateLayout())
	
	
	// IMPLEMENTED	-------------------
	
	override def content: C = _content
	
	override def children = components
	
	override def drawable = panel
	
	override protected def wrapped = panel
	
	override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].visible_=(visible)
	
	override def component = panel.component
	
	override protected def _set(content: C): Unit = {
		panel -= _content
		_content = content
		panel.insert(content, 0)
	}
	
	// Content size matches that of this panel
	override def updateLayout() = {
		content.size = this.size
		repaint()
	}
	
	override def calculatedStackSize = content.stackSize
	
	override def components = panel.components
	
	override def background_=(color: Color) = super[AwtComponentWrapperWrapper].background_=(color)
}
