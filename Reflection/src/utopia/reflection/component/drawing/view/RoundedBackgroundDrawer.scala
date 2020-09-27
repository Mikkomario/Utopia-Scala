package utopia.reflection.component.drawing.view

import utopia.flow.event.Changing
import utopia.flow.util.CollectionExtensions._
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.template
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.Background

object RoundedBackgroundDrawer
{
	/**
	  * @param colorPointer Drawing color source
	  * @param roundingFactorPointer Pointer to Factor used when calculating the rounding [0, 1] where 0 means no
	  *                              rounding and 1 means most circular shape available
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer
	  */
	def withFactor(colorPointer: Changing[Color], roundingFactorPointer: Changing[Double],
	               drawLevel: DrawLevel = Background) =
		new RoundedBackgroundDrawer(colorPointer, Right(roundingFactorPointer), drawLevel)
	
	/**
	  * @param colorPointer Drawing color source
	  * @param radiusPointer Pointer to rounding radius to use
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer that uses a static radius
	  */
	def withRadius(colorPointer: Changing[Color], radiusPointer: Changing[Double], drawLevel: DrawLevel = Background) =
		new RoundedBackgroundDrawer(colorPointer, Left(radiusPointer), drawLevel)
}

/**
  * Used for drawing a background using a rounded rectangle
  * @author Mikko Hilpinen
  * @since 12.9.2020, v1.3
  */
class RoundedBackgroundDrawer private(colorPointer: Changing[Color],
                                      roundingPointer: Either[Changing[Double], Changing[Double]],
                                      override val drawLevel: DrawLevel = Background)
	extends template.RoundedBackgroundDrawer
{
	override def color = colorPointer.value
	
	override protected def rounding = roundingPointer.mapBoth { _.value } { _.value }
}
