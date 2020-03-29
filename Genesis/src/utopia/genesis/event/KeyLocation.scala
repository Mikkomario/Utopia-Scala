package utopia.genesis.event

import java.awt.event.KeyEvent

/**
 * A keyLocation represents a varying location of a keyboard key. These locations match with those 
 * introduced in the java.awt.event.KeyEvent class.
 * @author Mikko Hilpinen
 * @since 21.2.2017
 * @param index The corresponding keyEvent key location index
 */
sealed abstract class KeyLocation(val index: Int)

object KeyLocation
{
    /**
     * The standard key location used when no other key locations are distinguishable
     */
    case object Standard extends KeyLocation(KeyEvent.KEY_LOCATION_STANDARD)
    /**
     * The key location for left side keys. Eg. left shift
     */
    case object Left extends KeyLocation(KeyEvent.KEY_LOCATION_LEFT)
    /**
     * The key location for right side keys. Eg. right control
     */
    case object Right extends KeyLocation(KeyEvent.KEY_LOCATION_RIGHT)
    /**
     * The key location for numpad keys
     */
    case object Numpad extends KeyLocation(KeyEvent.KEY_LOCATION_NUMPAD)
    
    
    /**
     * The different keyLocation values available
     */
    val values = Vector(Standard, Left, Right, Numpad)
    
    /**
     * The location matching the provided index, if available
     */
    def of(index: Int) = values.find { _.index == index }
}