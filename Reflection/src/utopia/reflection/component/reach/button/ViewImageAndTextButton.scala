package utopia.reflection.component.reach.button

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Changing
import utopia.reflection.color.ComponentColor
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.ButtonBackgroundViewDrawer
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.ViewImageAndTextLabel
import utopia.reflection.component.reach.template.{ButtonLike, ReachComponentWrapper}
import utopia.reflection.component.swing.button.ButtonImageSet
import utopia.reflection.event.{ButtonState, FocusListener}
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object ViewImageAndTextButton extends ContextInsertableComponentFactoryFactory[ButtonContextLike,
	ViewImageAndTextButtonFactory, ContextualViewImageAndTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewImageAndTextButtonFactory(hierarchy)
}

class ViewImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ButtonContextLike, ContextualViewImageAndTextButtonFactory]
{
	override def withContext[N <: ButtonContextLike](context: N) =
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
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
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
				 colorPointer: Changing[ComponentColor], fontPointer: Changing[Font],
				 enabledPointer: Changing[Boolean] = Changing.wrap(true),
				 imageInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
				 textInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
				 commonInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any), borderWidth: Double = 0.0,
				 alignment: Alignment = Alignment.Left, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
				 additionalDrawers: Vector[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
				 allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
				 useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: A => Unit) =
		new ViewImageAndTextButton[A](parentHierarchy, contentPointer, imagesPointer, colorPointer, fontPointer,
			enabledPointer, imageInsetsPointer, textInsetsPointer, commonInsetsPointer, borderWidth, alignment,
			displayFunction, betweenLinesMargin, hotKeys, hotKeyCharacters, additionalDrawers, additionalFocusListeners,
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
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
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
					   colorPointer: Changing[ComponentColor], fontPointer: Changing[Font],
					   enabledPointer: Changing[Boolean] = Changing.wrap(true),
					   imageInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
					   textInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
					   commonInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any), borderWidth: Double = 0.0,
					   alignment: Alignment = Alignment.Left, betweenLinesMargin: Double = 0.0,
					   hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
					   additionalDrawers: Vector[CustomDrawer] = Vector(),
					   additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
					   allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
					   useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit) =
		apply[LocalizedString](Changing.wrap(text), imagesPointer, colorPointer, fontPointer, enabledPointer,
			imageInsetsPointer, textInsetsPointer, commonInsetsPointer, borderWidth, alignment,
			DisplayFunction.identity, betweenLinesMargin, hotKeys, hotKeyCharacters, additionalDrawers,
			additionalFocusListeners, allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth) { _ => action }
}

case class ContextualViewImageAndTextButtonFactory[+N <: ButtonContextLike](factory: ViewImageAndTextButtonFactory,
																			context: N )
	extends ContextualComponentFactory[N, ButtonContextLike, ContextualViewImageAndTextButtonFactory]
{
	private implicit def c: ButtonContextLike = context
	
	override def withContext[N2 <: ButtonContextLike](newContext: N2) =
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
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
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
							 colorPointer: Changing[ComponentColor] = Changing.wrap(context.buttonColor),
							 fontPointer: Changing[Font] = Changing.wrap(context.font),
							 enabledPointer: Changing[Boolean] = Changing.wrap(true),
							 imageInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
							 textInsetsPointer: Changing[StackInsets] = Changing.wrap(context.textInsets / 2),
							 commonInsetsPointer: Changing[StackInsets] = Changing.wrap(context.textInsets / 2),
							 displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[Int] = Set(),
							 hotKeyCharacters: Iterable[Char] = Set(),
							 additionalDrawers: Vector[CustomDrawer] = Vector(),
							 additionalFocusListeners: Seq[FocusListener] = Vector(),
							 useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)
							(action: A => Unit) =
		factory[A](contentPointer, imagesPointer, colorPointer, fontPointer, enabledPointer, imageInsetsPointer,
			textInsetsPointer, commonInsetsPointer, context.borderWidth, context.textAlignment, displayFunction,
			context.betweenLinesMargin.optimal, hotKeys, hotKeyCharacters, additionalDrawers, additionalFocusListeners,
			context.allowLineBreaks, context.allowImageUpscaling, context.allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with image and text
	  * @param contentPointer Pointer to the displayed content, which determines text
	  * @param imagesPointer Pointer to the displayed image set
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param displayFunction Function for converting content to text (default = toString)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
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
				 enabledPointer: Changing[Boolean] = Changing.wrap(true),
				 displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[Int] = Set(),
				 hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
				 forceEqualBreadth: Boolean = false)(action: A => Unit) =
		withChangingStyle[A](contentPointer, imagesPointer, enabledPointer = enabledPointer,
			displayFunction = displayFunction, hotKeys = hotKeys, hotKeyCharacters = hotKeyCharacters,
			additionalDrawers = additionalDrawers, additionalFocusListeners = additionalFocusListeners,
			useLowPriorityImageSize = useLowPriorityImageSize, forceEqualBreadth = forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with image and text
	  * @param contentPointer Pointer to the displayed content, which determines text
	  * @param iconPointer A pointer to the icon displayed on this button
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param displayFunction Function for converting content to text (default = toString)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
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
					enabledPointer: Changing[Boolean] = Changing.wrap(true),
					displayFunction: DisplayFunction[A] = DisplayFunction.raw, hotKeys: Set[Int] = Set(),
					hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
					additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
					forceEqualBreadth: Boolean = false)(action: A => Unit) =
		apply[A](contentPointer, iconPointer.map { _.inButton }, enabledPointer, displayFunction, hotKeys,
			hotKeyCharacters, additionalDrawers, additionalFocusListeners, useLowPriorityImageSize,
			forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param imagesPointer Pointer to the displayed image set
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	def withStaticText(text: LocalizedString, imagesPointer: Changing[ButtonImageSet],
					   enabledPointer: Changing[Boolean] = Changing.wrap(true), hotKeys: Set[Int] = Set(),
					   hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
					   additionalFocusListeners: Seq[FocusListener] = Vector(),
					   useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit) =
		apply[LocalizedString](Changing.wrap(text), imagesPointer, enabledPointer, DisplayFunction.identity, hotKeys,
			hotKeyCharacters, additionalDrawers, additionalFocusListeners, useLowPriorityImageSize,
			forceEqualBreadth) { _ => action }
	
	/**
	  * Creates a new button with image and text
	  * @param text Text displayed on this button
	  * @param icon Icon displayed on this button
	  * @param enabledPointer Pointer to this button's enabled state (default = always enabled)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param additionalDrawers Custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the text and the image should be forced to have equal breadth (default = false)
	  * @param action Action performed when this button is pressed (accepts current content)
	  * @return A new button
	  */
	def withStaticTextAndIcon(text: LocalizedString, icon: SingleColorIcon,
							  enabledPointer: Changing[Boolean] = Changing.wrap(true), hotKeys: Set[Int] = Set(),
							  hotKeyCharacters: Iterable[Char] = Set(),
							  additionalDrawers: Vector[CustomDrawer] = Vector(),
							  additionalFocusListeners: Seq[FocusListener] = Vector(),
							  useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)
							 (action: => Unit) =
		withStaticText(text, Changing.wrap(icon.inButton), enabledPointer, hotKeys, hotKeyCharacters, additionalDrawers,
			additionalFocusListeners, useLowPriorityImageSize, forceEqualBreadth)(action)
}

/**
  * A pointer-based button that displays both an image and text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v2
  */
class ViewImageAndTextButton[A](parentHierarchy: ComponentHierarchy, contentPointer: Changing[A],
								imagesPointer: Changing[ButtonImageSet], colorPointer: Changing[ComponentColor],
								fontPointer: Changing[Font],
								enabledPointer: Changing[Boolean] = Changing.wrap(true),
								imageInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
								textInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
								commonInsetsPointer: Changing[StackInsets] = Changing.wrap(StackInsets.any),
								borderWidth: Double = 0.0, alignment: Alignment = Alignment.Left,
								displayFunction: DisplayFunction[A] = DisplayFunction.raw,
								betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(),
								hotKeyCharacters: Iterable[Char] = Set(),
								additionalDrawers: Vector[CustomDrawer] = Vector(),
								additionalFocusListeners: Seq[FocusListener] = Vector(),
								allowLineBreaks: Boolean = true, allowImageUpscaling: Boolean = true,
								allowTextShrink: Boolean = false, useLowPriorityImageSize: Boolean = false,
								forceEqualBreadth: Boolean = false)(action: A => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(ButtonState.default)
	override val statePointer = baseStatePointer.mergeWith(enabledPointer) { (state, enabled) =>
		state.copy(isEnabled = enabled) }
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	
	override protected val wrapped =
	{
		val actualImageInsetsPointer = imageInsetsPointer.mergeWith(commonInsetsPointer) { (imageInsets, commonInsets) =>
			imageInsets + commonInsets.withoutSides(alignment.opposite.directions) + borderWidth
		}
		val actualTextInsetsPointer = textInsetsPointer.mergeWith(commonInsetsPointer) { (textInsets, commonInsets) =>
			textInsets + commonInsets.withoutSides(alignment.directions) + borderWidth
		}
		val imagePointer = imagesPointer.mergeWith(statePointer) { _(_) }
		val textColorPointer = colorPointer.map { _.defaultTextColor }
		new ViewImageAndTextLabel[A](parentHierarchy, contentPointer, imagePointer, fontPointer, textColorPointer,
			actualImageInsetsPointer, actualTextInsetsPointer, alignment, displayFunction, betweenLinesMargin,
			ButtonBackgroundViewDrawer(colorPointer.map { c => c }, statePointer, borderWidth) +: additionalDrawers,
			allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)
	}
	
	
	// INITIAL CODE	------------------------------
	
	setup(baseStatePointer, hotKeys, hotKeyCharacters)
	colorPointer.addListener { _ => repaint() }
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def trigger() = action(contentPointer.value)
}
