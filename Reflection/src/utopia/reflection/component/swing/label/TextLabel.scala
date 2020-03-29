package utopia.reflection.component.swing.label

import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.TextDrawer
import utopia.reflection.component.stack.{CachingStackable, StackLeaf}
import utopia.reflection.component.SingleLineTextComponent
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentContext

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
	  * @param context Component creation context
	  * @return A new label
	  */
	def contextual(text: LocalizedString = LocalizedString.empty, isHint: Boolean = false)(implicit context: ComponentContext) =
	{
		val label = new TextLabel(text, context.font, if (isHint) context.hintTextColor else context.textColor,
			context.insets, context.textAlignment, context.textHasMinWidth)
		context.setBorderAndBackground(label)
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
	
	private val drawer = TextDrawer(initialText, TextDrawContext(initialFont, initialTextColor, initialAlignment, initialInsets))
	
	
	// INITIAL CODE	------------------
	
	addCustomDrawer(drawer)
	component.setFont(initialFont.toAwt)
	// Whenever context or text changes, revalidates this component
	drawer.textPointer.addListener { _ => revalidate() }
	drawer.contextPointer.addListener { e =>
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
	def drawContext = drawer.drawContext
	def drawContext_=(newContext: TextDrawContext) = drawer.drawContext = newContext
	
	
	// IMPLEMENTED	------------------
	
	override protected def updateVisibility(visible: Boolean) = super[Label].isVisible_=(visible)
	
	override def text = drawer.text
	/**
	  * @param newText The new text to be displayed on this label
	  */
	def text_=(newText: LocalizedString) = drawer.text = newText
	
	override def toString = s"Label($text)"
	
	override def updateLayout() = Unit
	
	
	// OTHER	----------------------
	
	/**
	  * Clears all text from this label
	  */
	def clear() = text = LocalizedString.empty
}
