package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.{Bounds, Point}

/**
  * Settings that specify line height when drawing text
  * @author Mikko Hilpinen
  * @since 29.1.2022, v2.6.3
  */
trait TextDrawHeight
{
	/**
	  * @return Whether measurement results include spacing between lines of text
	  */
	def includesLeading: Boolean
	
	/**
	  * @param text Text to draw (single line)
	  * @param context Measurements context in which the text is drawn
	  * @return Bounds of that text where (0,0) is placed at the drawing origin (baseline start)
	  */
	def drawBounds(text: String, context: FontMetricsWrapper): Bounds
}

object TextDrawHeight
{
	trait SimpleTextDrawHeight extends TextDrawHeight {
		protected def lineHeightFrom(context: FontMetricsWrapper): (Double, Double)
		
		override def drawBounds(text: String, context: FontMetricsWrapper) = {
			val width = context.widthOf(text)
			val (above, below) = lineHeightFrom(context)
			Bounds.between(Point(0, -above), Point(width, below))
		}
	}
	
	/**
	  * Height that includes leading (half above and half beneath).
	  * Some characters may extend beyond this height, although that should be rare.
	  */
	case object LineHeight extends SimpleTextDrawHeight
	{
		override def includesLeading = true
		
		override protected def lineHeightFrom(context: FontMetricsWrapper) = {
			val l = context.leading / 2.0
			(context.ascent + l, context.descent + l)
		}
	}
	
	/**
	  * Height that only includes ascent and descent. Some characters may extend beyond this height.
	  */
	case object Standard extends SimpleTextDrawHeight
	{
		override def includesLeading = false
		
		override protected def lineHeightFrom(context: FontMetricsWrapper) =
			context.ascent.toDouble -> context.descent.toDouble
	}
	
	/**
	  * Height that includes maximum possible ascent and descent. Will fit any character.
	  */
	case object Maximum extends SimpleTextDrawHeight
	{
		override def includesLeading = false
		
		override protected def lineHeightFrom(context: FontMetricsWrapper) =
			context.maxAscent.toDouble -> context.maxDescent.toDouble
	}
	
	/**
	  * Height that specifically accounts for the specified text boundaries.
	  * Returns different heights for different pieces of text.
	  */
	case object Actual extends TextDrawHeight
	{
		override def includesLeading = false
		
		override def drawBounds(text: String, context: FontMetricsWrapper) = context.boundsOf(text)
	}
}
