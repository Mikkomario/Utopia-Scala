package utopia.genesis.util

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

object Fps
{
	/**
	  * The default actions / frames per second value (60)
	  */
	val default = Fps(60)
}

/**
  * Represents frames per second
  * @author Mikko Hilpinen
  * @since 15.4.2019, v2+
  * @param fps The frames / actions per second value of this FPS
  */
case class Fps(fps: Int)
{
	// ATTRIBUTES	-----------------
	
	/**
	  * The interval between iterations
	  */
	val interval = FiniteDuration((1000.0 / fps * 1000000).toLong, TimeUnit.NANOSECONDS)
	
	
	// OPERATORS	-----------------
	
	/**
	  * Multiplies this FPS
	  * @param multiplier A multiplier
	  * @return Multiplied value
	  */
	def *(multiplier: Double) = Fps((fps * multiplier).toInt)
	
	/**
	  * Divides this FPS
	  * @param divider A divider
	  * @return A divided value
	  */
	def /(divider: Double) = Fps((fps / divider).toInt)
}
