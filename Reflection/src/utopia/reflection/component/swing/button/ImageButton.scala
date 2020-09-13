package utopia.reflection.component.swing.button

import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}

object ImageButton
{
	/**
	  * Creates a new button
	  * @param images Images used in this button
	  * @param allowImageUpscaling Whether image upscaling should be allowed (default = false)
	  * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
	  * @param action Action that is triggered when this button is pressed
	  * @return A new button
	  */
	def apply(images: ButtonImageSet, allowImageUpscaling: Boolean = false, isLowPriority: Boolean = false)
			 (action: () => Unit) =
	{
		val button = new ImageButton(images, allowImageUpscaling, isLowPriority)
		button.registerAction(action)
		button
	}
	
	/**
	 * Creates a new button using contextual information
	 * @param images Images used in this button
	 * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
	  * @param context Component creation context
	 * @return A new button
	 */
	def contextualWithoutAction(images: ButtonImageSet, isLowPriority: Boolean = false)(implicit context: BaseContextLike) =
		new ImageButton(images, context.allowImageUpscaling, isLowPriority)
	
	/**
	  * Creates a new button using contextual information
	  * @param images Images used in this button
	  * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
	  * @param action Action performed when this button is pressed
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextual(images: ButtonImageSet, isLowPriority: Boolean = false)(action: => Unit)(implicit context: BaseContextLike) =
	{
		val button = contextualWithoutAction(images, isLowPriority)
		button.registerAction(() => action)
		button
	}
}

/**
  * This button only displays an image
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  * @param images Images used in this button
  * @param allowImageUpscaling Whether image upscaling should be allowed (default = false)
  * @param isLowPriority Whether this button should be easily shrank when necessary (default = false)
  */
class ImageButton(val images: ButtonImageSet, allowImageUpscaling: Boolean = false, isLowPriority: Boolean = false)
	extends StackableAwtComponentWrapperWrapper with ButtonLike with SwingComponentRelated
{
	// ATTRIBUTES	--------------------
	
	private val label = new ImageLabel(images(state), allowUpscaling = allowImageUpscaling,
		isLowPriority = isLowPriority)
	
	
	// INITIAL CODE	--------------------
	
	// Uses hand cursor on buttons by default
	setHandCursor()
	initializeListeners()
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
