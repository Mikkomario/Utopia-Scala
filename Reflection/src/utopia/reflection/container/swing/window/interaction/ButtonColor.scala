package utopia.reflection.container.swing.window.interaction

import utopia.firmament.context.ColorContext
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole, ColorSet}

/**
  * A common trait for color definitions for standard dialog buttons
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.2
  */
trait ButtonColor
{
	/**
	  * @param context Context where the button is being created
	  * @return Button color used on that background
	  */
	def toColor(implicit context: ColorContext): Color
}

object ButtonColor
{
	// COMPUTED	-----------------------------
	
	/**
	  * Uses primary color scheme color, adjusting the shade according to context background. This button color is
	  * best for alternative (non-default) choice buttons.
	  */
	def primary = Role(ColorRole.Primary)
	
	/**
	  * Uses secondary color scheme color, adjusting the shade according to context background. This button color is
	  * best for the default/primary buttons.
	  */
	def secondary = Role(ColorRole.Secondary)
	
	/**
	  * Uses secondary color scheme color, adjusting the shade according to context background. This button color is
	  * best for additional highlighted buttons or to be used in specific contexts where secondary or primary colors
	  * are not suitable.
	  */
	def tertiary = Role(ColorRole.Tertiary)
	
	
	// NESTED	-----------------------------
	
	/**
	  * Uses color specific for targeted role and shade
	  * @param role Target color / button role
	  * @param preferredShade Preferred shade of that color (default = standard/default shade)
	  */
	case class Role(role: ColorRole, preferredShade: ColorLevel = Standard) extends ButtonColor
	{
		override def toColor(implicit context: ColorContext) = context.color.preferring(preferredShade)(role)
	}
	
	/**
	  * Always uses the specified fixed color. This is useful when you wish to override both the hue and the shade
	  * of the resulting button's background.
	  * @param color Color to use
	  */
	case class Fixed(color: Color) extends ButtonColor
	{
		override def toColor(implicit context: ColorContext) = color
	}
	
	/**
	  * Uses one color from the specified color set, selecting the shade based on context background. This is useful
	  * when you wish to use a color outside the color scheme primary or secondary
	  * @param colorSet Color set to use
	  */
	case class CustomSet(colorSet: ColorSet) extends ButtonColor
	{
		override def toColor(implicit context: ColorContext) = colorSet.against(context.background)
	}
}
