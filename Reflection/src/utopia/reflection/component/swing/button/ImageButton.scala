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
	  * @param action Action that is triggered when this button is pressed
	  * @return A new button
	  */
	def apply(images: ButtonImageSet, allowImageUpscaling: Boolean = false)(action: () => Unit) =
	{
		val button = new ImageButton(images, allowImageUpscaling)
		button.registerAction(action)
		button
	}
	
	/**
	 * Creates a new button using contextual information
	 * @param images Images used in this button
	 * @param context Component creation context
	 * @return A new button
	 */
	def contextualWithoutAction(images: ButtonImageSet)(implicit context: BaseContextLike) =
		new ImageButton(images, context.allowImageUpscaling)
	
	/**
	  * Creates a new button using contextual information
	  * @param images Images used in this button
	  * @param action Action performed when this button is pressed
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextual(images: ButtonImageSet)(action: => Unit)(implicit context: BaseContextLike) =
	{
		val button = contextualWithoutAction(images)
		button.registerAction(() => action)
		button
	}
}

/**
  * This button only displays an image
  * @author Mikko Hilpinen
  * @since 1.8.2019, v1+
  * @param images Images used in this button
  * @param allowImageUpscaling Whether image upscaling should be allowed
  */
class ImageButton(val images: ButtonImageSet, allowImageUpscaling: Boolean = false)
	extends StackableAwtComponentWrapperWrapper with ButtonLike with SwingComponentRelated
{
	// ATTRIBUTES	--------------------
	
	private val label = new ImageLabel(images(state), allowUpscaling = allowImageUpscaling)
	
	
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
