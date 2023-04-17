package utopia.firmament.drawing.template

import utopia.flow.view.mutable.Pointer
import utopia.genesis.graphics.{DrawSettings, Drawer, MeasuredText}
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.{Bounds, Point, Vector2D}
import utopia.paradigm.transform.AffineTransformation
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets

/**
  * Common trait for custom drawers that draw interactive text over a component
  * @author Mikko Hilpinen
  * @since 14.3.2020, Reflection v2
  */
trait SelectableTextDrawerLike extends CustomDrawer
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return A pointer that can store the latest draw settings (text top left position + scaling)
	  */
	protected def lastDrawStatusPointer: Pointer[(Point, Vector2D)]
	
	/**
	  * @return The text being drawn
	  */
	def text: MeasuredText
	
	/**
	  * @return The strings in text that are drawn normally (with their positions) and strings which should be drawn
	  *         with highlighting (with their positions and highlight bounds)
	  */
	def drawTargets: (Vector[(String, Point)], Vector[(String, Point, Bounds)])
	
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
	@deprecated("Between lines margin is already accounted to within the text", "v2.0")
	def betweenLinesMargin = text.betweenLinesAdjustment
	
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
		if (normalDrawTargets.exists { _._1.nonEmpty } || highlightedTargets.nonEmpty) {
			// Calculates draw bounds and possible scaling
			val textArea = alignment.positionWithInsets(text.size, bounds, insets)
			// Skips drawing if text area is outside of the clipping zone
			if (drawer.clippingBounds.forall { _.overlapsWith(textArea) }) {
				val scaling = (textArea.size / text.size).toVector
				// Updates recorded text draw settings
				lastDrawStatusPointer.value = textArea.position -> scaling
				
				// Applies transformation during the whole drawing process
				drawer.transformedWith(AffineTransformation(textArea.position.toVector, scaling = scaling)).use { drawer =>
					val drawnHighlightTargets = drawer.clippingBounds match {
						case Some(clipArea) => highlightedTargets.filter { _._3.overlapsWith(clipArea) }
						case None => highlightedTargets
					}
					
					// Draws highlight backgrounds, if applicable
					if (drawnHighlightTargets.nonEmpty)
						highlightedTextBackground.foreach { bg =>
							implicit val ds: DrawSettings = DrawSettings.onlyFill(bg)
							drawnHighlightTargets.foreach { case (_, _, area) => drawer.draw(area) }
						}
					// Draws the normal text, if applicable
					if (normalDrawTargets.nonEmpty)
						drawer.forTextDrawing(font.toAwt, normalTextColor).use { drawer =>
							normalDrawTargets.foreach { case (string, position) => drawer.draw(string, position) }
						}
					// Draws the highlighted text, if applicable
					if (drawnHighlightTargets.nonEmpty)
						drawer.forTextDrawing(font.toAwt, highlightedTextColor).use { drawer =>
							drawnHighlightTargets.foreach { case (string, position, _) =>
								drawer.draw(string, position)
							}
						}
					// Draws the caret, if applicable
					caret.foreach { caretBounds =>
						implicit val ds: DrawSettings = DrawSettings.onlyFill(caretColor)
						drawer.draw(caretBounds)
					}
				}
			}
		}
		// If no text is displayed, may still draw the caret
		else
			caret.foreach { caretBounds =>
				val caretArea = alignment.positionWithInsets(caretBounds.size, bounds, insets)
				val scaling = (caretArea.size / caretBounds.size).toVector
				val drawnCaretBounds = caretBounds.translated(caretArea.position) * scaling
				if (drawer.clippingBounds.forall { _.overlapsWith(drawnCaretBounds) }) {
					implicit val ds: DrawSettings = DrawSettings.onlyFill(caretColor)
					drawer.draw(drawnCaretBounds)
				}
			}
	}
}
