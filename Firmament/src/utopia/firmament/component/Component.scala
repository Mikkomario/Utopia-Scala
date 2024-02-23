package utopia.firmament.component

import utopia.genesis.graphics.FontMetricsWrapper
import utopia.genesis.handling.event.consume.ConsumeChoice.Preserve
import utopia.genesis.handling.event.consume.{Consumable, ConsumeChoice}
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.{Handleable, Handlers}
import utopia.genesis.text.Font
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
* This trait describes basic component features without any implementation
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait Component extends HasMutableBounds
{
    // ABSTRACT    ------------------------
    
    /**
      * @param font font for which the metrics are calculated
      * @return The font metrics object for this component
      */
    def fontMetricsWith(font: Font): FontMetricsWrapper
    
    /**
      * @return A handler used for distributing mouse button events within this component
      */
    def mouseButtonHandler: MouseButtonStateHandler
    /**
      * @return A handler used for distributing mouse move events within this component
      */
    def mouseMoveHandler: MouseMoveHandler
    /**
      * @return A handler used for distributing mouse wheel events within this component
      */
    def mouseWheelHandler: MouseWheelHandler
    
    /**
      * @return Handlers used for distributing events within this component.
      *         Typically used for distributing mouse-related events only.
      */
    def handlers: Handlers
    
    /**
      * @return The components under this component
      */
    def children: Seq[Component] = Vector()
    
    
    // COMPUTED    ---------------------------
    
    /**
      * Calculates text width within this component
      * @param font Font being used
      * @param text Text to be presented
      * @return The width the text would take in this component, using the specified font
      */
    def textWidthWith(font: Font, text: String) = {
        if (text.isEmpty)
            0
        else
            fontMetricsWith(font).widthOf(text)
    }
    /**
      * @param font Font being used
      * @return The height of a single line of text in this component, using the specified font
      */
    def textHeightWith(font: Font) = fontMetricsWith(font).lineHeight
    
    
    // OTHER    -------------------------
    
    /**
      * Distributes a mouse button event to this wrapper and children
      * @param event A mouse event. Should be within this component's parent's context
      *              (origin should be at the parent component's position). Events outside parent context shouldn't be
      *              distributed.
      */
    def distributeMouseButtonEvent(event: MouseButtonStateEvent): ConsumeChoice = {
        // Informs children first
        val (eventAfterChildren, childrenChoice) = distributeConsumableMouseEvent[MouseButtonStateEvent](event) { _.distributeMouseButtonEvent(_) }
        
        // Then informs own handler
        childrenChoice || mouseButtonHandler.onMouseButtonStateEvent(eventAfterChildren)
    }
    /**
      * Distributes a mouse move event to this wrapper and children
      * @param event A mouse move event. Should be within this component's parent's context
      *              (origin should be at the parent component's position). Events outside parent context shouldn't be
      *              distributed.
      */
    def distributeMouseMoveEvent(event: MouseMoveEvent): Unit = {
        // Informs own listeners first
        mouseMoveHandler.onMouseMove(event)
        
        distributeEvent[MouseMoveEvent](event, e => e.positions.map { _.relative },
            _.relativeTo(_), _.distributeMouseMoveEvent(_))
    }
    /**
      * Distributes a mouse wheel event to this wrapper and children
      * @param event A mouse wheel event. Should be within this component's parent's context
      *              (origin should be at the parent component's position). Events outside parent context shouldn't be
      *              distributed.
      */
    def distributeMouseWheelEvent(event: MouseWheelEvent): ConsumeChoice = {
        // Informs children first
        val (eventAfterChildren, childrenChoice) = distributeConsumableMouseEvent[MouseWheelEvent](
            event) { _.distributeMouseWheelEvent(_) }
        
        // Then informs own handler
        childrenChoice || mouseWheelHandler.onMouseWheelRotated(eventAfterChildren)
    }
    
    /**
      * Adds a new mouse button listener to this wrapper
      * @param listener A new listener
      */
    def addMouseButtonListener(listener: MouseButtonStateListener) =
        mouseButtonHandler += listener
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
      * Assigns a listener to be informed of events related to this component
      * @param listener A listener to be informed
      * @return This component
      */
    def +=(listener: Handleable) = {
        handlers += listener
        this
    }
    /**
      * Assigns 0-n listeners to be informed of events related to this component
      * @param listeners Listeners to be informed
      * @return This component
      */
    def ++=(listeners: IterableOnce[Handleable]) = {
        handlers ++= listeners
        this
    }
    
    /**
      * Removes a listener from this wrapper
      * @param listener A listener to be removed
      */
    def removeMouseListener(listener: Handleable) = forMeAndChildren { c =>
        c.mouseButtonHandler -= listener
        c.mouseMoveHandler -= listener
        c.mouseWheelHandler -= listener
    }
    /**
      * Removes a listener from this component, so that it won't be informed of events anymore
      * @param listener A listener
      * @return This component
      */
    def -=(listener: Handleable) = {
        forMeAndChildren { _.handlers -= listener }
        this
    }
    /**
      * Removes 0-n listeners so that they won't be informed of events within this component anymore
      * @param listeners Listeners to remove
      * @return This component
      */
    def --=(listeners: Iterable[Handleable]) = {
        if (listeners.nonEmpty)
            forMeAndChildren { _.handlers --= listeners }
        this
    }
    
    private def forMeAndChildren[U](operation: Component => U): Unit = {
        operation(this)
        children.foreach(operation)
    }
    
    private def distributeEvent[E](event: E, positionsFromEvent: E => Iterable[Point],
                                   translateEvent: (E, Point) => E, childAccept: (Component, E) => Unit) =
    {
        // If has children, informs them. Event position is modified and only events within this component's area
        // are relayed forward
        val myBounds = bounds
        if (positionsFromEvent(event).exists { myBounds.contains(_) }) {
            val translated = translateEvent(event, myBounds.position)
            // Only visible children are informed of events
            children.foreach { c => childAccept(c, translated) }
        }
    }
    
    private def distributeConsumableMouseEvent[E <: MouseEvent[E] with Consumable[E]](event: E)
                                                                                     (childAccept: (Component, E) => ConsumeChoice): (E, ConsumeChoice) =
    {
        val myBounds = bounds
        if (myBounds.contains(event.position)) {
            val translatedEvent = event.relativeTo(myBounds.position)
            translatedEvent.distribute(children)(childAccept)
        }
        else
            event -> Preserve
    }
}
