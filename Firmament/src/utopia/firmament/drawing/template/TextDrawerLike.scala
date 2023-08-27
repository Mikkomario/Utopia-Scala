package utopia.firmament.drawing.template

import utopia.genesis.graphics.{Drawer, MeasuredText}
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.transform.AffineTransformation
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * This custom drawer draws text over a component. This is a common trait for both mutable and immutable implementations.
  * @author Mikko Hilpinen
  * @since 14.3.2020, Reflection v1
  */
trait TextDrawerLike extends CustomDrawer
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return Text drawn by this drawer
	  */
	def text: MeasuredText
	/**
	  * @return Font used when drawing text
	  */
	def font: Font
	/**
	  * @return Insets placed around the text
	  */
	def insets: StackInsets
	/**
	  * @return Color used when drawing text
	  */
	def color: Color
	/**
	  * @return Alignment used by this drawer to position the text
	  */
	def alignment: Alignment
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return The vertical margin to use when drawing multiple text lines
	  */
	@deprecated("Between lines margin is already accounted to within the text", "v2.0")
	def betweenLinesMargin = text.betweenLinesAdjustment
	
	
	// IMPLEMENTED	-----------------------------
	
	override def opaque = false
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		// Calculates draw bounds and possible scaling
		val textArea = alignment.positionWithInsets(text.size, bounds, insets)
		// Skips drawing if the text is outside the clipping area
		if (drawer.clippingBounds.forall { _.overlapsWith(textArea) }) {
			val scaling = (textArea.size / text.size).toVector
			// Applies transformation during the whole drawing process
			drawer.transformedWith(AffineTransformation(textArea.position.toVector, scaling = scaling))
				.forTextDrawing(font.toAwt, color)
				.use { drawer =>
					text.defaultDrawTargets.foreach { case (string, position) => drawer.draw(string, position) }
				}
		}
	}
}
