package utopia.firmament.drawing.immutable

import utopia.paradigm.color.Color
import utopia.firmament.drawing.template
import utopia.genesis.graphics.DrawLevel2
import utopia.genesis.graphics.DrawLevel2.Background

object RoundedBackgroundDrawer
{
	/**
	  * @param color Drawing color
	  * @param roundingFactor Factor used when calculating the rounding [0, 1] where 0 means no rounding and 1 means
	  *                       most circular shape available
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer
	  */
	def withFactor(color: Color, roundingFactor: Double, drawLevel: DrawLevel2 = Background) =
		apply(color, Right(roundingFactor), drawLevel)
	
	/**
	  * @param color Drawing color
	  * @param radius Rounding radius to use
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer that uses a static radius
	  */
	def withRadius(color: Color, radius: Double, drawLevel: DrawLevel2 = Background) =
		apply(color, Left(radius), drawLevel)
}

/**
  * Used for drawing a background using a rounded rectangle
  * @author Mikko Hilpinen
  * @since 12.9.2020, Reflection v1.3
  */
case class RoundedBackgroundDrawer private(override val color: Color,
                                           override protected val rounding: Either[Double, Double],
										   override val drawLevel: DrawLevel2 = Background)
	extends template.RoundedBackgroundDrawerLike
