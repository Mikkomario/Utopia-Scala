package utopia.reflection.component.swing.button

import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.image.ButtonImageSet
import utopia.firmament.model.GuiElementStatus
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}

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
	
	private val label = new ImageLabel(if (initialState) onImages.default else offImages.default)
	override val valuePointer = EventfulPointer(initialState)
	
	
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
	
	override protected def updateStyleForState(newState: GuiElementStatus) = {
		if (isOn)
			label.image = onImages(newState)
		else
			label.image = offImages(newState)
	}
}
