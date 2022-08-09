package utopia.reach.component.button.text

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.button.MutableButtonLike
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.MutableTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.MutableCustomDrawableWrapper
import utopia.reflection.component.drawing.view.ButtonBackgroundViewDrawer
import utopia.reflection.component.template.text.MutableTextComponent
import utopia.reflection.event.{ButtonState, HotKey}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object MutableTextButton extends ContextInsertableComponentFactoryFactory[ButtonContextLike, MutableTextButtonFactory,
	ContextualMutableTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableTextButtonFactory(hierarchy)
}

class MutableTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ButtonContextLike, ContextualMutableTextButtonFactory]
{
	// IMPLEMENTED	-------------------------------
	
	override def withContext[N <: ButtonContextLike](context: N) =
		ContextualMutableTextButtonFactory(this, context)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button
	  * @param font Font used for the text
	  * @param color Button default background color
	  * @param textColor Text color used (default = standard black)
	  * @param alignment Text alignment (default = Center)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param borderWidth Button border width (default = 0 = no border)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, if there are many (default = 0.0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param allowLineBreaks Whether line breaks in text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space when necessary
	  *                        (default = false)
	  * @return A new button
	  */
	def withoutAction(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
					  alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
					  borderWidth: Double = 0.0, betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
					  allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		new MutableTextButton(parentHierarchy, text, font, color, textColor, alignment, textInsets, betweenLinesMargin,
			borderWidth, hotKeys, allowLineBreaks, allowTextShrink)
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button
	  * @param font Font used for the text
	  * @param color Button default background color
	  * @param textColor Text color used (default = standard black)
	  * @param alignment Text alignment (default = Center)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param borderWidth Button border width (default = 0 = no border)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, if there are many (default = 0.0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param allowLineBreaks Whether line breaks in text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space when necessary
	  *                        (default = false)
	  * @param action Action triggered when this button is pressed
	  * @return A new button
	  */
	def apply(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
			  alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
			  borderWidth: Double = 0.0, betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
			  allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false)(action: => Unit) =
	{
		val button = withoutAction(text, font, color, textColor, alignment, textInsets, borderWidth, betweenLinesMargin,
			hotKeys, allowLineBreaks, allowTextShrink)
		button.registerAction(action)
		button
	}
}

case class ContextualMutableTextButtonFactory[+N <: ButtonContextLike](buttonFactory: MutableTextButtonFactory,
																	   context: N)
	extends ContextualComponentFactory[N, ButtonContextLike, ContextualMutableTextButtonFactory]
{
	// IMPLEMENTED	--------------------------------
	
	override def withContext[N2 <: ButtonContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button (default = empty)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @return A new button
	  */
	def withoutAction(text: LocalizedString = LocalizedString.empty, hotKeys: Set[HotKey] = Set()) =
		buttonFactory.withoutAction(text, context.font, context.buttonColor, context.textColor,
			context.textAlignment, context.textInsets, context.borderWidth, context.betweenLinesMargin.optimal, hotKeys,
			context.allowLineBreaks, context.allowTextShrink)
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button (default = empty)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param action Action performed when this button is pressed
	  * @return A new button
	  */
	def apply(text: LocalizedString = LocalizedString.empty, hotKeys: Set[HotKey] = Set())(action: => Unit) =
	{
		val button = withoutAction(text, hotKeys)
		button.registerAction(action)
		button
	}
}

/**
  * A mutable Reach implementation of button
  * @author Mikko Hilpinen
  * @since 25.10.2020, v0.1
  */
class MutableTextButton(parentHierarchy: ComponentHierarchy, initialText: LocalizedString, initialFont: Font,
						initialColor: Color, initialTextColor: Color = Color.textBlack,
						initialAlignment: Alignment = Alignment.Center, initialTextInsets: StackInsets = StackInsets.any,
						borderWidth: Double = 0.0, initialBetweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
						allowLineBreaks: Boolean = true, override val allowTextShrink: Boolean = false)
	extends ReachComponentWrapper with MutableButtonLike with MutableTextComponent with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	---------------------------------
	
	private val _statePointer = new PointerWithEvents(ButtonState.default)
	
	protected val wrapped = new MutableTextLabel(parentHierarchy, initialText, initialFont, initialTextColor,
		initialAlignment, initialTextInsets, initialBetweenLinesMargin, allowLineBreaks, allowTextShrink)
	/**
	  * A mutable pointer to this button's base color
	  */
	val colorPointer = new PointerWithEvents(initialColor)
	
	var focusListeners: Seq[FocusListener] = Vector(new ButtonDefaultFocusListener(_statePointer))
	override val focusId = hashCode()
	
	override protected var actions: Seq[() => Unit] = Vector[() => Unit]()
	
	
	// INITIAL CODE	---------------------------------
	
	setup(_statePointer, hotKeys)
	
	// Adds background drawing
	wrapped.addCustomDrawer(ButtonBackgroundViewDrawer(colorPointer, statePointer, borderWidth))
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return The current color of this button
	  */
	def color = colorPointer.value
	def color_=(newColor: Color) = colorPointer.value = newColor
	
	
	// IMPLEMENTED	---------------------------------
	
	override def measuredText = wrapped.measuredText
	
	override def measure(text: LocalizedString) = wrapped.measure(text)
	
	override def enabled_=(newState: Boolean) = _statePointer.update { _.copy(isEnabled = newState) }
	
	override protected def drawable = wrapped
	
	override def statePointer = _statePointer.view
	
	override def trigger() = actions.foreach { _() }
	
	override def text_=(newText: LocalizedString) = wrapped.text = newText
	
	override def drawContext_=(newContext: TextDrawContext) = wrapped.drawContext = newContext
	
	override def drawContext = wrapped.drawContext
	
	override def repaint() = super[MutableCustomDrawableWrapper].repaint()
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
