package utopia.reflection.component.context

import utopia.reflection.color.{ColorScheme, ColorSet, ComponentColor}
import utopia.reflection.localization.Localizer
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.shape.LengthExtensions._

/**
  * This is a more specific instance of base context that also includes information about surrounding container's
  * background color
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
case class ColorContext(base: BaseContextLike, containerBackground: ComponentColor,
						colorSchemeOverride: Option[ColorScheme] = None)
	extends ColorContextLike with BaseContextWrapper with BackgroundSensitive[ColorContext] with ScopeUsable[ColorContext]
{
	// COMPUTED ------------------------------
	
	/**
	 * @return A copy of this context where the background is set to primary color scheme color. A shade is picked
	 *         based on existing container background.
	 */
	def withPrimaryBackground = forComponentWithBackground(colorScheme.primary)
	
	/**
	 * @return A copy of this context where the background is set to secondary color scheme color. A shade is picked
	 *         based on existing container background.
	 */
	def withSecondaryBackground = forComponentWithBackground(colorScheme.secondary)
	
	/**
	 * @return A copy of this context where background is set to gray color scheme color. A shade is picked based
	 *         on existing container background. Light gray is preferred.
	 */
	def withLightGrayBackground = copy(containerBackground =
		colorScheme.gray.forBackgroundPreferringLight(containerBackground))
	
	
	// IMPLEMENTED	--------------------------
	
	override def repr = this
	
	override def colorScheme = colorSchemeOverride.getOrElse(defaultColorScheme)
	
	override def inContextWithBackground(color: ComponentColor) = copy(containerBackground = color)
	
	
	// OTHER	------------------------------
	
	/**
	 * @param background Component background color options
	 * @return Context within that component
	 */
	def forComponentWithBackground(background: ColorSet) = copy(containerBackground =
		background.forBackground(containerBackground))
	
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
						  textInsets: StackInsets = StackInsets.symmetric(margins.small.any))
						 (implicit localizer: Localizer): TextContext =
		TextContext(this, textAlignment, textInsets, localizer)
}