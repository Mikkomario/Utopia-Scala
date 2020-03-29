package utopia.genesis.handling

import utopia.genesis.event.{ConsumeEvent, MouseButton, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.shape.shape2D.Area2D
import utopia.inception.handling.Handleable
import utopia.inception.util.{AnyFilter, Filter}

object MouseButtonStateListener
{
    /**
      * Creates a new simple mouse button state listener
      * @param filter A filter that determines, which events will come through (default = no filtering)
      * @param function A function that will be called when event is received
      * @return A mouse button state listener
      */
    def apply(filter: Filter[MouseButtonStateEvent] = AnyFilter)(function: MouseButtonStateEvent => Option[ConsumeEvent]): MouseButtonStateListener =
        new FunctionalMouseButtonStateListener(function, filter)
    
    /**
      * Creates a new simple mouse button state listener that is called on mouse button presses
      * @param button The button that should be pressed
      * @param f A function that will be called when the button is pressed
      * @return A new mouse button state listener
      */
    def onButtonPressed(button: MouseButton)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        apply(MouseButtonStateEvent.wasPressedFilter && MouseButtonStateEvent.buttonFilter(button))(f)
    
    /**
      * Creates a simple mouse button state listener that is called when left mouse button is pressed
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    def onLeftPressed(f: MouseButtonStateEvent => Option[ConsumeEvent]) = onButtonPressed(MouseButton.Left)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when right mouse button is pressed
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    def onRightPressed(f: MouseButtonStateEvent => Option[ConsumeEvent]) = onButtonPressed(MouseButton.Right)(f)
    
    /**
      * Creates a new simple mouse button state listener that is called on mouse button releases
      * @param button The button that should be released
      * @param f A function that will be called when the button is released
      * @return A new mouse button state listener
      */
    def onButtonReleased(button: MouseButton)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        apply(MouseButtonStateEvent.wasReleasedFilter && MouseButtonStateEvent.buttonFilter(button))(f)
    
    /**
      * Creates a simple mouse button state listener that is called when left mouse button is released
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    def onLeftReleased(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonReleased(MouseButton.Left)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when right mouse button is released
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    def onRightReleased(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonReleased(MouseButton.Right)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when a mouse button is pressed within a certain area
      * @param button The target mouse button
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    def onButtonPressedInside(button: MouseButton)(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        apply(MouseButtonStateEvent.wasPressedFilter && MouseButtonStateEvent.buttonFilter(button) && MouseEvent.isOverAreaFilter(getArea))(f)
    
    /**
      * Creates a simple mouse button state listener that is called when the left mouse button is pressed within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    def onLeftPressedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonPressedInside(MouseButton.Left)(getArea)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when the right mouse button is pressed within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    def onRightPressedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonPressedInside(MouseButton.Right)(getArea)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when a mouse button is released within a certain area
      * @param button The target mouse button
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    def onButtonReleasedInside(button: MouseButton)(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        apply(MouseButtonStateEvent.wasReleasedFilter && MouseButtonStateEvent.buttonFilter(button) && MouseEvent.isOverAreaFilter(getArea))(f)
    
    /**
      * Creates a simple mouse button state listener that is called when the left mouse button is released within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    def onLeftReleasedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonReleasedInside(MouseButton.Left)(getArea)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when the right mouse button is released within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    def onRightReleasedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonReleasedInside(MouseButton.Right)(getArea)(f)
}

/**
 * This trait is extended by classes which are interested in changes in mouse button statuses
 * @author Mikko Hilpinen
 * @since 18.2.2017
 */
trait MouseButtonStateListener extends Handleable
{
    // ABSTRACT ---------------------------
    
    /**
     * This method will be called in order to inform the listener about a new mouse button event
     * (a mouse button being pressed or released)
     * @param event The mouse event that occurred
      * @return If the read event was consumed during this process, should return the consume event
     */
    def onMouseButtonState(event: MouseButtonStateEvent): Option[ConsumeEvent]
    
    /**
     * The filter applied to the incoming mouse button events. The listener will only be informed
     * about the events accepted by the filter. The default filter accepts all mouse button events.
     */
    def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] = AnyFilter
    
    
    // COMPUTED ---------------------------
    
    /**
      * @return Whether this instance is currently willing to receive events for mouse button state changes
      */
    def isReceivingMouseButtonStateEvents = allowsHandlingFrom(MouseButtonStateHandlerType)
}

private class FunctionalMouseButtonStateListener(val function: MouseButtonStateEvent => Option[ConsumeEvent],
                                                 val filter: Filter[MouseButtonStateEvent] = AnyFilter)
    extends MouseButtonStateListener with utopia.inception.handling.immutable.Handleable
{
    override def onMouseButtonState(event: MouseButtonStateEvent) = function(event)
    
    override def mouseButtonStateEventFilter = filter
}