package utopia.genesis.image.transform

import utopia.paradigm.color.Color
import utopia.paradigm.angular.Angle

/**
  * This color transform adjusts hue for a specific range of colors
  * @author Mikko Hilpinen
  * @since 16.6.2019, v2.1+
  * @param sourceHue The targeted source hue angle
  * @param effectRange The hue range (angle) of the effect (how wide range of colors are affected)
  * @param targetHue The hue new hue for the specified source hue
  */
case class HueAdjust(sourceHue: Angle, effectRange: Angle, targetHue: Angle) extends PixelTransform
{
	private val maxRotation = targetHue - sourceHue
	private val rangeRadians = effectRange.radians
	
	override def apply(original: Color) =
	{
		// Checks whether pixel is within effect range
		val hueDifference = (original.hue - sourceHue).absolute.radians
		
		if (hueDifference < rangeRadians / 2)
		{
			// Range modifier [0, 1] where 1 is exactly target hue and 0 is at the edge
			// val rangeMod = 1 - (hueDifference.toRadians / (effectRange.toRadians / 2))
			// Uses a sin function for the effectiveness (based on range modifier)
			// (sin(2 * x * PI - PI) + 1) / 2
			// val effectMod = (Math.sin(2 * rangeMod * Math.PI - Math.PI) + 1) / 2
			
			// Applies effect (rotation)
			original + maxRotation// (maxRotation * effectMod)
		}
		else
			original
	}
}
