package utopia.reflection.component.swing.label

import utopia.firmament.component.text.MutableTextComponent
import utopia.firmament.context.text.StaticTextContext
import utopia.firmament.drawing.view.TextViewDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.graphics.MeasuredText
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackLeaf}

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
				  (implicit context: StaticTextContext) =
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
	def contextualWithBackground(backgroundColor: Color, text: LocalizedString = LocalizedString.empty,
								 isHint: Boolean = false)(implicit context: StaticTextContext) =
	{
		val label = contextual(text, isHint)(context.against(backgroundColor))
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
				hasMinWidth: Boolean = true)
	extends Label with MutableTextComponent with CachingReflectionStackable with ReflectionStackLeaf
{
	// ATTRIBUTES	------------------
	
	/**
	  * A mutable pointer that contains this label's text
	  */
	val textPointer = EventfulPointer(initialText)
	/**
	  * A mutable pointer that contains this label's styling
	  */
	val stylePointer = EventfulPointer(TextDrawContext(initialFont, initialTextColor, initialAlignment,
		initialInsets))
	
	private val measuredTextPointer = textPointer.mergeWith(stylePointer) { (text, style) =>
		MeasuredText(text.string, component.getFontMetrics(style.font.toAwt), allowLineBreaks = style.allowLineBreaks)
	}
	
	
	// INITIAL CODE	------------------
	
	addCustomDrawer(TextViewDrawer(measuredTextPointer, stylePointer))
	component.setFont(initialFont.toAwt)
	// Whenever context or text changes, revalidates this component
	textPointer.addContinuousAnyChangeListener { revalidate() }
	stylePointer.addContinuousListener { e =>
		if (e.newValue.font != e.oldValue.font) {
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
	def textDrawContext = stylePointer.value
	def textDrawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	
	// IMPLEMENTED	------------------
	
	override def allowTextShrink: Boolean = !hasMinWidth
	
	override def measuredText: MeasuredText = measuredTextPointer.value
	
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
