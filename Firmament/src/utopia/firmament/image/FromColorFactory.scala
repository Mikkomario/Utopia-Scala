package utopia.firmament.image

import utopia.firmament.context.ColorContext
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * Common trait for factories that produce items based on color input
  * @author Mikko Hilpinen
  * @since 15/01/2024, v1.1.1
  */
trait FromColorFactory[+A]
{
	// ABSTRACT ----------------------
	
	/**
	  * @param color Targeted color
	  * @return An item with that color
	  */
	def apply(color: Color): A
	
	
	// OTHER    ----------------------

	/**
	  * @param colors Color set to use
	  * @param context Implicit component creation context
	  * @return An item with a color from the specified set.
	  *         The shade is selected based on context.
	  */
	def apply(colors: ColorSet)(implicit context: ColorContext): A = apply(colors.against(context.background))
	/**
	  * @param role Targeted color role
	  * @param context Implicit color context
	  * @return An item matching that color role with a shade suitable for the current context
	  */
	def apply(role: ColorRole)(implicit context: ColorContext): A = apply(context.color(role))
	/**
	  * @param role Targeted color role
	  * @param preferredShade Preferred color shade (default = standard)
	  * @param context Implicit color context
	  * @return An item matching that color role with a shade suitable for the current context
	  */
	def apply(role: ColorRole, preferredShade: ColorLevel)(implicit context: ColorContext): A =
		apply(context.color.preferring(preferredShade)(role))
}
