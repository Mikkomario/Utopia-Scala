package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter, RejectAll}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.event.{ConsumeEvent, MouseButton}
import utopia.genesis.handling.event.{ConsumeChoice, ListenerFactory}
import utopia.genesis.handling.event.mouse.MouseEvent2.MouseFilteringFactory
import utopia.genesis.handling.template.Handleable2
import utopia.paradigm.shape.shape2d.area.Area2D

import scala.annotation.unused
import scala.language.implicitConversions

object MouseButtonStateListener2
{
    // TYPES    --------------------
    
    /**
      * Filter applied over mouse button state events
      */
    type MouseButtonStateEventFilter = Filter[MouseButtonStateEventLike[_]]
    
    
    // ATTRIBUTES   ----------------
    
    /**
      * A listener factory that doesn't apply any conditions
      */
    val unconditional = MouseButtonStateListenerFactory()
    
    
    // COMPUTED --------------------
    
    /**
      * @return Access point to mouse button state event filters
      */
    def filter = MouseButtonStateEventFilter
    
    
    // IMPLICIT --------------------
    
    implicit def objectToFactory(@unused o: MouseButtonStateListener2.type): MouseButtonStateListenerFactory =
        unconditional
    
    
    // OTHER    --------------------
    
    /**
      * Creates a new simple mouse button state listener that is called on mouse button presses
      * @param button The button that should be pressed
      * @param f A function that will be called when the button is pressed
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonPressed(MouseButton)(...) instead.", "v4.0")
    def onButtonPressed(button: MouseButton)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        unconditional.buttonPressed(button) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when left mouse button is pressed
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftPressed(...) instead.", "v4.0")
    def onLeftPressed(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) = onButtonPressed(MouseButton.Left)(f)
    /**
      * Creates a simple mouse button state listener that is called when right mouse button is pressed
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightPressed(...) instead.", "v4.0")
    def onRightPressed(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) = onButtonPressed(MouseButton.Right)(f)
    
    /**
      * Creates a new simple mouse button state listener that is called on mouse button releases
      * @param button The button that should be released
      * @param f A function that will be called when the button is released
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonReleased(MouseButton)(...) instead.", "v4.0")
    def onButtonReleased(button: MouseButton)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        unconditional.buttonReleased(button) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when left mouse button is released
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftReleased(...) instead.", "v4.0")
    def onLeftReleased(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) = onButtonReleased(MouseButton.Left)(f)
    /**
      * Creates a simple mouse button state listener that is called when right mouse button is released
      * @param f A function that will be called
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightReleased(...) instead.", "v4.0")
    def onRightReleased(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) = onButtonReleased(MouseButton.Right)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when a mouse button is pressed within a certain area
      * @param button The target mouse button
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonPressed(MouseButton).over(Area2D)(...) instead.", "v4.0")
    def onButtonPressedInside(button: MouseButton)(getArea: => Area2D)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        unconditional.buttonPressed(button).filtering(filter.over(getArea)) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when the left mouse button is pressed within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftPressed.over(Area2D)(...) instead.", "v4.0")
    def onLeftPressedInside(getArea: => Area2D)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        onButtonPressedInside(MouseButton.Left)(getArea)(f)
    /**
      * Creates a simple mouse button state listener that is called when the right mouse button is pressed within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightPressed.over(Area2D)(...) instead.", "v4.0")
    def onRightPressedInside(getArea: => Area2D)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        onButtonPressedInside(MouseButton.Right)(getArea)(f)
    
    /**
      * Creates a simple mouse button state listener that is called when a mouse button is released within a certain area
      * @param button The target mouse button
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .buttonReleased(MouseButton).over(Area2D)(...) instead.", "v4.0")
    def onButtonReleasedInside(button: MouseButton)(getArea: => Area2D)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        unconditional.buttonReleased(button).filtering(filter.over(getArea)) { e => ConsumeChoice(f(e)) }
    /**
      * Creates a simple mouse button state listener that is called when the left mouse button is released within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .leftReleased.over(Area2D)(...) instead.", "v4.0")
    def onLeftReleasedInside(getArea: => Area2D)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        onButtonReleasedInside(MouseButton.Left)(getArea)(f)
    /**
      * Creates a simple mouse button state listener that is called when the right mouse button is released within a certain area
      * @param getArea A function for calculating the target area. Will be called for each incoming event
      * @param f A function that will be called when a suitable event is received
      * @return A new mouse button state listener
      */
    @deprecated("Deprecated for removal. Please use .rightReleased.over(Area2D)(...) instead.", "v4.0")
    def onRightReleasedInside(getArea: => Area2D)(f: MouseButtonStateEvent2 => Option[ConsumeEvent]) =
        onButtonReleasedInside(MouseButton.Right)(getArea)(f)
    
    
    // NESTED   --------------------
    
    trait MouseButtonFilteringFactory[+E <: MouseButtonStateEventLike[_], +Repr] extends MouseFilteringFactory[E, Repr]
    {
        /**
          * @return A filter that only accepts button presses
          */
        def pressed = withFilter { _.pressed }
        /**
          * @return A filter that only accepts button releases
          */
        def released = withFilter { _.released }
        
        /**
          * @return An item that only accepts events concerning the left mouse button
          */
        def left = apply(MouseButton.Left)
        /**
          * @return An item that only accepts events concerning the right mouse button
          */
        def right = apply(MouseButton.Right)
        /**
          * @return An item that only accepts events concerning the middle mouse button
          */
        def middle = apply(MouseButton.Middle)
        
        /**
          * @return An item that only accepts events concerning left mouse button presses
          */
        def leftPressed = withFilter { e => e.concernsLeft && e.pressed }
        /**
          * @return An item that only accepts events concerning left mouse button releases
          */
        def leftReleased = withFilter { e => e.concernsLeft && e.released }
        /**
          * @return An item that only accepts events concerning right mouse button presses
          */
        def rightPressed = withFilter { e => e.concernsRight && e.pressed }
        /**
          * @return An item that only accepts events concerning right mouse button releases
          */
        def rightReleased = withFilter { e => e.concernsRight && e.released }
        
        /**
          * @param button Targeted mouse button
          * @return An item that only accepts events concerning that mouse button
          */
        def apply(button: MouseButton) = withFilter { _.button == button }
        /**
          * @param buttons Targeted mouse buttons
          * @return An item that only accepts events concerning those mouse buttons
          */
        def apply(buttons: Set[MouseButton]) = {
            if (buttons.isEmpty)
                withFilter(RejectAll)
            else
                withFilter { e => buttons.contains(e.button) }
        }
        /**
          * @return An item that only accepts events concerning the specified mouse buttons
          */
        def apply(button1: MouseButton, button2: MouseButton, more: MouseButton*): Repr =
            apply(Set(button1, button2) ++ more)
        
        /**
          * @param button Targeted button
          * @return An item that only accepts press events, and those only from the specified button
          */
        def buttonPressed(button: MouseButton) = withFilter { e => e.button == button && e.pressed }
        /**
          * @param button Targeted button
          * @return An item that only accepts release events, and those only from the specified button
          */
        def buttonReleased(button: MouseButton) = withFilter { e => e.button == button && e.released }
    }
    
    object MouseButtonStateEventFilter
        extends MouseButtonFilteringFactory[MouseButtonStateEventLike[_], MouseButtonStateEventFilter]
    {
        // IMPLEMENTED  ---------------------
        
        override protected def withFilter(filter: MouseButtonStateEventFilter): MouseButtonStateEventFilter = filter
        
        
        // OTHER    -------------------------
        
        /**
          * @param f A filter function for mouse button state events
          * @return A filter that uses the specified function
          */
        def apply(f: MouseButtonStateEventLike[_] => Boolean): MouseButtonStateEventFilter = Filter(f)
    }
    
    case class MouseButtonStateListenerFactory(condition: FlagLike = AlwaysTrue,
                                               filter: Filter[MouseButtonStateEvent2] = AcceptAll)
        extends ListenerFactory[MouseButtonStateEvent2, MouseButtonStateListenerFactory]
            with MouseButtonFilteringFactory[MouseButtonStateEvent2, MouseButtonStateListenerFactory]
    {
        // COMPUTED -------------------------
        
        /**
          * @return An item that only accepts events that haven't been consumed yet
          */
        def unconsumed = withFilter { _.unconsumed }
        
        
        // IMPLEMENTED  ---------------------
        
        override def usingFilter(filter: Filter[MouseButtonStateEvent2]): MouseButtonStateListenerFactory =
            copy(filter = filter)
        override def usingCondition(condition: Changing[Boolean]): MouseButtonStateListenerFactory =
            copy(condition = condition)
        
        override protected def withFilter(filter: Filter[MouseButtonStateEvent2]): MouseButtonStateListenerFactory =
            copy(filter = this.filter && filter)
            
        
        // OTHER    -------------------------
        
        /**
          * @param f A function to call on mouse button state events
          * @return A listener that calls the specified function, also applying this factory's conditions & filters
          */
        def apply(f: MouseButtonStateEvent2 => ConsumeChoice): MouseButtonStateListener2 =
            new _MouseButtonStateListener(condition, filter, f)
    }
    
    private class _MouseButtonStateListener(override val handleCondition: FlagLike,
                                            override val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2],
                                            f: MouseButtonStateEvent2 => ConsumeChoice)
        extends MouseButtonStateListener2
    {
        override def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice = f(event)
    }
}

/**
 * This trait is extended by classes which are interested in changes in mouse button states
 * @author Mikko Hilpinen
 * @since 18.2.2017
 */
trait MouseButtonStateListener2 extends Handleable2
{
    // ABSTRACT ---------------------------
    
    /**
      * The filter applied to the incoming mouse button events.
      * This listener will only be informed about the events accepted by the filter.
      */
    def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2]
    
    /**
     * This method will be called in order to inform this listener about a new mouse button event
     * (a mouse button being pressed or released).
      * Only events accepted by this listener's filter should be applied to this function.
     * @param event The mouse event that occurred
      * @return This listener's choice on whether it will or won't consume the specified event
     */
    def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice
    
    
    // OTHER    ---------------------------
    
    @deprecated("Please use .onMouseButtonStateEvent(MouseButtonStateEvent) instead", "v4.0")
    def onMouseButtonState(event: MouseButtonStateEvent2): Option[ConsumeEvent] =
        onMouseButtonStateEvent(event).eventIfConsumed
}

