package utopia.reflection.component.reach.button

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.MutableCustomDrawableWrapper
import utopia.reflection.component.drawing.view.ButtonBackgroundViewDrawer
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.MutableTextLabel
import utopia.reflection.component.reach.template.{ButtonLike, MutableFocusable, ReachComponentWrapper}
import utopia.reflection.component.template.text.MutableTextComponent
import utopia.reflection.event.{ButtonState, FocusListener}
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
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space when necessary
	  *                        (default = false)
	  * @return A new button
	  */
	def withoutAction(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
					  alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
					  borderWidth: Double = 0.0, hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
					  allowTextShrink: Boolean = false) = new MutableTextButton(parentHierarchy, text, font, color,
		textColor, alignment, textInsets, borderWidth, hotKeys, hotKeyCharacters, allowTextShrink)
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button
	  * @param font Font used for the text
	  * @param color Button default background color
	  * @param textColor Text color used (default = standard black)
	  * @param alignment Text alignment (default = Center)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param borderWidth Button border width (default = 0 = no border)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space when necessary
	  *                        (default = false)
	  * @param action Action triggered when this button is pressed
	  * @return A new button
	  */
	def apply(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
			  alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
			  borderWidth: Double = 0.0, hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
			  allowTextShrink: Boolean = false)(action: => Unit) =
	{
		val button = withoutAction(text, font, color, textColor, alignment, textInsets, borderWidth, hotKeys,
			hotKeyCharacters, allowTextShrink)
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
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @return A new button
	  */
	def withoutAction(text: LocalizedString = LocalizedString.empty, hotKeys: Set[Int] = Set(),
					  hotKeyCharacters: Iterable[Char] = Set()) =
		buttonFactory.withoutAction(text, context.font, context.buttonColor, context.textColor,
			context.textAlignment, context.textInsets, context.borderWidth, hotKeys, hotKeyCharacters,
			!context.textHasMinWidth)
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button (default = empty)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param action Action performed when this button is pressed
	  * @return A new button
	  */
	def apply(text: LocalizedString = LocalizedString.empty, hotKeys: Set[Int] = Set(),
			  hotKeyCharacters: Iterable[Char] = Set())(action: => Unit) =
	{
		val button = withoutAction(text, hotKeys, hotKeyCharacters)
		button.registerAction(action)
		button
	}
}

/**
  * A mutable Reach implementation of button
  * @author Mikko Hilpinen
  * @since 25.10.2020, v2
  */
class MutableTextButton(parentHierarchy: ComponentHierarchy, initialText: LocalizedString, initialFont: Font,
						initialColor: Color, initialTextColor: Color = Color.textBlack,
						initialAlignment: Alignment = Alignment.Center, initialTextInsets: StackInsets = StackInsets.any,
						borderWidth: Double = 0.0, hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
						allowTextShrink: Boolean = false)
	extends ReachComponentWrapper with ButtonLike with MutableTextComponent with MutableFocusable
		with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	---------------------------------
	
	private val _statePointer = new PointerWithEvents(ButtonState.default)
	
	protected val wrapped = new MutableTextLabel(parentHierarchy, initialText, initialFont, initialTextColor,
		initialAlignment, initialTextInsets, allowTextShrink)
	/**
	  * A mutable pointer to this buttons base color
	  */
	val colorPointer = new PointerWithEvents(initialColor)
	
	var focusListeners: Seq[FocusListener] = Vector(new ButtonDefaultFocusListener(_statePointer))
	
	private var actions = Vector[() => Unit]()
	
	
	// INITIAL CODE	---------------------------------
	
	setup(_statePointer, hotKeys, hotKeyCharacters)
	
	// Adds background drawing
	wrapped.addCustomDrawer(ButtonBackgroundViewDrawer(colorPointer, statePointer, borderWidth))
	
	
	// COMPUTED	-------------------------------------
	
	def enabled_=(newState: Boolean) = _statePointer.update { _.copy(isEnabled = newState) }
	
	/**
	  * @return A pointer to this button's current state
	  */
	def statePointer = _statePointer.view
	
	
	// IMPLEMENTED	---------------------------------
	
	override protected def drawable = wrapped
	
	override def state = _statePointer.value
	
	override def trigger() = actions.foreach { _() }
	
	override def text_=(newText: LocalizedString) = wrapped.text = newText
	
	override def drawContext_=(newContext: TextDrawContext) = wrapped.drawContext = newContext
	
	override def text = wrapped.text
	
	override def drawContext = wrapped.drawContext
	
	override def repaint() = super[MutableCustomDrawableWrapper].repaint()
	
	
	// OTHER	-------------------------------------
	
	/**
	  * Registers a new action to be performed each time this button is triggered
	  * @param action An action to perform when this button is triggered
	  */
	def registerAction(action: => Unit) = actions :+= (() => action)
}
