package utopia.reflection.component.swing.button

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.text.TextComponent
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.shape.Alignment
import utopia.reflection.text.Font

object TextButton
{
	/**
	  * Creates a new button
	  * @param text Text displayed in this button
	  * @param font Font used when displaying text
	  * @param color This button's background color
	  * @param textColor Color used for this button's text
	  * @param insets Insets placed around this button's text
	  * @param borderWidth Width of the border inside this button (in pixels)
	  * @param alignment Text alignment used
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param action Action performed when this button is pressed
	  * @return A new button
	  */
	def apply(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
			  insets: StackInsets = StackInsets.any, borderWidth: Double = 0.0, alignment: Alignment = Center,
			  hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set())
			 (action: => Unit) =
	{
		val button = new TextButton(text, font, color, textColor, insets, borderWidth, alignment, hotKeys, hotKeyChars)
		button.registerAction { () => action }
		button
	}
	
	/**
	  * Creates a new button that doesn't have a registered action yet
	  * @param text Button text
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param context Button creation context
	  * @return A new button
	  */
	def contextualWithoutAction(text: LocalizedString, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set())
	                           (implicit context: ButtonContextLike): TextButton =
	{
		new TextButton(text, context.font, context.buttonColor, context.textColor, context.textInsets,
			context.borderWidth, context.textAlignment, hotKeys, hotKeyChars)
	}
	
	/**
	  * Creates a new text button using external context
	  * @param text Button text
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param action Button action
	  * @param context Button context (implicit)
	  * @return The new button
	  */
	def contextual(text: LocalizedString, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set())
	              (action: => Unit)(implicit context: ButtonContextLike): TextButton =
	{
		val button = contextualWithoutAction(text, hotKeys, hotKeyChars)
		button.registerAction(() => action)
		button
	}
}

/**
  * Buttons are used for user interaction
  * @author Mikko Hilpinen
  * @since 25.4.2019, v1+
  * @param initialText Text displayed in this button
  * @param initialFont Font used when displaying text
  * @param color This button's background color
  * @param initialTextColor Text color used on this button (default = black)
  * @param initialInsets Insets placed around this button's text (default = 0)
  * @param borderWidth Width of the border inside this button (in pixels) (default = 0)
  * @param initialAlignment Alignment used for the text (default = Center)
  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
  */
class TextButton(initialText: LocalizedString, initialFont: Font, color: Color,
				 initialTextColor: Color = Color.textBlack, initialInsets: StackInsets = StackInsets.any,
				 borderWidth: Double = 0.0, initialAlignment: Alignment = Center,
				 hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set())
	extends ButtonWithBackground(color, borderWidth) with StackableAwtComponentWrapperWrapper with TextComponent
		with SwingComponentRelated with CustomDrawableWrapper
{
	// ATTRIBUTES	------------------
	
	private val label = new TextLabel(initialText, initialFont, initialTextColor, initialInsets + borderWidth,
		initialAlignment, hasMinWidth = true)
	
	
	// INITIAL CODE	------------------
	
	setup(hotKeys, hotKeyChars)
	
	
	// IMPLEMENTED	------------------
	
	override def insets = super.insets - borderWidth
	
	override def insets_=(newInsets: StackInsets) = super.insets_=(newInsets + borderWidth)
	
	override def drawContext = label.drawContext
	
	override def drawContext_=(newContext: TextDrawContext) = label.drawContext = newContext
	
	override def component = label.component
	
	override def text = label.text
	def text_=(newText: LocalizedString) = label.text = newText
	
	override protected def wrapped = label
	
	override def drawable = label
	
	override def toString = s"Button($text)"
}
