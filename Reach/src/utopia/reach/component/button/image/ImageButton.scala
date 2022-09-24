package utopia.reach.component.button.image

import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ColorShadeVariant}
import utopia.reflection.component.context.ColorContextLike
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.swing.button.ButtonImageSet
import utopia.reflection.event.{ButtonState, HotKey}
import utopia.reflection.image.SingleColorIcon
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackInsets

object ImageButton extends ContextInsertableComponentFactoryFactory[ColorContextLike, ImageButtonFactory,
	ContextualImageButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ImageButtonFactory(hierarchy)
}

class ImageButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualImageButtonFactory]
{
	// IMPLEMENTED	---------------------------
	
	override def withContext[N <: ColorContextLike](context: N) =
		ContextualImageButtonFactory(this, context)
	
	
	// OTHER	-------------------------------
	
	/**
	  * Creates a new button
	  * @param images Image set used in this button
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image within bounds (default = Center)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param allowUpscaling Whether the images should be allowed to scale up to their source resolution
	  *                       (default = true)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @param action Action performed each time this button is triggered
	  * @return A new button
	  */
	def apply(images: ButtonImageSet, insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
			  hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
			  additionalFocusListeners: Seq[FocusListener] = Vector(), allowUpscaling: Boolean = true,
			  useLowPrioritySize: Boolean = false)(action: => Unit) =
		new ImageButton(parentHierarchy, images, insets, alignment, hotKeys, additionalDrawers,
			additionalFocusListeners, allowUpscaling, useLowPrioritySize)(action)
}

case class ContextualImageButtonFactory[+N <: ColorContextLike](factory: ImageButtonFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualImageButtonFactory]
{
	// IMPLICIT	-----------------------------
	
	private implicit def c: ColorContextLike = context
	
	
	// IMPLEMENTED	-------------------------
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new button
	  * @param icon Icon used in this button
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image within bounds (default = Center)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @param action Action performed each time this button is triggered
	  * @return A new button
	  */
	def withIcon(icon: SingleColorIcon, insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
				 hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPrioritySize: Boolean = false)
				(action: => Unit) =
		factory(icon.asIndividualButton, insets, alignment, hotKeys, additionalDrawers,
			additionalFocusListeners, context.allowImageUpscaling, useLowPrioritySize)(action)
	
	/**
	  * Creates a new button
	  * @param icon Icon used in this button
	  * @param role The role of this button / the colour used in this button
	  * @param preferredShade Preferred color shade to use (default = standard)
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image within bounds (default = Center)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @param action Action performed each time this button is triggered
	  * @return A new button
	  */
	def withColouredIcon(icon: SingleColorIcon, role: ColorRole, preferredShade: ColorShade = Standard,
						 insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
						 hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
						 additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPrioritySize: Boolean = false)
						(action: => Unit) =
		factory(icon.asIndividualButtonWithColor(context.color(role, preferredShade)), insets, alignment, hotKeys,
			additionalDrawers, additionalFocusListeners, context.allowImageUpscaling, useLowPrioritySize)(action)
}

/**
  * A button that only draws an image
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ImageButton(parentHierarchy: ComponentHierarchy, images: ButtonImageSet, insets: StackInsets = StackInsets.zero,
				  alignment: Alignment = Alignment.Center, hotKeys: Set[HotKey] = Set(),
				  additionalDrawers: Vector[CustomDrawer] = Vector(),
				  additionalFocusListeners: Seq[FocusListener] = Vector(), allowUpscaling: Boolean = true,
				  useLowPrioritySize: Boolean = false)(action: => Unit) extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val _statePointer = new PointerWithEvents(ButtonState.default)
	
	override protected val wrapped = ViewImageLabel(parentHierarchy).withStaticLayout(
		_statePointer.map { state => images(state) }, insets, alignment, additionalDrawers, allowUpscaling,
		useLowPrioritySize)
	override val focusListeners = new ButtonDefaultFocusListener(_statePointer) +: additionalFocusListeners
	
	override val focusId = hashCode()
	
	/**
	  * The overall shade of this button (calculated based on the focused-state)
	  */
	lazy val shade = ColorShadeVariant.forLuminosity(images.focusImage.pixels.averageLuminosity)
	
	
	// INITIAL CODE	-----------------------------
	
	setup(_statePointer, hotKeys)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def statePointer = _statePointer.view
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor(shade.opposite)
}
