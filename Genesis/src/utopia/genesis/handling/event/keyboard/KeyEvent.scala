package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{Filter, RejectAll}
import utopia.genesis.handling.event.keyboard.Key.{ArrowKey, CharKey, Control, Shift}
import utopia.paradigm.enumeration.Direction2D

object KeyEvent
{
	// TYPES    ------------------------
	
	/**
	  * A filter that applies to any keyboard event
	  */
	type KeyEventFilter = Filter[KeyEvent]
	
	
	// NESTED   ------------------------
	
	/**
	  * Common trait for factory-like classes that support key-event -based filtering
	  * @tparam E Type of filtered event
	  * @tparam A Type of generated items
	  */
	trait KeyFilteringFactory[+E <: KeyEvent, +A]
	{
		// ABSTRACT -------------------------
		
		/**
		  * @param filter A filter to apply
		  * @return An item with that filter applied
		  */
		protected def withFilter(filter: Filter[E]): A
		
		
		// COMPUTED   ---------------------
		
		/**
		  * An item that only accepts arrow key -related events
		  */
		def anyArrow = apply(ArrowKey.values.toSet)
		
		/**
		  * An item that only applies while the control key (at any location) is down
		  */
		def whileControlDown = whileKeyDown(Control)
		/**
		  * An item that only applies while the shift key (at any location) is down
		  */
		def whileShiftDown = whileKeyDown(Shift)
		
		
		// OTHER    -------------------------
		
		/**
		  * @param key The targeted key
		  * @return An item that only accepts events concerning the specified key
		  */
		def apply(key: Key) = withFilter { _.concernsKey(key) }
		/**
		  * @param keys Targeted keys
		  * @return An item that only accepts events that concern one of the specified keys
		  */
		def apply(keys: IterableOnce[Key]) = indices(keys.iterator.map { _.index })
		/**
		  * @return An item that only accepts events that concern one of the specified keys
		  */
		def apply(keyIndex1: Key, keyIndex2: Key, moreIndices: Key*): A =
			apply(Set(keyIndex1, keyIndex2) ++ moreIndices)
		
		/**
		  * @param character A character (key)
		  * @return An item that only accepts events concerning that character-key
		  */
		def char(character: Char) = withFilter { _.concernsChar(character) }
		/**
		  * @param characters Character keys
		  * @return An item that only accepts events concerning those character-keys
		  */
		def chars(characters: Iterable[Char]) = {
			if (characters.isEmpty)
				withFilter(RejectAll)
			else
				withFilter { e => characters.exists(e.concernsChar) }
		}
		/**
		  * @return An item that only accepts events concerning the specified character-keys
		  */
		def chars(char1: Char, char2: Char, moreChars: Char*): A = chars(Set(char1, char2) ++ moreChars)
		
		/**
		  * @param direction Arrow key direction
		  * @return An item that only accepts events concerning the targeted arrow key
		  */
		def arrow(direction: Direction2D) = apply(ArrowKey(direction))
		
		/**
		  * @param key Targeted key
		  * @return An item that only accepts events while the specified key is held down
		  *         (i.e. accepts key-combos involving that key)
		  */
		def whileKeyDown(key: Key) = withFilter { _.keyboardState(key) }
		/**
		  * @param key Targeted keyboard key
		  * @param location Location where the key must be pressed
		  * @return An item that only accepts events while the specified key is held down
		  *         (i.e. accepts key-combos involving that key)
		  */
		def whileKeyDown(key: Key, location: KeyLocation) = withFilter { _.keyboardState(key, location) }
		/**
		  * @param key Targeted keyboard key
		  * @return An item that only accepts events while the specified key is in the released state
		  */
		def whileKeyReleased(key: Key) = withFilter { !_.keyboardState(key) }
		/**
		  * @param key Targeted keyboard key
		  * @param location Specific location of the targeted key
		  * @return An item that only accepts events while the specified key is in the released state
		  */
		def whileKeyReleased(key: Key, location: KeyLocation) = withFilter { !_.keyboardState(key, location) }
		
		private def indices(indices: IterableOnce[Int]) = {
			val actualIndices = Set.from(indices) - java.awt.event.KeyEvent.VK_UNDEFINED
			if (actualIndices.nonEmpty)
				withFilter { e => actualIndices.contains(e.index) }
			else
				withFilter(RejectAll)
		}
	}
	
	object KeyEventFilter extends KeyFilteringFactory[KeyEvent, KeyEventFilter]
	{
		override protected def withFilter(filter: KeyEventFilter): KeyEventFilter = filter
	}
}

/**
  * Common trait for keyboard key -related events
  * @author Mikko Hilpinen
  * @since 03/02/2024, v4.0
  */
trait KeyEvent
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Index of the key associated with this event
	  */
	def index: Int
	/**
	  * @return The keyboard state immediately after this event
	  */
	def keyboardState: KeyboardState
	
	
	// OTHER    -----------------------
	
	/**
	  * @param key A keyboard key
	  * @return Whether this event concerns the specified key
	  */
	def concernsKey(key: Key) = index == key.index
	/**
	  * @param char A character on a keyboard
	  * @return Whether this event concerns the specified character key
	  */
	def concernsChar(char: Char): Boolean = concernsKey(CharKey(char))
	
	/**
	  * Checks whether the event concerns a specific character key
	  */
	@deprecated("Replaced with concernsChar(Char)", "v4.0")
	def isCharacter(char: Char) = concernsChar(char)
}
