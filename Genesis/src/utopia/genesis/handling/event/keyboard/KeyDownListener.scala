package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter, RejectAll}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.keyboard.KeyDownListener.KeyDownEventFilter
import utopia.genesis.handling.event.keyboard.SpecificKeyEvent.SpecificKeyFilteringFactory
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object KeyDownListener
{
	// TYPES    -----------------------
	
	/**
	 * A filter that applies to key down -events
	 */
	type KeyDownEventFilter = Filter[KeyDownEvent]
	
	
	// ATTRIBUTES   -------------------
	
	/**
	 * An unconditional key-down listener factory
	 */
	val unconditional = KeyDownListenerFactory()
	
	
	// IMPLICIT -----------------------
	
	implicit def objectToFactory(@unused o: KeyDownListener.type): KeyDownListenerFactory = unconditional
	
	/**
	 * @param f A function to call whenever a key-down event occurs
	 * @return A listener that calls the specified function
	 */
	implicit def apply(f: KeyDownEvent => Unit): KeyDownListener = unconditional(f)
	
	
	// NESTED   -----------------------
	
	trait KeyDownFilteringFactory[+A] extends SpecificKeyFilteringFactory[KeyDownEvent, A]
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
	
	case class KeyDownListenerFactory(condition: FlagLike = AlwaysTrue, filter: KeyDownEventFilter = AcceptAll)
		extends ListenerFactory[KeyDownEvent, KeyDownListenerFactory]
			with KeyDownFilteringFactory[KeyDownListenerFactory]
	{
		// IMPLEMENTED  ------------------------
		
		override def usingFilter(filter: Filter[KeyDownEvent]): KeyDownListenerFactory = copy(filter = filter)
		override def usingCondition(condition: FlagLike): KeyDownListenerFactory = copy(condition = condition)
		
		override protected def withFilter(filter: Filter[KeyDownEvent]): KeyDownListenerFactory =
			copy(filter = this.filter && filter)
			
		
		// OTHER    ---------------------------
		
		/**
		 * Creates a new key-down -listener
		 * @param f A function to call whenever a key-down event occurs
		 *          (provided the event is accepted by the applied filter and listening is enabled)
		 * @tparam U Arbitrary function result type
		 * @return A new listener
		 */
		def apply[U](f: KeyDownEvent => U): KeyDownListener = new _KeyDownListener[U](condition, filter, f)
	}
	
	private class _KeyDownListener[U](override val handleCondition: FlagLike,
	                                  override val keyDownEventFilter: KeyDownEventFilter, f: KeyDownEvent => U)
		extends KeyDownListener
	{
		override def whileKeyDown(event: KeyDownEvent): Unit = f(event)
	}
}

/**
 * Common trait for listener classes that are interested in receiving continuous events about situations where
 * a key is being held down
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
trait KeyDownListener extends Handleable
{
	/**
	 * @return Filter applied to incoming [[KeyDownEvent]]s.
	 *         Only events accepted by this filter should be passed to [[whileKeyDown]](...)
	 */
	def keyDownEventFilter: KeyDownEventFilter
	
	/**
	 * This method is called continuously while a keyboard key is being held down,
	 * provided that the associated event is accepted by this listener's [[keyDownEventFilter]].
	 * @param event The latest event that occurred
	 */
	def whileKeyDown(event: KeyDownEvent): Unit
}
