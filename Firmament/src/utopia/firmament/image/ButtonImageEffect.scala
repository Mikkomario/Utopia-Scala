package utopia.firmament.image

import utopia.firmament.image.ButtonImageEffect.CombinedEffect
import utopia.flow.collection.immutable.caching.cache.{Cache, WeakCache}
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.paradigm.color.ColorShade
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.transform.Adjustment

/**
  * Modifies images based on a button state in order to form an interactive / animated button effect.
  * @author Mikko Hilpinen
  * @since 15/01/2024, v1.1.1
  */
trait ButtonImageEffect extends SelfCombinable[ButtonImageEffect]
{
	// ABSTRACT --------------------
	
	/**
	  * @param original The original images to modify
	  * @return A modified copy of the specified images
	  */
	def apply(original: ButtonImageSet): ButtonImageSet
	
	
	// IMPLEMENTED  ----------------
	
	override def +(other: ButtonImageEffect): ButtonImageEffect = new CombinedEffect(this, other)
}

object ButtonImageEffect
{
	// NESTED   --------------------------
	
	/**
	  * Changes image transparency for disabled buttons
	  * @param alphaMod Alpha channel modifier to apply in the disabled state
	  */
	case class LowerAlphaOnDisabled(alphaMod: Double = 0.55) extends ButtonImageEffect
	{
		def apply(original: ButtonImageSet): ButtonImageSet =
			original.copy(disabled = original.disabled.timesAlpha(alphaMod))
	}
	
	object Highlight
	{
		// ATTRIBUTES   -----------------
		
		// Weakly caches the generated image sets in order to improve performance
		// First key is preferred shade
		// Second key is button image set to apply
		// Third key is applied intensity
		private val caches = ColorShade.values.map { shade =>
			shade -> WeakCache.weakKeys { original: ButtonImageSet =>
				// Determines the direction of color change
				val averageLuminance = original.default.pixels.averageRelativeLuminance
				val direction = shade match {
					case Light => if (averageLuminance > 0.7) Dark else Light
					case Dark => if (averageLuminance < 0.3) Light else Dark
				}
				Cache { intensity: Double =>
					// Applies color change to focused and action -states
					original.copy(
						focus = original.focus.mapEachPixel { _.highlightedBy(intensity, direction) },
						action = original.action.mapEachPixel { _.highlightedBy(intensity * 2.0, direction) }
					)
				}
			}
		}.toMap
	}
	/**
	  * Changes image color to brighter or darker, as the button gets activated or focused
	  * @param intensity Intensity modifier, where 1.0 is the default intensity and 0.0 is no change.
	  * @param preferredShade Preferred color change direction. Default = Light = Make image brighter.
	  */
	case class Highlight(intensity: Double = 1.0, preferredShade: ColorShade = ColorShade.Light)
		extends ButtonImageEffect
	{
		private val cache = Highlight.caches(preferredShade)
		
		def apply(original: ButtonImageSet): ButtonImageSet = cache(original)(intensity)
	}
	
	object ChangeSize
	{
		// Caches generated sets in order to improve performance
		// First key is applied adjustment size
		// Second key is the set to modify
		private val caches = WeakCache.weakKeys { adj: Adjustment =>
			WeakCache { original: ButtonImageSet =>
				// Calculates the maximum image size, which determines the canvas size for all images
				// Default impact is -1. Focus impact is 0. Activated impact is 0.5 (total).
				val maxScaling = adj(0.5)
				val newSize = (original.action.size * maxScaling).round
				
				val default = (original.default * adj(-1)).withCanvasSize(newSize)
				val focus = original.focus.withCanvasSize(newSize)
				val activated = original.action.withSize(newSize)
				
				original.copy(default, focus, activated)
			}
		}
	}
	/**
	  * Changes the image size based on the button state
	  * @param adj Adjustment size used when altering image sizes (implicit)
	  */
	case class ChangeSize()(implicit adj: Adjustment) extends ButtonImageEffect
	{
		def apply(original: ButtonImageSet): ButtonImageSet = ChangeSize.caches(adj)(original)
	}
	
	private class CombinedEffect(first: ButtonImageEffect, second: ButtonImageEffect) extends ButtonImageEffect
	{
		def apply(original: ButtonImageSet): ButtonImageSet = second(first(original))
	}
}