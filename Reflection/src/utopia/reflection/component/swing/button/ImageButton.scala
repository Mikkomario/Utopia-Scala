package utopia.reflection.component.swing.button

import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.event.ButtonState

object ImageButton
{
	/**
	  * Creates a new button
	  * @param images Images used in this button
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param allowImageUpscaling Whether image upscaling should be allowed (default = false)
	  * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
	  * @param action Action that is triggered when this button is pressed
	  * @return A new button
	  */
	def apply(images: ButtonImageSet, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set(),
	          allowImageUpscaling: Boolean = false, isLowPriority: Boolean = false)(action: => Unit) =
	{
		val button = new ImageButton(images, hotKeys, hotKeyChars, allowImageUpscaling, isLowPriority)
		button.registerAction { () => action }
		button
	}
	
	/**
	 * Creates a new button using contextual information
	 * @param images Images used in this button
	 * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
	  * @param context Component creation context
	 * @return A new button
	 */
	def contextualWithoutAction(images: ButtonImageSet, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set(),
	                            isLowPriority: Boolean = false)(implicit context: BaseContextLike) =
		new ImageButton(images, hotKeys, hotKeyChars, context.allowImageUpscaling, isLowPriority)
	
	/**
	  * Creates a new button using contextual information
	  * @param images Images used in this button
	  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
	  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
	  * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
	  * @param action Action performed when this button is pressed
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextual(images: ButtonImageSet, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set(),
	               isLowPriority: Boolean = false)(action: => Unit)(implicit context: BaseContextLike) =
	{
		val button = contextualWithoutAction(images, hotKeys, hotKeyChars, isLowPriority)
		button.registerAction { () => action }
		button
	}
}

/**
  * This button only displays an image
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  * @param images Images used in this button
  * @param hotKeys Hotkey indices that can be used for triggering this button (default = empty)
  * @param hotKeyChars Characters on keyboard that can be used for triggering this button (default = empty)
  * @param allowImageUpscaling Whether image upscaling should be allowed (default = false)
  * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
  */
class ImageButton(val images: ButtonImageSet, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set(),
                  allowImageUpscaling: Boolean = false, isLowPriority: Boolean = false)
	extends StackableAwtComponentWrapperWrapper with ButtonLike with SwingComponentRelated
{
	// ATTRIBUTES	--------------------
	
	private val label = new ImageLabel(images(state), allowUpscaling = allowImageUpscaling,
		isLowPriority = isLowPriority)
	
	
	// INITIAL CODE	--------------------
	
	// Uses hand cursor on buttons by default
	setHandCursor()
	initializeListeners(hotKeys, hotKeyChars)
	component.setFocusable(true)
	
	
	// IMPLEMENTED	--------------------
	
	override def component = label.component
	
	override protected def wrapped = label
	
	override protected def updateStyleForState(newState: ButtonState) =
	{
		val newImage = images(newState)
		label.image = newImage
	}
}
