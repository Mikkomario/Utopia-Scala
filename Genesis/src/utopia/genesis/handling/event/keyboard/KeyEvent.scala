package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{Filter, RejectAll}
import utopia.genesis.handling.event.keyboard.Key._
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, Direction2D, HorizontalDirection, VerticalDirection}

object KeyEvent
{
	// TYPES    ------------------------
	
	/**
	  * A filter that applies to any keyboard event
	  */
	type KeyEventFilter = Filter[KeyEvent]
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Access to filters that may be applied to all key events
	  */
	def filter = KeyEventFilter
	
	
	// NESTED   ------------------------
	
	/**
	  * Common trait for factory-like classes that support key-event -based filtering
	  * @tparam E Type of filtered event
	  * @tparam A Type of generated items
	  */
	trait KeyFilteringFactory[+E <: KeyEvent, +A] extends Any
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
		  * @return An item that accepts digit key -related events
		  */
		def anyDigit = digitRange(0, 9)
		
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
		def apply(key1: Key, key2: Key, moreKeys: Key*): A = apply(Set(key1, key2) ++ moreKeys)
		
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
		  * @param digit A digit
		  * @return An item that only accepts events concerning that digit key
		  */
		def digit(digit: Byte) = apply(DigitKey(digit))
		/**
		  * @param digits n Digits
		  * @return An item that only accepts events concerning those digit keys
		  */
		def digits(digits: IterableOnce[Byte]) = apply(digits.iterator.map(DigitKey.apply))
		/**
		  * @param min Smallest accepted digit
		  * @param max Largest accepted digit
		  * @return An item that only accepts events concerning digits within the specified range
		  */
		def digitRange(min: Byte, max: Byte): A = digits((min to max).iterator.map { _.toByte })
		
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
		// IMPLEMENTED  ----------------
		
		override protected def withFilter(filter: KeyEventFilter): KeyEventFilter = filter
		
		
		// OTHER    --------------------
		
		/**
		 * @param f A key-event filter function
		 * @return A filter that uses the specified function
		 */
		def apply(f: KeyEvent => Boolean) = Filter(f)
	}
	
	
	// EXTENSIONS   --------------------
	
	implicit class RichKeyEventFilter[E <: KeyEvent](val f: Filter[E])
		extends AnyVal with KeyFilteringFactory[E, Filter[E]]
	{
		override protected def withFilter(filter: Filter[E]): Filter[E] = f && filter
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
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return Direction of the horizontal arrow key (left, right) that was changed.
	 *         None if the change didn't affect a horizontal arrow key.
	 */
	def horizontalArrow: Option[HorizontalDirection] = index match {
		case RightArrow.index => Some(Direction2D.Right)
		case LeftArrow.index => Some(Direction2D.Left)
		case _ => None
	}
	/**
	 * @return Direction of the vertical arrow key (up, down) that was changed.
	 *         None if the change didn't affect a vertical arrow key.
	 */
	def verticalArrow: Option[VerticalDirection] = index match {
		case UpArrow.index => Some(Direction2D.Up)
		case DownArrow.index => Some(Direction2D.Down)
		case _ => None
	}
	/**
	 * @return The direction of the arrow key that was changed.
	 *         None if the change didn't affect an arrow key.
	 */
	def arrow = ArrowKey(index).map { _.direction }
	
	/**
	  * @return The digit which this event concerns.
	  *         Note: Doesn't recognize numpad keys.
	  */
	def digit = DigitKey.values.find { _.index == index }.map { _.digit }
	
	@deprecated("Please use .keyboardState instead", "v4.0")
	def keyStatus = keyboardState
	
	
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
	 * @param axis Target axis
	 * @return Direction of the arrow key that lies in the specified axis and was changed. None if the change
	 *         didn't affect an arrow key on the specified axis.
	 */
	def arrowAlong(axis: Axis2D) = axis match {
		case X => horizontalArrow
		case Y => verticalArrow
	}
	
	/**
	  * Checks whether the event concerns a specific character key
	  */
	@deprecated("Replaced with concernsChar(Char)", "v4.0")
	def isCharacter(char: Char) = concernsChar(char)
}
