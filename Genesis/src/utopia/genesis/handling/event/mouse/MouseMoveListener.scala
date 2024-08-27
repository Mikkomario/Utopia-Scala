package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.mouse.MouseMoveEvent.MouseMoveFilteringFactory
import utopia.genesis.handling.template.Handleable
import utopia.paradigm.shape.shape2d.area.Area2D

import scala.annotation.unused
import scala.language.implicitConversions

object MouseMoveListener
{
    // ATTRIBUTES   ------------------
    
    /**
      * A factory for constructing mouse move listeners.
      * Doesn't apply any listening conditions, nor event filters.
      */
    val unconditional = MouseMoveEventListenerFactory()
    
    
    // IMPLICIT ----------------------
    
    implicit def objectToFactory(@unused o: MouseMoveListener.type): MouseMoveEventListenerFactory = unconditional
    
    
    // OTHER    ----------------------
    
    /*
    /**
      * Creates a new mouse move listener that calls the specified function
      * @param filter A filter that determines which events trigger the function (default = no filtering)
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    @deprecated("Please use .filtering(Filter).apply(...) instead", "v4.0")
    def apply(filter: Filter[MouseMoveEvent2] = AcceptAll)(f: MouseMoveEvent2 => Unit): MouseMoveListener2 =
        unconditional.usingFilter(filter)(f)
    */
    /**
      * Creates a new mouse move listener that calls specified function on drags (with left mouse button)
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    @deprecated("Please use .whileLeftDown(...) instead", "v4.0")
    def onLeftDragged(f: MouseMoveEvent => Unit) = unconditional.filtering(MouseEvent.filter.whileLeftDown)(f)
    /**
      * Creates a new mouse move listener that calls specified function on drags (with right mouse button)
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    @deprecated("Please use .whileRightDown(...) instead", "v4.0")
    def onRightDragged(f: MouseMoveEvent => Unit) =
        unconditional.filtering(MouseEvent.filter.whileRightDown)(f)
    
    /**
      * Creates a new mouse move listener that calls specified function each time mouse enters specified area
      * @param getArea a function for calculating the target area
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    @deprecated("Please use .entered(...) instead", "v4.0")
    def onEnter(getArea: => Area2D)(f: MouseMoveEvent => Unit) =
        unconditional.filtering { e => e.entered(getArea) }(f)
    /**
      * Creates a new mouse move listener that calls specified function each time mouse exits specified area
      * @param getArea a function for calculating the target area
      * @param f A function that is called on mouse events
      * @return A new mouse move listener
      */
    @deprecated("Please use .exited(...) instead", "v4.0")
    def onExit(getArea: => Area2D)(f: MouseMoveEvent => Unit) = unconditional.filtering { e => e.exited(getArea) }(f)
    
    
    // NESTED   ----------------------
    
    case class MouseMoveEventListenerFactory(condition: Flag = AlwaysTrue,
                                             filter: Filter[MouseMoveEvent] = AcceptAll)
        extends ListenerFactory[MouseMoveEvent, MouseMoveEventListenerFactory]
            with MouseMoveFilteringFactory[MouseMoveEvent, MouseMoveEventListenerFactory]
    {
        // IMPLEMENTED  -------------------
        
        override protected def withFilter(filter: Filter[MouseMoveEvent]): MouseMoveEventListenerFactory =
            copy(filter = this.filter && filter)
        
        override def usingFilter(filter: Filter[MouseMoveEvent]): MouseMoveEventListenerFactory =
            copy(filter = filter)
        override def usingCondition(condition: Flag): MouseMoveEventListenerFactory = copy(condition = condition)
            
        
        // OTHER    -----------------------
        
        /**
          * @param f A function to call on accepted mouse move events
          * @return A listener that calls the specified function for events accepted by this factory's filter,
          *         while the listening condition allows it.
          */
        def apply(f: MouseMoveEvent => Unit): MouseMoveListener = new _MouseMoveListener(condition, filter, f)
    }
    
    private class _MouseMoveListener(override val handleCondition: Flag,
                                     override val mouseMoveEventFilter: Filter[MouseMoveEvent],
                                     f: MouseMoveEvent => Unit)
        extends MouseMoveListener
    {
        override def onMouseMove(event: MouseMoveEvent): Unit = f(event)
    }
}

/**
 * MouseMoveListeners are interested in receiving mouse move events
 * @author Mikko Hilpinen
 * @since 21.1.2017
 */
trait MouseMoveListener extends Handleable
{
    /**
     * This filter is applied over mouse move events the listener would receive.
      * Only events accepted by this filter should be delivered to this listener.
     */
    def mouseMoveEventFilter: Filter[MouseMoveEvent]
    
    /**
     * This method is used for informing this listener of new mouse events.
      * This method should only be called for events that are accepted by this listener's event filter.
     * @param event The event that occurred.
     */
    def onMouseMove(event: MouseMoveEvent): Unit
}


