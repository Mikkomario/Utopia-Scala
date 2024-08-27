package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.keyboard.KeyDownEvent.{KeyDownEventFilter, KeyDownFilteringFactory}
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused
import scala.language.implicitConversions

object KeyDownListener
{
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
	
	case class KeyDownListenerFactory(condition: Flag = AlwaysTrue, filter: KeyDownEventFilter = AcceptAll)
		extends ListenerFactory[KeyDownEvent, KeyDownListenerFactory]
			with KeyDownFilteringFactory[KeyDownListenerFactory]
	{
		// IMPLEMENTED  ------------------------
		
		override def usingFilter(filter: Filter[KeyDownEvent]): KeyDownListenerFactory = copy(filter = filter)
		override def usingCondition(condition: Flag): KeyDownListenerFactory = copy(condition = condition)
		
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
	
	private class _KeyDownListener[U](override val handleCondition: Flag,
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
