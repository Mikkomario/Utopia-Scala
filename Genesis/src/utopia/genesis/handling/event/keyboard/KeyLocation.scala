package utopia.genesis.handling.event.keyboard

import java.awt.event.KeyEvent

/**
 * A keyLocation represents a varying location of a keyboard key.
  * These locations match with those introduced in the [[java.awt.event.KeyEvent]] class.
 * @author Mikko Hilpinen
 * @since 21.2.2017
 */
sealed trait KeyLocation
{
    /**
      * @return The corresponding [[java.awt.event.KeyEvent]] key-location index
      */
    def index: Int
}

object KeyLocation
{
    // ATTRIBUTES   -------------------
    
    /**
      * The different KeyLocation values that are available
      */
    val values = Vector[KeyLocation](Standard, Left, Right, Numpad)
    
    
    // OTHER    -----------------------
    
    /**
      * The location matching the provided index, if available
      */
    def of(index: Int): Option[KeyLocation] = values.find { _.index == index }
    
    
    // VALUES   -----------------------
    
    /**
     * The standard key location used when no other key locations are distinguishable
     */
    case object Standard extends KeyLocation
    {
        override def index: Int = KeyEvent.KEY_LOCATION_STANDARD
    }
    /**
     * The key location for left side keys. Eg. left shift
     */
    case object Left extends KeyLocation
    {
        override def index: Int = KeyEvent.KEY_LOCATION_LEFT
    }
    /**
     * The key location for right side keys. Eg. right control
     */
    case object Right extends KeyLocation
    {
        override def index: Int = KeyEvent.KEY_LOCATION_RIGHT
    }
    /**
     * The key location for numpad keys
     */
    case object Numpad extends KeyLocation
    {
        override def index: Int = KeyEvent.KEY_LOCATION_NUMPAD
    }
}