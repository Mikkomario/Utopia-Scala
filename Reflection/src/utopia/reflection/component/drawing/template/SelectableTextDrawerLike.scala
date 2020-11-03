package utopia.reflection.component.drawing.template

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Bounds, Point, Transformation}
import utopia.genesis.util.Drawer
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.{Font, MeasuredText}

/**
  * Common trait for custom drawers that draw interactive text over a component
  * @author Mikko Hilpinen
  * @since 14.3.2020, v2
  */
trait SelectableTextDrawerLike extends CustomDrawer
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return The text being drawn
	  */
	def text: MeasuredText
	
	/**
	  * @return The strings in text that are drawn normally (with their positions) and strings which should be drawn
	  *         with highlighting (with their bounds)
	  */
	def drawTargets: (Vector[(String, Point)], Vector[(String, Bounds)])
	
	/**
	  * @return The drawn caret bounds
	  */
	def caret: Option[Bounds]
	
	/**
	  * @return Font used when drawing text
	  */
	def font: Font
	
	/**
	  * @return Insets to place around the text
	  */
	def insets: StackInsets
	
	/**
	  * @return Color to use when drawing the caret
	  */
	def caretColor: Color
	
	/**
	  * @return Text color to use with the normal text
	  */
	def normalTextColor: Color
	
	/**
	  * @return Color to use on the highlighted text
	  */
	def highlightedTextColor: Color
	
	/**
	  * @return Background color to use for the highlighted text. None if no background should be drawn
	  */
	def highlightedTextBackground: Option[Color]
	
	
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
		val (normalDrawTargets, highlightedTargets) = drawTargets
		if (normalDrawTargets.nonEmpty || highlightedTargets.nonEmpty)
		{
			// Calculates draw bounds and possible scaling
			val textArea = alignment.position(text.size, bounds, insets)
			val scaling = (textArea.size / bounds.size).toVector
			// Applies transformation during the whole drawing process
			drawer.transformed(Transformation.position(textArea.position).scaled(scaling)).disposeAfter { drawer =>
				// Draws highlight backgrounds, if applicable
				if (highlightedTargets.nonEmpty)
					highlightedTextBackground.foreach { bg =>
						drawer.onlyFill(bg).disposeAfter { drawer =>
							highlightedTargets.foreach { case (_, area) => drawer.draw(area) }
						}
					}
				// Draws the normal text, if applicable
				if (normalDrawTargets.nonEmpty)
					drawer.forTextDrawing(font.toAwt, normalTextColor).disposeAfter { drawer =>
						normalDrawTargets.foreach { case (string, position) => drawer.drawTextRaw(string, position) }
					}
				// Draws the highlighted text, if applicable
				if (highlightedTargets.nonEmpty)
					drawer.forTextDrawing(font.toAwt, highlightedTextColor).disposeAfter { drawer =>
						highlightedTargets.foreach { case (string, bounds) =>
							drawer.drawTextRaw(string, bounds.position)
						}
					}
				// Draws the caret, if applicable
				caret.foreach { caretBounds =>
					drawer.onlyFill(caretColor).draw(caretBounds)
				}
			}
		}
	}
}
