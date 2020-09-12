package utopia.reflection.component.drawing.immutable

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.{CustomDrawer, DrawLevel}
import utopia.reflection.component.drawing.template.DrawLevel.Background

object RoundedBackgroundDrawer
{
	/**
	  * @param color Drawing color
	  * @param roundingFactor Factor used when calculating the rounding [0, 1] where 0 means no rounding and 1 means
	  *                       most circular shape available
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer
	  */
	def withFactor(color: Color, roundingFactor: Double, drawLevel: DrawLevel = Background) =
		apply(color, Right(roundingFactor), drawLevel)
	
	/**
	  * @param color Drawing color
	  * @param radius Rounding radius to use
	  * @param drawLevel Depth to use when drawing this background (default = Background = bottom)
	  * @return A new background drawer that uses a static radius
	  */
	def withRadius(color: Color, radius: Double, drawLevel: DrawLevel = Background) =
		apply(color, Left(radius), drawLevel)
}

/**
  * Used for drawing a background using a rounded rectangle
  * @author Mikko Hilpinen
  * @since 12.9.2020, v1.3
  */
case class RoundedBackgroundDrawer private(color: Color, rounding: Either[Double, Double],
										   override val drawLevel: DrawLevel = Background) extends CustomDrawer
{
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		if (bounds.size.isPositive)
		{
			drawer.onlyFill(color).disposeAfter { d =>
				rounding match
				{
					case Left(radius) =>
						// Won't scale the radius over maximum (circular shape)
						val maxRadius = bounds.minDimension / 2.0
						d.draw(bounds.toRoundedRectangleWithRadius(radius min maxRadius))
					case Right(factor) => d.draw(bounds.toRoundedRectangle(factor))
				}
			}
		}
	}
}
