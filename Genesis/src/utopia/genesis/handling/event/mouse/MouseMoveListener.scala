package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.mouse.MouseEvent.MouseFilteringFactory
import utopia.genesis.handling.template.Handleable2
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.OriginType.Relative
import utopia.paradigm.enumeration.{Direction2D, OriginType}
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.shape.shape2d.area.Area2D

import scala.annotation.unused
import scala.language.implicitConversions

object MouseMoveListener
{
    // TYPES    ----------------------
    
    /**
      * A filter that processes any type of mouse move events
      */
    type MouseMoveEventFilter = Filter[MouseMoveEventLike[_]]
    
    
    // ATTRIBUTES   ------------------
    
    /**
      * A factory for constructing mouse move listeners.
      * Doesn't apply any listening conditions, nor event filters.
      */
    val unconditional = MouseMoveEventListenerFactory()
    
    
    // COMPUTED ----------------------
    
    /**
      * @return Access point for constructing mouse move event filters
      */
    def filter = MouseMoveEventFilter
    
    
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
    
    trait MouseMoveFilteringFactory[+E <: MouseMoveEventLike[_], +A] extends MouseFilteringFactory[E, A]
    {
        // COMPUTED ------------------
        
        /**
          * @return An item that only accepts mouse movements going (mostly) up
          */
        def up = direction(Up)
        /**
          * @return An item that only accepts mouse movements going (mostly) down
          */
        def down = direction(Down)
        /**
          * @return An item that only accepts mouse movements going (mostly) left
          */
        def left = direction(Direction2D.Left)
        /**
          * @return An item that only accepts mouse movements going (mostly) right
          */
        def right = direction(Direction2D.Right)
        
        
        // OTHER    ------------------
        
        /**
          * @param threshold A velocity threshold
          * @return An item that only accepts movements with greater (or equal) velocity than the one specified
          */
        def velocityOver(threshold: LinearVelocity) = withFilter { _.velocity.linear >= threshold }
        /**
          * @param center Center of the targeted range of directions / angles
          * @param maximumVariance Maximum difference (in either direction) from the central angle,
          *                        that's still accepted
          * @return An item that only accepts movements towards the specified direction
          *         (with a certain amount of variance allowed)
          */
        def directionWithin(center: Angle, maximumVariance: Rotation) = {
            val minimum = center + maximumVariance.counterclockwise
            val maximum = center + maximumVariance.clockwise
            if (minimum > maximum)
                withFilter { e =>
                    val angle = e.transition.direction
                    angle >= minimum || angle <= maximum
                }
            else
                withFilter { e =>
                    val angle = e.transition.direction
                    angle >= minimum && angle <= maximum
                }
        }
        /**
          * @param direction Targeted direction
          * @param maximumVariance How much the actual / precise direction angle may vary from the specified target
          *                        (to either direction)
          * @return An item that only accepts movement events towards the specified direction
          *         (with the specified variance allowed)
          */
        def direction(direction: Direction2D, maximumVariance: Rotation = Rotation.circles(0.2)) =
            directionWithin(Angle(direction), maximumVariance)
        
        /**
          * @param area A function that yields the targeted area
          * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
          *                 or whether it is an "absolute" area (on the screen)
          * @return An item that only accepts events where the starting location is within the specified area
          */
        def startedOver(area: => Area2D, areaType: OriginType = Relative) =
            withFilter { _.startedOver(area, areaType) }
        /**
          * @param area A function that yields the targeted area
          * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
          *                 or whether it is an "absolute" area (on the screen)
          * @return An item that only accepts events where the starting location is outside the specified area
          */
        def startedOutside(area: => Area2D, areaType: OriginType = Relative) =
            withFilter { _.startedOutside(area, areaType) }
        /**
          * @param area A function that yields the targeted area
          * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
          *                 or whether it is an "absolute" area (on the screen)
          * @return An item that only accepts events where the cursor moved within the specified area
          */
        def entered(area: => Area2D, areaType: OriginType = Relative) = withFilter { _.entered(area, areaType) }
        /**
          * @param area A function that yields the targeted area
          * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
          *                 or whether it is an "absolute" area (on the screen)
          * @return An item that only accepts events where the cursor exited the specified area
          */
        def exited(area: => Area2D, areaType: OriginType = Relative) = withFilter { _.exited(area, areaType) }
        /**
          * @param area A function that yields the targeted area
          * @param areaType Whether the specified area is relative to mouse event / coordinate context (default),
          *                 or whether it is an "absolute" area (on the screen)
          * @return An item that only accepts events where the cursor either entered or exited the specified area
          */
        def enteredOrExited(area: => Area2D, areaType: OriginType = Relative) =
            withFilter { _.enteredOrExited(area, areaType) }
    }
    
    object MouseMoveEventFilter extends MouseMoveFilteringFactory[MouseMoveEventLike[_], MouseMoveEventFilter]
    {
        // IMPLEMENTED  ---------------------
        
        override protected def withFilter(filter: Filter[MouseMoveEventLike[_]]): MouseMoveEventFilter = filter
        
        
        // OTHER    -------------------------
        
        /**
          * @param f A filtering function for mouse move events
          * @return A filter that utilizes that function
          */
        def apply(f: MouseMoveEventLike[_] => Boolean): MouseMoveEventFilter = Filter(f)
    }
    
    case class MouseMoveEventListenerFactory(condition: FlagLike = AlwaysTrue,
                                             filter: Filter[MouseMoveEvent] = AcceptAll)
        extends ListenerFactory[MouseMoveEvent, MouseMoveEventListenerFactory]
            with MouseMoveFilteringFactory[MouseMoveEvent, MouseMoveEventListenerFactory]
    {
        // IMPLEMENTED  -------------------
        
        override protected def withFilter(filter: Filter[MouseMoveEvent]): MouseMoveEventListenerFactory =
            copy(filter = this.filter && filter)
        
        override def usingFilter(filter: Filter[MouseMoveEvent]): MouseMoveEventListenerFactory =
            copy(filter = filter)
        override def usingCondition(condition: Changing[Boolean]): MouseMoveEventListenerFactory =
            copy(condition = condition)
            
        
        // OTHER    -----------------------
        
        /**
          * @param f A function to call on accepted mouse move events
          * @return A listener that calls the specified function for events accepted by this factory's filter,
          *         while the listening condition allows it.
          */
        def apply(f: MouseMoveEvent => Unit): MouseMoveListener = new _MouseMoveListener(condition, filter, f)
    }
    
    private class _MouseMoveListener(override val handleCondition: FlagLike,
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
trait MouseMoveListener extends Handleable2
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


