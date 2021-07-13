package utopia.reflection.text

import java.awt.FontMetrics

/**
  * Common trait for sources from which a font metrics instance can be acquired from
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.0
  */
// TODO: Work in progress
trait FontMetricsFactory
{
	/**
	  * @param font A font instance
	  * @return Metrics for that font
	  */
	def apply(font: java.awt.Font): FontMetrics
	
	def apply(font: Font): FontMetrics = apply(font.toAwt)
}
