package utopia.genesis.event.keyboard

import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.event.KeyLocation
import utopia.genesis.event.KeyLocation.Standard
import utopia.genesis.event.keyboard.Key.CharKey

import java.awt.event.KeyEvent
import scala.annotation.unused
import scala.language.implicitConversions

object KeyboardState
{
    // ATTRIBUTES   ------------------
    
    /**
      * A keyboard state where all keys are in the released state
      */
    val default = apply(Map())
    
    
    // IMPLICIT ----------------------
    
    // Implicitly converts this object to the default keyboard state
    implicit def objectToInstance(@unused o: KeyboardState.type): KeyboardState = default
}

/**
 * This immutable class is used for keeping record of keyboard button states.
 * @author Mikko Hilpinen
 * @since 21.2.2017, rewritten 4.2.2024, v3.6
 */
case class KeyboardState(downIndices: Map[Int, Set[KeyLocation]])
{
    // COMPUTED PROPERTIES    -------
    
    /**
      * @return Whether this key state has all keys in a released state
      */
    def areAllReleased = downIndices.isEmpty
    /**
      * @return Whether this state has at least key pressed down
      */
    def areSomeDown = !areAllReleased
    
    /**
      * @return The key-indices that are currently pressed in any location
      */
    def keysDown = downIndices.keySet
    
    /**
     * Whether the left arrow key is down
     */
    @deprecated("Deprecated for removal. With the introduction of Key class, these helpers should be redundant", "v3.6")
    def left = apply(KeyEvent.VK_LEFT)
    /**
     * Whether the right arrow key is down
     */
    @deprecated("Deprecated for removal. With the introduction of Key class, these helpers should be redundant", "v3.6")
    def right = apply(KeyEvent.VK_RIGHT)
    /**
     * Whether the up arrow key is down
     */
    @deprecated("Deprecated for removal. With the introduction of Key class, these helpers should be redundant", "v3.6")
    def up = apply(KeyEvent.VK_UP)
    /**
     * Whether the down arrow key is down
     */
    @deprecated("Deprecated for removal. With the introduction of Key class, these helpers should be redundant", "v3.6")
    def down = apply(KeyEvent.VK_DOWN)
    
    /**
     * Whether the space bar is down
     */
    @deprecated("Deprecated for removal. With the introduction of Key class, these helpers should be redundant", "v3.6")
    def space = apply(KeyEvent.VK_SPACE)
    
    /**
      * @return Whether a control key is down
      */
    @deprecated("Deprecated for removal. With the introduction of Key class, these helpers should be redundant", "v3.6")
    def control = apply(KeyEvent.VK_CONTROL)
    /**
      * @return Whether a shift key is down
      */
    @deprecated("Deprecated for removal. With the introduction of Key class, these helpers should be redundant", "v3.6")
    def shift = apply(KeyEvent.VK_SHIFT)
    
    
    // OTHER    -----------------
    
    /**
     * Checks the status of a single key index.
      * Returns if the key is down / pressed at any key location.
     * @return whether a key matching the index is down at any location
     */
    def apply(index: Int) = downIndices.contains(index)
    /**
      * @param key A keyboard key
      * @return Whether that key is pressed down at any location
      */
    def apply(key: Key): Boolean = apply(key.index)
    /**
     * Checks the status of a single key at a specific key location
     * @return Whether the key with the specified index and location is currently down / pressed
     */
    def apply(index: Int, location: KeyLocation) = downIndices.get(index).exists { _.contains(location) }
    /**
      * @param key A keyboard key
      * @param location Location where that key should be pressed down
      * @return Whether that key at that specific location is currently pressed down
      */
    def apply(key: Key, location: KeyLocation): Boolean = apply(key.index, location)
    /**
     * Checks the status of a key indicated by a specific character
     * @return Whether a key indicated by the character is currently down / pressed
     */
    def apply(char: Char): Boolean = apply(CharKey(char))
    
    /**
      * @param key A key + its specific location
      * @return Copy of this state with the specified key pressed down at the specified location
      */
    def +(key: (Key, KeyLocation)) = withKeyDown(key._1, key._2)
    /**
      * @param key A key (assumed to be at the [[utopia.genesis.event.KeyLocation.Standard]] location)
      * @return Copy of this state with the specified key pressed down
      */
    def +(key: Key) = withKeyDown(key)
    /**
      * @param key A key + its specific location
      * @return Copy of this state with the specified key released at the specified location
      */
    def -(key: (Key, KeyLocation)) = withKeyReleased(key._1, key._2)
    /**
      * @param key A key (assumed to be at the [[utopia.genesis.event.KeyLocation.Standard]] location)
      * @return Copy of this state with the specified key released
      */
    def -(key: Key) = withKeyReleased(key)
    
    /**
      * Combines two key statuses so that a key is down when it's down in either status
      * @param other Another key status
      * @return A key status with both of these statuses' keys down
      */
    def ||(other: KeyboardState) = KeyboardState(downIndices.mergeWith(other.downIndices) { _ ++ _ })
    /**
      * Combines two key statuses so that a key is down only when it's down in both of these statuses
      * @param other Another key status
      * @return A key status with only common keys down
      */
    def &&(other: KeyboardState) =
        KeyboardState(downIndices.mergeWith(other.downIndices) { _ & _ }.filterNot { _._2.isEmpty })
    
    /**
      * @param index Target key index
      * @param location Target key location
      * @param down New key status
      * @return A copy of this KyeStatus with specified status added
      */
    def withKeyState(index: Int, location: KeyLocation, down: Boolean) = {
        downIndices.get(index) match {
            case Some(currentLocations) =>
                // Case: Specified key is currently down
                if (currentLocations.contains(location)) {
                    // Case: Set as pressed => No-op
                    if (down)
                        this
                    // Case: Remove from larger set => Keep index, remove location
                    else if (currentLocations.hasSize > 1)
                        copy(downIndices = downIndices + (index -> (currentLocations - location)))
                    // Case: Remove only instance => Remove index
                    else
                        copy(downIndices = downIndices - index)
                }
                // Case: Specified key currently released & set => Marks as pressed
                else if (down)
                    copy(downIndices = downIndices + (index -> Set(location)))
                // Case: Specified key released at that location & release => No-op
                else
                    this
            // Case: Specified key not currently down anywhere
            case None =>
                // Case: Set => Marks as pressed
                if (down)
                    copy(downIndices = downIndices + (index -> Set(location)))
                // Case: Release => No-op
                else
                    this
        }
    }
    /**
      * Marks a key as pressed or released
      * @param key Targeted key
      * @param location Targeted specific key-location
      * @param down Whether the specified key should be considered pressed (true) or released (false)
      * @return Copy of this state with the specified key's state updated
      */
    def withKeyState(key: Key, location: KeyLocation, down: Boolean): KeyboardState =
        withKeyState(key.index, location, down)
    
    /**
      * @param index Target key index
      * @param location Target key location
      * @return A copy of this keyboard state with the specified key down
      */
    def withKeyDown(index: Int, location: KeyLocation) = withKeyState(index, location, down = true)
    /**
      * @param key Targeted key
      * @param location Target key location (default = standard)
      * @return A copy of this keyboard state with the specified key down
      */
    def withKeyDown(key: Key, location: KeyLocation = Standard): KeyboardState = withKeyDown(key.index, location)
    /**
      * @param index Target key index
      * @param location Target key location (default = standard)
      * @return A copy of this key status with specified key released / up
      */
    def withKeyReleased(index: Int, location: KeyLocation) = withKeyState(index, location, down = false)
    /**
      * @param key Targeted key
      * @param location Specific location of that key (default = standard)
      * @return Copy of this keyboard state with the specified key marked as released
      */
    def withKeyReleased(key: Key, location: KeyLocation = Standard): KeyboardState =
        withKeyReleased(key.index, location)
    
    /**
      * @param previous Previous key status
      * @return A key status that only contains keys that were pressed down since the last status
      */
    def keysPressesSince(previous: KeyboardState) =
        copy(downIndices = downIndices
            .map { case (index, locations) => index -> locations.filterNot { previous(index, _) } }
            .filter { _._2.nonEmpty })
    /**
      * @param previous The previous key status
      * @return A map of key index -> key locations of the keys that were released since the last status
      */
    def keysReleasedSince(previous: KeyboardState) = previous.keysPressesSince(this).downIndices
}