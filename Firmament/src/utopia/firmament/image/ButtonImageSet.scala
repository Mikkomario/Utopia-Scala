package utopia.firmament.image

import utopia.firmament.image.ButtonImageEffect.{ChangeSize, Highlight, LowerAlphaOnDisabled}
import utopia.firmament.model.enumeration.GuiElementState.{Activated, Focused, Hover}
import utopia.firmament.model.GuiElementStatus
import utopia.flow.operator.sign.Sign.Negative
import utopia.genesis.image.Image
import utopia.paradigm.transform.Adjustment

object ButtonImageSet
{
	/**
	  * @param image The image to display for every state
	  * @return An image set that doesn't change
	  */
	def apply(image: Image): ButtonImageSet = apply(image, image, image, image)
	
	/**
	  * Creates a new button image set that always presents the same image
	  * @param image The image presented in the new set
	  * @return A set that only contains the specified image
	  */
	@deprecated("Please use apply instead", "v1.1.1")
	def fixed(image: Image) = ButtonImageSet(image, image, image, image)
	
	/**
	  * Creates a new button mage set that presents a modified version of the specified image
	  * @param image The original image
	  * @param intensity Brightening intensity modifier (default = 1)
	  * @return A set that will a) Use 55% alpha while disabled and b) brighten the image on focus and pressed -states
	  */
	@deprecated("Please use apply(Image).highlighting instead", "v1.1.1")
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
	@deprecated("Please use apply(Image) + Highlight(preferredShade = Dark) instead", "v1.1.1")
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
	@deprecated("Please use apply(Image).lowAlphaOnDisabled instead", "v1.1.1")
	def lowAlphaOnDisabled(image: Image, alphaOnDisabled: Double = 0.55) =
		ButtonImageSet(image, image, image, image.timesAlpha(alphaOnDisabled))
	
	/**
	  * Creates a button image set where the image changes size based on the button state
	  * @param image Image for the default state
	  * @param alphaOnDisabled Alpha modifier for the disabled state (default = 0.55 = 55%)
	  * @param adjustment Implicit adjustment that determines the size of the size changes
	  * @return A set that uses the specified image in different sizes
	  */
	@deprecated("Please use apply(Image).sizeChanging instead", "v1.1.1")
	def changingSize(image: Image, alphaOnDisabled: Double = 0.55)(implicit adjustment: Adjustment) =
		apply(image).sizeChanging
}

/**
  * Used in buttons to display images differently based on button state
  * @author Mikko Hilpinen
  * @since 1.8.2019, Reflection v1+
  * @param default The image that is displayed by default
  * @param focus The image that is displayed when button is in focus (or mouse is over button)
  * @param action The image that is displayed when button is being pressed
  * @param disabled The image that is displayed while button is disabled
  */
case class ButtonImageSet(default: Image, focus: Image, action: Image, disabled: Image)
{
	// COMPUTED ---------------------
	
	@deprecated("Renamed to .default", "v1.1.1")
	def defaultImage = default
	@deprecated("Renamed to .focus", "v1.1.1")
	def focusImage = focus
	@deprecated("Renamed to .action", "v1.1.1")
	def actionImage = action
	@deprecated("Renamed to .disabled", "v1.1.1")
	def disabledImage = disabled
	
	/**
	  * @return Copy of this image-set where the disabled state has a lower alpha value (i.e. higher transparency)
	  */
	def lowerAlphaOnDisabled = this + LowerAlphaOnDisabled()
	/**
	  * @param adj Implicit adjustment size to use when changing size
	  * @return Copy of this set where the image changes size according to the applicable state
	  */
	def sizeChanging(implicit adj: Adjustment) = this + ChangeSize()
	/**
	  * @return Copy of this image-set where the image grows brighter or darker based on the applicable state
	  */
	def highlighting = this + Highlight()
	
	
	// OTHER    ---------------------
	
	/**
	  * @param state A button state
	  * @return An image that represents that state
	  */
	def apply(state: GuiElementStatus) = {
		if (state is Activated)
			action
		else if (state.is(Focused) || state.is(Hover))
			focus
		else if (state.states.exists { _.effect == Negative })
			disabled
		else
			default
	}
	
	/**
	  * @param f A mapping function applied to the default state
	  * @return Copy of this set with a mapped default value
	  */
	def mapDefault(f: Image => Image) = copy(default = f(default))
	/**
	  * @param f A mapping function applied to the focused state
	  * @return Copy of this set with a mapped focused value
	  */
	def mapFocus(f: Image => Image) = copy(focus = f(focus))
	/**
	  * @param f A mapping function applied to the action state
	  * @return Copy of this set with a mapped action value
	  */
	def mapAction(f: Image => Image) = copy(action = f(action))
	/**
	  * @param f A mapping function applied to the disabled state
	  * @return Copy of this set with a mapped disabled value
	  */
	def mapDisabled(f: Image => Image) = copy(disabled = f(disabled))
	
	/**
	  * Applies an effect over this image set
	  * @param effect An effect to apply
	  * @return Copy of this image set with the effect applied
	  */
	def +(effect: ButtonImageEffect) = effect(this)
	/**
	  * Applies 0-n effects over this image set
	  * @param effects Effects to apply
	  * @return Copy of this image set with the specified effects applied
	  */
	def ++(effects: IterableOnce[ButtonImageEffect]) = effects.iterator.foldLeft(this) { _ + _ }
}
