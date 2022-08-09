package utopia.reflection.component.swing.label

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.paradigm.color.Color
import utopia.reflection.color.ComponentColor
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.template.layout.stack.{CachingStackable, StackLeaf}
import utopia.reflection.component.context.{BackgroundSensitive, TextContextLike}
import utopia.reflection.component.drawing.view.TextViewDrawer
import utopia.reflection.component.template.text.SingleLineTextComponent
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object TextLabel
{
	/**
	  * @param text The localized text displayed in this label
	  * @param font The text font
	  * @param textColor Color used for the text (default = black)
	  * @param insets The insets around the text in this label
	 *  @param alignment Alignment used for the text (default = left)
	  * @param hasMinWidth Whether this label always presents the whole text (default = true)
	  * @return A new label with specified text
	  */
	def apply(text: LocalizedString, font: Font, textColor: Color = Color.textBlack,
			  insets: StackInsets = StackInsets.any, alignment: Alignment = Alignment.Left, hasMinWidth: Boolean = true) =
		new TextLabel(text, font, textColor, insets, alignment, hasMinWidth)
	
	/**
	  * Creates a new label using contextual information
	  * @param text Label text (default = empty)
	  * @param isHint Whether this label should be considered a hint (default = false)
	  * @param context Component creation context (implicit)
	  * @return A new label
	  */
	def contextual(text: LocalizedString = LocalizedString.empty, isHint: Boolean = false)
				  (implicit context: TextContextLike) =
	{
		val label = new TextLabel(text, context.font, if (isHint) context.hintTextColor else context.textColor,
			context.textInsets, context.textAlignment, !context.allowTextShrink)
		label
	}
	
	/**
	  * Creates a new label that has an opaque background color
	  * @param backgroundColor Background color / fill color of this label
	  * @param text Text displayed in this label (default = empty)
	  * @param isHint Whether this label is considered a hint (default = false)
	  * @param context Component creation context (implicit)
	  * @return A new label
	  */
	def contextualWithBackground(backgroundColor: ComponentColor, text: LocalizedString = LocalizedString.empty,
								 isHint: Boolean = false)(implicit context: BackgroundSensitive[TextContextLike]) =
	{
		val label = contextual(text, isHint)(context.inContextWithBackground(backgroundColor))
		label.background = backgroundColor
		label
	}
}

/**
  * This label presents (localized) text
  * @author Mikko Hilpinen
  * @since 23.4.2019, v1+
  * @param initialText The text initially displayed in this label
  * @param initialFont The font used in this label
  * @param initialTextColor Color used in this label's text
  * @param initialInsets The insets placed around the text initially (default = 0 on each side)
  * @param initialAlignment Alignment used for positioning the text within this label
  * @param hasMinWidth Whether this text label always presents the whole text (default = true)
  */
class TextLabel(initialText: LocalizedString, initialFont: Font, initialTextColor: Color = Color.textBlack,
				initialInsets: StackInsets = StackInsets.any, initialAlignment: Alignment = Alignment.Left,
				override val hasMinWidth: Boolean = true)
	extends Label with SingleLineTextComponent with CachingStackable with StackLeaf
{
	// ATTRIBUTES	------------------
	
	/**
	  * A mutable pointer that contains this label's text
	  */
	val textPointer = new PointerWithEvents(initialText)
	/**
	  * A mutable pointer that contains this label's styling
	  */
	val stylePointer = new PointerWithEvents(TextDrawContext(initialFont, initialTextColor, initialAlignment,
		initialInsets))
	
	
	// INITIAL CODE	------------------
	
	addCustomDrawer(TextViewDrawer(textPointer, stylePointer))
	component.setFont(initialFont.toAwt)
	// Whenever context or text changes, revalidates this component
	textPointer.addListener { _ => revalidate() }
	stylePointer.addListener { e =>
		if (e.newValue.font != e.oldValue.font)
		{
			component.setFont(e.newValue.font.toAwt)
			revalidate()
		}
		else if (e.newValue.insets != e.oldValue.insets)
			revalidate()
		else
			repaint()
	}
	
	
	// COMPUTED	----------------------
	
	/**
	  * @return The current drawing context used
	  */
	def drawContext = stylePointer.value
	def drawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	
	// IMPLEMENTED	------------------
	
	override protected def updateVisibility(visible: Boolean) = super[Label].visible_=(visible)
	
	override def text = textPointer.value
	/**
	  * @param newText The new text to be displayed on this label
	  */
	def text_=(newText: LocalizedString) = textPointer.value = newText
	
	override def toString = s"Label($text)"
	
	override def updateLayout() = repaint()
	
	
	// OTHER	----------------------
	
	/**
	  * Clears all text from this label
	  */
	def clear() = text = LocalizedString.empty
}
