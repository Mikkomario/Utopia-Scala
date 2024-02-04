package utopia.genesis.util

import utopia.flow.operator.combine.LinearScalable
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.time.TimeExtensions._

import scala.language.implicitConversions

object Fps
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * The default actions / frames per second value (60)
	  */
	val default = Fps(60)
	
	
	// IMPLICIT -----------------------------
	
	implicit def numberToFps(n: Int): Fps = apply(n)
}

/**
  * Represents a frames-per-second value
  * @author Mikko Hilpinen
  * @since 15.4.2019, v2+
  * @param fps The frames / actions per second value of this FPS
  */
case class Fps(fps: Int) extends SelfComparable[Fps] with LinearScalable[Fps]
{
	// ATTRIBUTES	-----------------
	
	/**
	  * The interval between iterations
	  */
	val interval = 1.seconds / fps
	
	
	// IMPLEMENTED  -----------------
	
	override def self = this
	
	override def compareTo(o: Fps) = fps - o.fps
	
	override def *(multiplier: Double) = Fps((fps * multiplier).round.toInt)
}
