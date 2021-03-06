package utopia.reflection.component.drawing.template

import utopia.flow.datastructure.mutable.Settable
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.transform.AffineTransformation
import utopia.genesis.shape.shape2D.{Bounds, Point, Vector2D}
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
	  * @return A pointer that can store the latest draw settings (text top left position + scaling)
	  */
	protected def lastDrawStatusPointer: Settable[(Point, Vector2D)]
	
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
	
	/**
	  * @return The position where the text was drawn the last time (relative to draw origin)
	  */
	def lastDrawPosition = lastDrawStatusPointer.value._1
	
	/**
	  * @return 2D Scaling applied the last time the text was drawn
	  */
	def lastDrawScaling = lastDrawStatusPointer.value._2
	
	
	// IMPLEMENTED	-----------------------------
	
	override def opaque = false
	
	override def draw(drawer: Drawer, bounds: Bounds) =
	{
		val (normalDrawTargets, highlightedTargets) = drawTargets
		if (normalDrawTargets.exists { _._1.nonEmpty } || highlightedTargets.nonEmpty)
		{
			// Calculates draw bounds and possible scaling
			val textArea = alignment.position(text.size, bounds, insets)
			// Skips drawing if text area is outside of the clipping zone
			if (drawer.clipBounds.forall { _.overlapsWith(textArea) })
			{
				val scaling = (textArea.size / text.size).toVector
				// Updates recorded text draw settings
				lastDrawStatusPointer.value = textArea.position -> scaling
				
				// Applies transformation during the whole drawing process
				drawer.transformed(AffineTransformation(textArea.position.toVector, scaling = scaling)).disposeAfter { drawer =>
					val drawnHighlightTargets = drawer.clipBounds match
					{
						case Some(clipArea) => highlightedTargets.filter { _._2.overlapsWith(clipArea) }
						case None => highlightedTargets
					}
					
					// Draws highlight backgrounds, if applicable
					if (drawnHighlightTargets.nonEmpty)
						highlightedTextBackground.foreach { bg =>
							drawer.onlyFill(bg).disposeAfter { drawer =>
								drawnHighlightTargets.foreach { case (_, area) => drawer.draw(area) }
							}
						}
					// Draws the normal text, if applicable
					if (normalDrawTargets.nonEmpty)
						drawer.forTextDrawing(font.toAwt, normalTextColor).disposeAfter { drawer =>
							normalDrawTargets.foreach { case (string, position) => drawer.drawTextRaw(string, position) }
						}
					// Draws the highlighted text, if applicable
					if (drawnHighlightTargets.nonEmpty)
						drawer.forTextDrawing(font.toAwt, highlightedTextColor).disposeAfter { drawer =>
							drawnHighlightTargets.foreach { case (string, bounds) =>
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
		// If no text is displayed, may still draw the caret
		else
			caret.foreach { caretBounds =>
				val caretArea = alignment.position(caretBounds.size, bounds, insets)
				val scaling = (caretArea.size / caretBounds.size).toVector
				val drawnCaretBounds = caretBounds.translated(caretArea.position) * scaling
				if (drawer.clipBounds.forall { _.overlapsWith(drawnCaretBounds) })
					drawer.onlyFill(caretColor).draw(drawnCaretBounds)
			}
	}
}
