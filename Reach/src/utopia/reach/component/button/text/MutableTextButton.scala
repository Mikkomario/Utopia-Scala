package utopia.reach.component.button.text

import utopia.firmament.component.text.MutableTextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.stack.StackInsets
import utopia.firmament.model.{GuiElementStatus, HotKey, TextDrawContext}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.button.MutableButtonLike
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.factory.contextual.TextContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.MutableTextLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

object MutableTextButton extends Cff[MutableTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableTextButtonFactory(hierarchy)
}

class MutableTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[TextContext, ContextualMutableTextButtonFactory]
{
	// IMPLEMENTED	-------------------------------
	
	override def withContext(context: TextContext) = ContextualMutableTextButtonFactory(parentHierarchy, context)
	
	
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
		new MutableTextButton(parentHierarchy, text, TextDrawContext(font, textColor, alignment, textInsets, None,
			betweenLinesMargin, allowLineBreaks), color, borderWidth, hotKeys, allowTextShrink)
	
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

case class ContextualMutableTextButtonFactory(parentHierarchy: ComponentHierarchy, context: TextContext)
	extends TextContextualFactory[ContextualMutableTextButtonFactory]
{
	// IMPLEMENTED	--------------------------------
	
	override def self: ContextualMutableTextButtonFactory = this
	
	override def withContext(newContext: TextContext) = copy(context = newContext)
	
	
	// OTHER	------------------------------------
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button (default = empty)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @return A new button
	  */
	def withoutAction(text: LocalizedString = LocalizedString.empty, hotKeys: Set[HotKey] = Set()) =
		new MutableTextButton(parentHierarchy, text, context.textDrawContext, context.background,
			context.buttonBorderWidth, hotKeys, context.allowTextShrink)
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button (default = empty)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param action Action performed when this button is pressed
	  * @return A new button
	  */
	def apply(text: LocalizedString = LocalizedString.empty, hotKeys: Set[HotKey] = Set())(action: => Unit) = {
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
class MutableTextButton(parentHierarchy: ComponentHierarchy, initialText: LocalizedString,
                        initialStyle: TextDrawContext,
						initialColor: Color, borderWidth: Double = 0.0, hotKeys: Set[HotKey] = Set(),
						override val allowTextShrink: Boolean = false)
	extends ReachComponentWrapper with MutableButtonLike with MutableTextComponent with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	---------------------------------
	
	private val _statePointer = new EventfulPointer(GuiElementStatus.identity)
	
	protected val wrapped = new MutableTextLabel(parentHierarchy, initialText, initialStyle, allowTextShrink)
	/**
	  * A mutable pointer to this button's base color
	  */
	val colorPointer = new EventfulPointer(initialColor)
	
	var focusListeners: Seq[FocusListener] = Vector(new ButtonDefaultFocusListener(_statePointer))
	override val focusId = hashCode()
	
	override protected var actions: Seq[() => Unit] = Vector[() => Unit]()
	
	
	// INITIAL CODE	---------------------------------
	
	setup(_statePointer, hotKeys)
	
	// Adds background drawing
	wrapped.addCustomDrawer(ButtonBackgroundViewDrawer(colorPointer, statePointer, Fixed(borderWidth)))
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return The current color of this button
	  */
	def color = colorPointer.value
	def color_=(newColor: Color) = colorPointer.value = newColor
	
	
	// IMPLEMENTED	---------------------------------
	
	override def measuredText = wrapped.measuredText
	
	override def enabled_=(newState: Boolean) = _statePointer.update { _ + (Disabled -> !enabled) }
	
	override protected def drawable = wrapped
	
	override def statePointer = _statePointer.view
	
	override def trigger() = actions.foreach { _() }
	
	override def text = wrapped.text
	override def text_=(newText: LocalizedString) = wrapped.text = newText
	
	override def textDrawContext_=(newContext: TextDrawContext) = wrapped.textDrawContext = newContext
	
	override def textDrawContext = wrapped.textDrawContext
	
	override def repaint() = super[MutableCustomDrawableWrapper].repaint()
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
