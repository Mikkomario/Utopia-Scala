package utopia.reflection.component.drawing.template

import utopia.genesis.shape.Axis.X
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.localization.LocalizedString

/**
  * This custom drawer draws text over a component. This is a common trait for both mutable and immutable implementations.
  * @author Mikko Hilpinen
  * @since 14.3.2020, v1
  */
trait TextDrawer extends CustomDrawer
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return The context used when drawing the text
	  */
	def drawContext: TextDrawContext
	
	/**
	  * @return The text being drawn
	  */
	def text: LocalizedString
	
	
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
	
	
	// IMPLEMENTED	-----------------------------
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		// TODO: Add support for placing multiline text correctly
		// Only draws non-empty text
		val textToDraw = text.string
		if (textToDraw.nonEmpty)
		{
			// Specifies the drawer
			drawer.withEdgeColor(color).clippedTo(bounds).disposeAfter { d =>
				// Draws the text with correct positioning
				d.drawTextPositioned(textToDraw, font.toAwt) { textSize => alignment.position(textSize, bounds,
					insets, fitWithinBounds = false).topLeft.positiveAlong(X) }
			}
		}
	}
}
