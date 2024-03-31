package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.event.keyboard.Key.ArrowKey
import utopia.genesis.handling.event.keyboard.KeyEvent.KeyEventFilter
import utopia.genesis.handling.event.keyboard.SpecificKeyEvent.SpecificKeyFilteringFactory
import utopia.paradigm.enumeration.Direction2D

object KeyStateEvent
{
    // TYPES    -------------------------
    
    /**
      * Type for filters applied to key state -events
      */
    type KeyStateEventFilter = Filter[KeyStateEvent]
    
    
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
    
    
    // NESTED   -----------------------------
    
    /**
      * Common trait for factory-like classes that support key-state-event -based filtering
      * @tparam A Type of generated items
      */
    trait KeyStateFilteringFactory[+A] extends SpecificKeyFilteringFactory[KeyStateEvent, A]
    {
        // COMPUTED   ---------------------
        
        /**
          * An item that only accepts key-pressed events
          */
        def pressed = withFilter { _.pressed }
        /**
          * An item that only accepts key-released events
          */
        def released = withFilter { _.released }
    }
    
    object KeyStateEventFilter extends KeyStateFilteringFactory[KeyStateEventFilter]
    {
        // ATTRIBUTES   ---------------------
        
        /**
          * A filter that only accepts key-pressed events
          */
        override lazy val pressed = super.pressed
        /**
          * A filter that only accepts key-released events
          */
        override lazy val released = super.released
        
        
        // IMPLEMENTED  ---------------------
        
        override protected def withFilter(filter: Filter[KeyStateEvent]): KeyStateEventFilter = filter
        
        
        // OTHER    -------------------------
        
        /**
          * @param f A filter function
          * @return A key-state event-filter that applies the specified function
          */
        def apply(f: KeyStateEvent => Boolean): KeyStateEventFilter = Filter[KeyStateEvent](f)
        
        /**
          * @param char A character (key)
          * @return A filter that only accepts events of that key's pressed-events,
          *         and only while a control key is being held down
          */
        def controlChar(char: Char) = whileControlDown && pressed && this.char(char)
    }
}

/**
 * This event is used for informing instances when a keyboard key is either pressed or released
 * @author Mikko Hilpinen
 * @since 21.2.2017
  * @param index The index of the changed key
  * @param location The specific location where the key was changed
  * @param keyboardState The state of the keyboard immediately after this event
 */
case class KeyStateEvent(index: Int, location: KeyLocation, keyboardState: KeyboardState, pressed: Boolean)
    extends SpecificKeyEvent
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
    
    
    // IMPLEMENTED  -----------------
    
    override def toString = s"$index ${ if (pressed) "was pressed" else "was released" } at location: $location"
}