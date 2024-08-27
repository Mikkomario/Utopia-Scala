package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.consume.{ConsumeChoice, ConsumeEvent}
import utopia.genesis.handling.event.mouse.MouseButtonStateEvent.{MouseButtonFilteringFactory, MouseButtonStateEventFilter}
import utopia.genesis.handling.template.Handleable
import utopia.paradigm.shape.shape2d.area.Area2D

import scala.annotation.unused
import scala.language.implicitConversions

object MouseButtonStateListener
{
    // ATTRIBUTES   ----------------
    
    /**
      * A listener factory that doesn't apply any conditions
      */
    val unconditional = MouseButtonStateListenerFactory()
    
    
    // IMPLICIT --------------------
    
    implicit def objectToFactory(@unused o: MouseButtonStateListener.type): MouseButtonStateListenerFactory =
        unconditional
    
    
    // OTHER    --------------------
    
    /**
      * Creates a new simple mouse button state listener that is called on mouse button presses
      * @param button The button that should be pressed
      * @param f A function that will be called when the button is pressed
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonPressed(MouseButton)(...) instead.", "v4.0")
    def onButtonPressed(button: MouseButton)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        unconditional.buttonPressed(button) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when left mouse button is pressed
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftPressed(...) instead.", "v4.0")
    def onLeftPressed(f: MouseButtonStateEvent => Option[ConsumeEvent]) = onButtonPressed(MouseButton.Left)(f)
    /**
      * Creates a simple mouse button state listener that is called when right mouse button is pressed
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightPressed(...) instead.", "v4.0")
    def onRightPressed(f: MouseButtonStateEvent => Option[ConsumeEvent]) = onButtonPressed(MouseButton.Right)(f)
    
    /**
      * Creates a new simple mouse button state listener that is called on mouse button releases
      * @param button The button that should be released
      * @param f A function that will be called when the button is released
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonReleased(MouseButton)(...) instead.", "v4.0")
    def onButtonReleased(button: MouseButton)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        unconditional.buttonReleased(button) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when left mouse button is released
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftReleased(...) instead.", "v4.0")
    def onLeftReleased(f: MouseButtonStateEvent => Option[ConsumeEvent]) = onButtonReleased(MouseButton.Left)(f)
    /**
      * Creates a simple mouse button state listener that is called when right mouse button is released
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightReleased(...) instead.", "v4.0")
    def onRightReleased(f: MouseButtonStateEvent => Option[ConsumeEvent]) = onButtonReleased(MouseButton.Right)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when a mouse button is pressed within a certain area
      * @param button The target mouse button
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonPressed(MouseButton).over(Area2D)(...) instead.", "v4.0")
    def onButtonPressedInside(button: MouseButton)(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        unconditional.buttonPressed(button)
            .filtering(MouseButtonStateEventFilter.over(getArea)) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when the left mouse button is pressed within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftPressed.over(Area2D)(...) instead.", "v4.0")
    def onLeftPressedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonPressedInside(MouseButton.Left)(getArea)(f)
    /**
      * Creates a simple mouse button state listener that is called when the right mouse button is pressed within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightPressed.over(Area2D)(...) instead.", "v4.0")
    def onRightPressedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonPressedInside(MouseButton.Right)(getArea)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when a mouse button is released within a certain area
      * @param button The target mouse button
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonReleased(MouseButton).over(Area2D)(...) instead.", "v4.0")
    def onButtonReleasedInside(button: MouseButton)(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        unconditional.buttonReleased(button)
            .filtering(MouseButtonStateEventFilter.over(getArea)) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when the left mouse button is released within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftReleased.over(Area2D)(...) instead.", "v4.0")
    def onLeftReleasedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonReleasedInside(MouseButton.Left)(getArea)(f)
    /**
      * Creates a simple mouse button state listener that is called when the right mouse button is released within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightReleased.over(Area2D)(...) instead.", "v4.0")
    def onRightReleasedInside(getArea: => Area2D)(f: MouseButtonStateEvent => Option[ConsumeEvent]) =
        onButtonReleasedInside(MouseButton.Right)(getArea)(f)
    
    
    // NESTED   --------------------------
    
    case class MouseButtonStateListenerFactory(condition: Flag = AlwaysTrue,
                                               filter: Filter[MouseButtonStateEvent] = AcceptAll)
        extends ListenerFactory[MouseButtonStateEvent, MouseButtonStateListenerFactory]
            with MouseButtonFilteringFactory[MouseButtonStateEvent, MouseButtonStateListenerFactory]
    {
        // COMPUTED -------------------------
        
        /**
          * @return An item that only accepts events that haven't been consumed yet
          */
        def unconsumed = withFilter { _.unconsumed }
        
        
        // IMPLEMENTED  ---------------------
        
        override def usingFilter(filter: Filter[MouseButtonStateEvent]): MouseButtonStateListenerFactory =
            copy(filter = filter)
        override def usingCondition(condition: Flag): MouseButtonStateListenerFactory = copy(condition = condition)
        
        override protected def withFilter(filter: Filter[MouseButtonStateEvent]): MouseButtonStateListenerFactory =
            copy(filter = this.filter && filter)
            
        
        // OTHER    -------------------------
        
        /**
          * @param f A function to call on mouse button state events
          * @return A listener that calls the specified function, also applying this factory's conditions & filters
          */
        def apply(f: MouseButtonStateEvent => ConsumeChoice): MouseButtonStateListener =
            new _MouseButtonStateListener(condition, filter, f)
    }
    
    private class _MouseButtonStateListener(override val handleCondition: Flag,
                                            override val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent],
                                            f: MouseButtonStateEvent => ConsumeChoice)
        extends MouseButtonStateListener
    {
        override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = f(event)
    }
}

/**
 * This trait is extended by classes which are interested in changes in mouse button states
 * @author Mikko Hilpinen
 * @since 18.2.2017
 */
trait MouseButtonStateListener extends Handleable
{
    // ABSTRACT ---------------------------
    
    /**
      * The filter applied to the incoming mouse button events.
      * This listener will only be informed about the events accepted by the filter.
      */
    def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent]
    
    /**
     * This method will be called in order to inform this listener about a new mouse button event
     * (a mouse button being pressed or released).
      * Only events accepted by this listener's filter should be applied to this function.
     * @param event The mouse event that occurred
      * @return This listener's choice on whether it will or won't consume the specified event
     */
    def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice
    
    
    // OTHER    ---------------------------
    
    @deprecated("Please use .onMouseButtonStateEvent(MouseButtonStateEvent) instead", "v4.0")
    def onMouseButtonState(event: MouseButtonStateEvent): Option[ConsumeEvent] =
        onMouseButtonStateEvent(event).eventIfConsumed
}

