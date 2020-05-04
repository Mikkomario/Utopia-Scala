package utopia.reflection.container.swing.window.dialog.interaction

import utopia.reflection.color.{ColorSet, ComponentColor}
import utopia.reflection.component.context.TextContextLike

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
	def toColor(implicit context: TextContextLike): ComponentColor
}

object ButtonColor
{
	/**
	  * Uses primary color scheme color, adjusting the shade according to context background. This button color is
	  * best for alternative (non-default) choice buttons.
	  */
	case object Primary extends ButtonColor
	{
		override def toColor(implicit context: TextContextLike)  = context.colorScheme.primary.forBackground(
			context.containerBackground)
	}
	
	/**
	  * Uses secondary color scheme color, adjusting the shade according to context background. This button color is
	  * best for the default/primary buttons.
	  */
	case object Secondary extends ButtonColor
	{
		override def toColor(implicit context: TextContextLike) = context.colorScheme.secondary.forBackground(
			context.containerBackground)
	}
	
	/**
	  * Always uses the specified fixed color. This is useful when you wish to override both the hue and the shade
	  * of the resulting button's background.
	  * @param color Color to use
	  */
	case class Fixed(color: ComponentColor) extends ButtonColor
	{
		override def toColor(implicit context: TextContextLike) = color
	}
	
	/**
	  * Uses one color from the specified color set, selecting the shade based on context background. This is useful
	  * when you wish to use a color outside the color scheme primary or secondary
	  * @param colorSet Color set to use
	  */
	case class CustomSet(colorSet: ColorSet) extends ButtonColor
	{
		override def toColor(implicit context: TextContextLike) = colorSet.forBackground(context.containerBackground)
	}
}
