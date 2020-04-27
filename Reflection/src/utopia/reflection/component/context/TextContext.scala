package utopia.reflection.component.context

import utopia.genesis.color.Color
import utopia.reflection.color.ColorSet
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font

/**
  * This class specifies a context for components that display text
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
case class TextContext(base: ColorContextLike, textAlignment: Alignment, textInsets: StackInsets,
					   localizer: Localizer = NoLocalization, fontOverride: Option[Font] = None,
					   promptFontOverride: Option[Font] = None, textColorOverride: Option[Color] = None)
	extends TextContextLike with ColorContextWrapper
{
	// COMPUTED	------------------------------
	
	def forPrimaryColorButtons = ButtonContext.forPrimaryColorButtons(this)
	
	def forSecondaryColorButtons = ButtonContext.forSecondaryColorButtons(this)
	
	
	// IMPLEMENTED	--------------------------
	
	override def font = fontOverride.getOrElse(defaultFont)
	
	override def promptFont = promptFontOverride.getOrElse(font)
	
	override def textColor = textColorOverride.getOrElse(containerBackground.defaultTextColor)
	
	
	// OTHER	------------------------------
	
	def withFont(font: Font) = copy(fontOverride = Some(font))
	
	def withTextAlignment(alignment: Alignment) = copy(textAlignment = alignment)
	
	def withInsets(newInsets: StackInsets) = copy(textInsets = newInsets)
	
	def mapInsets(f: StackInsets => StackInsets) = withInsets(f(textInsets))
	
	def withTextColor(textColor: Color) = copy(textColorOverride = Some(textColor))
	
	def localizedWith(localizer: Localizer) = copy(localizer = localizer)
	
	def forCustomColorButtons(color: ColorSet) = ButtonContext.forCustomColorButtons(this, color)
}