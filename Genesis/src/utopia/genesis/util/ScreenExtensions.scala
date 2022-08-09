package utopia.genesis.util

import utopia.paradigm.measurement.{Distance, DistanceUnit}

/**
  * Extensions that utilize the screen
  * @author Mikko Hilpinen
  * @since 9.8.2022, v3.0
  */
object ScreenExtensions
{
	implicit class ExtendedDistanceUnit(val u: DistanceUnit) extends AnyVal
	{
		/**
		  * @return A modifier from this unit to pixels in the current screen
		  */
		def toScreenPixels = u.toPixels(Screen.ppi)
	}
	
	implicit class ExtendedDistance(val d: Distance) extends AnyVal
	{
		/**
		  * @return This distance in pixels on the current screen
		  */
		def toScreenPixels = d.amount * d.unit.toScreenPixels
	}
}
