package utopia.firmament.drawing.template

import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Bounds

/**
  * Used for drawing a background using a rounded rectangle
  * @author Mikko Hilpinen
  * @since 12.9.2020, Reflection v1.3
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
	
	override def opaque = false
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		if (bounds.size.sign.isPositive) {
			implicit val ds: DrawSettings = DrawSettings.onlyFill(color)
			rounding match {
				case Left(radius) =>
					// Won't scale the radius over maximum (circular shape)
					val maxRadius = bounds.size.minDimension / 2.0
					drawer.draw(bounds.toRoundedRectangleWithRadius(radius min maxRadius))
				case Right(factor) => drawer.draw(bounds.toRoundedRectangle(factor))
			}
		}
	}
}
