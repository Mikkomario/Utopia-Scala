package utopia.reach.component.button.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageAndTextLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.stack.StackInsets

object ViewImageAndTextButton extends ContextInsertableComponentFactoryFactory[TextContext,
	ViewImageAndTextButtonFactory, ContextualViewImageAndTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewImageAndTextButtonFactory(hierarchy)
}

class ViewImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContext, ContextualViewImageAndTextButtonFactory]
{
	override def withContext[N <: TextContext](context: N) =
		ContextualViewImageAndTextButtonFactory(this, context)
	
	/**
	  * Creates a new button with image and text
	  * @param contentPointer Pointer to the displayed content, which determines text
	  * @param imagesPointer Pointer to the displayed image set
	  * @param colorPointer Pointer to button color
	  * @param fontPointer Pointer to font used
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param imageInsetsPointer Pointer to insets placed around the image (default = any, preferring 0)
	  * @param textInsetsPointer Pointer to insets placed around the text (default = any, preferring 0)
	  * @param commonInsetsPointer Pointer to insets placed around this button (default = any, preferring 0)
	  * @param borderWidth Width of the border in this button (default = 0 = no border)
	  * @param alignment Alignment used for the text (default = Left)
	  * @param displayFunction Function for converting content to text (default = toString)
	  * @param betweenLinesMargin Vertical margin between text lines (default = 0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param allowLineBreaks Whether text should be allowed to use line breaks (default = true)
	  * @param allowImageUpscaling Whether image should be allowed to scale up to its source resolution (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @tparam A Type of content in this button
	  * @return A new button
	  */
	def apply[A](contentPointer: Changing[A], imagesPointer: Changing[ButtonImageSet],
	             colorPointer: Changing[Color], fontPointer: Changing[Font],
	             enabledPointer: Changing[Boolean] = AlwaysTrue,
	             imageInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	             textInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	             commonInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any), borderWidth: Double = 0.0,
	             alignment: Alignment = Alignment.Left, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	             betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
	             additionalDrawers: Vector[CustomDrawer] = Vector(),
	             additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
	             allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
	             useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: A => Unit) =
		new ViewImageAndTextButton[A](parentHierarchy, contentPointer, imagesPointer, colorPointer, fontPointer,
			enabledPointer, imageInsetsPointer, textInsetsPointer, commonInsetsPointer, borderWidth, alignment,
			displayFunction, betweenLinesMargin, hotKeys, additionalDrawers, additionalFocusListeners,
			allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param imagesPointer Pointer to the displayed image set
	  * @param colorPointer Pointer to button color
	  * @param fontPointer Pointer to font used
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param imageInsetsPointer Pointer to insets placed around the image (default = any, preferring 0)
	  * @param textInsetsPointer Pointer to insets placed around the text (default = any, preferring 0)
	  * @param commonInsetsPointer Pointer to insets placed around this button (default = any, preferring 0)
	  * @param borderWidth Width of the border in this button (default = 0 = no border)
	  * @param alignment Alignment used for the text (default = Left)
	  * @param betweenLinesMargin Vertical margin between text lines (default = 0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param allowLineBreaks Whether text should be allowed to use line breaks (default = true)
	  * @param allowImageUpscaling Whether image should be allowed to scale up to its source resolution (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	def withStaticText(text: LocalizedString, imagesPointer: Changing[ButtonImageSet],
	                   colorPointer: Changing[Color], fontPointer: Changing[Font],
	                   enabledPointer: Changing[Boolean] = AlwaysTrue,
	                   imageInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	                   textInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	                   commonInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any), borderWidth: Double = 0.0,
	                   alignment: Alignment = Alignment.Left, betweenLinesMargin: Double = 0.0,
	                   hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
	                   additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
	                   allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
	                   useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit) =
		apply[LocalizedString](Fixed(text), imagesPointer, colorPointer, fontPointer, enabledPointer,
			imageInsetsPointer, textInsetsPointer, commonInsetsPointer, borderWidth, alignment,
			DisplayFunction.identity, betweenLinesMargin, hotKeys, additionalDrawers,
			additionalFocusListeners, allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth) { _ => action }
}

case class ContextualViewImageAndTextButtonFactory[+N <: TextContext](factory: ViewImageAndTextButtonFactory,
																			context: N )
	extends ContextualComponentFactory[N, TextContext, ContextualViewImageAndTextButtonFactory]
{
	private implicit def c: TextContext = context
	
	override def withContext[N2 <: TextContext](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new button with image and text
	  * @param contentPointer Pointer to the displayed content, which determines text
	  * @param imagesPointer Pointer to the displayed image set
	  * @param colorPointer Pointer to button color (default = determined by context)
	  * @param fontPointer Pointer to font used (default = determined by context)
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param imageInsetsPointer Pointer to insets placed around the image (default = determined by context)
	  * @param textInsetsPointer Pointer to insets placed around the text (default = determined by context)
	  * @param commonInsetsPointer Pointer to insets placed around this button (default = determined by context)
	  * @param displayFunction Function for converting content to text (default = toString)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @tparam A Type of content in this button
	  * @return A new button
	  */
	def withChangingStyle[A](contentPointer: Changing[A], imagesPointer: Changing[ButtonImageSet],
	                         colorPointer: Changing[Color] = Fixed(context.background),
	                         fontPointer: Changing[Font] = Fixed(context.font),
	                         enabledPointer: Changing[Boolean] = AlwaysTrue,
	                         imageInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
	                         textInsetsPointer: Changing[StackInsets] = Fixed(context.textInsets / 2),
	                         commonInsetsPointer: Changing[StackInsets] = Fixed(context.textInsets / 2),
	                         displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[HotKey] = Set(),
	                         additionalDrawers: Vector[CustomDrawer] = Vector(),
	                         additionalFocusListeners: Seq[FocusListener] = Vector(),
	                         useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)
							(action: A => Unit) =
		factory[A](contentPointer, imagesPointer, colorPointer, fontPointer, enabledPointer, imageInsetsPointer,
			textInsetsPointer, commonInsetsPointer, context.buttonBorderWidth, context.textAlignment, displayFunction,
			context.betweenLinesMargin.optimal, hotKeys, additionalDrawers, additionalFocusListeners,
			context.allowLineBreaks, context.allowImageUpscaling, context.allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with image and text
	  * @param contentPointer Pointer to the displayed content, which determines text
	  * @param imagesPointer Pointer to the displayed image set
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param displayFunction Function for converting content to text (default = toString)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @tparam A Type of content in this button
	  * @return A new button
	  */
	def apply[A](contentPointer: Changing[A], imagesPointer: Changing[ButtonImageSet],
	             enabledPointer: Changing[Boolean] = AlwaysTrue,
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[HotKey] = Set(),
	             additionalDrawers: Vector[CustomDrawer] = Vector(),
	             additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
	             forceEqualBreadth: Boolean = false)(action: A => Unit) =
		withChangingStyle[A](contentPointer, imagesPointer, enabledPointer = enabledPointer,
			displayFunction = displayFunction, hotKeys = hotKeys,
			additionalDrawers = additionalDrawers, additionalFocusListeners = additionalFocusListeners,
			useLowPriorityImageSize = useLowPriorityImageSize, forceEqualBreadth = forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with image and text
	  * @param contentPointer Pointer to the displayed content, which determines text
	  * @param iconPointer A pointer to the icon displayed on this button
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param displayFunction Function for converting content to text (default = toString)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @tparam A Type of content in this button
	  * @return A new button
	  */
	def withIcon[A](contentPointer: Changing[A], iconPointer: Changing[SingleColorIcon],
	                enabledPointer: Changing[Boolean] = AlwaysTrue,
	                displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[HotKey] = Set(),
	                additionalDrawers: Vector[CustomDrawer] = Vector(),
	                additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
	                forceEqualBreadth: Boolean = false)(action: A => Unit) =
		apply[A](contentPointer, iconPointer.map { _.inButton.contextual }, enabledPointer, displayFunction, hotKeys,
			additionalDrawers, additionalFocusListeners, useLowPriorityImageSize,
			forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param imagesPointer Pointer to the displayed image set
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	def withStaticText(text: LocalizedString, imagesPointer: Changing[ButtonImageSet],
	                   enabledPointer: Changing[Boolean] = AlwaysTrue, hotKeys: Set[HotKey] = Set(),
	                   additionalDrawers: Vector[CustomDrawer] = Vector(),
	                   additionalFocusListeners: Seq[FocusListener] = Vector(),
	                   useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit) =
		apply[LocalizedString](Fixed(text), imagesPointer, enabledPointer, DisplayFunction.identity, hotKeys,
			additionalDrawers, additionalFocusListeners, useLowPriorityImageSize, forceEqualBreadth) { _ => action }
	
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param icon Icon displayed on this button
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	def withStaticTextAndIcon(text: LocalizedString, icon: SingleColorIcon,
	                          enabledPointer: Changing[Boolean] = AlwaysTrue, hotKeys: Set[HotKey] = Set(),
	                          additionalDrawers: Vector[CustomDrawer] = Vector(),
	                          additionalFocusListeners: Seq[FocusListener] = Vector(),
	                          useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)
							 (action: => Unit) =
		withStaticText(text, Fixed(icon.inButton.contextual), enabledPointer, hotKeys, additionalDrawers,
			additionalFocusListeners, useLowPriorityImageSize, forceEqualBreadth)(action)
}

/**
  * A pointer-based button that displays both an image and text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v0.1
  */
class ViewImageAndTextButton[A](parentHierarchy: ComponentHierarchy, contentPointer: Changing[A],
                                imagesPointer: Changing[ButtonImageSet], colorPointer: Changing[Color],
                                fontPointer: Changing[Font],
                                enabledPointer: Changing[Boolean] = AlwaysTrue,
                                imageInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                textInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                commonInsetsPointer: Changing[StackInsets] = Fixed(StackInsets.any),
                                borderWidth: Double = 0.0, alignment: Alignment = Alignment.Left,
                                displayFunction: DisplayFunction[A] = DisplayFunction.raw,
                                betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
                                additionalDrawers: Vector[CustomDrawer] = Vector(),
                                additionalFocusListeners: Seq[FocusListener] = Vector(),
                                allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
                                allowTextShrink: Boolean = false, useLowPriorityImageSize: Boolean = false,
                                forceEqualBreadth: Boolean = false)(action: A => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	override val statePointer = baseStatePointer
		.mergeWith(enabledPointer) { (state, enabled) => state + (Disabled -> !enabled) }
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	
	override protected val wrapped = {
		val actualImageInsetsPointer = imageInsetsPointer.mergeWith(commonInsetsPointer) { (imageInsets, commonInsets) =>
			imageInsets + commonInsets.withoutSides(alignment.opposite.directions) + borderWidth
		}
		val actualTextInsetsPointer = textInsetsPointer.mergeWith(commonInsetsPointer) { (textInsets, commonInsets) =>
			textInsets + commonInsets.withoutSides(alignment.directions) + borderWidth
		}
		val imagePointer = imagesPointer.mergeWith(statePointer) { _(_) }
		val textColorPointer = colorPointer.map { _.shade.defaultTextColor }
		new ViewImageAndTextLabel[A](parentHierarchy, contentPointer, imagePointer, fontPointer, textColorPointer,
			actualImageInsetsPointer, actualTextInsetsPointer, alignment, displayFunction, betweenLinesMargin,
			ButtonBackgroundViewDrawer(colorPointer.map { c => c }, statePointer, borderWidth) +: additionalDrawers,
			allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)
	}
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return This button's current background color
	  */
	def color = colorPointer.value
	
	
	// INITIAL CODE	------------------------------
	
	setup(baseStatePointer, hotKeys)
	colorPointer.addContinuousAnyChangeListener { repaint() }
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def trigger() = action(contentPointer.value)
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
