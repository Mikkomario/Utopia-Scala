package utopia.reach.component.button

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Fixed
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.ButtonBackgroundViewDrawer
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.TextLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.event.FocusListener
import utopia.reflection.event.ButtonState
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object TextButton extends ContextInsertableComponentFactoryFactory[ButtonContextLike, TextButtonFactory,
	ContextualTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new TextButtonFactory(hierarchy)
}

class TextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ButtonContextLike, ContextualTextButtonFactory]
{
	// IMPLEMENTED	-------------------------------
	
	override def withContext[N <: ButtonContextLike](context: N) =
		ContextualTextButtonFactory(this, context)
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Creates a new text button
	  * @param text Text displayed on this button
	  * @param font Font used when drawing the text
	  * @param color Button background color
	  * @param textColor Button text color (default = standard black)
	  * @param alignment Text alignment (default = Center)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param borderWidth Width of the border on this button (default = 0 = no border)
	  * @param betweenLinesMargin Margin placed between horizontal text lines in case there are multiple (default = 0.0)
	  * @param hotKeys Keys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Character keys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers applied (default = empty)
	  * @param additionalFocusListeners Focus listeners applied (default = empty)
	  * @param allowLineBreaks Whether line breaks in the drawn text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text size should be allowed to decrease to conserve space (default = false)
	  * @param action Action performed each time this button is triggered (call by name)
	  * @return A new text button
	  */
	def apply(text: LocalizedString, font: Font, color: Color, textColor: Color = Color.textBlack,
			  alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
			  borderWidth: Double = 0.0, betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(),
			  hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
			  additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
			  allowTextShrink: Boolean = false)(action: => Unit) =
		new TextButton(parentHierarchy, text, TextDrawContext(font, textColor, alignment, textInsets + borderWidth,
			betweenLinesMargin), color, borderWidth, hotKeys, hotKeyCharacters, additionalDrawers,
			additionalFocusListeners, allowLineBreaks, allowTextShrink)(action)
}

case class ContextualTextButtonFactory[+N <: ButtonContextLike](buttonFactory: TextButtonFactory, context: N)
	extends ContextualComponentFactory[N, ButtonContextLike, ContextualTextButtonFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N2 <: ButtonContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new text button
	  * @param text The text displayed on this button
	  * @param hotKeys Keys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Character keys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers applied (default = empty)
	  * @param additionalFocusListeners Focus listeners applied (default = empty)
	  * @param action Action performed each time this button is triggered (call by name)
	  * @return A new text button
	  */
	def apply(text: LocalizedString, hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
			  additionalDrawers: Seq[CustomDrawer] = Vector(), additionalFocusListeners: Seq[FocusListener] = Vector())
			 (action: => Unit) =
		buttonFactory(text, context.font, context.buttonColor, context.textColor, context.textAlignment,
			context.textInsets, context.borderWidth, context.betweenLinesMargin.optimal, hotKeys, hotKeyCharacters,
			additionalDrawers, additionalFocusListeners, context.allowLineBreaks, context.allowTextShrink)(action)
}

/**
  * An immutable button that only draws text
  * @author Mikko Hilpinen
  * @since 24.10.2020, v2
  */
class TextButton(parentHierarchy: ComponentHierarchy, text: LocalizedString, textDrawContext: TextDrawContext,
				 color: Color, borderWidth: Double = 0.0, hotKeys: Set[Int] = Set(),
				 hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
				 allowTextShrink: Boolean = false)(action: => Unit)
	extends ButtonLike with ReachComponentWrapper
{
	// ATTRIBUTES	-----------------------------
	
	private val _statePointer = new PointerWithEvents(ButtonState.default)
	
	override val focusListeners = new ButtonDefaultFocusListener(_statePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	override protected val wrapped = new TextLabel(parentHierarchy, text, textDrawContext,
		ButtonBackgroundViewDrawer(Fixed(color), statePointer, borderWidth) +: additionalDrawers,
		allowLineBreaks, allowTextShrink)
	
	
	// INITIAL CODE	-----------------------------
	
	setup(_statePointer, hotKeys, hotKeyCharacters)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def statePointer = _statePointer.view
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
