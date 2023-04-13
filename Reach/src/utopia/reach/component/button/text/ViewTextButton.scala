package utopia.reach.component.button.text

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.{GuiElementStatus, HotKey, TextDrawContext}
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.stack.StackInsets

object ViewTextButton extends ContextInsertableComponentFactoryFactory[TextContext, ViewTextButtonFactory,
	ContextualViewTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewTextButtonFactory(hierarchy)
}

class ViewTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContext, ContextualViewTextButtonFactory]
{
	// IMPLEMENTED	-----------------------------
	
	override def withContext[N <: TextContext](context: N) =
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
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text size should be allowed to decrease to conserve space when necessary
	  *                        (default = false)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	def apply[A](contentPointer: Changing[A], font: Font, colorPointer: Changing[Color],
	             enabledPointer: Changing[Boolean] = AlwaysTrue,
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw, borderWidth: Double = 0.0,
	             alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
	             betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
	             additionalDrawers: Seq[CustomDrawer] = Vector(),
	             additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
	             allowTextShrink: Boolean = false)(action: A => Unit) =
		new ViewTextButton[A](parentHierarchy, contentPointer, font, colorPointer, enabledPointer, displayFunction,
			borderWidth, alignment, textInsets, betweenLinesMargin, hotKeys, additionalDrawers,
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
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text size should be allowed to decrease to conserve space when necessary
	  *                        (default = false)
	  * @param action The action performed when this button is pressed
	  * @return A new button
	  */
	def withStaticText(text: LocalizedString, font: Font, colorPointer: Changing[Color],
	                   enabledPointer: Changing[Boolean] = AlwaysTrue, borderWidth: Double = 0.0,
	                   alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
	                   betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
	                   additionalDrawers: Seq[CustomDrawer] = Vector(),
	                   additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
	                   allowTextShrink: Boolean = false)(action: => Unit) =
		new ViewTextButton[LocalizedString](parentHierarchy, Fixed(text), font, colorPointer, enabledPointer,
			DisplayFunction.identity, borderWidth, alignment, textInsets, betweenLinesMargin, hotKeys,
			additionalDrawers, additionalFocusListeners, allowLineBreaks, allowTextShrink)(_ => action)
}

object ContextualViewTextButtonFactory
{
	implicit class ButtonContextualViewTextButtonFactory[N <: TextContext]
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
		  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
		  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
		  * @param action The action performed when this button is pressed (accepts currently displayed content)
		  * @tparam A Type of displayed content
		  * @return A new button
		  */
		def apply[A](contentPointer: Changing[A], enabledPointer: Changing[Boolean] = AlwaysTrue,
		             displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[HotKey] = Set(),
		             additionalDrawers: Seq[CustomDrawer] = Vector(),
		             additionalFocusListeners: Seq[FocusListener] = Vector())(action: A => Unit) =
		{
			val context = factory.context
			factory.factory[A](contentPointer, context.font, Fixed(context.background), enabledPointer,
				displayFunction, context.buttonBorderWidth, context.textAlignment, context.textInsets,
				context.betweenLinesMargin.optimal, hotKeys, additionalDrawers,
				additionalFocusListeners, context.allowLineBreaks, context.allowTextShrink)(action)
		}
		
		/**
		  * Creates a new button
		  * @param text Text displayed on this string
		  * @param enabledPointer Pointer that contains whether this button should be enabled (true) or disabled (false).
		  *                       Default = always enabled.
		  * @param hotKeys Keys that can be used for triggering this button even when it doesn't have focus
		  *                (default = empty)
		  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
		  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
		  * @param action The action performed when this button is pressed
		  * @return A new button
		  */
		def withStaticText(text: LocalizedString, enabledPointer: Changing[Boolean] = AlwaysTrue,
		                   hotKeys: Set[HotKey] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
		                   additionalFocusListeners: Seq[FocusListener] = Vector())(action: => Unit) =
			apply[LocalizedString](Fixed(text), enabledPointer, DisplayFunction.identity, hotKeys,
				additionalDrawers, additionalFocusListeners) { _ => action }
	}
}

case class ContextualViewTextButtonFactory[+N <: TextContext](factory: ViewTextButtonFactory, context: N)
	extends ContextualComponentFactory[N, TextContext, ContextualViewTextButtonFactory]
{
	// IMPLEMENTED	------------------------------
	
	override def withContext[N2 <: TextContext](newContext: N2) =
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
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	def withChangingColor[A](contentPointer: Changing[A], colorPointer: Changing[Color],
	                         enabledPointer: Changing[Boolean] = AlwaysTrue,
	                         displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                         borderWidth: Double = context.margins.verySmall, hotKeys: Set[HotKey] = Set(),
	                         additionalDrawers: Seq[CustomDrawer] = Vector(),
	                         additionalFocusListeners: Seq[FocusListener] = Vector())(action: A => Unit) =
		factory[A](contentPointer, context.font, colorPointer, enabledPointer, displayFunction, borderWidth,
			context.textAlignment, context.textInsets, context.betweenLinesMargin.optimal, hotKeys,
			additionalDrawers, additionalFocusListeners, context.allowLineBreaks,
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
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param action The action performed when this button is pressed (accepts currently displayed content)
	  * @tparam A Type of displayed content
	  * @return A new button
	  */
	def withChangingRole[A](contentPointer: Changing[A], rolePointer: Changing[ColorRole],
	                        enabledPointer: Changing[Boolean] = AlwaysTrue,
	                        displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                        preferredShade: ColorLevel = Standard, borderWidth: Double = context.margins.verySmall,
	                        hotKeys: Set[HotKey] = Set(), additionalDrawers: Seq[CustomDrawer] = Vector(),
	                        additionalFocusListeners: Seq[FocusListener] = Vector())(action: A => Unit) =
		withChangingColor[A](contentPointer, rolePointer.map { role => context.color.preferring(preferredShade)(role) },
			enabledPointer, displayFunction, borderWidth, hotKeys, additionalDrawers,
			additionalFocusListeners)(action)
}

/**
  * A button that matches the states of various pointers (not offering any mutable interface itself)
  * @author Mikko Hilpinen
  * @since 26.10.2020, v0.1
  */
class ViewTextButton[A](parentHierarchy: ComponentHierarchy, contentPointer: Changing[A], font: Font,
                        colorPointer: Changing[Color],
                        enabledPointer: Changing[Boolean] = AlwaysTrue,
                        displayFunction: DisplayFunction[A] = DisplayFunction.raw, borderWidth: Double = 0.0,
                        alignment: Alignment = Alignment.Center, textInsets: StackInsets = StackInsets.any,
                        betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
                        additionalDrawers: Seq[CustomDrawer] = Vector(),
                        additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
                        allowTextShrink: Boolean = false)(action: A => Unit)
	extends ButtonLike with ReachComponentWrapper
{
	// ATTRIBUTES	---------------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	private val _statePointer = baseStatePointer.mergeWith(enabledPointer) { (state, enabled) =>
		state + (Disabled -> !enabled) }
	private val actualTextInsets = if (borderWidth > 0) textInsets + borderWidth else textInsets
	private val stylePointer = colorPointer.mergeWith(enabledPointer) { (color, enabled) =>
		TextDrawContext(font, if (enabled) color.shade.defaultTextColor else color.shade.defaultHintTextColor,
			alignment, actualTextInsets, betweenLinesMargin, allowLineBreaks)
	}
	
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	override protected val wrapped = new ViewTextLabel[A](parentHierarchy, contentPointer, stylePointer,
		displayFunction, ButtonBackgroundViewDrawer(colorPointer.map { c => c: Color }, statePointer, borderWidth) +:
			additionalDrawers, allowTextShrink)
	
	
	// INITIAL CODE	---------------------------------
	
	setup(baseStatePointer, hotKeys)
	colorPointer.addContinuousAnyChangeListener { repaint() }
	
	
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
