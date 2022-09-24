package utopia.reach.component.button.image

import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.ChangingLike
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

object ViewImageButton extends ContextInsertableComponentFactoryFactory[ColorContextLike, ViewImageButtonFactory,
	ContextualViewImageButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewImageButtonFactory(hierarchy)
}

class ViewImageButtonFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualViewImageButtonFactory]
{
	// IMPLEMENTED	---------------------------
	
	override def withContext[N <: ColorContextLike](context: N) =
		ContextualViewImageButtonFactory(this, context)
	
	
	// OTHER	-------------------------------
	
	/**
	  * Creates a new button
	  * @param imagesPointer A pointer to the images used in this button
	  * @param enabledPointer A pointer to this button's enabled state (default = always enabled)
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
	def apply(imagesPointer: ChangingLike[ButtonImageSet], enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
			  insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
			  hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
			  additionalFocusListeners: Seq[FocusListener] = Vector(), allowUpscaling: Boolean = true,
			  useLowPrioritySize: Boolean = false)(action: => Unit) =
		new ViewImageButton(parentHierarchy, imagesPointer, enabledPointer, insets, alignment, hotKeys,
			additionalDrawers, additionalFocusListeners, allowUpscaling, useLowPrioritySize)(action)
}

case class ContextualViewImageButtonFactory[+N <: ColorContextLike](factory: ViewImageButtonFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualViewImageButtonFactory]
{
	// IMPLICIT	-----------------------------
	
	private implicit def c: ColorContextLike = context
	
	
	// IMPLEMENTED	-------------------------
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) =
		copy(context = newContext)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Creates a new button
	  * @param iconPointer A pointer to the icon used in this button
	  * @param enabledPointer A pointer to this button's enabled state (default = always enabled)
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image within bounds (default = Center)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param additionalFocusListeners Additional focus listeners assigned to this button (default = empty)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @param action Action performed each time this button is triggered
	  * @return A new button
	  */
	def withIcon(iconPointer: ChangingLike[SingleColorIcon], enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
				 insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
				 hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
				 additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPrioritySize: Boolean = false)
				(action: => Unit) =
		factory(iconPointer.map { _.asIndividualButton }, enabledPointer, insets, alignment, hotKeys,
			additionalDrawers, additionalFocusListeners, context.allowImageUpscaling, useLowPrioritySize)(action)
	
	/**
	  * Creates a new button
	  * @param iconPointer A pointer to the icon used in this button
	  * @param rolePointer A pointer to the role this button serves / the color set it should use
	  * @param enabledPointer A pointer to this button's enabled state (default = always enabled)
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
	def withColouredIcon(iconPointer: ChangingLike[SingleColorIcon], rolePointer: ChangingLike[ColorRole],
						 enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
						 preferredShade: ColorShade = Standard, insets: StackInsets = StackInsets.zero,
						 alignment: Alignment = Alignment.Center, hotKeys: Set[HotKey] = Set(),
						 additionalDrawers: Vector[CustomDrawer] = Vector(),
						 additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPrioritySize: Boolean = false)
						(action: => Unit) =
	{
		val colorPointer = rolePointer.map { context.color(_, preferredShade) }
		val imagesPointer = iconPointer.mergeWith(colorPointer) { _.asIndividualButtonWithColor(_) }
		factory(imagesPointer, enabledPointer, insets, alignment, hotKeys, additionalDrawers,
			additionalFocusListeners, context.allowImageUpscaling, useLowPrioritySize)(action)
	}
}

/**
  * A button that only draws images and whose state is dependent from a number of pointers
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ViewImageButton(parentHierarchy: ComponentHierarchy, imagesPointer: ChangingLike[ButtonImageSet],
					  enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
					  insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
					  hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
					  additionalFocusListeners: Seq[FocusListener] = Vector(), allowUpscaling: Boolean = true,
					  useLowPrioritySize: Boolean = false)(action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(ButtonState.default)
	
	override val statePointer = baseStatePointer.mergeWith(enabledPointer) { (base, enabled) =>
		base.copy(isEnabled = enabled) }
	override protected val wrapped = ViewImageLabel(parentHierarchy).withStaticLayout(
		statePointer.mergeWith(imagesPointer) { (state, images) => images(state) }, insets, alignment,
		additionalDrawers, allowUpscaling, useLowPrioritySize)
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	
	/**
	  * A pointer to this button's current overall shade (based on the focused-state)
	  */
	val shadePointer = imagesPointer.lazyMap { images =>
		ColorShadeVariant.forLuminosity(images.focusImage.pixels.averageLuminosity) }
	
	
	// INITIAL CODE	-----------------------------
	
	setup(baseStatePointer, hotKeys)
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return The current overall shade of this button (based on the focused-state)
	  */
	def shade = shadePointer.value
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def trigger() = action
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor(shade)
}
