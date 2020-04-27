package utopia.reflection.component.context

import utopia.reflection.color.{ColorScheme, ComponentColor}
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.shape.LengthExtensions._

/**
  * This is a more specific instance of base context that also includes information about surrounding container's
  * background color
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
case class ColorContext(base: BaseContextLike, containerBackground: ComponentColor,
						colorSchemeOverride: Option[ColorScheme] = None) extends ColorContextLike with BaseContextWrapper
{
	// IMPLEMENTED	--------------------------
	
	override def colorScheme = colorSchemeOverride.getOrElse(defaultColorScheme)
	
	
	// OTHER	------------------------------
	
	/**
	  * @param colorScheme New color scheme
	  * @return A copy of this context with specified color scheme being used
	  */
	def withColorScheme(colorScheme: ColorScheme) = copy(colorSchemeOverride = Some(colorScheme))
	
	/**
	  * @param textAlignment Text alignment used (default = Left)
	  * @param textInsets Insets placed around the text when drawn (default = small margins, flexible)
	  * @param localizer Localizer used (default = no localization used)
	  * @return Copy of this context that can be used for text components
	  */
	def forTextComponents(textAlignment: Alignment = Alignment.Left,
						  textInsets: StackInsets = StackInsets.symmetric(margins.small.any),
						  localizer: Localizer = NoLocalization) =
		TextContext(this, textAlignment, textInsets, localizer)
}