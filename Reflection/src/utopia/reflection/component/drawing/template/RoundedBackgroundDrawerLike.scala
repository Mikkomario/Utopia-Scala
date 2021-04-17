package utopia.reflection.component.drawing.template

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer

/**
  * Used for drawing a background using a rounded rectangle
  * @author Mikko Hilpinen
  * @since 12.9.2020, v1.3
  */
trait RoundedBackgroundDrawerLike extends CustomDrawer
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Background color
	  */
	def color: Color
	
	/**
	  * @return Rounding amount (Left means radius and right means a factor)
	  */
	protected def rounding: Either[Double, Double]
	
	
	// IMPLEMENTED  -----------------------
	
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
