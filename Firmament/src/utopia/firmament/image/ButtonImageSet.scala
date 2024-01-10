package utopia.firmament.image

import utopia.firmament.model.enumeration.GuiElementState.{Activated, Focused, Hover}
import utopia.firmament.model.GuiElementStatus
import utopia.flow.operator.sign.Sign.Negative
import utopia.genesis.image.Image
import utopia.paradigm.transform.Adjustment

object ButtonImageSet
{
	/**
	  * Creates a new button image set that always presents the same image
	  * @param image The image presented in the new set
	  * @return A set that only contains the specified image
	  */
	def fixed(image: Image) = ButtonImageSet(image, image, image, image)
	
	/**
	  * Creates a new button mage set that presents a modified version of the specified image
	  * @param image The original image
	  * @param intensity Brightening intensity modifier (default = 1)
	  * @return A set that will a) Use 55% alpha while disabled and b) brighten the image on focus and pressed -states
	  */
	def brightening(image: Image, intensity: Double = 1) = {
		val disabled = image.timesAlpha(0.55)
		val focused = image.mapEachPixel { _.lightenedBy(intensity) }
		val pressed = image.mapEachPixel { _.lightenedBy(intensity * 2) }
		
		ButtonImageSet(image, focused, pressed, disabled)
	}
	/**
	  * Creates a new button mage set that presents a modified version of the specified image
	  * @param image The original image
	  * @param intensity Darkening intensity modifier (default = 1)
	  * @return A set that will a) Use 55% alpha while disabled and b) darken the image on focus and pressed -states
	  */
	def darkening(image: Image, intensity: Double = 1) = {
		val disabled = image.timesAlpha(0.55)
		val focused = image.mapEachPixel { _.darkenedBy(intensity) }
		val pressed = image.mapEachPixel { _.darkenedBy(intensity) }
		
		ButtonImageSet(image, focused, pressed, disabled)
	}
	
	/**
	  * Creates a new button image set that changes alpha value based on button state
	  * @param image The source image
	  * @param defaultAlpha Alpha used in button defeult state
	  * @param maxAlpha Alpha used in button pressed state
	  * @return A button image set that uses varying alpha value
	  */
	def varyingAlpha(image: Image, defaultAlpha: Double, maxAlpha: Double) = {
		val default = image.timesAlpha(defaultAlpha)
		val disabled = image.timesAlpha(defaultAlpha * 0.55)
		val pressed = image.timesAlpha(maxAlpha)
		val focus = image.timesAlpha((maxAlpha * 2 + defaultAlpha) / 3)
		
		ButtonImageSet(default, focus, pressed, disabled)
	}
	
	/**
	 * Creates a new button image set that displays a fixed image normally, but lowers alpha when disabled
	 * @param image The default image
	 * @param alphaOnDisabled Alpha modifier on disabled image (default = 55% = 0.55)
	 * @return A button image set
	 */
	def lowAlphaOnDisabled(image: Image, alphaOnDisabled: Double = 0.55) =
		ButtonImageSet(image, image, image, image.timesAlpha(alphaOnDisabled))
	
	/**
	  * Creates a button image set where the image changes size based on the button state
	  * @param image Image for the default state
	  * @param alphaOnDisabled Alpha modifier for the disabled state (default = 0.55 = 55%)
	  * @param adjustment Implicit adjustment that determines the size of the size changes
	  * @return A set that uses the specified image in different sizes
	  */
	def changingSize(image: Image, alphaOnDisabled: Double = 0.55)(implicit adjustment: Adjustment) = {
		// Calculates the maximum image size, which determines the canvas size for all images
		// Default impact is -1. Focus impact is 0. Activated impact is 0.5 (total).
		val maxScaling = adjustment(0.5)
		val newSize = (image.size * maxScaling).round
		
		val default = (image * adjustment(-1)).withCanvasSize(newSize)
		val focus = image.withCanvasSize(newSize)
		val activated = image.withSize(newSize)
		apply(default, focus, activated, image.timesAlpha(alphaOnDisabled))
	}
}

/**
  * Used in buttons to display images differently based on button state
  * @author Mikko Hilpinen
  * @since 1.8.2019, Reflection v1+
  * @param defaultImage The image that is displayed by default
  * @param focusImage The image that is displayed when button is in focus (or mouse is over button)
  * @param actionImage The image that is displayed when button is being pressed
  * @param disabledImage The image that is displayed while button is disabled
  */
case class ButtonImageSet(defaultImage: Image, focusImage: Image, actionImage: Image, disabledImage: Image)
{
	/**
	  * @param state A button state
	  * @return An image that represents that state
	  */
	def apply(state: GuiElementStatus) = {
		if (state is Activated)
			actionImage
		else if (state.is(Focused) || state.is(Hover))
			focusImage
		else if (state.states.exists { _.effect == Negative })
			disabledImage
		else
			defaultImage
	}
}
