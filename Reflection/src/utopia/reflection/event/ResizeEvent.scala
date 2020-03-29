package utopia.reflection.event

import utopia.genesis.shape.shape2D.Size

/**
* Resize events are generated when a component, or other element, is resized
* @author Mikko Hilpinen
* @since 26.3.2019
**/
case class ResizeEvent(oldSize: Size, newSize: Size)
{
	// COMPUTED    ---------------
    
    /**
     * The size increase that occurred during this event
     */
    def increase = newSize - oldSize
    
    
    // IMPLEMENTED  -------------
    
    override def toString = s"Resize from $oldSize to $newSize"
}