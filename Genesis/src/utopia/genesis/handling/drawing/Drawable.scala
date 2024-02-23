package utopia.genesis.handling.drawing

import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.Priority.Normal
import utopia.genesis.graphics.{DrawOrder, Drawer, Priority}
import utopia.genesis.handling.template.Handleable
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
 * Drawable instances can be drawn on a canvas using a graphics object and support depth sorting
 * @author Mikko Hilpinen
 * @since 28.12.2016
 */
trait Drawable extends Handleable
{
    // ABSTRACT ------------------------
    
    /**
      * @return Drawing level information for this item.
      *         Affects which item is drawn on top of which, when there is overlap.
      */
    def drawOrder: DrawOrder
    
    /**
      * @return Whether this item covers the whole [[drawBounds]] with no background showing through
      */
    def opaque: Boolean
    
    /**
      * @return A pointer to the bounds on which this item is drawn
      */
    def drawBoundsPointer: Changing[Bounds]
    
    /**
      * @return List of currently attached repaint listeners
      */
    def repaintListeners: Iterable[RepaintListener]
    
    /**
     * Draws the drawable instance using a specific drawer.
     * @param drawer The drawer object used for drawing this instance
      * @param bounds This item's draw bounds, relative to the drawer's (0,0) origin
     */
    def draw(drawer: Drawer, bounds: Bounds): Unit
    
    /**
      * Adds a new repaint-listener to this item
      * @param listener A listener to add
      */
    def addRepaintListener(listener: RepaintListener): Unit
    /**
      * Removes a repaint-listener from this item
      * @param listener A listener to remove
      */
    def removeRepaintListener(listener: RepaintListener): Unit
    
    
    // COMPUTED -----------------------
    
    /**
      * @return The current area where this item draws its content
      */
    def drawBounds = drawBoundsPointer.value
    
    @deprecated("Please use .drawOrder instead", "v4.0")
    def drawDepth = drawOrder.level.index * 1000 + drawOrder.orderIndex
    
    
    // OTHER    ----------------------
    
    /**
      * Requests a repaint for this item
      * @param subRegion The region within this item's bounds, which should be drawn.
      *                  Relative to this item's draw bounds.
      *                  None if this item's whole draw bounds should be drawn (default).
      * @param priority Drawing priority (default = Normal)
      */
    def repaint(subRegion: Option[Bounds] = None, priority: Priority = Normal) =
        repaintListeners.foreach { _.repaint(this, subRegion, priority) }
}
