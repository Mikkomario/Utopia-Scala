package utopia.reflection.component.swing.button

import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.reflection.component.drawing.mutable.MutableCustomDrawableWrapper
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.input.InteractionWithPointer
import utopia.reflection.event.ButtonState

/**
  * Used for switching a value on or off
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  * @param offImages Images to display while this box is not checked
  * @param onImages Images to display while this box is checked
  * @param hotKeys Key indices that change the state of this box (default = empty)
  * @param hotKeyChars Characters in keyboard that change the state of this box (default = empty)
  * @param initialState Whether this box should be checked initially (default = false)
  */
class ImageCheckBox(offImages: ButtonImageSet, onImages: ButtonImageSet, hotKeys: Set[Int] = Set(),
                    hotKeyChars: Iterable[Char] = Set(), initialState: Boolean = false)
	extends StackableAwtComponentWrapperWrapper with MutableCustomDrawableWrapper with ButtonLike
		with InteractionWithPointer[Boolean] with SwingComponentRelated
{
	// ATTRIBUTES	---------------------
	
	private val label = new ImageLabel(if (initialState) onImages.defaultImage else offImages.defaultImage)
	override val valuePointer = new PointerWithEvents(initialState)
	
	
	// INITIAL CODE	---------------------
	
	initializeListeners(hotKeys, hotKeyChars)
	valuePointer.addContinuousAnyChangeListener { updateStyleForState(state) }
	registerAction { () => value = !value }
	setHandCursor()
	component.setFocusable(true)
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Whether this check box has been checked
	  */
	def isOn = value
	/**
	  * @return Whether this check box is empty
	  */
	def isOff = !isOn
	
	
	// IMPLEMENTED	---------------------
	
	override def component = label.component
	
	override protected def wrapped = label
	
	override def drawable = label
	
	override protected def updateStyleForState(newState: ButtonState) = {
		if (isOn)
			label.image = onImages(newState)
		else
			label.image = offImages(newState)
	}
}
