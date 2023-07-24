package utopia.reflection.component.swing.label

import utopia.firmament.component.display.PoolWithPointer
import utopia.firmament.component.text.MutableStyleTextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.model.TextDrawContext
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.MeasuredText
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.StackInsets

object ViewLabel
{
	/**
	  * Creates a new item label
	  * @param pointer Pointer to the displayed content
	  * @param font Font used in this label's text
	  * @param displayFunction Function for converting item to a localized string (default = use toString)
	  * @param insets Insets to place around the text in this label (default = any insets, preferring 0)
	  * @param alignment Alignment used when placing the text (default = left)
	  * @param textColor Color used when drawing the text in this label (default = black)
	  * @param hasMinWidth Whether this label has a minimum width based on the displayed text (default = true).
	  *                    If false, text size may be shrank in order to fit it into this label.
	  * @tparam A Type of displayed item
	  * @return A new item label
	  */
	def apply[A](pointer: Changing[A], font: Font, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	             insets: StackInsets = StackInsets.any, alignment: Alignment = Alignment.Left,
	             textColor: Color = Color.textBlack, hasMinWidth: Boolean = true) =
		new ViewLabel[A](pointer, displayFunction, font, textColor, insets, alignment, hasMinWidth)
	
	/**
	  * Creates a new label using specified content pointer and contextual information
	  * @param pointer Label content pointer
	  * @param displayFunction Display function for label data (default = toString)
	  * @param context Component creation context (implicit)
	  * @tparam A Type of displayed item
	  * @return A new label
	  */
	def contextual[A](pointer: Changing[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw)
					 (implicit context: TextContext) =
	{
		new ViewLabel[A](pointer, displayFunction, context.font, context.textColor, context.textInsets,
			context.textAlignment, !context.allowTextShrink)
	}
	
	/**
	  * Creates a new opaque (non-transparent) item label
	  * @param color Label background
	  * @param pointer Label content pointer
	  * @param displayFunction Display function for content (default = toString)
	  * @param context Component creation context (implicit)
	  * @tparam A Type of displayed item
	  * @return A new label
	  */
	def contextualWithBackground[A](color: Color, pointer: Changing[A],
	                                displayFunction: DisplayFunction[A] = DisplayFunction.raw)
								   (implicit context: TextContext) =
	{
		val label = contextual(pointer, displayFunction)(context.against(color))
		label.background = color
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
class ViewLabel[A](override val contentPointer: Changing[A], displayFunction: DisplayFunction[A], initialFont: Font,
                   initialTextColor: Color = Color.textBlack, initialInsets: StackInsets = StackInsets.any,
                   initialAlignment: Alignment = Alignment.Left, hasMinWidth: Boolean = true)
	extends StackableAwtComponentWrapperWrapper with MutableStyleTextComponent with SwingComponentRelated
		with MutableCustomDrawableWrapper with PoolWithPointer[A, Changing[A]]
{
	// ATTRIBUTES	--------------------
	
	private val label = new TextLabel(displayFunction(contentPointer.value), initialFont, initialTextColor,
		initialInsets, initialAlignment, hasMinWidth)
	
	
	// INITIAL CODE	--------------------
	
	// Reacts to changes in text
	addContentListener(ContentUpdateListener)
	
	
	// IMPLEMENTED	--------------------
	
	override def component = label.component
	
	override protected def wrapped = label
	
	override def textDrawContext = label.textDrawContext
	
	override def textDrawContext_=(newContext: TextDrawContext) = label.textDrawContext = newContext
	
	override def drawable = label
	
	override def toString = s"Label($text)"
	
	override def measuredText: MeasuredText = label.measuredText
	
	override def allowTextShrink: Boolean = label.allowTextShrink
	
	def text = label.text
	
	
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
