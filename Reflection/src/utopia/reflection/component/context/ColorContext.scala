package utopia.reflection.component.context

import utopia.flow.operator.ScopeUsable
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorScheme, ColorSet, ColorShade, ComponentColor}
import utopia.reflection.localization.Localizer

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
	
	/**
	  * @param localizer Localizer used (default = no localization used)
	  * @return Copy of this context that can be used for text components
	  */
	def forTextComponents(implicit localizer: Localizer): TextContext = TextContext(this, localizer)
	
	
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
	  * @param colorRole Role for the new background
	  * @param preferredShade Preferred color shade (default = standard)
	  * @return A copy of this context with a background color from the specified set
	  *         (most suitable against current background)
	  */
	def forChildComponentWithRole(colorRole: ColorRole, preferredShade: ColorShade = Standard) =
		copy(containerBackground = color(colorRole, preferredShade))
}