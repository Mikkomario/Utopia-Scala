package utopia.reflection.component.context

import utopia.genesis.color.Color
import utopia.reflection.color.ComponentColor
import utopia.reflection.container.swing.window.dialog.interaction.ButtonColor
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font

/**
  * This class specifies a context for components that display text
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
case class TextContext(base: ColorContext, textAlignment: Alignment, textInsets: StackInsets,
					   localizer: Localizer = NoLocalization, fontOverride: Option[Font] = None,
					   promptFontOverride: Option[Font] = None, textColorOverride: Option[Color] = None,
					   textHasMinWidth: Boolean = true)
	extends TextContextLike with ColorContextWrapper with BackgroundSensitive[TextContext] with ScopeUsable[TextContext]
{
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
	def forGrayFields = forCustomColorButtons(colorScheme.gray.forBackground(containerBackground))
	
	/**
	  * @return A copy of this context, except with zero insets
	  */
	def withoutInsets = withInsets(StackInsets.zero)
	
	
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
	  * @param alignment New text alignment
	  * @return A copy of this context with specified text alignment
	  */
	def withTextAlignment(alignment: Alignment) = copy(textAlignment = alignment)
	
	/**
	  * @param newInsets New text insets
	  * @return A copy of this context with specified insets
	  */
	def withInsets(newInsets: StackInsets) = copy(textInsets = newInsets)
	
	/**
	  * @param f A mapping function for insets
	  * @return A copy of this context with mapped insets
	  */
	def mapInsets(f: StackInsets => StackInsets) = withInsets(f(textInsets))
	
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