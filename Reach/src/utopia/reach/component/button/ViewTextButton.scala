package utopia.reach.component.button

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangingLike, Fixed}
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{ButtonContextLike, TextContextLike}
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.ButtonBackgroundViewDrawer
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.ViewTextLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.event.FocusListener
import utopia.reflection.event.ButtonState
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object ViewTextButton extends ContextInsertableComponentFactoryFactory[TextContextLike, ViewTextButtonFactory,
	ContextualViewTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewTextButtonFactory(hierarchy)
}

class ViewTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualViewTextButtonFactory]
{
	// IMPLEMENTED	-----------------------------
	
	override def withContext[N <: TextContextLike](context: N) =
		ContextualViewTextButtonFactory(this, context)
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new button
	  * @param contentPointer Pointer that contains the displayed button content
	  * @param font Font to use when drawing text
	  * @param colorPointer Pointer that contains this button's background color
	  * @param enabledPointer Pointer that contains whether this button should be enabled (true) or disabled (false).
	  *                       Default = always enabled.
	  * @param displayFunction A function for converting the displayed value to a localized string (default = toString)
	  * @param borderWidth Width of this button's borders (default = 0 = no border)
	  * @param alignment Text alignment used (default = Center)
	  * @param textInsets Insets placed around the text (in addition to borders) (default = any, preferring 0)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0.0)
	  * @param hotKeys Keys that can be used for triggering this button even when it doesn't have focus
	  *                (default = empty)
	  * @param hotKeyCharacters Character keys that can be used for triggering this button even when it doesn't have
	  *                         focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text size should be allowed to decrease to conserve space when necessary
	  *                        (default = false)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	def apply[A](contentPointer: ChangingLike[A], font: Font, colorPointer: ChangingLike[ComponentColor],
				 enabledPointer: ChangingLike[Boolean] = Fixed(true),
				 displayFunction: DisplayFunction[A] = DisplayFunction.raw, borderWidth: Double = 0.0,
				 alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
				 betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
				 additionalDrawers: Seq[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
				 allowTextShrink: Boolean = false)(action: A => Unit) =
		new ViewTextButton[A](parentHierarchy, contentPointer, font, colorPointer, enabledPointer, displayFunction,
			borderWidth, alignment, textInsets, betweenLinesMargin, hotKeys, hotKeyCharacters, additionalDrawers,
			additionalFocusListeners, allowLineBreaks, allowTextShrink)(action)
	
	/**
	  * Creates a new button
	  * @param text Text displayed on this button
	  * @param font Font to use when drawing text
	  * @param colorPointer Pointer that contains this button's background color
	  * @param enabledPointer Pointer that contains whether this button should be enabled (true) or disabled (false).
	  *                       Default = always enabled.
	  * @param borderWidth Width of this button's borders (default = 0 = no border)
	  * @param alignment Text alignment used (default = Center)
	  * @param textInsets Insets placed around the text (in addition to borders) (default = any, preferring 0)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0.0)
	  * @param hotKeys Keys that can be used for triggering this button even when it doesn't have focus
	  *                (default = empty)
	  * @param hotKeyCharacters Character keys that can be used for triggering this button even when it doesn't have
	  *                         focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text size should be allowed to decrease to conserve space when necessary
	  *                        (default = false)
	  * @param action The action performed when this button is pressed
	  * @return A new button
	  */
	def withStaticText(text: LocalizedString, font: Font, colorPointer: ChangingLike[ComponentColor],
					   enabledPointer: ChangingLike[Boolean] = Fixed(true), borderWidth: Double = 0.0,
					   alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
					   betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(),
					   hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
					   additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
					   allowTextShrink: Boolean = false)(action: => Unit) =
		new ViewTextButton[LocalizedString](parentHierarchy, Fixed(text), font, colorPointer, enabledPointer,
			DisplayFunction.identity, borderWidth, alignment, textInsets, betweenLinesMargin, hotKeys, hotKeyCharacters,
			additionalDrawers, additionalFocusListeners, allowLineBreaks, allowTextShrink)(_ => action)
}

object ContextualViewTextButtonFactory
{
	implicit class ButtonContextualViewTextButtonFactory[N <: ButtonContextLike]
	(val factory: ContextualViewTextButtonFactory[N]) extends AnyVal
	{
		/**
		  * Creates a new button
		  * @param contentPointer Pointer that contains the displayed button content
		  * @param enabledPointer Pointer that contains whether this button should be enabled (true) or disabled (false).
		  *                       Default = always enabled.
		  * @param displayFunction A function for converting the displayed value to a localized string (default = toString)
		  * @param hotKeys Keys that can be used for triggering this button even when it doesn't have focus
		  *                (default = empty)
		  * @param hotKeyCharacters Character keys that can be used for triggering this button even when it doesn't have
		  *                         focus (default = empty)
		  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
		  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
		  * @param action The action performed when this button is pressed (accepts currently displayed content)
		  * @tparam A Type of displayed content
		  * @return A new button
		  */
		def apply[A](contentPointer: ChangingLike[A], enabledPointer: ChangingLike[Boolean] = Fixed(true),
					 displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[Int] = Set(),
					 hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
					 additionalFocusListeners: Seq[FocusListener] = Vector())(action: A => Unit) =
		{
			val context = factory.context
			factory.factory[A](contentPointer, context.font, Fixed(context.buttonColor), enabledPointer,
				displayFunction, context.borderWidth, context.textAlignment, context.textInsets,
				context.betweenLinesMargin.optimal, hotKeys, hotKeyCharacters, additionalDrawers,
				additionalFocusListeners, context.allowLineBreaks, context.allowTextShrink)(action)
		}
		
		/**
		  * Creates a new button
		  * @param text Text displayed on this string
		  * @param enabledPointer Pointer that contains whether this button should be enabled (true) or disabled (false).
		  *                       Default = always enabled.
		  * @param hotKeys Keys that can be used for triggering this button even when it doesn't have focus
		  *                (default = empty)
		  * @param hotKeyCharacters Character keys that can be used for triggering this button even when it doesn't have
		  *                         focus (default = empty)
		  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
		  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
		  * @param action The action performed when this button is pressed
		  * @return A new button
		  */
		def withStaticText(text: LocalizedString, enabledPointer: ChangingLike[Boolean] = Fixed(true),
						   hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
						   additionalDrawers: Seq[CustomDrawer] = Vector(),
						   additionalFocusListeners: Seq[FocusListener] = Vector())(action: => Unit) =
			apply[LocalizedString](Fixed(text), enabledPointer, DisplayFunction.identity, hotKeys,
				hotKeyCharacters, additionalDrawers, additionalFocusListeners) { _ => action }
	}
}

case class ContextualViewTextButtonFactory[+N <: TextContextLike](factory: ViewTextButtonFactory, context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualViewTextButtonFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a new button that changes its color based on a pointer value
	  * @param contentPointer Pointer that contains the displayed button content
	  * @param colorPointer Pointer that contains this button's background color
	  * @param enabledPointer Pointer that contains whether this button should be enabled (true) or disabled (false).
	  *                       Default = always enabled.
	  * @param displayFunction A function for converting the displayed value to a localized string (default = toString)
	  * @param borderWidth Width of this button's borders (default = border equal to very small margins)
	  * @param hotKeys Keys that can be used for triggering this button even when it doesn't have focus
	  *                (default = empty)
	  * @param hotKeyCharacters Character keys that can be used for triggering this button even when it doesn't have
	  *                         focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	def withChangingColor[A](contentPointer: ChangingLike[A], colorPointer: ChangingLike[ComponentColor],
							 enabledPointer: ChangingLike[Boolean] = Fixed(true),
							 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							 borderWidth: Double = context.margins.verySmall, hotKeys: Set[Int] = Set(),
							 hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
							 additionalFocusListeners: Seq[FocusListener] = Vector())(action: A => Unit) =
		factory[A](contentPointer, context.font, colorPointer, enabledPointer, displayFunction, borderWidth,
			context.textAlignment, context.textInsets, context.betweenLinesMargin.optimal, hotKeys,
			hotKeyCharacters, additionalDrawers, additionalFocusListeners, context.allowLineBreaks,
			context.allowTextShrink)(action)
	
	/**
	  * Creates a new button that changes its color based on a pointer value
	  * @param contentPointer Pointer that contains the displayed button content
	  * @param rolePointer A pointer that contains this button's role
	  * @param enabledPointer Pointer that contains whether this button should be enabled (true) or disabled (false).
	  *                       Default = always enabled.
	  * @param displayFunction A function for converting the displayed value to a localized string (default = toString)
	  * @param preferredShade Preferred shade of color to use (default = standard)
	  * @param borderWidth Width of this button's borders (default = border equal to very small margins)
	  * @param hotKeys Keys that can be used for triggering this button even when it doesn't have focus
	  *                (default = empty)
	  * @param hotKeyCharacters Character keys that can be used for triggering this button even when it doesn't have
	  *                         focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	def withChangingRole[A](contentPointer: ChangingLike[A], rolePointer: ChangingLike[ColorRole],
							enabledPointer: ChangingLike[Boolean] = Fixed(true),
							displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							preferredShade: ColorShade = Standard, borderWidth: Double = context.margins.verySmall,
							hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
							additionalDrawers: Seq[CustomDrawer] = Vector(),
							additionalFocusListeners: Seq[FocusListener] = Vector())(action: A => Unit) =
		withChangingColor[A](contentPointer, rolePointer.map { role => context.color(role, preferredShade) },
			enabledPointer, displayFunction, borderWidth, hotKeys, hotKeyCharacters, additionalDrawers,
			additionalFocusListeners)(action)
}

/**
  * A button that matches the states of various pointers (not offering any mutable interface itself)
  * @author Mikko Hilpinen
  * @since 26.10.2020, v2
  */
class ViewTextButton[A](parentHierarchy: ComponentHierarchy, contentPointer: ChangingLike[A], font: Font,
						colorPointer: ChangingLike[ComponentColor],
						enabledPointer: ChangingLike[Boolean] = Fixed(true),
						displayFunction: DisplayFunction[A] = DisplayFunction.raw, borderWidth: Double = 0.0,
						alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
						betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(),
						hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
						additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
						allowTextShrink: Boolean = false)(action: A => Unit)
	extends ButtonLike with ReachComponentWrapper
{
	// ATTRIBUTES	---------------------------------
	
	private val baseStatePointer = new PointerWithEvents(ButtonState.default)
	private val _statePointer = baseStatePointer.mergeWith(enabledPointer) { (state, enabled) =>
		state.copy(isEnabled = enabled) }
	private val actualTextInsets = if (borderWidth > 0) textInsets + borderWidth else textInsets
	private val stylePointer = colorPointer.mergeWith(enabledPointer) { (color, enabled) =>
		TextDrawContext(font, if (enabled) color.defaultTextColor else color.textColorStandard.hintTextColor,
			alignment, actualTextInsets, betweenLinesMargin)
	}
	
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	override protected val wrapped = new ViewTextLabel[A](parentHierarchy, contentPointer, stylePointer,
		displayFunction, ButtonBackgroundViewDrawer(colorPointer.map { c => c: Color }, statePointer, borderWidth) +:
			additionalDrawers, allowLineBreaks, allowTextShrink)
	
	
	// INITIAL CODE	---------------------------------
	
	setup(baseStatePointer, hotKeys, hotKeyCharacters)
	colorPointer.addListener { _ => repaint() }
	
	
	// COMPUTED	-------------------------------------
	
	/**
	  * @return This button's current background color
	  */
	def color = colorPointer.value
	
	
	// IMPLEMENTED	---------------------------------
	
	override def statePointer = _statePointer
	
	override protected def trigger() = action(contentPointer.value)
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
