package utopia.reach.component.button.image

import utopia.firmament.context.TextContext
import utopia.firmament.drawing.view.ButtonBackgroundViewDrawer
import utopia.firmament.image.SingleColorIcon
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ImageAndTextLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.reflection.shape.stack.StackInsets

object ImageAndTextButton extends ContextInsertableComponentFactoryFactory[TextContext,
	ImageAndTextButtonFactory, ContextualImageAndTextButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ImageAndTextButtonFactory(hierarchy)
}

class ImageAndTextButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContext, ContextualImageAndTextButtonFactory]
{
	override def withContext[N <: TextContext](context: N) =
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
			  borderWidth: Double = 0.0, betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
			  additionalDrawers: Vector[CustomDrawer] = Vector(),
			  additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
			  allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
			  useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit) =
		new ImageAndTextButton(parentHierarchy, image, text, color, font, textColor, alignment, imageInsets,
			textInsets, commonInsets, borderWidth, betweenLinesMargin, hotKeys, additionalDrawers,
			additionalFocusListeners, allowLineBreaks, allowImageUpscaling, allowTextShrink, useLowPriorityImageSize,
			forceEqualBreadth)(action)
}

case class ContextualImageAndTextButtonFactory[+N <: TextContext](factory: ImageAndTextButtonFactory, context: N)
	extends ContextualComponentFactory[N, TextContext, ContextualImageAndTextButtonFactory]
{
	private implicit def c: TextContext = context
	
	override def withContext[N2 <: TextContext](newContext: N2) =
		copy(context = newContext)
	
	/**
	  * Creates a new button with both image and text
	  * @param image Image displayed on this button
	  * @param text Text displayed on this button
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned for this component (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be force to have equal breadth (default = false)
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def apply(image: Image, text: LocalizedString, imageInsets: StackInsets = StackInsets.any,
			  hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
			  additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
			  forceEqualBreadth: Boolean = false)(action: => Unit) =
		factory(image, text, context.background, context.font, context.textColor, context.textAlignment, imageInsets,
			context.textInsets / 2, context.textInsets / 2, context.buttonBorderWidth, context.betweenLinesMargin.optimal,
			hotKeys, additionalDrawers, additionalFocusListeners, context.allowLineBreaks,
			context.allowImageUpscaling, context.allowTextShrink, useLowPriorityImageSize, forceEqualBreadth)(action)
	
	/**
	  * Creates a new button with both image and text
	  * @param icon Icon displayed on this button
	  * @param text Text displayed on this button
	  * @param imageInsets Insets placed around the image (default = any, preferring 0)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned for this component (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPriorityImageSize Whether low priority size constraints should be used for the image
	  *                                (default = false)
	  * @param forceEqualBreadth Whether the image and the text should be force to have equal breadth (default = false)
	  * @param action Action called whenever this button is triggered
	  * @return A new button
	  */
	def withIcon(icon: SingleColorIcon, text: LocalizedString, imageInsets: StackInsets = StackInsets.any,
				 hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPriorityImageSize: Boolean = false,
				 forceEqualBreadth: Boolean = false)(action: => Unit) =
		apply(icon.contextual, text, imageInsets, hotKeys, additionalDrawers,
			additionalFocusListeners, useLowPriorityImageSize, forceEqualBreadth)(action)
}

/**
  * A button which displays both an image and some text
  * @author Mikko Hilpinen
  * @since 10.11.2020, v0.1
  */
class ImageAndTextButton(parentHierarchy: ComponentHierarchy, image: Image, text: LocalizedString, color: Color,
						 font: Font, textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						 imageInsets: StackInsets = StackInsets.any, textInsets: StackInsets = StackInsets.any,
						 commonInsets: StackInsets = StackInsets.zero, borderWidth: Double = 0.0,
						 betweenLinesMargin: Double = 0.0, hotKeys: Set[HotKey] = Set(),
						 additionalDrawers: Vector[CustomDrawer] = Vector(),
						 additionalFocusListeners: Seq[FocusListener] = Vector(), allowLineBreaks: Boolean = true,
						 allowImageUpscaling: Boolean = true, allowTextShrink: Boolean = false,
						 useLowPriorityImageSize: Boolean = false, forceEqualBreadth: Boolean = false)(action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val _statePointer = new PointerWithEvents(GuiElementStatus.identity)
	
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
	
	setup(_statePointer, hotKeys)
	
	
	// IMPLEMENTED	------------------------------
	
	override def statePointer = _statePointer.view
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(color)
}
