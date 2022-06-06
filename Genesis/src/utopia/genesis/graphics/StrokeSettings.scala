package utopia.genesis.graphics

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.color.Color

import java.awt.{BasicStroke, Graphics2D, Paint}

object StrokeSettings
{
	/**
	  * Default stroke settings (1 px stroke width, black color)
	  */
	val default = apply()
	
	/**
	  * @param color Color to use when drawing edges (default = black)
	  * @param strokeWidth Width of the drawn stroke in pixels (default = 1 px)
	  * @param isRounded Whether stroke edges should be rounded (default = false)
	  * @return A new set of settings
	  */
	def apply(color: Color = Color.black, strokeWidth: Double = 1.0, isRounded: Boolean = false): StrokeSettings =
		apply(Right(color), strokeWidth, isRounded)
	
	/**
	  * @param color Color to use when drawing edges
	  * @param strokeWidth Width of the drawn stroke in pixels (default = 1 px)
	  * @return A new set of settings that use rounded strokes
	  */
	def rounded(color: Color = Color.black, strokeWidth: Double = 1.0) =
		apply(color, strokeWidth, isRounded = true)
	
	/**
	  * @param paint Paint to use when drawing edges
	  * @param strokeWidth Width of the drawn strokes in pixels (default = 1 px)
	  * @param isRounded Whether stroke edges should be rounded (default = false)
	  * @return A new set of settings
	  */
	def withPaint(paint: Paint, strokeWidth: Double = 1.0, isRounded: Boolean = false) =
		apply(Left(paint), strokeWidth, isRounded)
}

/**
  * Used for specifying edge drawing settings when drawing shapes
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
case class StrokeSettings(_color: Either[java.awt.Paint, Color], strokeWidth: Double, isRounded: Boolean)
{
	// ATTRIBUTES   ---------------------------
	
	/**
	  * The stroke instance defined by these settings. (Please don't modify)
	  */
	lazy val stroke = {
		if (isRounded)
			new BasicStroke(strokeWidth.toFloat, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
		else
			new BasicStroke(strokeWidth.toFloat)
	}
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return Whether edges should be drawn when using these settings. False if draw color is fully transparent or
	  *         if edges are drawn with 0 or negative width.
	  */
	def shouldDraw = strokeWidth > 0 && (_color match {
		case Right(c) => c.alpha > 0
		case Left(_) => true
	})
	
	/**
	  * @return Drawing color used. Transparent black if the used paint is not a color.
	  */
	def color = _color.rightOrMap {
		case c: java.awt.Color => Color.fromAwt(c)
		case _ => Color.transparentBlack
	}
	/**
	  * @return The paint used when drawing
	  */
	def paint = _color.leftOrMap { _.toAwt }
	
	/**
	  * @return A copy of these settings where strokes are rounded
	  */
	def rounded = if (isRounded) this else copy(isRounded = true)
	/**
	  * @return A copy of these settings where strokes are sharp
	  */
	def sharp = if (isRounded) copy(isRounded = false) else this
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param color Color to use when drawing edges
	  * @return A copy of these settings with that color
	  */
	def withColor(color: Color) = if (_color.contains(color)) this else copy(_color = Right(color))
	/**
	  * @param paint Paint to use when drawing edges
	  * @return A copy of these settings with that paint
	  */
	def withPaint(paint: Paint) = copy(_color = Left(paint))
	/**
	  * @param width Width of the strokes to draw (in pixels)
	  * @return A copy of these settings with the specified stroke width
	  */
	def withStrokeWidth(width: Double) = if (strokeWidth == width) this else copy(strokeWidth = width)
	
	/**
	  * Modifies a graphics instance so that it adheres to these settings
	  * @param graphics A graphics instance to modify
	  */
	def modify(graphics: Graphics2D) = {
		graphics.setPaint(paint)
		graphics.setStroke(stroke)
	}
}