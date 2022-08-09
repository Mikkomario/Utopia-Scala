package utopia.reflection.component.swing.label

import utopia.paradigm.color.Color
import utopia.reflection.util.ComponentContext

object EmptyLabel
{
	/**
	  * Creates a new label using contextual information
	  * @param context Component creation context
	  * @return A new empty label
	  */
	@deprecated("Please use withBackground instead", "v1.2")
	def contextual(implicit context: ComponentContext) =
	{
		val label = new EmptyLabel
		context.setBorderAndBackground(label)
		label
	}
	
	/**
	  * @param backgroundColor Label background color
	  * @return A new label with specified background color
	  */
	def withBackground(backgroundColor: Color) =
	{
		val label = new EmptyLabel
		label.background = backgroundColor
		label
	}
}

/**
  * This is a simple label that doesn't hold or show any content
  * @author Mikko Hilpinen
  * @since 23.4.2019, v1+
  */
class EmptyLabel extends Label
