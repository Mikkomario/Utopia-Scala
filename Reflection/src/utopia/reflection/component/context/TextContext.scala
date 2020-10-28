package utopia.reflection.component.context

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Direction2D
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.container.swing.window.interaction.ButtonColor
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.{StackInsets, StackLength}
import utopia.reflection.text.Font
import utopia.reflection.shape.LengthExtensions._

/**
  * This class specifies a context for components that display text
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
case class TextContext(base: ColorContext, localizer: Localizer = NoLocalization,
					   textAlignment: Alignment = Alignment.Left, fontOverride: Option[Font] = None,
					   promptFontOverride: Option[Font] = None, textColorOverride: Option[Color] = None,
					   textInsetsOverride: Option[StackInsets] = None,
					   betweenLinesMarginOverride: Option[StackLength] = None,
					   allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false)
	extends TextContextLike with ColorContextWrapper with BackgroundSensitive[TextContext] with ScopeUsable[TextContext]
{
	// ATTRIBUTES	--------------------------
	
	override lazy val textInsets = textInsetsOverride.getOrElse {
		StackInsets.symmetric(margins.small.any, margins.verySmall.any) }
	
	override lazy val betweenLinesMargin = betweenLinesMarginOverride.getOrElse {
		StackLength(0, margins.verySmall, margins.small)
	}
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return A copy of this context that can be used in primary color buttons (specified by color scheme)
	  */
	def forPrimaryColorButtons = ButtonContext.forPrimaryColorButtons(this)
	
	/**
	  * @return A copy of this context that can be used in secondary color buttons (specified by color scheme)
	  */
	def forSecondaryColorButtons = ButtonContext.forSecondaryColorButtons(this)
	
	/**
	  * @return A copy of this context that can be used in gray fields (based on color scheme)
	  */
	def forGrayFields = forCustomColorButtons(colorScheme.gray.forBackgroundPreferringLight(containerBackground))
	
	/**
	  * @return A copy of this context, except with zero insets
	  */
	def withoutInsets = withInsets(StackInsets.zero)
	
	/**
	 * @return A copy of this context with insets that easily expand horizontally
	 */
	def expandingHorizontally = mapInsets { _.mapHorizontal { _.expanding } }
	
	/**
	 * @return A copy of this context where insets expand more easily to right
	 */
	def expandingToRight = expandingTo(Direction2D.Right)
	
	
	// IMPLEMENTED	--------------------------
	
	override def repr = this
	
	override def font = fontOverride.getOrElse(defaultFont)
	
	override def promptFont = promptFontOverride.getOrElse(font)
	
	override def inContextWithBackground(color: ComponentColor) = copy(base = base.inContextWithBackground(color))
	
	override def textColor = textColorOverride.getOrElse(containerBackground.defaultTextColor)
	
	
	// OTHER	------------------------------
	
	/**
	  * @param font A new font
	  * @return Copy of this context with specified font
	  */
	def withFont(font: Font) = copy(fontOverride = Some(font))
	
	/**
	 * @param f A function for transforming the previous font
	 * @return A copy of this context with the specified font
	 */
	def mapFont(f: Font => Font) = copy(fontOverride = Some(f(font)))
	
	/**
	  * @param font A font to be used in prompts
	  * @return Copy of this context with specified font used for prompts
	  */
	def withPromptFont(font: Font) = copy(promptFontOverride = Some(font))
	
	/**
	  * @param alignment New text alignment
	  * @return A copy of this context with specified text alignment
	  */
	def withTextAlignment(alignment: Alignment) = copy(textAlignment = alignment)
	
	/**
	  * @param newInsets New text insets
	  * @return A copy of this context with specified insets
	  */
	def withInsets(newInsets: StackInsets) = copy(textInsetsOverride = Some(newInsets))
	
	/**
	  * @param f A mapping function for insets
	  * @return A copy of this context with mapped insets
	  */
	def mapInsets(f: StackInsets => StackInsets) = withInsets(f(textInsets))
	
	/**
	 * @param direction Direction towards which insets should expand more easily
	 * @return A copy of this context with insets expanding to that direction
	 */
	def expandingTo(direction: Direction2D) = withInsets(textInsets.mapSide(direction) { _.expanding })
	
	/**
	  * @param newMargin New margin to place between lines of text
	  * @return A copy of this context with the specified margin amount
	  */
	def withBetweenLinesMargin(newMargin: StackLength) = copy(betweenLinesMarginOverride = Some(newMargin))
	
	/**
	  * @param textColor New text color
	  * @return A copy of this context with the new text color
	  */
	def withTextColor(textColor: Color) = copy(textColorOverride = Some(textColor))
	
	/**
	  * @param localizer A new localizer
	  * @return A copy of this context using the specified localizer
	  */
	def localizedWith(localizer: Localizer) = copy(localizer = localizer)
	
	/**
	  * @param role Button role
	  * @param preferredShade Preferred button color shade (default = standard)
	  * @return A copy of this context that can be used for creating buttons
	  */
	def forButtons(role: ColorRole, preferredShade: ColorShade = Standard) =
		ButtonContext.forRole(this, role, preferredShade)
	
	/**
	  * @param color Button color
	  * @return A copy of this context that can be used for creating buttons of desired color
	  */
	def forButtons(color: ButtonColor) = ButtonContext.forCustomColorButtons(this,
		color.toColor(this))
	
	/**
	  * @param color Button color
	  * @return A copy of this context that can be used in buttons
	  */
	def forCustomColorButtons(color: ComponentColor) = ButtonContext.forCustomColorButtons(this, color)
}