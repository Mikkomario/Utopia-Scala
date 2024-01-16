package utopia.reach.component.factory

import utopia.firmament.image.ButtonImageEffect
import utopia.firmament.image.ButtonImageEffect.{ChangeSize, Highlight, LowerAlphaOnDisabled}
import utopia.paradigm.transform.Adjustment

/**
  * Common trait for factory classes that apply button image effects
  * @author Mikko Hilpinen
  * @since 15/01/2024, v1.2
  */
trait AppliesButtonImageEffectsFactory[+Repr]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return This factory instance
	  */
	def self: Repr
	
	/**
	  * Effects applied to generated image sets
	  */
	def imageEffects: Vector[ButtonImageEffect]
	/**
	  * Effects applied to generated image sets
	  * @param effects New image effects to use.
	  * Effects applied to generated image sets
	  * @return Copy of this factory with the specified image effects
	  */
	def withImageEffects(effects: Vector[ButtonImageEffect]): Repr
	
	
	// COMPUTED --------------------
	
	/**
	  * @return Copy of this factory that doesn't apply any image effects
	  */
	def withoutImageEffects = withImageEffects(Vector.empty)
	
	/**
	  * @return Copy of this factory that uses a lower alpha channel value (i.e. higher transparency)
	  *         for the disabled button state.
	  *         If this effect was already applied, returns this instance without applying any modifications.
	  */
	def withLowerAlphaOnDisabled = {
		val effects = imageEffects
		if (effects.exists { _.isInstanceOf[LowerAlphaOnDisabled] })
			self
		else
			withImageEffects(effects :+ LowerAlphaOnDisabled())
	}
	/**
	  * @return Copy of this factory that makes the images brighter or darker based on the button state.
	  *         If this effect was already applied, no change is made and this instance is returned instead.
	  */
	def highlighting = {
		val effects = imageEffects
		if (effects.exists { _.isInstanceOf[Highlight] })
			self
		else
			withImageEffects(effects :+ Highlight())
	}
	/**
	  * @param adj Implicit adjustments that determine the impact of the size changes
	  * @return Copy of this factory that applies image size changes.
	  *         If this factory already applied size changes, returns this instance instead.
	  */
	def sizeChanging(implicit adj: Adjustment) = {
		val effects = imageEffects
		if (effects.exists { _.isInstanceOf[ChangeSize] })
			self
		else
			withImageEffects(effects :+ ChangeSize())
	}
	
	
	// OTHER    --------------------
	
	/**
	  * Modifies the applied image effects using a custom mapping function
	  * @param f A mapping function that modifies applied image effects
	  * @return Copy of this factory with modified effects
	  */
	def mapImageEffects(f: Vector[ButtonImageEffect] => Vector[ButtonImageEffect]) =
		withImageEffects(f(imageEffects))
	
	/**
	  * Applies an image effect to generated components. Previously listed effects are also applied.
	  * @param effect An effect to add to the created components.
	  * @return Copy of this factory that applies the specified effect.
	  */
	def withImageEffect(effect: ButtonImageEffect) = mapImageEffects { _ :+ effect }
	/**
	  * Applies only a single image effect to generated components.
	  * Previously introduced effects are overwritten.
	  * @param effect An effect to add to the created components.
	  * @return Copy of this factory that only applies the specified effect.
	  */
	def withSingleImageEffect(effect: ButtonImageEffect) = withImageEffects(Vector(effect))
	
	/**
	  * Applies 0-n image additional effects to generated components. Previously listed effects are also applied.
	  * @param effects Effects to add to the created components.
	  * @return Copy of this factory that also applies the specified effects.
	  */
	def withAdditionalImageEffects(effects: IterableOnce[ButtonImageEffect]) = mapImageEffects { _ ++ effects }
}
