package utopia.reflection.component.swing.label

import utopia.reflection.component.drawing.mutable.{MutableCustomDrawable, MutableCustomDrawableWrapper}
import utopia.reflection.component.swing.EmptyJComponent
import utopia.reflection.component.swing.template.JWrapper

object Label
{
	/**
	  * @return A new empty label
	  */
	def apply() = new EmptyLabel()
}

/**
  * Labels are used as basic UI-elements to display either text or an image. Labels may, of course, also be empty
  * @author Mikko Hilpinen
  * @since 21.4.2019, v1+
  */
class Label extends JWrapper with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	-----------------
	
	private val _label = EmptyJComponent()
	
	
	// IMPLEMENTED	-----------------
	
	override def drawable: MutableCustomDrawable = _label
	
	override def component = _label
}
