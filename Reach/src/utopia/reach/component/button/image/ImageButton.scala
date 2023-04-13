package utopia.reach.component.button.image

import utopia.firmament.context.ColorContext
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole, ColorShade}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.firmament.model.stack.StackInsets

object ImageButton extends ContextInsertableComponentFactoryFactory[ColorContext, ImageButtonFactory,
	ContextualImageButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ImageButtonFactory(hierarchy)
}

class ImageButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContext, ContextualImageButtonFactory]
{
	// IMPLEMENTED	---------------------------
	
	override def withContext[N <: ColorContext](context: N) =
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

case class ContextualImageButtonFactory[+N <: ColorContext](factory: ImageButtonFactory, context: N)
	extends ContextualComponentFactory[N, ColorContext, ContextualImageButtonFactory]
{
	// IMPLICIT	-----------------------------
	
	private implicit def c: ColorContext = context
	
	
	// IMPLEMENTED	-------------------------
	
	override def withContext[N2 <: ColorContext](newContext: N2) =
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
		factory(icon.asButton.contextual, insets, alignment, hotKeys, additionalDrawers,
			additionalFocusListeners, context.allowImageUpscaling, useLowPrioritySize)(action)
	
	/**
	  * Creates a new button
	  * @param icon Icon used in this button
	  * @param role The role of this button / the colour used in this button
	  * @param preferredShade Preferred color shade to use (default = standard)
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image within bounds (default = Center)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param customDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @param action Action performed each time this button is triggered
	  * @return A new button
	  */
	def withColouredIcon(icon: SingleColorIcon, role: ColorRole, preferredShade: ColorLevel = Standard,
	                     insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
	                     hotKeys: Set[HotKey] = Set(), customDrawers: Vector[CustomDrawer] = Vector(),
	                     additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPrioritySize: Boolean = false)
						(action: => Unit) =
		factory(icon.asButton(context.color.preferring(preferredShade)(role)), insets, alignment, hotKeys,
			customDrawers, additionalFocusListeners, context.allowImageUpscaling, useLowPrioritySize)(action)
}

/**
  * A button that only draws an image
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
// TODO: Add enabled & disabled -feature
class ImageButton(parentHierarchy: ComponentHierarchy, images: ButtonImageSet, insets: StackInsets = StackInsets.zero,
				  alignment: Alignment = Alignment.Center, hotKeys: Set[HotKey] = Set(),
				  additionalDrawers: Vector[CustomDrawer] = Vector(),
				  additionalFocusListeners: Seq[FocusListener] = Vector(), allowUpscaling: Boolean = true,
				  useLowPrioritySize: Boolean = false)(action: => Unit) extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val _statePointer = new PointerWithEvents(GuiElementStatus.identity)
	
	override protected val wrapped = ViewImageLabel(parentHierarchy).withStaticLayout(
		_statePointer.map { state => images(state) }, insets, alignment, additionalDrawers, allowUpscaling,
		useLowPrioritySize)
	override val focusListeners = new ButtonDefaultFocusListener(_statePointer) +: additionalFocusListeners
	
	override val focusId = hashCode()
	
	/**
	  * The overall shade of this button (calculated based on the focused-state)
	  */
	lazy val shade = ColorShade.forLuminosity(images.focusImage.pixels.averageLuminosity)
	
	
	// INITIAL CODE	-----------------------------
	
	setup(_statePointer, hotKeys)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def statePointer = _statePointer.view
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor.over(shade)
}
