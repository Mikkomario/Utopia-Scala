package utopia.genesis.handling.event.keyboard

import utopia.flow.time.TimeExtensions._

import scala.concurrent.duration.FiniteDuration

/**
 * An event that is consistently generated while a keyboard key is being held down
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
case class KeyDownEvent(index: Int, location: KeyLocation, duration: FiniteDuration, totalDuration: FiniteDuration,
                        keyboardState: KeyboardState)
	extends SpecificKeyEvent
{
	// IMPLEMENTED  --------------------------
	
	override def toString = s"$index held down for ${ duration.description }"
}