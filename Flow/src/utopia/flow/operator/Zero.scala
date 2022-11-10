package utopia.flow.operator

import utopia.flow.time.Days

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

/**
  * An object that represents a generic zero value
  * @author Mikko Hilpinen
  * @since 7.11.2022, v2.0
  */
object Zero
{
	// ATTRIBUTES ------------------------
	
	val toDouble = 0.0
	val toInt = 0
	val toLong = 0L
	
	
	// COMPUTED --------------------------
	
	def toDuration = Duration.Zero
	def toDays = Days.zero
	
	
	// IMPLICIT --------------------------
	
	implicit def _toDouble(z: Zero.type): Double = toDouble
	implicit def _toInt(z: Zero.type): Int = toInt
	implicit def _toLong(z: Zero.type): Long = toLong
	implicit def _toDuration(z: Zero.type): FiniteDuration = toDuration
	implicit def _toDays(z: Zero.type): Days = toDays
}
