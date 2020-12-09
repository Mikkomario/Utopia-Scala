package utopia.reflection.component.reach.button

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Fixed
import utopia.genesis.color.Color
import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.component.context.ButtonContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.ButtonBackgroundViewDrawer
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.label.ImageAndTextLabel
import utopia.reflection.component.reach.template.{ButtonLike, ReachComponentWrapper}
import utopia.reflection.cursor.Cursor
import utopia.reflection.event.{ButtonState, FocusListener}
import utopia.reflection.image.SingleColorIcon
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object ImageAndTextButton extends ContextInsertableComponentFactoryFactory[ButtonContextLike,
	ImageAndTextButtonFactory, ContextualImageAndTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ImageAndTextButtonFactory(hierarchy)
}

class ImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ButtonContextLike, ContextualImageAndTextButtonFactory]
{
	override def withContext[N <: ButtonContextLike](context: N) =
		ContextualImageAndTextButtonFactory(this, context)
	
	/**
	  * Creates a new button with both image and text
	  * @param image Image displayed on this button
	  * @param text Text displayed on this button
	  * @param color Button default background color
	  * @param font Font used when drawing text
	  * @param textColor Color used when drawing text (default = standard black)
	  * @param alignment Text alignment (default = Left)
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param textInsets Insets placed around the text (default = any, preferring 0)
	  * @param commonInsets Insets placed around both the image and the text (default = any, preferring 0)
	  * @param borderWidth Width of the border in this button (default = 0 = no border)
	  * @param betweenLinesMargin Vertical margin between text lines (default = 0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned for this component (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param allowLineBreaks Whether text should be allowed to use line breaks (default = true)
	  * @param allowImageUpscaling Whether image should be allowed to scale up to its source resolution (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be force to have equal breadth (default = false)
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def apply(image: Image, text: LocalizedString, color: Color, font: Font, textColor: Color = Color.textBlack,
			  alignment: Alignment = Alignment.Left, imageInsets: StackInsets = StackInsets.any,
			  textInsets: StackInsets = StackInsets.any, commonInsets: StackInsets = StackInsets.any,
			  borderWidth: Double = 0.0, betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(),
			  hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
			  additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
			  allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
			  useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit) =
		new ImageAndTextButton(parentHierarchy, image, text, color, font, textColor, alignment, imageInsets,
			textInsets, commonInsets, borderWidth, betweenLinesMargin, hotKeys, hotKeyCharacters, additionalDrawers,
			additionalFocusListeners, allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth)(action)
}

case class ContextualImageAndTextButtonFactory[+N <: ButtonContextLike](factory: ImageAndTextButtonFactory, context: N)
	extends ContextualComponentFactory[N, ButtonContextLike, ContextualImageAndTextButtonFactory]
{
	private implicit def c: ButtonContextLike = context
	
	override def withContext[N2 <: ButtonContextLike](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new button with both image and text
	  * @param image Image displayed on this button
	  * @param text Text displayed on this button
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned for this component (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be force to have equal breadth (default = false)
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def apply(image: Image, text: LocalizedString, imageInsets: StackInsets = StackInsets.any,
			  hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
			  additionalDrawers: Vector[CustomDrawer] = Vector(),
			  additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
			  forceEqualBreadth: Boolean = false)(action: => Unit) =
		factory(image, text, context.buttonColor, context.font, context.textColor, context.textAlignment, imageInsets,
			context.textInsets / 2, context.textInsets / 2, context.borderWidth, context.betweenLinesMargin.optimal,
			hotKeys, hotKeyCharacters, additionalDrawers, additionalFocusListeners, context.allowLineBreaks,
			context.allowImageUpscaling, context.allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with both image and text
	  * @param icon Icon displayed on this button
	  * @param text Text displayed on this button
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Hotkey characters used for triggering this button even when it doesn't have focus
	  *                         (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned for this component (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be force to have equal breadth (default = false)
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def withIcon(icon: SingleColorIcon, text: LocalizedString, imageInsets: StackInsets = StackInsets.any,
				 hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set(),
				 additionalDrawers: Vector[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
				 forceEqualBreadth: Boolean = false)(action: => Unit) =
		apply(icon.singleColorImage, text, imageInsets, hotKeys, hotKeyCharacters, additionalDrawers,
			additionalFocusListeners, useLowPriorityImageSize, forceEqualBreadth)(action)
}

/**
  * A button which displays both an image and some text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v2
  */
class ImageAndTextButton(parentHierarchy: ComponentHierarchy, image: Image, text: LocalizedString, color: Color,
						 font: Font, textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						 imageInsets: StackInsets = StackInsets.any, textInsets: StackInsets = StackInsets.any,
						 commonInsets: StackInsets = StackInsets.zero, borderWidth: Double = 0.0,
						 betweenLinesMargin: Double = 0.0, hotKeys: Set[Int] = Set(),
						 hotKeyCharacters: Iterable[Char] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
						 additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
						 allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
						 useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val _statePointer = new PointerWithEvents(ButtonState.default)
	
	override val focusListeners = new ButtonDefaultFocusListener(_statePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	
	override protected val wrapped =
	{
		val actualImageInsets = imageInsets + commonInsets.withoutSides(alignment.opposite.directions) + borderWidth
		val actualTextInsets = textInsets + commonInsets.withoutSides(alignment.directions) + borderWidth
		new ImageAndTextLabel(parentHierarchy, image, text, font, textColor, alignment,
			actualImageInsets, actualTextInsets, betweenLinesMargin,
			ButtonBackgroundViewDrawer(Fixed(color), statePointer, borderWidth) +: additionalDrawers,
			allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)
	}
	
	
	// INITIAL CODE	------------------------------
	
	setup(_statePointer, hotKeys, hotKeyCharacters)
	
	
	// IMPLEMENTED	------------------------------
	
	override def statePointer = _statePointer.view
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
