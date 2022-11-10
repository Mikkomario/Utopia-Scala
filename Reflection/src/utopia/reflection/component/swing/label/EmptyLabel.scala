package utopia.reflection.component.swing.label

import utopia.paradigm.color.Color

object EmptyLabel
{
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
