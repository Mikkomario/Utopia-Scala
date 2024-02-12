package utopia.genesis.handling.event.keyboard

import utopia.genesis.handling.event.keyboard.Key.{ArrowKey, DownArrow, LeftArrow, RightArrow, UpArrow}
import utopia.genesis.handling.event.keyboard.KeyEvent.KeyEventFilter
import utopia.genesis.handling.event.keyboard.KeyStateListener2.KeyStateEventFilter
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, Direction2D, HorizontalDirection, VerticalDirection}

object KeyStateEvent2
{
    // ATTRIBUTES   -------------------------
    
    /**
      * This event filter only accepts events caused by key presses
      */
    @deprecated("Please use KeyStateEventFilter.pressed instead", "v4.0")
    def wasPressedFilter: KeyStateEventFilter = KeyStateEventFilter.pressed
    /**
      * This event filter only accepts events caused by key releases
      */
    @deprecated("Please use KeyStateEventFilter.released instead", "v4.0")
    def wasReleasedFilter = KeyStateEventFilter.released
    /**
      * This filter only accepts arrow key events
      */
    @deprecated("Please use KeyEventFilter.anyArrow instead", "v4.0")
    def arrowKeysFilter = KeyEventFilter.anyArrow
    /**
      * This event filter only accepts key events while control key is being held down (includes presses of ctrl key itself)
      */
    @deprecated("Please use KeyEventFilter.whileControlDown instead", "v4.0")
    def controlDownFilter = KeyEventFilter.whileControlDown
    
    
    // COMPUTED ------------------------
    
    /**
      * @return An access point for filters related to these events
      */
    def filter = KeyStateEventFilter
    
    
    // OTHER    ------------------------
    
    /**
      * This event filter only accepts events for the specified key index
      */
    @deprecated("Please use KeyEventFilter.key(Key) instead", "v4.0")
    def keyFilter(index: Int) = KeyEventFilter.apply(Key(index))
    /**
      * This event filter only accepts events for the specified key index + location
      */
    @deprecated("Please use KeyStateEventFilter.specificKey(Key, KeyLocation) instead", "v4.0")
    def keyFilter(index: Int, location: KeyLocation) = KeyStateEventFilter.specificKey(Key(index), location)
    /**
      * @param acceptedKeys Keys that are accepted by the filter
      * @return Event filter that only accepts events concerning specified keys
      */
    @deprecated("Please use KeyEventFilter.keys(IterableOnce) instead", "v4.0")
    def keysFilter(acceptedKeys: Set[Int]) = KeyEventFilter.apply(acceptedKeys.map(Key.apply))
    /**
      * This event filter only accepts events for the specified key indices
      */
    @deprecated("Please use KeyEventFilter.keys(...) instead", "v4.0")
    def keysFilter(firstIndex: Int, secondIndex: Int, moreIndices: Int*): KeyEventFilter =
        keysFilter(Set(firstIndex, secondIndex) ++ moreIndices)
    /**
      * @param notAcceptedKeys Keys that are not accepted by the filter
      * @return A filter that accepts events for all keys except those specified
      */
    @deprecated("Please use !KeyEventFilter.keys(IterableOnce) instead", "v4.0")
    def notKeysFilter(notAcceptedKeys: Set[Int]) = !keysFilter(notAcceptedKeys)
    
    /**
      * @param direction An arrow key direction
      * @return A filter that only accepts events concerning that arrow key
      */
    @deprecated("Please use KeyEventFilter.arrow(Direction2D) instead", "v4.0")
    def arrowKeyFilter(direction: Direction2D) = KeyEventFilter.arrow(direction)
    
    /**
      * This event filter only accepts events for the specified character key
      */
    @deprecated("Please use KeyEventFilter.char(Char) instead", "v4.0")
    def charFilter(char: Char) = KeyEventFilter.char(char)
    /**
      * @param acceptedChars Characters that are accepted by the filter
      * @return Event filter that only accepts events concerning specified characters
      */
    @deprecated("Please use KeyEventFilter.chars(Iterable) instead", "v4.0")
    def charsFilter(acceptedChars: Iterable[Char]) = KeyEventFilter.chars(acceptedChars)
    
    /**
      * @param char Target combo character
      * @return A filter that only accepts events where control is being held down while specified character key is pressed
      */
    @deprecated("Please use KeyEventFilter.char(Char) && KeyEventFilter.whileControlDown instead", "v4.0")
    def controlCharComboFilter(char: Char) = KeyEventFilter.char(char) && KeyEventFilter.whileControlDown
    
    /**
      * @param direction Arrow key direction
      * @return A key code matching that arrow key
      */
    @deprecated("Please use ArrowKey(Direction2D).index instead", "v4.0")
    def arrowKeyIndex(direction: Direction2D) = ArrowKey(direction).index
}

/**
 * This event is used for informing instances when a keyboard key is either pressed or released
 * @author Mikko Hilpinen
 * @since 21.2.2017
  * @param index The index of the changed key
  * @param location The specific location where the key was changed
  * @param keyboardState The state of the keyboard immediately after this event
 */
case class KeyStateEvent2(index: Int, location: KeyLocation, keyboardState: KeyboardState, pressed: Boolean)
    extends KeyEvent
{
    // COMPUTED ---------------------
    
    @deprecated("Renamed to .pressed", "v4.0")
    def isDown = pressed
    /**
      * @return Whether the key was just released
      */
    def released = !pressed
    @deprecated("Renamed to .released", "v4.0")
    def isReleased = !isDown
    
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
    
    
    // IMPLEMENTED  -----------------
    
    override def toString = s"$index ${ if (pressed) "was pressed" else "was released" } at location: $location"
    
    
    // OTHER    ---------------------
    
    /**
      * @param key Targeted key
      * @param location Targeted specific key location
      * @return Whether this event concerns that key at that location
      */
    def concernsKey(key: Key, location: KeyLocation): Boolean = concernsKey(key) && this.location == location
    
    /**
      * @param axis Target axis
      * @return Direction of the arrow key that lies in the specified axis and was changed. None if the change
      *         didn't affect an arrow key on the specified axis.
      */
    def arrowAlong(axis: Axis2D) = axis match {
        case X => horizontalArrow
        case Y => verticalArrow
    }
}