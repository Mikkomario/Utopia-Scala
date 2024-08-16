package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.event.keyboard.KeyEvent.KeyFilteringFactory

object SpecificKeyEvent
{
	// COMPUTED ------------------------
	
	/**
	 * @return Access to constructing filters that apply to all key-specific events
	 */
	def filter = SpecificKeyEventFilter
	
	
	// NESTED   ------------------------
	
	trait SpecificKeyFilteringFactory[+E <: SpecificKeyEvent, +A] extends Any with KeyFilteringFactory[E, A]
	{
		// OTHER    -----------------------
		
		/**
		 * @param location Targeted location
		 * @return Filter that only accepts key events at that location
		 */
		def location(location: KeyLocation) = withFilter { _.location == location }
		/**
		 * @param key Targeted key
		 * @param location Targeted specific key location
		 * @return Filter that only accepts key events of that key at that specific location
		 */
		def specificKey(key: Key, location: KeyLocation) = withFilter { _.concernsKey(key, location) }
	}
	
	object SpecificKeyEventFilter extends SpecificKeyFilteringFactory[SpecificKeyEvent, Filter[SpecificKeyEvent]]
	{
		override protected def withFilter(filter: Filter[SpecificKeyEvent]): Filter[SpecificKeyEvent] = filter
	}
	
	
	// EXTENSIONS   -----------------------
	
	implicit class RIchSpecificKeyEventFilter[E <: SpecificKeyEvent](val f: Filter[E])
		extends AnyVal with SpecificKeyFilteringFactory[E, Filter[E]]
	{
		override protected def withFilter(filter: Filter[E]): Filter[E] = f && filter
	}
}

/**
 * Common trait for key events that can specify, what exact key was pressed.
 * In other words, these events distinguish between same key-code presses in different locations,
 * such as events concerning the left shift key from those concerning the right shift key (where applicable).
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
trait SpecificKeyEvent extends KeyEvent
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The specific location of the key that triggered this event (represented with [[index]])
	 */
	def location: KeyLocation
	
	
	// OTHER    ---------------------
	
	/**
	 * @param key Targeted key
	 * @param location Targeted specific key location
	 * @return Whether this event concerns that key at that location
	 */
	def concernsKey(key: Key, location: KeyLocation): Boolean = concernsKey(key) && this.location == location
}
