package utopia.reach.component.button.image

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.shape.shape2D.Point
import utopia.reach.component.button.MutableButtonLike
import utopia.reach.component.factory.ComponentFactoryFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.label.image.ViewImageLabel
import utopia.reach.component.template.ReachComponentWrapper
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reflection.color.ColorShadeVariant
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.swing.button.ButtonImageSet
import utopia.reflection.event.{ButtonState, HotKey}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets

object MutableImageButton extends ComponentFactoryFactory[MutableImageButtonFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableImageButtonFactory(hierarchy)
}

class MutableImageButtonFactory(parentHierarchy: ComponentHierarchy)
{
	/**
	  * Creates a new image button
	  * @param images The images used in this button
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image (default = center)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param allowUpscaling Whether the images should be allowed to scale up to their source resolution
	  *                       (default = true)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @return A new image button
	  */
	def withoutAction(images: ButtonImageSet, insets: StackInsets = StackInsets.zero,
					  alignment: Alignment = Alignment.Center, hotKeys: Set[HotKey] = Set(),
					  additionalDrawers: Vector[CustomDrawer] = Vector(),
					  allowUpscaling: Boolean = true, useLowPrioritySize: Boolean = false) =
		new MutableImageButton(parentHierarchy, images, insets, alignment, hotKeys, additionalDrawers,
			allowUpscaling, useLowPrioritySize)
	
	/**
	  * Creates a new image button
	  * @param images The images used in this button
	  * @param insets Insets placed around the image (default = always 0)
	  * @param alignment Alignment used when placing the image (default = center)
	  * @param hotKeys Hotkeys used for triggering this button even when it doesn't have focus (default = empty)
	  * @param additionalDrawers Additional custom drawers assigned to this button (default = empty)
	  * @param allowUpscaling Whether the images should be allowed to scale up to their source resolution
	  *                       (default = true)
	  * @param useLowPrioritySize Whether low priority size constraints should be used (default = false)
	  * @param action Action performed each time this button is triggered
	  * @return A new image button
	  */
	def apply(images: ButtonImageSet, insets: StackInsets = StackInsets.zero,
			  alignment: Alignment = Alignment.Center, hotKeys: Set[HotKey] = Set(),
			  additionalDrawers: Vector[CustomDrawer] = Vector(),
			  allowUpscaling: Boolean = true, useLowPrioritySize: Boolean = false)(action: => Unit) =
	{
		val button = withoutAction(images, insets, alignment, hotKeys, additionalDrawers,
			allowUpscaling, useLowPrioritySize)
		button.registerAction(action)
		button
	}
}

/**
  * A mutable implementation of a button that only contains an image
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
class MutableImageButton(parentHierarchy: ComponentHierarchy, initialImages: ButtonImageSet,
						 initialInsets: StackInsets = StackInsets.zero, initialAlignment: Alignment = Alignment.Center,
						 hotKeys: Set[HotKey] = Set(), additionalDrawers: Vector[CustomDrawer] = Vector(),
						 allowUpscaling: Boolean = true, useLowPrioritySize: Boolean = false)
	extends ReachComponentWrapper with MutableButtonLike
{
	// ATTRIBUTES	--------------------------------
	
	/**
	  * A mutable pointer to this button's current image set
	  */
	val imagesPointer = new PointerWithEvents(initialImages)
	/**
	  * A mutable pointer to this button's current insets
	  */
	val insetsPointer = new PointerWithEvents(initialInsets)
	/**
	  * A mutable pointer to this button's current alignment
	  */
	val alignmentPointer = new PointerWithEvents[Alignment](initialAlignment)
	
	private val _statePointer = new PointerWithEvents(ButtonState.default)
	/**
	  * A pointer to this button's currently displayed image
	  */
	val imagePointer = _statePointer.mergeWith(imagesPointer) { (state, images) => images(state) }
	/**
	  * A pointer to the current overall shade of this button (based on the focused-image)
	  */
	val shadePointer = imagesPointer.lazyMap { images =>
		ColorShadeVariant.forLuminosity(images.focusImage.pixels.averageLuminosity) }
	
	override var focusListeners: Seq[FocusListener] = Vector[FocusListener](new ButtonDefaultFocusListener(_statePointer))
	override protected var actions: Seq[() => Unit] = Vector()
	override protected val wrapped = new ViewImageLabel(parentHierarchy, imagePointer, insetsPointer, alignmentPointer,
		additionalDrawers, allowUpscaling, useLowPrioritySize)
	
	override val focusId = hashCode()
	
	
	// INITIAL CODE	-------------------------------
	
	setup(_statePointer, hotKeys)
	
	
	// COMPUTED	-----------------------------------
	
	/**
	  * @return Currently used image set in this button
	  */
	def images = imagesPointer.value
	def images_=(newImages: ButtonImageSet) = imagesPointer.value = newImages
	
	/**
	  * @return Currently placed insets around the drawn image
	  */
	def insets = insetsPointer.value
	def insets_=(newInsets: StackInsets) = insetsPointer.value = newInsets
	
	/**
	  * @return Currently used alignment when placing the drawn image
	  */
	def alignment = alignmentPointer.value
	def alignment_=(newAlignment: Alignment) = alignmentPointer.value = newAlignment
	
	/**
	  * Current overall shade of this button (based on the focused-image)
	  */
	def shade = shadePointer.value
	
	
	// IMPLEMENTED	-------------------------------
	
	override def enabled_=(newState: Boolean) = _statePointer.update { _.copy(isEnabled = newState) }
	
	override def statePointer = _statePointer
	
	override protected def trigger() = actions.foreach { _() }
	
	override def cursorToImage(cursor: Cursor, position: Point) = cursor(shade)
}
