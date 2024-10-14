package utopia.reflection.component.swing.button

import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.image.{ButtonImageSet, SingleColorIcon}
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.paradigm.color.Color
import utopia.reflection.component.swing.label.ImageLabel
import utopia.reflection.component.swing.template.{StackableAwtComponentWrapperWrapper, SwingComponentRelated}

object FramedImageButton
{
	/**
	  * Creates a new button utilizing contextual information
	  * @param icon Icon displayed on this button
	  * @param hotKeys Hotkey indices that trigger this button (default = empty)
	  * @param hotKeyChars Keyboard characters that trigger this button (default = empty)
	  * @param isLowPriority Whether this button should shrink relatively easily (default = false)
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextualWithoutAction(icon: SingleColorIcon, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set(),
	                            isLowPriority: Boolean = false)
	                           (implicit context: StaticColorContext) =
		new FramedImageButton(icon.inButton.contextual, context.background, context.buttonBorderWidth, hotKeys,
			hotKeyChars, context.allowImageUpscaling, isLowPriority)
	
	/**
	  * Creates a new button utilizing contextual information
	  * @param icon Icon displayed on this button
	  * @param hotKeys Hotkey indices that trigger this button (default = empty)
	  * @param hotKeyChars Keyboard characters that trigger this button (default = empty)
	  * @param isLowPriority Whether this button should shrink relatively easily (default = false)
	  * @param action Function that is called when this button is pressed
	  * @param context Component creation context
	  * @return A new button
	  */
	def contextual[U](icon: SingleColorIcon, hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set(),
	               isLowPriority: Boolean = false)(action: => U)
	                 (implicit context: StaticColorContext) =
	{
		val button = contextualWithoutAction(icon, hotKeys, hotKeyChars, isLowPriority)
		button.registerAction { () => action }
		button
	}
}

/**
  * This button shows an image against a solid background
  * @author Mikko Hilpinen
  * @since 24.9.2020, v1.3
  */
class FramedImageButton(images: ButtonImageSet, color: Color, borderWidth: Double = 0.0, hotKeys: Set[Int] = Set(),
                        hotKeyChars: Iterable[Char] = Set(),
                        allowImageUpscaling: Boolean = false, isLowPriority: Boolean = false)
	extends ButtonWithBackground(color, borderWidth) with StackableAwtComponentWrapperWrapper
		with SwingComponentRelated with MutableCustomDrawableWrapper
{
	// ATTRIBUTES   ---------------------------
	
	private val label = new ImageLabel(images(state), allowUpscaling = allowImageUpscaling,
		isLowPriority = isLowPriority)
	private val content = {
		if (borderWidth > 0)
			label.framed(StackInsets.symmetric(borderWidth.fixed))
		else
			label
	}
	
	
	// INITIAL CODE ---------------------------
	
	setup(hotKeys, hotKeyChars)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def component = content.component
	
	override protected def wrapped = content
	
	override def drawable = content
	
	override protected def updateStyleForState(newState: GuiElementStatus) = {
		super.updateStyleForState(newState)
		label.image = images(newState)
	}
}
