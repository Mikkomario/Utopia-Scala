package utopia.reflection.component.swing.label

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.swing.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.{RefreshableWithPointer, TextComponent}
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.{Alignment, StackInsets}
import utopia.reflection.text.Font
import utopia.reflection.util.ComponentContext

object ItemLabel
{
	/**
	  * Creates a new label using contextual information
	  * @param content Initial label content
	  * @param displayFunction A function for displaying label data
	  * @param context Component creation context
	  * @tparam A Type of presented item
	  * @return A new label
	  */
	def contextual[A](content: A, displayFunction: DisplayFunction[A] = DisplayFunction.raw)
					 (implicit context: ComponentContext) = contextualWithPointer(
		new PointerWithEvents(content), displayFunction)
	
	def contextualWithPointer[A](pointer: PointerWithEvents[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw)
								(implicit context: ComponentContext) =
	{
		val label = new ItemLabel[A](pointer, displayFunction, context.font, context.textColor, context.insets,
			context.textAlignment, context.textHasMinWidth)
		context.setBorderAndBackground(label)
		label
	}
}

/**
  * These labels display an item of a specific type, transforming it into text format
  * @author Mikko Hilpinen
  * @since 24.4.2019, v1
  * @tparam A The type of item displayed in this label
  * @param contentPointer Pointer for the value displayed by this label
  * @param displayFunction A function that transforms the item to displayable text
  * @param initialFont The font used in this label
  * @param initialTextColor The color used for displaying text (default = black)
  * @param initialInsets The insets around the text in this label (default = 0 on each side)
 *  @param initialAlignment The alignment used for this component initially (default = Left)
  * @param hasMinWidth Whether this label should have minimum width (always show all content text) (default = true)
  */
class ItemLabel[A](override val contentPointer: PointerWithEvents[A], val displayFunction: DisplayFunction[A], initialFont: Font,
				   initialTextColor: Color = Color.textBlack, initialInsets: StackInsets = StackInsets.any,
				   initialAlignment: Alignment = Alignment.Left, hasMinWidth: Boolean = true)
	extends StackableAwtComponentWrapperWrapper with TextComponent with SwingComponentRelated
		with CustomDrawableWrapper with RefreshableWithPointer[A]
{
	// ATTRIBUTES	--------------------
	
	private val label = new TextLabel(displayFunction(contentPointer.value), initialFont, initialTextColor, initialInsets,
		initialAlignment, hasMinWidth)
	
	
	// INITIAL CODE	--------------------
	
	// Reacts to changes in text
	addContentListener(ContentUpdateListener)
	
	
	// IMPLEMENTED	--------------------
	
	override def component = label.component
	
	override protected def wrapped = label
	
	override def drawContext = label.drawContext
	
	override def drawContext_=(newContext: TextDrawContext) = label.drawContext = newContext
	
	override def drawable = label
	
	override def toString = s"Label($text)"
	
	override def text = label.text
	
	
	// OTHER	------------------------
	
	/**
	  * Refreshes the displayed text (but not content) in this label. Useful when using display functions that rely
	  * on external state.
	  */
	def refreshText() = label.text = displayFunction(content)
	
	
	// NESTED CLASSES	----------------
	
	private object ContentUpdateListener extends ChangeListener[A]
	{
		override def onChangeEvent(event: ChangeEvent[A]) = label.text = displayFunction(event.newValue)
	}
}
