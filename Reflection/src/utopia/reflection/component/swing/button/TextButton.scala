package utopia.reflection.component.swing.button

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.{BorderDrawer, CustomDrawableWrapper}
import utopia.reflection.component.TextComponent
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.component.swing.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.swing.label.TextLabel
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, Border, StackInsets}
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentContext

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
	  * @param action Action performed when this button is pressed
	  * @return A new button
	  */
	def apply(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
			  insets: StackInsets = StackInsets.any, borderWidth: Double = 0.0, alignment: Alignment = Center)
			 (action: () => Unit) =
	{
		val button = new TextButton(text, font, color, textColor, insets, borderWidth, alignment)
		button.registerAction(action)
		button
	}
	
	/**
	  * Creates a new text button using external context
	  * @param text Button text
	  * @param action The action performed when this button is pressed, if any (Default = None)
	  * @param context Button context (implicit)
	  * @return The new button
	  */
	def contextual(text: LocalizedString, action: Option[() => Unit] = None)(implicit context: ComponentContext): TextButton =
	{
		val button = new TextButton(text, context.font, context.buttonBackground, context.textColor, context.insets,
			context.borderWidth, context.textAlignment)
		action.foreach(button.registerAction)
		button
	}
	
	/**
	  * Creates a new text button using external context
	  * @param text Button text
	  * @param action Button action
	  * @param context Button context (implicit)
	  * @return The new button
	  */
	def contextual(text: LocalizedString, action: () => Unit)(implicit context: ComponentContext): TextButton =
		contextual(text, Some(action))
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
  */
class TextButton(initialText: LocalizedString, initialFont: Font, val color: Color,
				 initialTextColor: Color = Color.textBlack, initialInsets: StackInsets = StackInsets.any,
				 val borderWidth: Double = 0.0, initialAlignment: Alignment = Center)
	extends StackableAwtComponentWrapperWrapper with TextComponent with ButtonLike with SwingComponentRelated with CustomDrawableWrapper
{
	// ATTRIBUTES	------------------
	
	private val label = new TextLabel(initialText, initialFont, initialTextColor, initialInsets + borderWidth,
		initialAlignment, hasMinWidth = true)
	private val borderPointer = new PointerWithEvents(makeBorder(color))
	
	
	// INITIAL CODE	------------------
	
	component.setFocusable(true)
	setHandCursor()
	background = color
	
	initializeListeners()
	
	// Adds border drawing
	if (borderWidth > 0)
	{
		addCustomDrawer(new BorderDrawer(borderPointer))
		borderPointer.addListener { _ => repaint() }
	}
	
	
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
	
	override protected def updateStyleForState(newState: ButtonState) =
	{
		val newColor = newState.modify(color)
		background = newColor
		borderPointer.value = makeBorder(newColor)
	}
	
	override def toString = s"Button($text)"
	
	
	// OTHER	----------------------
	
	private def makeBorder(baseColor: Color) = Border.raised(borderWidth, baseColor, 0.5)
}
