package utopia.firmament.drawing.view

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.View
import utopia.paradigm.color.Color
import utopia.firmament.drawing.template
import utopia.genesis.graphics.DrawLevel2
import utopia.genesis.graphics.DrawLevel2.Background

object RoundedBackgroundViewDrawer
{
	/**
	  * @param colorPointer Drawing color source
	  * @param roundingFactorPointer Pointer to Factor used when calculating the rounding [0, 1] where 0 means no
	  *                              rounding and 1 means most circular shape available
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer
	  */
	def withFactor(colorPointer: View[Color], roundingFactorPointer: View[Double],
	               drawLevel: DrawLevel2 = Background) =
		new RoundedBackgroundViewDrawer(colorPointer, Right(roundingFactorPointer), drawLevel)
	
	/**
	  * @param colorPointer Drawing color source
	  * @param radiusPointer Pointer to rounding radius to use
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer that uses a static radius
	  */
	def withRadius(colorPointer: View[Color], radiusPointer: View[Double], drawLevel: DrawLevel2 = Background) =
		new RoundedBackgroundViewDrawer(colorPointer, Left(radiusPointer), drawLevel)
}

/**
  * Used for drawing a background using a rounded rectangle
  * @author Mikko Hilpinen
  * @since 12.9.2020, Reflection v1.3
  */
class RoundedBackgroundViewDrawer private(colorPointer: View[Color],
                                          roundingPointer: Either[View[Double], View[Double]],
                                          override val drawLevel: DrawLevel2 = Background)
	extends template.RoundedBackgroundDrawerLike
{
	override def color = colorPointer.value
	
	override protected def rounding = roundingPointer.mapBoth { _.value } { _.value }
}
