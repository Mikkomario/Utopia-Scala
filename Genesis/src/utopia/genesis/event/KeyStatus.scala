package utopia.genesis.event

import scala.collection.immutable.{HashMap, VectorBuilder}
import java.awt.event.KeyEvent

object KeyStatus
{
    /**
      * An empty key status (all keys are in released-state)
      */
    val empty = new KeyStatus(HashMap())
    
    /**
      * @param index Key index
      * @param location Specific key location (default = standard)
      * @return A new key status with this single key down
      */
    def keyDown(index: Int, location: KeyLocation = KeyLocation.Standard) = new KeyStatus(HashMap(index -> Set(location)))
}

/**
 * This immutable class is used for keeping record of keyboard button states. The class has value
 * semantics. This class uses the java.awt.KeyEvent enhanced key codes as indices.
 * @author Mikko Hilpinen
 * @since 21.2.2017
 */
case class KeyStatus private(private val status: Map[Int, Set[KeyLocation]])
{
    // COMPUTED PROPERTIES    -------
    
    /**
      * @return Whether this key status has all keys in a released state
      */
    def isEmpty = status.isEmpty
    
    /**
      * @return The keys that are currently pressed in any location
      */
    def keysDown = status.keySet
    
    /**
     * Whether the left arrow key is down
     */
    def left = apply(KeyEvent.VK_LEFT)
    
    /**
     * Whether the right arrow key is down
     */
    def right = apply(KeyEvent.VK_RIGHT)
    
    /**
     * Whether the up arrow key is down
     */
    def up = apply(KeyEvent.VK_UP)
    
    /**
     * Whether the down arrow key is down
     */
    def down = apply(KeyEvent.VK_DOWN)
    
    /**
     * Whether the space bar is down
     */
    def space = apply(KeyEvent.VK_SPACE)
    
    
    // OPERATORS    -----------------
    
    /**
     * Checks the status of a single key index. Returns if the key is down /
     * pressed at any key location.
     * @return whether a key matching the index is down at any location
     */
    def apply(index: Int) = status.contains(index)
    
    /**
     * Checks the status of a single key at a specific key location
     * @return Whether the key with the specified index and location is currently down / pressed
     */
    def apply(index: Int, location: KeyLocation) = status.get(index).exists { _.contains(location) }
    
    /**
     * Checks the status of a key indicated by a specific character
     * @return Whether a key indicated by the character is currently down / pressed
     */
    def apply(char: Char): Boolean = apply(KeyEvent.getExtendedKeyCodeForChar(char))
    
    /**
      * @param index Target key index
      * @param location Target key location
      * @param status New key status
      * @return A copy of this KyeStatus with specified status added
      */
    def +(index: Int, location: KeyLocation, status: Boolean) = withStatus(index, location, status)
    
    /**
      * Combines two key statuses so that a key is down when it's down in either status
      * @param other Another key status
      * @return A key status with both of these statuses' keys down
      */
    def ||(other: KeyStatus) =
    {
        // Separates the indices into three categories
        val myIndicesBuilder = new VectorBuilder[Int]()
        val otherIndicesBuilder = new VectorBuilder[Int]()
        val commonIndicesBuilder = new VectorBuilder[Int]()
        
        status.keys.foreach { myIndex => if (other.status.contains(myIndex))
            commonIndicesBuilder += myIndex else myIndicesBuilder += myIndex }
        other.status.keys.filterNot(status.contains).foreach(otherIndicesBuilder.+=)
        
        // Builds the final result from three types of sources
        val buffer = new VectorBuilder[(Int, Set[KeyLocation])]()
        
        myIndicesBuilder.result().foreach { index => buffer += (index -> status(index)) }
        otherIndicesBuilder.result().foreach { index => buffer += (index -> other.status(index)) }
        
        commonIndicesBuilder.result().foreach { index => buffer += (index -> (status(index) ++ other.status(index))) }
        
        new KeyStatus(buffer.result().toMap)
    }
    
    /**
      * Combines two key statuses so that a key is down only when it's down in both of these statuses
      * @param other Another key status
      * @return A key status with only common keys down
      */
    def &&(other: KeyStatus) =
    {
        // Only preserves locations also present in the other status. Filters out empty vectors
        val newStatus = status.map { case (index, locations) => index -> locations.filter { other(index, _) } }
            .filterNot { _._2.isEmpty }
        new KeyStatus(newStatus)
    }
    
    
    // OTHER    ----------------------
    
    /**
      * @param index Target key index
      * @param location Target key location
      * @param newStatus New key status
      * @return A copy of this KyeStatus with specified status added
      */
    def withStatus(index: Int, location: KeyLocation, newStatus: Boolean) =
    {
        // If already has specified status, no change is done
        if (apply(index, location) == newStatus)
            this
        // If a new key is being added, simply updates the locations
        else if (newStatus)
        {
            if (status.contains(index))
                new KeyStatus(status + (index -> (status(index) + location)))
            else
                new KeyStatus(status + (index -> Set(location)))
        }
        else
        {
            // If a key is being removed, either removes list altogether or shortens it
            // No 0 sized lists are left
            val oldLocations = status(index)
            if (oldLocations.size > 1)
                new KeyStatus(status + (index -> oldLocations.filterNot { _ == location }))
            else
                new KeyStatus(status.filterKeys { _ != index })
        }
    }
    
    /**
      * @param index Target key index
      * @param location Target key location (default = standard)
      * @return A copy of this key status with specified key down
      */
    def withKeyDown(index: Int, location: KeyLocation = KeyLocation.Standard) = withStatus(index, location, true)
    
    /**
      * @param index Target key index
      * @param location Target key location (default = standard)
      * @return A copy of this key status with specified key released / up
      */
    def withKeyReleased(index: Int, location: KeyLocation = KeyLocation.Standard) = withStatus(index, location, false)
    
    /**
      * @param previous Previous key status
      * @return A key status that only contains keys that were pressed down since the last status
      */
    def keyPressesSince(previous: KeyStatus) =
    {
        val newStatus = status.map { case (index, locations) => index -> locations.filterNot { previous(index, _) } }
            .filterNot { _._2.isEmpty }
        new KeyStatus(newStatus)
    }
    
    /**
      * @param previous The previous key status
      * @return A map of key index -> key locations of the keys that were released since the last status
      */
    def keyReleasesSince(previous: KeyStatus) = previous.keyPressesSince(this).status
}