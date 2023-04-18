package utopia.reach.component.button.image

import utopia.firmament.context.ColorContext
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.model.enumeration.GuiElementState.Disabled
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{ColorLevel, ColorRole, ColorShade}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.factory.{ColorContextualFactory, FromContextFactory, FromGenericContextComponentFactoryFactory, FromGenericContextFactory, GenericContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.template.{ButtonLike, ReachComponentWrapper}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.stack.StackInsets
import utopia.reach.component.factory.ComponentFactoryFactory.Cff

object ViewImageButton extends Cff[ViewImageButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new ViewImageButtonFactory(hierarchy)
}

class ViewImageButtonFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[ColorContext, ContextualViewImageButtonFactory]
{
	// IMPLEMENTED	---------------------------
	
	override def withContext(context: ColorContext) =
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
	def apply(imagesPointer: Changing[ButtonImageSet], enabledPointer: Changing[Boolean] = AlwaysTrue,
	          insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
	          hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
	          additionalFocusListeners: Seq[FocusListener] = Vector(), allowUpscaling: Boolean = true,
	          useLowPrioritySize: Boolean = false)(action: => Unit) =
		new ViewImageButton(parentHierarchy, imagesPointer, enabledPointer, insets, alignment, hotKeys,
			additionalDrawers, additionalFocusListeners, allowUpscaling, useLowPrioritySize)(action)
}

case class ContextualViewImageButtonFactory(factory: ViewImageButtonFactory, context: ColorContext)
	extends ColorContextualFactory[ContextualViewImageButtonFactory]
{
	// IMPLICIT	-----------------------------
	
	private implicit def c: ColorContext = context
	
	
	// IMPLEMENTED	-------------------------
	
	override def self: ContextualViewImageButtonFactory = this
	
	override def withContext(newContext: ColorContext) = copy(context = newContext)
	
	
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
	def withIcon(iconPointer: Changing[SingleColorIcon], enabledPointer: Changing[Boolean] = AlwaysTrue,
	             insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
	             hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
	             additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPrioritySize: Boolean = false)
				(action: => Unit) =
		factory(iconPointer.map { _.asButton.contextual }, enabledPointer, insets, alignment, hotKeys,
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
	def withColouredIcon(iconPointer: Changing[SingleColorIcon], rolePointer: Changing[ColorRole],
	                     enabledPointer: Changing[Boolean] = AlwaysTrue,
	                     preferredShade: ColorLevel = Standard, insets: StackInsets = StackInsets.zero,
	                     alignment: Alignment = Alignment.Center, hotKeys: Set[HotKey] = Set(),
	                     additionalDrawers: Vector[CustomDrawer] = Vector(),
	                     additionalFocusListeners: Seq[FocusListener] = Vector(), useLowPrioritySize: Boolean = false)
						(action: => Unit) =
	{
		val colorPointer = rolePointer.map { context.color.preferring(preferredShade)(_) }
		val imagesPointer = iconPointer.mergeWith(colorPointer) { _.asButton(_) }
		factory(imagesPointer, enabledPointer, insets, alignment, hotKeys, additionalDrawers,
			additionalFocusListeners, context.allowImageUpscaling, useLowPrioritySize)(action)
	}
}

/**
  * A button that only draws images and whose state is dependent from a number of pointers
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class ViewImageButton(parentHierarchy: ComponentHierarchy, imagesPointer: Changing[ButtonImageSet],
                      enabledPointer: Changing[Boolean] = AlwaysTrue,
                      insets: StackInsets = StackInsets.zero, alignment: Alignment = Alignment.Center,
                      hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
                      additionalFocusListeners: Seq[FocusListener] = Vector(), allowUpscaling: Boolean = true,
                      useLowPrioritySize: Boolean = false)(action: => Unit)
	extends ReachComponentWrapper with ButtonLike
{
	// ATTRIBUTES	-----------------------------
	
	private val baseStatePointer = new PointerWithEvents(GuiElementStatus.identity)
	
	override val statePointer = baseStatePointer
		.mergeWith(enabledPointer) { (base, enabled) => base + (Disabled -> !enabled) }
	override protected val wrapped = ViewImageLabel(parentHierarchy).withStaticLayout(
		statePointer.mergeWith(imagesPointer) { (state, images) => images(state) }, insets, alignment,
		additionalDrawers, allowUpscaling, useLowPrioritySize)
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	
	/**
	  * A pointer to this button's current overall shade (based on the focused-state)
	  */
	val shadePointer = imagesPointer.lazyMap { images =>
		ColorShade.forLuminosity(images.focusImage.pixels.averageLuminosity) }
	
	
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
