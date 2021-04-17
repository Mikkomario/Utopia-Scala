package utopia.reflection.text

import java.awt.FontMetrics

/**
  * A text measurement context that is based on a swing / awt component
  * @author Mikko Hilpinen
  * @since 1.11.2020, v2
  */
case class FontMetricsContext(fontMetrics: FontMetrics, marginBetweenLines: Double) extends TextMeasurementContext
{
	override def lineHeight = fontMetrics.getHeight
	
	override def lineWidthOf(string: String) = fontMetrics.stringWidth(string)
}
