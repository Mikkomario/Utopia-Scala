package utopia.reflection.component.swing.label

import utopia.reflection.util.ComponentContext

object EmptyLabel
{
	/**
	  * Creates a new label using contextual information
	  * @param context Component creation context
	  * @return A new empty label
	  */
	def contextual(implicit context: ComponentContext) =
	{
		val label = new EmptyLabel
		context.setBorderAndBackground(label)
		label
	}
}

/**
  * This is a simple label that doesn't hold or show any content
  * @author Mikko Hilpinen
  * @since 23.4.2019, v1+
  */
class EmptyLabel extends Label
