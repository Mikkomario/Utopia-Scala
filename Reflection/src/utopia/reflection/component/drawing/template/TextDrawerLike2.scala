package utopia.reflection.component.drawing.template

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.transform.AffineTransformation
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.{Font, MeasuredText}

/**
  * This custom drawer draws text over a component. This is a common trait for both mutable and immutable implementations.
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
trait TextDrawerLike2 extends CustomDrawer
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
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return Alignment used by this drawer to position the text
	  */
	def alignment = text.alignment
	
	/**
	  * @return The vertical margin to use when drawing multiple text lines
	  */
	def betweenLinesMargin = text.context.marginBetweenLines
	
	
	// IMPLEMENTED	-----------------------------
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		// Calculates draw bounds and possible scaling
		val textArea = alignment.position(text.size, bounds, insets)
		// Skips drawing if the text is outside the clipping area
		if (drawer.clipBounds.forall { _.overlapsWith(textArea) })
		{
			val scaling = (textArea.size / text.size).toVector
			// Applies transformation during the whole drawing process
			drawer.transformed(AffineTransformation(textArea.position.toVector, scaling = scaling))
				.forTextDrawing(font.toAwt, color)
				.disposeAfter { drawer =>
					text.defaultDrawTargets.foreach { case (string, position) => drawer.drawTextRaw(string, position) }
				}
		}
	}
}
