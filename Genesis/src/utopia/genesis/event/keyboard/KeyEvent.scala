package utopia.genesis.event.keyboard

import utopia.flow.operator.filter.{Filter, RejectAll}
import utopia.genesis.event.{KeyLocation, KeyStatus}
import utopia.genesis.event.keyboard.Key.{ArrowKey, Control, Shift}
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
	  * @tparam A Type of generated items
	  */
	trait KeyFilteringFactory[+A]
	{
		// ABSTRACT -------------------------
		
		/**
		  * @param filter A filter to apply
		  * @return An item with that filter applied
		  */
		protected def withFilter(filter: KeyEventFilter): A
		
		
		// COMPUTED   ---------------------
		
		/**
		  * An item that only accepts arrow key -related events
		  */
		def anyArrow = keys(ArrowKey.values.toSet)
		
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
		def key(key: Key) = withFilter { _.index == key.index }
		/**
		  * @param keys Targeted keys
		  * @return An item that only accepts events that concern one of the specified keys
		  */
		def keys(keys: IterableOnce[Key]) = indices(keys.iterator.map { _.index })
		/**
		  * @return An item that only accepts events that concern one of the specified keys
		  */
		def keys(keyIndex1: Key, keyIndex2: Key, moreIndices: Key*): A =
			keys(Set(keyIndex1, keyIndex2) ++ moreIndices)
		
		/**
		  * @param character A character (key)
		  * @return An item that only accepts events concerning that character-key
		  */
		def char(character: Char) = withFilter { _.isCharacter(character) }
		/**
		  * @param characters Character keys
		  * @return An item that only accepts events concerning those character-keys
		  */
		def chars(characters: Iterable[Char]) = {
			if (characters.isEmpty)
				withFilter(RejectAll)
			else
				withFilter { e => characters.exists(e.isCharacter) }
		}
		/**
		  * @return An item that only accepts events concerning the specified character-keys
		  */
		def chars(char1: Char, char2: Char, moreChars: Char*): A = chars(Set(char1, char2) ++ moreChars)
		
		/**
		  * @param direction Arrow key direction
		  * @return An item that only accepts events concerning the targeted arrow key
		  */
		def arrow(direction: Direction2D) = key(ArrowKey(direction))
		
		// TODO: Refactor to use the new Key class (requires refactored KeyState class)
		/**
		  * @param keyIndex Index of the targeted key (see [[java.awt.event.KeyEvent]])
		  * @return An item that only accepts events while the specified key is held down
		  *         (i.e. accepts key-combos involving that key)
		  */
		def whileKeyDown(keyIndex: Int) = withFilter { _.keyStatus(keyIndex) }
		/**
		  * @param keyIndex Index of the targeted key (see [[java.awt.event.KeyEvent]])
		  * @param location Location where the key must be pressed
		  * @return An item that only accepts events while the specified key is held down
		  *         (i.e. accepts key-combos involving that key)
		  */
		def whileKeyDown(keyIndex: Int, location: KeyLocation) = withFilter { _.keyStatus(keyIndex, location) }
		
		private def indices(indices: IterableOnce[Int]) = {
			val actualIndices = Set.from(indices) - java.awt.event.KeyEvent.VK_UNDEFINED
			if (actualIndices.nonEmpty)
				withFilter { e => actualIndices.contains(e.index) }
			else
				withFilter(RejectAll)
		}
	}
}

/**
  * Common trait for keyboard key -related events
  * @author Mikko Hilpinen
  * @since 03/02/2024, v3.6
  */
trait KeyEvent
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Index of the key associated with this event
	  */
	def index: Int
	/**
	  * @return The keyboard state at (i.e. immediately after) this event
	  */
	def keyStatus: KeyStatus
	
	
	// OTHER    -----------------------
	
	/**
	  * @param char A character on a keyboard
	  * @return Whether this event concerns the specified character key
	  */
	def isCharacter(char: Char): Boolean = index == java.awt.event.KeyEvent.getExtendedKeyCodeForChar(char)
}
