package utopia.reflection.component.drawing.template

import utopia.paradigm.shape.shape2d.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.LengthExtensions._

/**
  * This custom drawer draws text over a component. This is a common trait for both mutable and immutable implementations.
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
trait TextDrawerLike extends CustomDrawer
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return The context used when drawing the text
	  */
	def drawContext: TextDrawContext
	
	/**
	  * @return The text being drawn
	  */
	def drawnText: Either[LocalizedString, Seq[LocalizedString]]
	
	
	// COMPUTED	---------------------------------
	
	/**
	  * @return Font used by this drawer
	  */
	def font = drawContext.font
	
	/**
	  * @return Color used by this drawer
	  */
	def color = drawContext.color
	
	/**
	  * @return Alignment used by this drawer to position the text
	  */
	def alignment = drawContext.alignment
	
	/**
	  * @return Insets used by this drawer when positioning the text
	  */
	def insets = drawContext.insets
	
	/**
	  * @return The vertical margin to use when drawing multiple text lines
	  */
	def betweenLinesMargin = drawContext.betweenLinesMargin
	
	
	// IMPLEMENTED	-----------------------------
	
	override def opaque = false
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		drawnText match
		{
			case Left(line) =>
				// Only draws non-empty text
				val textToDraw = line.string
				if (textToDraw.nonEmpty)
				{
					// Specifies the drawer
					drawer.withEdgeColor(color).clippedTo(bounds).disposeAfter { d =>
						// Draws the text with correct positioning
						d.drawSingleLineTextPositioned(textToDraw, font.toAwt) { textSize =>
							alignment.positionWithInsets(textSize, bounds, insets)
						}
					}
				}
			case Right(lines) =>
				if (lines.nonEmpty)
				{
					drawer.withEdgeColor(color).clippedTo(bounds).disposeAfter { d =>
						// Draws the text with correct positioning
						d.drawTextLinesPositioned(lines.map { _.string }, font.toAwt, betweenLinesMargin) { textSize =>
							alignment.positionWithInsets(textSize, bounds, insets) }(alignment.x.position)
					}
				}
		}
	}
}
