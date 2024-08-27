package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.consume.{ConsumeChoice, ConsumeEvent}
import utopia.genesis.handling.event.mouse.MouseWheelEvent.{MouseWheelEventFilter, MouseWheelFilteringFactory}
import utopia.genesis.handling.template.Handleable
import utopia.paradigm.shape.shape2d.area.Area2D

import scala.annotation.unused
import scala.language.implicitConversions

object MouseWheelListener
{
    // ATTRIBUTES   ------------------
    
    /**
      * A mouse event listener factory that doesn't apply any conditions or event filters
      */
    val unconditional = MouseWheelListenerFactory()
    
    
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
    
    case class MouseWheelListenerFactory(condition: Flag = AlwaysTrue, filter: MouseWheelEventFilter = AcceptAll)
        extends ListenerFactory[MouseWheelEvent, MouseWheelListenerFactory]
            with MouseWheelFilteringFactory[MouseWheelListenerFactory]
    {
        // IMPLEMENTED  --------------
        
        override def usingFilter(filter: Filter[MouseWheelEvent]): MouseWheelListenerFactory = copy(filter = filter)
        override def usingCondition(condition: Flag): MouseWheelListenerFactory = copy(condition = condition)
        
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
    
    private class _MouseWheelListener(override val handleCondition: Flag,
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
trait MouseWheelListener extends Handleable
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

