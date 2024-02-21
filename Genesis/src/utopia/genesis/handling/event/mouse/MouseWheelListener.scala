package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.consume.{ConsumeChoice, ConsumeEvent}
import utopia.genesis.handling.event.mouse.MouseEvent2.MouseFilteringFactory
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.template.Handleable2
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.VerticalDirection
import utopia.paradigm.shape.shape2d.area.Area2D

import scala.annotation.unused

object MouseWheelListener
{
    // TYPES    ----------------------
    
    /**
      * Event filter for mouse wheel events
      */
    type MouseWheelEventFilter = Filter[MouseWheelEvent]
    
    
    // ATTRIBUTES   ------------------
    
    /**
      * A mouse event listener factory that doesn't apply any conditions or event filters
      */
    val unconditional = MouseWheelListenerFactory()
    
    
    // COMPUTED ----------------------
    
    /**
      * @return Access point to constructing mouse wheel event filters
      */
    def filter = MouseWheelEventFilter
    
    
    // IMPLICIT ----------------------
    
    implicit def objectToFactory(@unused o: MouseWheelListener.type): MouseWheelListenerFactory = unconditional
    
    
    // OTHER    ----------------------
    
    /**
      * Creates a new mouse wheel listener that calls specified function on wheel rotations
      * @param filter A filter that specifies, which events will trigger the function (default = no filtering)
      * @param f A function that will be called on wheel rotations
      * @return A new mouse event listener
      */
    @deprecated("Please use .usingFilter(Filter)(...) instead", "v4.0")
    def apply(filter: Filter[MouseWheelEvent] = AcceptAll)
             (f: MouseWheelEvent => Option[ConsumeEvent]): MouseWheelListener =
        unconditional.usingFilter(filter) { e => ConsumeChoice(f(e)) }
    
    /**
      * Creates a new mouse wheel listener that calls specified function on wheel rotations inside a specific area
      * @param getArea A function for getting the target area
      * @param f A function that will be called on wheel rotations
      * @return A new mouse event listener
      */
    @deprecated("Please use .over(Area2D)(...) instead", "v4.0")
    def onWheelInsideArea(getArea: => Area2D)(f: MouseWheelEvent => Option[ConsumeEvent]) =
        apply { e => e.isOverArea(getArea) }(f)
    /**
      * Creates a new mouse wheel listener that calls specified function on wheel rotations inside a specific area
      * @param getArea A function for getting the target area
      * @param f A function that will be called on wheel rotations
      * @return A new mouse event listener
      */
    @deprecated("Please use .over(Area2D)(...) instead", "v4.0")
    def onWheelInsideAreaNoConsume(getArea: => Area2D)(f: MouseWheelEvent => Unit) =
        onWheelInsideArea(getArea) { e => f(e); None }
    
    
    // NESTED   ----------------------
    
    trait MouseWheelFilteringFactory[+A] extends MouseFilteringFactory[MouseWheelEvent, A]
    {
        // COMPUTED ------------------
        
        /**
          * @return An item that only accepts events where the wheel rotated up / away from the user
          */
        def rotatedAway = rotated(Up)
        /**
          * @return An item that only accepts events where the wheel rotated down / towards the user
          */
        def rotatedTowards = rotated(Down)
        
        /**
          * @return An item that only accepts unconsumed events
          */
        def unconsumed = withFilter { _.unconsumed }
        
        
        // OTHER    ------------------
        
        /**
          * @param rotationDirection Accepted direction of rotation
          * @return An item that only accepts mouse wheel events towards the specified direction
          */
        def rotated(rotationDirection: VerticalDirection) =
            withFilter { e => Sign.of(e.wheelTurn) == rotationDirection.sign }
    }
    
    object MouseWheelEventFilter extends MouseWheelFilteringFactory[MouseWheelEventFilter]
    {
        // IMPLEMENTED  --------------
        
        override protected def withFilter(filter: Filter[MouseWheelEvent]): MouseWheelEventFilter = filter
        
        
        // OTHER    ------------------
        
        /**
          * @param f A filter function
          * @return A filter that uses the specified function
          */
        def other(f: MouseWheelEvent => Boolean): MouseWheelEventFilter = Filter(f)
    }
    
    case class MouseWheelListenerFactory(condition: FlagLike = AlwaysTrue, filter: MouseWheelEventFilter = AcceptAll)
        extends ListenerFactory[MouseWheelEvent, MouseWheelListenerFactory]
            with MouseWheelFilteringFactory[MouseWheelListenerFactory]
    {
        // IMPLEMENTED  --------------
        
        override def usingFilter(filter: Filter[MouseWheelEvent]): MouseWheelListenerFactory = copy(filter = filter)
        override def usingCondition(condition: Changing[Boolean]): MouseWheelListenerFactory =
            copy(condition = condition)
        
        override protected def withFilter(filter: Filter[MouseWheelEvent]): MouseWheelListenerFactory =
            copy(filter = this.filter && filter)
            
        
        // OTHER    ------------------
        
        /**
          * @param f A function that processes a mouse wheel event
          * @return A listener that uses the specified function.
          *         The resulting listener will apply this factory's handling condition and event filter.
          */
        def apply(f: MouseWheelEvent => ConsumeChoice): MouseWheelListener =
            new _MouseWheelListener(condition, filter, f)
    }
    
    private class _MouseWheelListener(override val handleCondition: FlagLike,
                                      override val mouseWheelEventFilter: MouseWheelEventFilter,
                                      f: MouseWheelEvent => ConsumeChoice)
        extends MouseWheelListener
    {
        override def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice = f(event)
    }
}

/**
  * Common trait for classes which want to be notified when the mouse wheel rotates
  * @author Mikko Hilpinen
  * @since 6.2.2024, v4.0
  */
trait MouseWheelListener extends Handleable2
{
    /**
      * @return A filter applied to incoming events.
      *         Only events accepted by this filter should trigger [[onMouseWheelRotated]].
      */
    def mouseWheelEventFilter: Filter[MouseWheelEvent]
    
    /**
      * Delivers a mouse wheel event to this listener.
      * This listener should only be informed of events that are accepted by its event filter.
      * @param event A mouse wheel event
      * @return Whether this listener chose to consume the specified event
      */
    def onMouseWheelRotated(event: MouseWheelEvent): ConsumeChoice
}

