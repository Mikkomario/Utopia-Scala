package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Single
import utopia.flow.operator.equality.EqualsBy

trait MouseButton extends EqualsBy
{
    // ABSTRACT ---------------------
    
    /**
      * @return The index of this mouse button (see: [[java.awt.event.MouseEvent]])
      */
    def index: Int
    
    
    // COMPUTED --------------------
    
    @deprecated("Please use .index instead", "v4.0")
    def buttonIndex = index
    
    
    // IMPLEMENTED  ----------------
    
    override protected def equalsProperties= Single(index)
}

/**
 * This object contains the commonly known mouse buttons, each of which is tied to a mouse button
 * index
 * @author Mikko Hilpinen
 * @since 17.2.2017
 */
object MouseButton
{
    // ATTRIBUTES    ---------------
    
    /**
      * The different pre-specified button types, does not include possible special buttons
      */
    val standardValues = Vector[MouseButton](Left, Middle, Right)
    
    
    // COMPUTED ---------------------
    
    @deprecated("Please use .standardValues instead", "v4.0")
    def values = standardValues
    
    
    // OTHER   ------------
    
    /**
      * @param index Mouse button index
      * @return Button that matches that index (Other if doesn't match any of the standard buttons)
      */
    def apply(index: Int) = standardValues.find { _.index == index }.getOrElse(Other(index))
    
    /**
      * Finds the basic button matching the provided button index. Only works for the basic buttons
      * (left, right, middle)
      * @param buttonIndex the index of the button
      */
    @deprecated("Please use .apply(Int) instead", "v4.0")
    def forIndex(buttonIndex: Int) = standardValues.find { _.index == buttonIndex }
    
    
    // NESTED    ------------------
    
    /**
     * The left mouse button
     */
    case object Left extends MouseButton {
        override val index: Int = 1
    }
    /**
     * The middle mouse button
     */
    case object Middle extends MouseButton {
        override val index: Int = 2
    }
    /**
     * The right mouse button
     */
    case object Right extends MouseButton {
        override val index: Int = 3
    }
    
    /**
      * Custom mouse button wrapper class.
      * Intended to be used with additional (non-standard) mouse buttons.
      * @param index Wrapped mouse button index
      */
    case class Other(index: Int) extends MouseButton
}