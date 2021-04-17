package utopia.reflection.component.template

import java.awt.FontMetrics

import utopia.genesis.event._
import utopia.genesis.handling.mutable._
import utopia.genesis.handling.{MouseButtonStateListener, MouseMoveListener, MouseWheelListener}
import utopia.genesis.shape.shape2D.Point
import utopia.inception.handling.Handleable
import utopia.reflection.component.template.layout.Area
import utopia.reflection.text.Font

/**
* This trait describes basic component features without any implementation
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait ComponentLike2 extends Area
{
    // ABSTRACT    ------------------------
    
    /**
      * @param font font for which the metrics are calculated
      * @return The font metrics object for this component
      */
    def fontMetrics(font: Font): FontMetrics
    
    def mouseButtonHandler: MouseButtonStateHandler
    def mouseMoveHandler: MouseMoveHandler
    def mouseWheelHandler: MouseWheelHandler
    
    /**
      * @return The components under this component
      */
    def children: Seq[ComponentLike2] = Vector()
    
    
    // COMPUTED    ---------------------------
    
    /**
      * Calculates text width within this component
      * @param font Font being used
      * @param text Text to be presented
      * @return The width the text would take in this component, using the specified font
      */
    def textWidthWith(font: Font, text: String) =
    {
        if (text.isEmpty)
            0
        else
            fontMetrics(font).stringWidth(text)
    }
    
    /**
      * @param font Font being used
      * @return The height of a single line of text in this component, using the specified font
      */
    def textHeightWith(font: Font) = fontMetrics(font).getHeight
    
    
    // OTHER    -------------------------
    
    /**
      * Distributes a mouse button event to this wrapper and children
      * @param event A mouse event. Should be within this component's parent's context
      *              (origin should be at the parent component's position). Events outside parent context shouldn't be
      *              distributed.
      */
    def distributeMouseButtonEvent(event: MouseButtonStateEvent): Option[ConsumeEvent] =
    {
        // Informs children first
        val consumeEvent = distributeConsumableMouseEvent[MouseButtonStateEvent](event, _.distributeMouseButtonEvent(_))
        
        // Then informs own handler
        mouseButtonHandler.onMouseButtonState(consumeEvent.map(event.consumed).getOrElse(event))
    }
    
    /**
      * Distributes a mouse move event to this wrapper and children
      * @param event A mouse move event. Should be within this component's parent's context
      *              (origin should be at the parent component's position). Events outside parent context shouldn't be
      *              distributed.
      */
    def distributeMouseMoveEvent(event: MouseMoveEvent): Unit =
    {
        // Informs own listeners first
        mouseMoveHandler.onMouseMove(event)
        
        distributeEvent[MouseMoveEvent](event, e => Vector(e.mousePosition, e.previousMousePosition),
            _.relativeTo(_), _.distributeMouseMoveEvent(_))
    }
    
    /**
      * Distributes a mouse wheel event to this wrapper and children
      * @param event A mouse wheel event. Should be within this component's parent's context
      *              (origin should be at the parent component's position). Events outside parent context shouldn't be
      *              distributed.
      */
    def distributeMouseWheelEvent(event: MouseWheelEvent): Option[ConsumeEvent] =
    {
        // Informs children first
        val consumeEvent = distributeConsumableMouseEvent[MouseWheelEvent](event, _.distributeMouseWheelEvent(_))
        
        // Then informs own handler
        mouseWheelHandler.onMouseWheelRotated(consumeEvent.map(event.consumed).getOrElse(event))
    }
    
    /**
      * Adds a new mouse button listener to this wrapper
      * @param listener A new listener
      */
    def addMouseButtonListener(listener: MouseButtonStateListener) = mouseButtonHandler += listener
    
    /**
      * Adds a new mouse move listener to this wrapper
      * @param listener A new listener
      */
    def addMouseMoveListener(listener: MouseMoveListener) = mouseMoveHandler += listener
    
    /**
      * Adds a new mouse wheel listener to this wrapper
      * @param listener A new listener
      */
    def addMouseWheelListener(listener: MouseWheelListener) = mouseWheelHandler += listener
    
    /**
      * Removes a listener from this wrapper
      * @param listener A listener to be removed
      */
    def removeMouseListener(listener: Handleable) = forMeAndChildren
    {
        c =>
            c.mouseButtonHandler -= listener
            c.mouseMoveHandler -= listener
            c.mouseWheelHandler -= listener
    }
    
    private def forMeAndChildren[U](operation: ComponentLike2 => U): Unit =
    {
        operation(this)
        children.foreach(operation)
    }
    
    private def distributeEvent[E](event: E, positionsFromEvent: E => Iterable[Point],
                                   translateEvent: (E, Point) => E, childAccept: (ComponentLike2, E) => Unit) =
    {
        // If has children, informs them. Event position is modified and only events within this component's area
        // are relayed forward
        val myBounds = bounds
        if (positionsFromEvent(event).exists { myBounds.contains(_) })
        {
            val translated = translateEvent(event, myBounds.position)
            // Only visible children are informed of events
            children.foreach { c => childAccept(c, translated) }
        }
    }
    
    private def distributeConsumableMouseEvent[E <: MouseEvent[E] with Consumable[E]](event: E, childAccept: (ComponentLike2, E) => Option[ConsumeEvent]) =
    {
        val myBounds = bounds
        if (myBounds.contains(event.mousePosition))
        {
            val translatedEvent = event.relativeTo(myBounds.position)
            translatedEvent.distributeAmong(children)(childAccept)
        }
        else
            None
    }
}
