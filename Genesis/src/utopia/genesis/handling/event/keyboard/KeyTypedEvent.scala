package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{Filter, RejectAll}
import utopia.genesis.handling.event.keyboard.KeyEvent.KeyFilteringFactory

object KeyTypedEvent
{
	// TYPES    --------------------------
	
	/**
	  * Filter class for key typed -events
	  */
	type KeyTypedEventFilter = Filter[KeyTypedEvent]
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return Factory for constructing key typed event filters
	 */
	def filter = KeyTypedEventFilter
	
	
	// NESTED   --------------------------
	
	trait KeyTypedFilteringFactory[+A] extends Any with KeyFilteringFactory[KeyTypedEvent, A]
	{
		/**
		  * @param char Targeted character
		  * @return An item that only accepts events concerning that typed character
		  */
		def apply(char: Char): A = withFilter { _.typedChar == char }
		/**
		  * @param chars Targeted characters
		  * @return An item that only accepts events concerning those typed characters
		  */
		def chars(chars: Set[Char]): A = {
			if (chars.isEmpty)
				withFilter(RejectAll)
			else
				withFilter { e => chars.contains(e.typedChar) }
		}
	}
	
	object KeyTypedEventFilter extends KeyTypedFilteringFactory[KeyTypedEventFilter]
	{
		// IMPLEMENTED  -------------------
		
		override protected def withFilter(filter: Filter[KeyTypedEvent]): KeyTypedEventFilter = filter
		
		
		// OTHER    ----------------------
		
		/**
		  * @param f A filtering-function for key typed -events
		  * @return A filter that uses the specified function
		  */
		def apply(f: KeyTypedEvent => Boolean) = Filter(f)
	}
	
	
	// EXTENSIONS   ----------------------
	
	implicit class RichKeyTypedEventFilter(val f: KeyTypedEventFilter)
		extends AnyVal with KeyTypedFilteringFactory[KeyTypedEventFilter]
	{
		override protected def withFilter(filter: Filter[KeyTypedEvent]): KeyTypedEventFilter = f && filter
	}
}

/**
 * These events are fired whenever the user types something on the keyboard.
  * Unlike KeyState events, these events focus less on the keyboard state changes and more on the characters typed
  * by the user.
 * @author Mikko Hilpinen
 * @since 23.2.2017
  * @param typedChar The character typed by the user
  * @param index The key index associated with this event
  * @param keyboardState The state of the keyboard immediately after this event
 */
case class KeyTypedEvent(typedChar: Char, index: Int, keyboardState: KeyboardState) extends KeyEvent
{
	/**
	  * @return The digit typed, if the typed character was a digit. None otherwise.
	  */
	def digit = if (typedChar.isDigit) Some(typedChar.asDigit) else None
}
