package utopia.reflection.component.context

import utopia.reflection.color.ColorRole.{Error, Gray, Primary, Secondary, Tertiary, Warning}
import utopia.reflection.color.ColorShade.{Dark, Light, Standard}
import utopia.reflection.color.{ColorRole, ColorScheme, ColorShade, ComponentColor}

/**
  * A common trait for contexts that contain container color information
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
@deprecated("Moved to Firmament", "v2.0")
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
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return The default primary color used in this context
	  */
	def primaryColor = color(Primary)
	
	/**
	  * @return The default secondary color used in this context
	  */
	def secondaryColor = color(Secondary)
	
	/**
	  * @return The default tertiary color used in this context
	  */
	def tertiaryColor = color(Tertiary)
	
	/**
	  * @return A lighter shade of gray for this context
	  */
	def lighterGray = color(Gray, Light)
	
	/**
	  * @return A darker shade of gray for this context
	  */
	def darkerGray = color(Gray, Dark)
	
	/**
	  * @return An error color for this context
	  */
	def errorColor = color(Error)
	
	/**
	  * @return A warning color for this context
	  */
	def warningColor = color(Warning)
	
	
	// OTHER	-------------------------
	
	/**
	  * Finds a color suitable for the target situation
	  * @param targetRole Required color's role / function
	  * @param preferredShade Preferred shade for this context (default = standard/default shade)
	  * @return A color most appropriate for that purpose
	  */
	def color(targetRole: ColorRole, preferredShade: ColorShade = Standard) =
		colorScheme(targetRole).forBackgroundPreferring(containerBackground, preferredShade)
}
