package utopia.reflection.component.context

import utopia.reflection.color.{ColorScheme, ComponentColor}

/**
  * A common trait for contexts that contain container color information
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
trait ColorContextLike extends BaseContextLike
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Color scheme used in this context
	  */
	def colorScheme: ColorScheme
	
	/**
	  * @return Background color of the current container component
	  */
	def containerBackground: ComponentColor
}
