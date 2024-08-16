package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter, RejectAll}
import utopia.flow.time.TimeExtensions._
import utopia.genesis.handling.event.keyboard.SpecificKeyEvent.SpecificKeyFilteringFactory

import scala.concurrent.duration.{Duration, FiniteDuration}

object KeyDownEvent
{
	// TYPES    -----------------------
	
	/**
	  * A filter that applies to key down -events
	  */
	type KeyDownEventFilter = Filter[KeyDownEvent]
	
	
	// COMPUTED --------------------
	
	/**
	 * @return A factory for filters that may be applied to key-down events
	 */
	def filter = KeyDownEventFilter
	
	
	// NESTED   -----------------------
	
	trait KeyDownFilteringFactory[+A] extends Any with SpecificKeyFilteringFactory[KeyDownEvent, A]
	{
		/**
		  * @param durationThreshold A time threshold after which key-down events should be ignored.
		  * @return An item that only accepts events where the key has been held down for a duration shorter
		  *         than the specified time threshold.
		  *         Key-releases restart the tracked duration.
		  */
		def until(durationThreshold: Duration) = durationThreshold.finite match {
			case Some(d) => withFilter { _.totalDuration < d }
			case None => withFilter(AcceptAll)
		}
		/**
		  * @param durationThreshold A time threshold before which key-down events should be ignored.
		  * @return An item that only accepts events where the key has been held down for a duration longer
		  *         than the specified time threshold.
		  *         Key-releases restart the tracked duration.
		  */
		def after(durationThreshold: Duration) = durationThreshold.finite match {
			case Some(d) => withFilter { _.totalDuration > d }
			case None => withFilter(RejectAll)
		}
	}
	
	object KeyDownEventFilter extends KeyDownFilteringFactory[KeyDownEventFilter]
	{
		// IMPLEMENTED  ---------------------
		
		override protected def withFilter(filter: Filter[KeyDownEvent]): KeyDownEventFilter = filter
		
		
		// OTHER    -------------------------
		
		/**
		  * @param f A filter function applicable for key-down events
		  * @return A filter that uses the specified function
		  */
		def apply(f: KeyDownEvent => Boolean) = Filter(f)
	}
	
	
	// EXTENSIONS   -------------------------
	
	implicit class RichKeyDownEventFilter(val f: KeyDownEventFilter)
		extends AnyVal with KeyDownFilteringFactory[KeyDownEventFilter]
	{
		override protected def withFilter(filter: Filter[KeyDownEvent]): KeyDownEventFilter = f && filter
	}
}

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