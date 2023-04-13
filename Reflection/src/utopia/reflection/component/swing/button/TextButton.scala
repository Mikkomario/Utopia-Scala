package utopia.reflection.component.swing.button

import utopia.firmament.component.text.MutableTextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.model.TextDrawContext
import utopia.genesis.graphics.MeasuredText
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.StackInsets

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
	                           (implicit context: TextContext): TextButton =
	{
		new TextButton(text, context.font, context.background, context.textColor, context.textInsets,
			context.buttonBorderWidth, context.textAlignment, hotKeys, hotKeyChars)
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
	              (action: => Unit)(implicit context: TextContext): TextButton =
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
	extends ButtonWithBackground(color, borderWidth) with StackableAwtComponentWrapperWrapper with MutableTextComponent
		with SwingComponentRelated with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	------------------
	
	private val label = new TextLabel(initialText, initialFont, initialTextColor, initialInsets + borderWidth,
		initialAlignment, hasMinWidth = true)
	
	
	// INITIAL CODE	------------------
	
	setup(hotKeys, hotKeyChars)
	
	
	// IMPLEMENTED	------------------
	
	override def measuredText: MeasuredText = label.measuredText
	
	override def allowTextShrink: Boolean = label.allowTextShrink
	
	override def textInsets = super.textInsets - borderWidth
	
	override def textInsets_=(newInsets: StackInsets) = super.textInsets_=(newInsets + borderWidth)
	
	override def textDrawContext = label.textDrawContext
	
	override def textDrawContext_=(newContext: TextDrawContext) = label.textDrawContext = newContext
	
	override def component = label.component
	
	override def text = label.text
	def text_=(newText: LocalizedString) = label.text = newText
	
	override protected def wrapped = label
	
	override def drawable = label
	
	override def toString = s"Button($text)"
}
