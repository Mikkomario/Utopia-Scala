package utopia.genesis.event

sealed abstract class MouseButton(val buttonIndex: Int)

/**
 * This object contains the commonly known mouse buttons, each of which is tied to a mouse button
 * index
 * @author Mikko Hilpinen
 * @since 17.2.2017
 */
object MouseButton
{
    // VARIANTS    ------------------
    
    /**
     * The left mouse button
     */
    case object Left extends MouseButton(1)
    /**
     * The middle mouse button
     */
    case object Middle extends MouseButton(2)
    /**
     * The right mouse button
     */
    case object Right extends MouseButton(3)
    
    
    // ATTRIBUTES    ---------------
    
    /**
     * The different specified button types, does not include possible special buttons
     */
    val values = Vector(Left, Middle, Right)
    
    
    // OTHER METHODS    ------------
    
    /**
     * Finds the basic button matching the provided button index. Only works for the basic buttons
     * (left, right, middle)
     * @param buttonIndex the index of the button
     */
    def forIndex(buttonIndex: Int) = values.find { _.buttonIndex == buttonIndex }
}