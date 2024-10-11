package utopia.firmament.context.color

import utopia.firmament.context.base.BaseContextPropsView
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.Color

/**
  * Common trait for context instances which specify basic information and color,
  * but only provide read access to the properties, not assuming a static property nature.
  * @author Mikko Hilpinen
  * @since 29.9.2024, v1.4
  */
trait ColorContextPropsView extends BaseContextPropsView
{
	// ABSTRACT	------------------------
	
	/**
	  * @return A pointer that contains the background color
	  *         of the current container component or other such component context
	  */
	def backgroundPointer: Changing[Color]
	/**
	  * @return A pointer that determines the color used in text (and possibly icon) elements
	  */
	def textColorPointer: Changing[Color]
	/**
	  * @return A color to use for text that represents a hint or a disabled element
	  */
	def hintTextColorPointer: Changing[Color]
	
	/**
	  * @return Access to color pointers applicable in this context
	  */
	def colorPointer: ColorAccess[Changing[Color]]
}
