package utopia.genesis.event

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D

import java.awt.event.KeyEvent
import utopia.inception.util.Filter
import utopia.paradigm.enumeration.Direction2D
import utopia.paradigm.enumeration.Direction2D.{Down, Up}

object KeyStateEvent
{
    /**
     * This event filter only accepts events caused by key presses
     */
    val wasPressedFilter: Filter[KeyStateEvent] = e => e.isDown
    
    /**
     * This event filter only accepts events caused by key releases
     */
    val wasReleasedFilter: Filter[KeyStateEvent] = e => e.isReleased
    
    /**
     * This filter only accepts arrow key events
     */
    val arrowKeysFilter = keysFilter(Direction2D.values.map(arrowKeyIndex).toSet)
    
    /**
     * This event filter only accepts key events while control key is being held down (includes presses of ctrl key itself)
     */
    val controlDownFilter: Filter[KeyStateEvent] = e => e.keyStatus.apply(KeyEvent.VK_CONTROL)
    
    /**
     * This event filter only accepts events for the specified key index
     */
    def keyFilter(index: Int): Filter[KeyStateEvent] = e => e.index == index
    
    /**
     * This event filter only accepts events for the specified key index + location
     */
    def keyFilter(index: Int, location: KeyLocation): Filter[KeyStateEvent] = e => e.index == index && e.location == location
    
    /**
      * @param direction An arrow key direction
      * @return A filter that only accepts events concerning that arrow key
      */
    def arrowKeyFilter(direction: Direction2D) = keyFilter(arrowKeyIndex(direction))
    
    /**
     * This event filter only accepts events for the specified character key
     */
    def charFilter(char: Char): Filter[KeyStateEvent] = e => e.isCharacter(char)
    
    /**
      * @param acceptedChars Characters that are accepted by the filter
      * @return Event filter that only accepts events concerning specified characters
      */
    def charsFilter(acceptedChars: Iterable[Char]): Filter[KeyStateEvent] = e => acceptedChars.exists(e.isCharacter)
    
    /**
     * This event filter only accepts events for the specified key indices
     */
    def keysFilter(firstIndex: Int, secondIndex: Int, moreIndices: Int*): Filter[KeyStateEvent] =
        keysFilter(Set(firstIndex, secondIndex) ++ moreIndices)
    
    /**
      * @param acceptedKeys Keys that are accepted by the filter
      * @return Event filter that only accepts events concerning specified keys
      */
    def keysFilter(acceptedKeys: Set[Int]): Filter[KeyStateEvent] = e => acceptedKeys.contains(e.index)
    
    /**
      * @param notAcceptedKeys Keys that are not accepted by the filter
      * @return A filter that accepts events for all keys except those specified
      */
    def notKeysFilter(notAcceptedKeys: Set[Int]): Filter[KeyStateEvent] = e => !notAcceptedKeys.contains(e.index)
    
    /**
     * @param char Target combo character
     * @return A filter that only accepts events where control is being held down while specified character key is pressed
     */
    def controlCharComboFilter(char: Char): Filter[KeyStateEvent] = controlDownFilter && wasPressedFilter && charFilter(char)
    
    /**
      * @param direction Arrow key direction
      * @return A key code matching that arrow key
      */
    def arrowKeyIndex(direction: Direction2D) = direction match
    {
        case Up => KeyEvent.VK_UP
        case Down => KeyEvent.VK_DOWN
        case Direction2D.Left => KeyEvent.VK_LEFT
        case Direction2D.Right => KeyEvent.VK_RIGHT
    }
}

/**
 * This event is used for informing instances when a pressed state changes for a single keyboard 
 * button
 * @author Mikko Hilpinen
 * @since 21.2.2017
  * @param index The index of the changed key
  * @param location The specific location where the key was changed
  * @param keyStatus The current overall key status
 */
case class KeyStateEvent(index: Int, location: KeyLocation, isDown: Boolean, keyStatus: KeyStatus)
{
    // COMPUTED ---------------------
    
    /**
      * @return Whether the key was just released
      */
    def isReleased = !isDown
    
    /**
      * @return Direction of the horizontal arrow key (left, right) that was changed. None if the change didn't affect
      *         a horizontal arrow key.
      */
    def horizontalArrow = index match
    {
        case KeyEvent.VK_RIGHT => Some(Direction2D.Right)
        case KeyEvent.VK_LEFT => Some(Direction2D.Left)
        case _ => None
    }
    
    /**
      * @return Direction of the vertical arrow key (up, down) that was changed. None if the change didn't affect
      *         a vertical arrow key.
      */
    def verticalArrow = index match
    {
        case KeyEvent.VK_UP => Some(Direction2D.Up)
        case KeyEvent.VK_DOWN => Some(Direction2D.Down)
        case _ => None
    }
    
    /**
      * @return The direction of the arrow key that was changed. None if the change didn't affect an arrow key.
      */
    def arrow = horizontalArrow orElse verticalArrow
    
    /**
      * @param axis Target axis
      * @return Direction of the arrow key that lies in the specified axis and was changed. None if the change
      *         didn't affect an arrow key on the specified axis.
      */
    def arrowAlong(axis: Axis2D) = axis match
    {
        case X => horizontalArrow
        case Y => verticalArrow
    }
    
    
    // IMPLEMENTED  -----------------
    
    override def toString = s"$index ${ if (isDown) "was pressed" else "was released" } at location: $location"
    
    
    // OTHER    ---------------------
    
    /**
     * Checks whether the event concerns a specific character key
     */
    def isCharacter(char: Char) = index == KeyEvent.getExtendedKeyCodeForChar(char)
}